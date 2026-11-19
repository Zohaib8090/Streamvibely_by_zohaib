package app.streamvibely.playback.resolve

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.reflect.KClass

/**
 * Reflection-based compatibility adapter for InnerTubeX.
 * Invokes innertubex reflectively to bypass Kotlin metadata version differences.
 * Configures Faraday player configs from:
 * https://raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player_configs.json
 */
class InnerTubeXStreamExtractor(
    private val context: Context,
) {
    private val failedProfilesUntil = ConcurrentHashMap<String, Long>()
    private val runtime: RuntimeStack? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { createRuntime() }
            .onFailure { Log.w(TAG, "InnerTubeX runtime initialization failed", it) }
            .getOrNull()
    }

    suspend fun resolve(
        videoId: String,
        visitorData: String? = null,
        cookie: String? = null,
        maxBitrateBps: Int? = null,
    ): YouTubeAudioStream? = withContext(Dispatchers.IO) {
        val stack = runtime ?: return@withContext null
        try {
            setProperty(stack.innerTube, "VisitorData", visitorData)
            setProperty(stack.innerTube, "Cookie", cookie)
            setProperty(stack.innerTube, "UseLoginForBrowse", !cookie.isNullOrBlank())

            val hints = newInstance("com.metrolist.innertubex.extraction.ContentHints")
            val withCapabilities = hints.javaClass.methods.firstOrNull {
                it.name == "withStreamCapabilities" && it.parameterCount == 3
            } ?: return@withContext null

            // (allowHls, allowSabr, allowBoundedRange). Bounded ranges are ON (true)
            // matching ChunkedRangeDataSource (<=512 KB) for token-free clients.
            val constrainedHints = withCapabilities.invoke(hints, true, false, true)

            // AUTO ranks Opus (webm) first, then the highest bitrate.
            val quality = if (maxBitrateBps == null) "AUTO" else "LOW"
            val audioQuality = enumValue("com.metrolist.innertubex.extraction.AudioQuality", quality)
            val now = System.currentTimeMillis()
            failedProfilesUntil.entries.removeIf { it.value <= now }

            val excludedProfiles = failedProfilesUntil
                .filter { (key, expiry) -> key.startsWith("$videoId|") && expiry > now }
                .mapTo(mutableSetOf()) { it.key.substringAfterLast('|') }
                .apply { addAll(APPLE_CLIENTS) }

            val extractMethod = stack.extractor.javaClass.methods.firstOrNull {
                it.name == "extract" && it.parameterCount == 6
            } ?: return@withContext null

            val extracted = invokeSuspend(
                receiver = stack.extractor,
                method = extractMethod,
                arguments = arrayOf(
                    videoId,
                    constrainedHints,
                    excludedProfiles,
                    audioQuality,
                    UUID.randomUUID().toString().replace("-", ""),
                ),
            ) ?: return@withContext null

            extracted.toAudioStream(videoId)?.takeIf { stream ->
                maxBitrateBps == null || stream.bitrate <= maxBitrateBps
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.w(TAG, "InnerTubeX extraction failed for $videoId: ${error.message}")
            null
        }
    }

    fun reportPlaybackFailure(videoId: String, clientProfile: String?) {
        val profile = clientProfile
            ?.removePrefix("INNERTUBEX:")
            ?.takeIf(String::isNotBlank)
            ?: return
        failedProfilesUntil["$videoId|$profile"] = System.currentTimeMillis() + CLIENT_FAILURE_COOLDOWN_MS
    }

    private fun Any.toAudioStream(videoId: String): YouTubeAudioStream? {
        val url = readString(this, "getUrl") ?: return null
        val mime = readString(this, "getMimeType")?.substringBefore(';')?.trim().orEmpty()
        val isHls = mime.equals("application/x-mpegurl", true) || url.substringBefore('?').endsWith(".m3u8", true)
        if (!isHls && !mime.startsWith("audio/", true)) return null
        if (!url.startsWith("https://", true)) return null

        val rawHeaders = (readValue(this, "getHeaders") as? Map<*, *>)
            ?.mapNotNull { (name, value) ->
                val key = name as? String
                val headerValue = value as? String
                if (key.isNullOrBlank() || headerValue.isNullOrBlank()) null else key to headerValue
            }
            ?.toMap()
            .orEmpty()
        val clientName = readString(this, "getClientName").orEmpty()
        val profileId = readString(this, "getProfileId").orEmpty()

        if (APPLE_CLIENT_PREFIXES.any { clientName.startsWith(it, ignoreCase = true) }) return null

        val headers = rawHeaders.toMutableMap().apply {
            if (keys.none { it.equals("User-Agent", ignoreCase = true) }) {
                diagnosticUserAgent(readValue(this@toAudioStream, "getStreamDiagnostics"), clientName, profileId)
                    ?.let { put("User-Agent", it) }
            }
        }
        val expiresAt = extractExpiry(url) ?: instantToEpochMillis(readValue(this, "getExpiresAt"))
        val durationMs = readValue(this, "getMediaMetadata")
            ?.let { readLong(it, "getDurationSeconds") }
            ?.times(1_000L)
        val profile = profileId.ifBlank { clientName.ifBlank { "INNERTUBEX" } }

        return YouTubeAudioStream(
            videoId = videoId,
            url = url,
            itag = readInt(this, "getItag"),
            mimeType = mime.ifBlank { null },
            codec = if (isHls) "hls" else readString(this, "getCodecs"),
            bitrate = readInt(this, "getBitrate") ?: 0,
            sampleRateHz = readInt(this, "getSampleRate"),
            durationMs = durationMs,
            contentLength = readLong(this, "getContentLengthBytes"),
            isAdaptive = isHls,
            clientProfile = "INNERTUBEX:$profile",
            requestHeaders = headers,
            expiresAtEpochMs = expiresAt ?: (System.currentTimeMillis() + UNKNOWN_STREAM_EXPIRY_TTL_MS),
            isDrc = isDrcAudioUrl(url),
        )
    }

    private fun createRuntime(): RuntimeStack {
        val httpClient = createKtorHttpClient()
        val repository = playerConfigRepository()
        val configStore = constructWithDefaults(
            "com.metrolist.innertubex.cipher.RemotePlayerConfigStore",
            arrayOf(httpClient, repository),
            defaultMask = 4,
        )
        val innerTube = constructWithDefaults(
            "com.metrolist.innertubex.InnerTube",
            arrayOf(httpClient),
            defaultMask = 6,
        )
        val cipher = constructWithDefaults(
            "com.metrolist.innertubex.cipher.YouTubeCipherService",
            arrayOf(httpClient, configStore),
            defaultMask = 4,
        )
        val parser = constructWithDefaults(
            "com.metrolist.innertubex.extraction.YtConfigParserImpl",
            arrayOf(httpClient, innerTube, configStore),
            defaultMask = 24,
        )
        val extractor = constructWithDefaults(
            "com.metrolist.innertubex.extraction.InnerTubeExtractor",
            arrayOf(parser, cipher, innerTube),
            defaultMask = 120,
        )
        return RuntimeStack(innerTube, extractor, parser, cipher)
    }

    /**
     * Dynamic proxy for PlayerConfigRepository targeting Faraday player configs registry.
     */
    private fun playerConfigRepository(): Any {
        val type = Class.forName("com.metrolist.innertubex.cipher.PlayerConfigRepository")
        val prefs = context.getSharedPreferences(PLAYER_CONFIG_PREFS, Context.MODE_PRIVATE)
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            val value = args?.firstOrNull()
            when (method.name) {
                "getEnabled" -> true
                "getSourceUrl", "getDefaultSourceUrl" -> PLAYER_CONFIG_URL
                "getCachedJson" -> prefs.getString("json", "").orEmpty()
                "getCachedSourceUrl" -> prefs.getString("source_url", "").orEmpty()
                "getCachedEtag" -> prefs.getString("etag", "").orEmpty()
                "getCachedAtMs" -> prefs.getLong("cached_at_ms", 0L)
                "setCachedJson" -> prefs.edit().putString("json", value as String).apply().let { null }
                "setCachedSourceUrl" -> prefs.edit().putString("source_url", value as String).apply().let { null }
                "setCachedEtag" -> prefs.edit().putString("etag", value as String).apply().let { null }
                "setCachedAtMs" -> prefs.edit().putLong("cached_at_ms", value as Long).apply().let { null }
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === value
                "toString" -> "FaradayPlayerConfigRepository"
                else -> null
            }
        }
    }

    private fun createKtorHttpClient(): Any {
        runCatching { createOkHttpClient() }.onSuccess { return it }
        runCatching { createCioHttpClient() }.onSuccess { return it }
        val candidates = listOf("io.ktor.client.HttpClientJvmKt", "io.ktor.client.HttpClientKt")
        for (className in candidates) {
            val method = runCatching {
                Class.forName(className).methods.firstOrNull {
                    it.name == "HttpClient" && Modifier.isStatic(it.modifiers) &&
                        (it.parameterCount == 0 ||
                            (it.parameterCount == 1 && Function::class.java.isAssignableFrom(it.parameterTypes[0])))
                }
            }.getOrNull() ?: continue
            val result = if (method.parameterCount == 0) {
                method.invoke(null)
            } else {
                method.invoke(null, { _: Any? -> Unit })
            }
            if (result != null) return result
        }
        error("Ktor JVM HttpClient factory was not found")
    }

    private fun createOkHttpClient(): Any {
        val engine = Class.forName("io.ktor.client.engine.okhttp.OkHttp")
            .getField("INSTANCE")
            .get(null)
        val factory = Class.forName("io.ktor.client.HttpClientKt").methods.first {
            it.name == "HttpClient" && it.parameterCount == 2 &&
                it.parameterTypes[0].name == "io.ktor.client.engine.HttpClientEngineFactory"
        }
        return factory.invoke(null, engine, { _: Any? -> Unit })!!
    }

    private fun createCioHttpClient(): Any {
        val systemCas = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
        val trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(systemCas) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()
        val cio = Class.forName("io.ktor.client.engine.cio.CIO").getField("INSTANCE").get(null)
        val factory = Class.forName("io.ktor.client.HttpClientKt").methods.first {
            it.name == "HttpClient" && it.parameterCount == 2 &&
                it.parameterTypes[0].name == "io.ktor.client.engine.HttpClientEngineFactory"
        }
        val configureClient = { clientConfig: Any? ->
            val engine = clientConfig!!.javaClass.methods.first { it.name == "engine" && it.parameterCount == 1 }
            val configureEngine = { engineConfig: Any? ->
                val https = engineConfig!!.javaClass.getMethod("getHttps").invoke(engineConfig)
                https.javaClass.getMethod("setTrustManager", TrustManager::class.java).invoke(https, trustManager)
                Unit
            }
            engine.invoke(clientConfig, configureEngine)
            Unit
        }
        return factory.invoke(null, cio, configureClient)!!
    }

    private suspend fun invokeSuspend(
        receiver: Any,
        method: Method,
        arguments: Array<Any?>,
    ): Any? = suspendCancellableCoroutine { continuation ->
        val completion = object : Continuation<Any?> {
            override val context: CoroutineContext = continuation.context

            override fun resumeWith(result: Result<Any?>) {
                continuation.resumeWith(result)
            }
        }
        try {
            val result = method.invoke(receiver, *(arguments + completion))
            if (result !== COROUTINE_SUSPENDED && continuation.isActive) continuation.resume(result)
        } catch (error: InvocationTargetException) {
            if (continuation.isActive) continuation.resumeWith(Result.failure(error.targetException))
        } catch (error: Throwable) {
            if (continuation.isActive) continuation.resumeWith(Result.failure(error))
        }
    }

    private fun constructWithDefaults(
        className: String,
        required: Array<Any?>,
        defaultMask: Int,
    ): Any {
        val type = Class.forName(className)
        val constructor = type.declaredConstructors.firstOrNull { candidate ->
            candidate.parameterCount >= required.size + 2 &&
                candidate.parameterTypes[candidate.parameterCount - 2] == Int::class.javaPrimitiveType &&
                candidate.parameterTypes.last().name == "kotlin.jvm.internal.DefaultConstructorMarker" &&
                required.withIndex().all { (index, value) ->
                    value == null || candidate.parameterTypes[index].isAssignableFrom(value.javaClass)
                }
        } ?: error("No compatible constructor for $className")
        constructor.isAccessible = true
        val originalParameterCount = constructor.parameterCount - 2
        val arguments = arrayOfNulls<Any?>(constructor.parameterCount)
        required.copyInto(arguments)
        arguments[originalParameterCount] = defaultMask
        return constructor.newInstance(*arguments)
    }

    private fun setProperty(instance: Any, suffix: String, value: Any?) {
        instance.javaClass.methods.firstOrNull {
            it.name == "set$suffix" && it.parameterCount == 1
        }?.invoke(instance, value)
    }

    private fun newInstance(className: String): Any =
        Class.forName(className).getDeclaredConstructor().newInstance()

    private fun enumValue(className: String, name: String): Any =
        (Class.forName(className).enumConstants as Array<*>).first { (it as Enum<*>).name == name }!!

    private fun readValue(instance: Any, getter: String): Any? =
        instance.javaClass.methods.firstOrNull { it.name == getter && it.parameterCount == 0 }?.invoke(instance)

    private fun readString(instance: Any, getter: String): String? = readValue(instance, getter) as? String

    private fun readInt(instance: Any, getter: String): Int? = (readValue(instance, getter) as? Number)?.toInt()

    private fun readLong(instance: Any, getter: String): Long? = (readValue(instance, getter) as? Number)?.toLong()

    private fun diagnosticUserAgent(diagnostics: Any?, clientName: String, profileId: String): String? {
        val attempts = readValue(diagnostics ?: return null, "getAttempts") as? Iterable<*> ?: return null
        val match = attempts.firstOrNull { attempt ->
            attempt != null &&
                (readString(attempt, "getClientName") == clientName || clientName.isBlank()) &&
                (readString(attempt, "getProfileId") == profileId || profileId.isBlank())
        } ?: return null
        return readString(match, "getUserAgent")?.takeIf(String::isNotBlank)
    }

    private fun extractExpiry(url: String): Long? =
        Uri.parse(url).getQueryParameter("expire")?.toLongOrNull()?.times(1_000L)

    private fun instantToEpochMillis(value: Any?): Long? {
        value ?: return null
        val method = value.javaClass.methods.firstOrNull {
            it.name == "toEpochMilliseconds" && it.parameterCount == 0
        } ?: return null
        return (runCatching { method.invoke(value) }.getOrNull() as? Number)?.toLong()
    }

    private fun isDrcAudioUrl(url: String): Boolean {
        val xtags = url.substringAfter("xtags=", "").substringBefore('&')
        if (xtags.isEmpty()) return false
        return runCatching { java.net.URLDecoder.decode(xtags, "UTF-8") }.getOrDefault(xtags).contains("drc=1")
    }

    private data class RuntimeStack(
        val innerTube: Any,
        val extractor: Any,
        val parser: Any,
        val cipher: Any,
    )

    companion object {
        private const val TAG = "InnerTubeXStreamExtr"
        private const val UNKNOWN_STREAM_EXPIRY_TTL_MS = 5 * 60 * 1_000L
        private const val CLIENT_FAILURE_COOLDOWN_MS = 5 * 60_000L
        private val APPLE_CLIENTS = setOf(
            "VISIONOS", "VISIONOS_0_1", "VISIONOS_SABR",
            "IOS", "IOS_SABR", "IOS_MUSIC", "IPADOS",
        )
        private val APPLE_CLIENT_PREFIXES = listOf("IOS", "IPADOS", "VISIONOS")
        private const val PLAYER_CONFIG_PREFS = "innertubex_player_config"
        const val PLAYER_CONFIG_URL =
            "https://raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player_configs.json"
    }
}

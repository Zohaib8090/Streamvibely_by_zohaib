package app.streamvibely.spotify

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores one secret string (the Spotify session cookie) encrypted with an Android Keystore AES-GCM key.
 * The key never leaves the device's secure hardware, so a copied preferences file cannot be decrypted elsewhere.
 * A plain value left by an older version is encrypted once and the plain copy is removed.
 */
internal class SecureSession(
    private val prefs: SharedPreferences,
    private val name: String,
    legacyPlainKey: String,
) {
    init {
        val legacy = prefs.getString(legacyPlainKey, null)
        if (!legacy.isNullOrBlank()) {
            if (!prefs.contains(encryptedKey)) put(legacy)
            prefs.edit().remove(legacyPlainKey).apply()
        }
    }

    private val encryptedKey get() = "${name}_enc"

    fun get(): String? = try {
        val raw = Base64.decode(prefs.getString(encryptedKey, null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, IV_BYTES)) }
        String(cipher.doFinal(raw, IV_BYTES, raw.size - IV_BYTES))
    } catch (e: Exception) {
        null // key lost or data damaged: treated as signed out, the user reconnects
    }

    fun put(value: String) {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val blob = cipher.iv + cipher.doFinal(value.toByteArray())
        prefs.edit().putString(encryptedKey, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    fun clear() {
        prefs.edit().remove(encryptedKey).apply()
    }

    fun isPresent(): Boolean = prefs.contains(encryptedKey)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "streamvibely_spotify_session"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}

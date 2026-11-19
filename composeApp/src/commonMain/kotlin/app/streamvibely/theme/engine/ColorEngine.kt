package app.streamvibely.theme.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import app.streamvibely.domain.PaletteVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates Material 3 style color roles from a seed color, using OKLCH tonal palettes and CIE L*
 * tones, so a tone difference of 50 gives about 4.5:1 contrast and 40 about 3:1.
 */
object ColorEngine {
    /** Standard, high-contrast and low-contrast tone for one role. */
    private class T(val std: Int, val hi: Int, val lo: Int)

    private class Plan(
        val pHue: Double, val pC: Double,
        val sHue: Double, val sC: Double,
        val tHue: Double, val tC: Double,
        val nC: Double, val nvC: Double,
    )

    private fun planFor(variant: PaletteVariant, hue: Double, seedC: Double): Plan {
        val colorful = seedC >= 0.02
        if (!colorful) return Plan(hue, 0.0, hue, 0.0, hue, 0.0, 0.0, 0.0)
        return when (variant) {
            PaletteVariant.TonalSpot ->
                Plan(hue, seedC.coerceIn(0.07, 0.13), hue, 0.04, hue + 60, 0.07, 0.012, 0.02)
            PaletteVariant.Vibrant ->
                Plan(hue, seedC.coerceIn(0.13, 0.20), hue + 30, 0.10, hue + 60, 0.12, 0.012, 0.02)
            PaletteVariant.Expressive ->
                Plan(hue + 15, 0.11, hue, 0.06, hue + 90, 0.11, 0.012, 0.02)
            PaletteVariant.Neutral ->
                Plan(hue, 0.025, hue, 0.02, hue, 0.02, 0.01, 0.015)
            PaletteVariant.Monochrome ->
                Plan(hue, 0.0, hue, 0.0, hue, 0.0, 0.0, 0.0)
        }
    }

    // Light tones.
    private val lMain = T(40, 30, 48)
    private val lOn = T(100, 100, 100)
    private val lContainer = T(90, 95, 85)
    private val lOnContainer = T(10, 5, 20)
    private val lOnSurface = T(10, 5, 20)
    private val lOnSurfaceVariant = T(30, 20, 40)
    private val lOutline = T(50, 35, 55)
    private val lOutlineVariant = T(80, 60, 85)

    // Dark tones.
    private val dMain = T(80, 90, 72)
    private val dOn = T(20, 10, 20)
    private val dContainer = T(30, 22, 36)
    private val dOnContainer = T(90, 95, 85)
    private val dOnSurface = T(90, 97, 82)
    private val dOnSurfaceVariant = T(80, 90, 72)
    private val dOutline = T(60, 75, 55)
    private val dOutlineVariant = T(30, 50, 26)

    private fun at(t: T, c: Double): Int {
        val target = if (c >= 0) t.hi else t.lo
        val a = if (c >= 0) c else -c
        return (t.std + (target - t.std) * a).roundToInt().coerceIn(0, 100)
    }

    /**
     * Builds the role set for [seedArgb].
     *
     * @param contrast -1 (less separation, never below 3:1 for primary/onPrimary) .. 1 (more).
     * @param oled when true and [dark], background and surface become pure black.
     */
    fun generate(
        seedArgb: Int,
        dark: Boolean,
        variant: PaletteVariant,
        contrast: Float,
        oled: Boolean = false,
    ): GeneratedColors {
        val lch = Oklab.fromColor(Color(seedArgb))
        val plan = planFor(variant, lch.h, lch.c)
        val c = if (contrast.isFinite()) contrast.coerceIn(-1f, 1f).toDouble() else 0.0

        val primary = TonalPalette(plan.pHue, plan.pC)
        val secondary = TonalPalette(plan.sHue, plan.sC)
        val tertiary = TonalPalette(plan.tHue, plan.tC)
        val error = TonalPalette(25.0, 0.2)
        val neutral = TonalPalette(lch.h, plan.nC)
        val neutralVariant = TonalPalette(lch.h, plan.nvC)

        val main = if (dark) dMain else lMain
        val on = if (dark) dOn else lOn
        val container = if (dark) dContainer else lContainer
        val onContainer = if (dark) dOnContainer else lOnContainer
        val onSurface = if (dark) dOnSurface else lOnSurface
        val onSurfaceVariant = if (dark) dOnSurfaceVariant else lOnSurfaceVariant
        val outline = if (dark) dOutline else lOutline
        val outlineVariant = if (dark) dOutlineVariant else lOutlineVariant

        fun n(tone: Int) = neutral.tone(tone)

        val black = Color(0, 0, 0)
        val useOled = oled && dark
        val surfaceColor = if (useOled) black else n(if (dark) 6 else 98)

        return GeneratedColors(
            primary = primary.tone(at(main, c)), onPrimary = primary.tone(at(on, c)),
            primaryContainer = primary.tone(at(container, c)), onPrimaryContainer = primary.tone(at(onContainer, c)),
            secondary = secondary.tone(at(main, c)), onSecondary = secondary.tone(at(on, c)),
            secondaryContainer = secondary.tone(at(container, c)), onSecondaryContainer = secondary.tone(at(onContainer, c)),
            tertiary = tertiary.tone(at(main, c)), onTertiary = tertiary.tone(at(on, c)),
            tertiaryContainer = tertiary.tone(at(container, c)), onTertiaryContainer = tertiary.tone(at(onContainer, c)),
            error = error.tone(at(main, c)), onError = error.tone(at(on, c)),
            errorContainer = error.tone(at(container, c)), onErrorContainer = error.tone(at(onContainer, c)),
            background = surfaceColor, onBackground = n(at(onSurface, c)),
            surface = surfaceColor, onSurface = n(at(onSurface, c)),
            surfaceVariant = neutralVariant.tone(if (dark) 30 else 90),
            onSurfaceVariant = neutralVariant.tone(at(onSurfaceVariant, c)),
            surfaceDim = if (useOled) black else n(if (dark) 6 else 87),
            surfaceBright = n(if (dark) 24 else 98),
            surfaceContainerLowest = if (useOled) black else n(if (dark) 4 else 100),
            surfaceContainerLow = n(if (useOled) 2 else if (dark) 10 else 96),
            surfaceContainer = n(if (useOled) 5 else if (dark) 12 else 94),
            surfaceContainerHigh = n(if (useOled) 8 else if (dark) 17 else 92),
            surfaceContainerHighest = n(if (useOled) 12 else if (dark) 22 else 90),
            outline = neutralVariant.tone(at(outline, c)),
            outlineVariant = neutralVariant.tone(at(outlineVariant, c)),
            inverseSurface = n(if (dark) 90 else 20),
            inverseOnSurface = n(if (dark) 20 else 95),
            inversePrimary = primary.tone(if (dark) 40 else 80),
            scrim = black,
        )
    }

    private fun linearLuminance(color: Color): Double = Oklab.luminanceOfLinear(
        Oklab.srgbToLinear(color.red.toDouble()),
        Oklab.srgbToLinear(color.green.toDouble()),
        Oklab.srgbToLinear(color.blue.toDouble()),
    )

    /** WCAG 2.x contrast ratio, 1..21, symmetric in its arguments. */
    fun contrastRatio(a: Color, b: Color): Double {
        val la = linearLuminance(a)
        val lb = linearLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private val nearWhite = Color(0xFFFAFAFA)
    private val nearBlack = Color(0xFF0B0B0F)

    /**
     * Near-white or near-black text for [background]: the one with more contrast, falling back to
     * pure white or black if neither reaches [minContrast].
     */
    fun onColorFor(background: Color, minContrast: Double = 4.5): Color {
        val white = contrastRatio(background, nearWhite)
        val blackC = contrastRatio(background, nearBlack)
        val best = if (white >= blackC) nearWhite else nearBlack
        if (max(white, blackC) >= minContrast) return best
        val pureWhite = Color(255, 255, 255)
        val pureBlack = Color(0, 0, 0)
        return if (contrastRatio(background, pureWhite) >= contrastRatio(background, pureBlack)) pureWhite else pureBlack
    }

    /** Linear interpolation in sRGB: [t] 0 gives [a], 1 gives [b]. */
    fun blend(a: Color, b: Color, t: Float): Color {
        val f = if (t.isFinite()) t.coerceIn(0f, 1f) else 0f
        return Color(
            red = a.red + (b.red - a.red) * f,
            green = a.green + (b.green - a.green) * f,
            blue = a.blue + (b.blue - a.blue) * f,
            alpha = a.alpha + (b.alpha - a.alpha) * f,
        )
    }

    private const val MAX_SAMPLES = 4000
    private const val BINS = 36

    /**
     * Dominant color of an ARGB pixel buffer, as an opaque ARGB int, or null if there is no opaque
     * pixel. Samples about 4000 pixels on an even grid, weights by chroma and drops near-black and
     * near-white pixels, finds the heaviest 10 degree hue bin and returns its mean hue and chroma
     * (chroma at least 0.08) at lightness 0.65. A picture without color returns its mean gray.
     */
    fun seedFromPixels(argb: IntArray, width: Int, height: Int): Int? {
        if (width <= 0 || height <= 0) return null
        val rows = min(height, argb.size / width)
        if (rows <= 0) return null
        val stride = max(1, ceil(sqrt(width.toDouble() * rows / MAX_SAMPLES)).toInt())

        val binW = DoubleArray(BINS)
        val binA = DoubleArray(BINS)
        val binB = DoubleArray(BINS)
        val binC = DoubleArray(BINS)
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0

        var y = 0
        while (y < rows) {
            var x = 0
            while (x < width) {
                val p = argb[y * width + x]
                x += stride
                if ((p ushr 24) < 128) continue
                val r = ((p shr 16) and 0xFF) / 255.0
                val g = ((p shr 8) and 0xFF) / 255.0
                val b = (p and 0xFF) / 255.0
                sumR += r; sumG += g; sumB += b; count++
                val lch = Oklab.fromSrgb(r, g, b)
                if (lch.c < 0.02) continue
                val penalty = when {
                    lch.l < 0.15 -> lch.l / 0.15
                    lch.l > 0.95 -> (1.0 - lch.l) / 0.05
                    else -> 1.0
                }
                val w = (lch.c + 0.03) * penalty
                if (w <= 0.0) continue
                val bin = (lch.h / 10.0).toInt().coerceIn(0, BINS - 1)
                val rad = lch.h * PI / 180.0
                binW[bin] += w
                binA[bin] += w * lch.c * cos(rad)
                binB[bin] += w * lch.c * sin(rad)
                binC[bin] += w * lch.c
            }
            y += stride
        }
        if (count == 0) return null

        var best = -1
        var bestScore = 0.0
        for (i in 0 until BINS) {
            if (binW[i] <= 0.0) continue
            val score = binW[i] + 0.5 * (binW[(i + BINS - 1) % BINS] + binW[(i + 1) % BINS])
            if (score > bestScore) { bestScore = score; best = i }
        }
        if (best < 0) {
            return packRgb(sumR / count, sumG / count, sumB / count)
        }
        val chroma = max(binC[best] / binW[best], 0.08)
        var hue = atan2(binB[best], binA[best]) * 180.0 / PI
        if (hue < 0) hue += 360.0
        return Oklab.toColor(0.65, chroma, hue).toArgb()
    }

    private fun packRgb(r: Double, g: Double, b: Double): Int {
        fun ch(v: Double) = (v * 255).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    private const val SEED_GRID = 64

    /**
     * Dominant color of [bitmap]. Reads at most a 64x64 point-sampled grid (one `readPixels` call
     * per sampled row, so large covers are never fully decoded into an array) on
     * `Dispatchers.Default`. Returns null for an empty bitmap or when pixels cannot be read.
     */
    suspend fun seedFromBitmap(bitmap: ImageBitmap): Int? = withContext(Dispatchers.Default) {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return@withContext null
        val cols = min(w, SEED_GRID)
        val rows = min(h, SEED_GRID)
        val out = IntArray(cols * rows)
        try {
            val row = IntArray(w)
            for (j in 0 until rows) {
                val sy = (((j + 0.5) * h) / rows).toInt().coerceIn(0, h - 1)
                bitmap.readPixels(row, 0, sy, w, 1)
                for (i in 0 until cols) {
                    val sx = (((i + 0.5) * w) / cols).toInt().coerceIn(0, w - 1)
                    out[j * cols + i] = row[sx]
                }
            }
        } catch (e: Exception) {
            return@withContext null
        }
        seedFromPixels(out, cols, rows)
    }
}

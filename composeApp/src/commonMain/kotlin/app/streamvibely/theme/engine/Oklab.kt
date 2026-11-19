package app.streamvibely.theme.engine

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A color in OKLCH: lightness 0..1, chroma (about 0..0.4) and hue in degrees 0..360. */
data class Oklch(val l: Double, val c: Double, val h: Double)

/**
 * sRGB, linear sRGB, OKLab and OKLCH conversions (matrices by Bjorn Ottosson) and gamut mapping
 * that reduces chroma, never lightness. Colors are assumed to be in the sRGB color space.
 */
object Oklab {
    private const val GAMUT_EPS = 1e-6

    fun srgbToLinear(v: Double): Double =
        if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    fun linearToSrgb(v: Double): Double =
        if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1.0 / 2.4) - 0.055

    private fun cbrt(x: Double): Double = if (x < 0) -(-x).pow(1.0 / 3.0) else x.pow(1.0 / 3.0)

    /** Linear sRGB (0..1) to OKLab, returned as [L, a, b]. */
    fun linearToOklab(r: Double, g: Double, b: Double): DoubleArray {
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    /** OKLab to linear sRGB, returned as [r, g, b] and NOT clamped. */
    fun oklabToLinear(l: Double, a: Double, b: Double): DoubleArray {
        val l_ = l + 0.3963377774 * a + 0.2158037573 * b
        val m_ = l - 0.1055613458 * a - 0.0638541728 * b
        val s_ = l - 0.0894841775 * a - 1.2914855480 * b
        val lc = l_ * l_ * l_
        val mc = m_ * m_ * m_
        val sc = s_ * s_ * s_
        return doubleArrayOf(
            4.0767416621 * lc - 3.3077115913 * mc + 0.2309699292 * sc,
            -1.2684380046 * lc + 2.6097574011 * mc - 0.3413193965 * sc,
            -0.0041960863 * lc - 0.7034186147 * mc + 1.7076147010 * sc,
        )
    }

    fun fromSrgb(r: Double, g: Double, b: Double): Oklch {
        val lab = linearToOklab(srgbToLinear(r), srgbToLinear(g), srgbToLinear(b))
        return labToLch(lab[0], lab[1], lab[2])
    }

    fun fromColor(color: Color): Oklch =
        fromSrgb(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())

    fun labToLch(l: Double, a: Double, b: Double): Oklch {
        val c = sqrt(a * a + b * b)
        var h = atan2(b, a) * 180.0 / PI
        if (h < 0) h += 360.0
        return Oklch(l, c, h)
    }

    fun normalizeHue(h: Double): Double {
        if (!h.isFinite()) return 0.0
        val r = h % 360.0
        return if (r < 0) r + 360.0 else r
    }

    private fun inGamut(rgb: DoubleArray): Boolean =
        rgb.all { it >= -GAMUT_EPS && it <= 1.0 + GAMUT_EPS }

    private fun lchToLinear(l: Double, c: Double, hDeg: Double): DoubleArray {
        val h = hDeg * PI / 180.0
        return oklabToLinear(l, c * cos(h), c * sin(h))
    }

    /**
     * OKLCH to linear sRGB inside the gamut: if the color does not fit, chroma is reduced (lightness
     * and hue are kept) by bisection. Result channels are clamped to 0..1.
     */
    fun toLinearInGamut(l: Double, c: Double, hDeg: Double): DoubleArray {
        val lc = if (l.isFinite()) l.coerceIn(0.0, 1.0) else 0.0
        val cc = if (c.isFinite()) c.coerceAtLeast(0.0) else 0.0
        val hue = normalizeHue(hDeg)
        var rgb = lchToLinear(lc, cc, hue)
        if (!inGamut(rgb)) {
            var lo = 0.0
            var hi = cc
            repeat(24) {
                val mid = (lo + hi) / 2
                if (inGamut(lchToLinear(lc, mid, hue))) lo = mid else hi = mid
            }
            rgb = lchToLinear(lc, lo, hue)
        }
        return DoubleArray(3) { rgb[it].coerceIn(0.0, 1.0) }
    }

    /** OKLCH to an opaque sRGB [Color], chroma gamut-mapped, quantized to 8 bits per channel. */
    fun toColor(l: Double, c: Double, hDeg: Double): Color {
        val lin = toLinearInGamut(l, c, hDeg)
        return Color(
            (linearToSrgb(lin[0]) * 255).roundToInt().coerceIn(0, 255),
            (linearToSrgb(lin[1]) * 255).roundToInt().coerceIn(0, 255),
            (linearToSrgb(lin[2]) * 255).roundToInt().coerceIn(0, 255),
        )
    }

    /** WCAG relative luminance (0..1) of linear sRGB channels. */
    fun luminanceOfLinear(r: Double, g: Double, b: Double): Double =
        0.2126 * r + 0.7152 * g + 0.0722 * b
}

private const val LSTAR_EPS = 216.0 / 24389.0
private const val LSTAR_KAPPA = 24389.0 / 27.0

private fun toneFromY(y: Double): Double {
    val f = if (y > LSTAR_EPS) y.pow(1.0 / 3.0) else (LSTAR_KAPPA * y + 16.0) / 116.0
    return (116.0 * f - 16.0).coerceIn(0.0, 100.0)
}

private fun yFromTone(tone: Double): Double {
    val ft = (tone + 16.0) / 116.0
    val ft3 = ft * ft * ft
    return if (ft3 > LSTAR_EPS) ft3 else (116.0 * ft - 16.0) / LSTAR_KAPPA
}

/** CIE L* tone, 0 (black) to 100 (white), from the relative luminance of [color]. */
fun toneOf(color: Color): Double {
    val y = Oklab.luminanceOfLinear(
        Oklab.srgbToLinear(color.red.toDouble()),
        Oklab.srgbToLinear(color.green.toDouble()),
        Oklab.srgbToLinear(color.blue.toDouble()),
    )
    return toneFromY(y)
}

/**
 * The sRGB color with the given OKLCH hue whose L* tone equals [tone], using at most [chroma]
 * (reduced to fit the gamut). Found by bisection on OKLab lightness. Tone 0 is black, 100 white.
 */
fun colorForTone(hueDeg: Double, chroma: Double, tone: Double): Color {
    val t = if (tone.isFinite()) tone.coerceIn(0.0, 100.0) else 0.0
    if (t <= 0.0) return Color(0, 0, 0)
    if (t >= 100.0) return Color(255, 255, 255)
    val target = yFromTone(t)
    var lo = 0.0
    var hi = 1.0
    repeat(40) {
        val mid = (lo + hi) / 2
        val lin = Oklab.toLinearInGamut(mid, chroma, hueDeg)
        if (Oklab.luminanceOfLinear(lin[0], lin[1], lin[2]) < target) lo = mid else hi = mid
    }
    return Oklab.toColor((lo + hi) / 2, chroma, hueDeg)
}

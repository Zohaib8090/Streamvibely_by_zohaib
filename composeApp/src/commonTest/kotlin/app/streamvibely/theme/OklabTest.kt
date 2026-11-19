package app.streamvibely.theme

import androidx.compose.ui.graphics.Color
import app.streamvibely.theme.engine.ColorEngine
import app.streamvibely.theme.engine.Oklab
import app.streamvibely.theme.engine.colorForTone
import app.streamvibely.theme.engine.toneOf
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OklabTest {
    @Test
    fun srgbOklchRoundTripWithinOneCode() {
        val rnd = Random(1)
        repeat(50) {
            val r = rnd.nextInt(256)
            val g = rnd.nextInt(256)
            val b = rnd.nextInt(256)
            val src = Color(r, g, b)
            val lch = Oklab.fromColor(src)
            val back = Oklab.toColor(lch.l, lch.c, lch.h)
            val tol = 1.0 / 255 + 1e-3
            assertTrue(abs(back.red - src.red) <= tol, "red $r,$g,$b")
            assertTrue(abs(back.green - src.green) <= tol, "green $r,$g,$b")
            assertTrue(abs(back.blue - src.blue) <= tol, "blue $r,$g,$b")
        }
    }

    @Test
    fun toneOfExtremes() {
        assertEquals(0.0, toneOf(Color(0, 0, 0)), 0.01)
        assertEquals(100.0, toneOf(Color(255, 255, 255)), 0.01)
    }

    @Test
    fun colorForToneHitsRequestedTone() {
        for (hue in listOf(0.0, 25.0, 90.0, 140.0, 200.0, 265.0, 330.0)) {
            for (chroma in listOf(0.0, 0.04, 0.1, 0.2, 0.4)) {
                for (tone in listOf(1, 4, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 98, 99)) {
                    val c = colorForTone(hue, chroma, tone.toDouble())
                    val got = toneOf(c)
                    assertTrue(abs(got - tone) <= 1.0, "hue $hue chroma $chroma tone $tone got $got")
                    assertTrue(c.red.isFinite() && c.green.isFinite() && c.blue.isFinite())
                }
            }
        }
    }

    @Test
    fun toneDifferenceGivesContrast() {
        for (hue in listOf(0.0, 60.0, 120.0, 180.0, 240.0, 300.0)) {
            for (low in listOf(0, 10, 20, 30, 40, 50)) {
                val a = colorForTone(hue, 0.15, low.toDouble())
                val b40 = colorForTone(hue, 0.15, (low + 40).toDouble())
                val b50 = colorForTone(hue, 0.15, (low + 50).toDouble())
                assertTrue(ColorEngine.contrastRatio(a, b40) >= 3.0, "40 apart: hue $hue low $low")
                if (low >= 10) {
                    assertTrue(ColorEngine.contrastRatio(a, b50) >= 4.4, "50 apart: hue $hue low $low")
                }
            }
        }
    }

    @Test
    fun gamutMappingKeepsHueAndStaysInRange() {
        val c = Oklab.toColor(0.6, 0.5, 150.0)
        assertTrue(c.red in 0f..1f && c.green in 0f..1f && c.blue in 0f..1f)
        val hue = Oklab.fromColor(c).h
        assertTrue(abs(hue - 150.0) < 6.0, "hue $hue")
    }
}

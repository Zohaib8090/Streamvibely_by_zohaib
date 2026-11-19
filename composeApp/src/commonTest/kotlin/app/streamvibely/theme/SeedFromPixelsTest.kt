package app.streamvibely.theme

import androidx.compose.ui.graphics.Color
import app.streamvibely.theme.engine.ColorEngine
import app.streamvibely.theme.engine.Oklab
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeedFromPixelsTest {
    private fun hueOf(argb: Int) = Oklab.fromColor(Color(argb)).h

    @Test
    fun grayImageWithBluePatchGivesBlue() {
        val w = 64
        val h = 64
        val px = IntArray(w * h) { 0xFF808080.toInt() }
        for (y in 20 until 36) for (x in 20 until 36) px[y * w + x] = 0xFF2040E0.toInt()
        val seed = assertNotNull(ColorEngine.seedFromPixels(px, w, h))
        val hue = hueOf(seed)
        assertTrue(hue in 230.0..280.0, "hue $hue")
    }

    @Test
    fun redImageGivesRed() {
        val px = IntArray(100 * 80) { 0xFFE53935.toInt() }
        val seed = assertNotNull(ColorEngine.seedFromPixels(px, 100, 80))
        val hue = hueOf(seed)
        assertTrue(hue < 50.0 || hue > 340.0, "hue $hue")
    }

    @Test
    fun grayImageGivesGray() {
        val px = IntArray(50 * 50) { 0xFF808080.toInt() }
        val seed = assertNotNull(ColorEngine.seedFromPixels(px, 50, 50))
        val c = Color(seed)
        assertTrue(abs(c.red - c.green) < 0.02f && abs(c.green - c.blue) < 0.02f)
        assertTrue(abs(c.red - 128 / 255f) < 0.02f)
    }

    @Test
    fun emptyGivesNull() {
        assertNull(ColorEngine.seedFromPixels(IntArray(0), 0, 0))
        assertNull(ColorEngine.seedFromPixels(IntArray(0), 10, 10))
    }

    @Test
    fun transparentOnlyGivesNull() {
        assertNull(ColorEngine.seedFromPixels(IntArray(16) { 0x00FF0000 }, 4, 4))
    }

    @Test
    fun largeImageIsSampledAndFinite() {
        val w = 1000
        val h = 1000
        val px = IntArray(w * h) { 0xFF101010.toInt() }
        for (i in px.indices step 3) px[i] = 0xFF22AA44.toInt()
        val seed = assertNotNull(ColorEngine.seedFromPixels(px, w, h))
        val hue = hueOf(seed)
        assertTrue(hue in 120.0..170.0, "hue $hue")
    }
}

package app.streamvibely.theme

import androidx.compose.ui.graphics.Color
import app.streamvibely.domain.PaletteVariant
import app.streamvibely.theme.engine.ColorEngine
import app.streamvibely.theme.engine.GeneratedColors
import app.streamvibely.theme.engine.Oklab
import app.streamvibely.theme.engine.ThemePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorEngineTest {
    private val seeds = ThemePresets.all.map { it.seedColor }
    private val contrasts = listOf(-1f, 0f, 1f)

    private fun all(g: GeneratedColors): List<Color> = listOf(
        g.primary, g.onPrimary, g.primaryContainer, g.onPrimaryContainer,
        g.secondary, g.onSecondary, g.secondaryContainer, g.onSecondaryContainer,
        g.tertiary, g.onTertiary, g.tertiaryContainer, g.onTertiaryContainer,
        g.error, g.onError, g.errorContainer, g.onErrorContainer,
        g.background, g.onBackground, g.surface, g.onSurface, g.surfaceVariant, g.onSurfaceVariant,
        g.surfaceDim, g.surfaceBright, g.surfaceContainerLowest, g.surfaceContainerLow,
        g.surfaceContainer, g.surfaceContainerHigh, g.surfaceContainerHighest,
        g.outline, g.outlineVariant, g.inverseSurface, g.inverseOnSurface, g.inversePrimary, g.scrim,
    )

    private fun forEachScheme(block: (String, GeneratedColors, Float) -> Unit) {
        for (seed in seeds) for (dark in listOf(false, true)) for (v in PaletteVariant.values()) for (c in contrasts) {
            block("seed=${seed.toUInt().toString(16)} dark=$dark $v contrast=$c", ColorEngine.generate(seed, dark, v, c), c)
        }
    }

    private fun check(label: String, fg: Color, bg: Color, min: Double) {
        val ratio = ColorEngine.contrastRatio(fg, bg)
        assertTrue(ratio >= min, "$label: $ratio < $min")
    }

    @Test
    fun noNaNAnywhere() {
        forEachScheme { label, g, _ ->
            for (c in all(g)) {
                assertTrue(
                    c.red.isFinite() && c.green.isFinite() && c.blue.isFinite() && c.alpha.isFinite(),
                    label,
                )
            }
        }
    }

    @Test
    fun primaryPairContrast() {
        forEachScheme { label, g, c ->
            check("onPrimary $label", g.onPrimary, g.primary, if (c < 0f) 3.0 else 4.5)
        }
    }

    @Test
    fun standardContrastPairs() {
        forEachScheme { label, g, c ->
            if (c == 0f) {
                check("onSurface $label", g.onSurface, g.surface, 7.0)
                check("onPrimaryContainer $label", g.onPrimaryContainer, g.primaryContainer, 4.5)
                check("onSecondaryContainer $label", g.onSecondaryContainer, g.secondaryContainer, 4.5)
                check("onTertiaryContainer $label", g.onTertiaryContainer, g.tertiaryContainer, 4.5)
                check("onError $label", g.onError, g.error, 4.5)
                check("onErrorContainer $label", g.onErrorContainer, g.errorContainer, 4.5)
                check("onSecondary $label", g.onSecondary, g.secondary, 4.5)
                check("onTertiary $label", g.onTertiary, g.tertiary, 4.5)
            }
        }
    }

    @Test
    fun highContrastIsNotWorseThanStandardForPrimary() {
        for (seed in seeds) for (dark in listOf(false, true)) {
            val std = ColorEngine.generate(seed, dark, PaletteVariant.TonalSpot, 0f)
            val high = ColorEngine.generate(seed, dark, PaletteVariant.TonalSpot, 1f)
            assertTrue(
                ColorEngine.contrastRatio(high.onSurface, high.surface) >=
                    ColorEngine.contrastRatio(std.onSurface, std.surface) - 0.01,
            )
        }
    }

    @Test
    fun oledIsPureBlack() {
        for (seed in seeds) for (v in PaletteVariant.values()) for (c in contrasts) {
            val g = ColorEngine.generate(seed, dark = true, variant = v, contrast = c, oled = true)
            assertEquals(Color(0xFF000000), g.background)
            assertEquals(Color(0xFF000000), g.surface)
            assertEquals(Color(0xFF000000), g.surfaceContainerLowest)
        }
    }

    @Test
    fun oledIgnoredInLightMode() {
        val g = ColorEngine.generate(seeds.first(), dark = false, variant = PaletteVariant.TonalSpot, contrast = 0f, oled = true)
        assertTrue(g.background != Color(0xFF000000))
    }

    @Test
    fun monochromeHasNoPrimaryChroma() {
        val g = ColorEngine.generate(seeds.first(), true, PaletteVariant.Monochrome, 0f)
        assertTrue(Oklab.fromColor(g.primary).c < 0.01)
    }

    @Test
    fun primaryKeepsSeedHue() {
        val seed = 0xFF8AA4FF.toInt()
        val g = ColorEngine.generate(seed, false, PaletteVariant.TonalSpot, 0f)
        val seedHue = Oklab.fromColor(Color(seed)).h
        val hue = Oklab.fromColor(g.primary).h
        assertTrue(kotlin.math.abs(hue - seedHue) < 10.0, "hue $hue vs $seedHue")
    }

    @Test
    fun onColorForMeetsMinimum() {
        for (seed in seeds) {
            val bg = Color(seed)
            assertTrue(ColorEngine.contrastRatio(ColorEngine.onColorFor(bg), bg) >= 4.5)
        }
        assertTrue(ColorEngine.onColorFor(Color.White).red < 0.2f)
        assertTrue(ColorEngine.onColorFor(Color.Black).red > 0.8f)
    }

    @Test
    fun contrastRatioBounds() {
        assertEquals(21.0, ColorEngine.contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, ColorEngine.contrastRatio(Color.Red, Color.Red), 1e-9)
    }

    @Test
    fun blendEndpointsAndMiddle() {
        assertEquals(Color.Black, ColorEngine.blend(Color.Black, Color.White, 0f))
        assertEquals(Color.White, ColorEngine.blend(Color.Black, Color.White, 1f))
        assertEquals(0.5f, ColorEngine.blend(Color.Black, Color.White, 0.5f).red, 0.01f)
    }

    @Test
    fun presetsAreWellFormed() {
        assertTrue(ThemePresets.all.size >= 12)
        assertEquals(ThemePresets.all.map { it.id }.toSet().size, ThemePresets.all.size)
        assertEquals(ThemePresets.all.first(), ThemePresets.default)
        assertNotNull(ThemePresets.all.firstOrNull { it.seedColor == 0xFFC1F479.toInt() })
    }

    @Test
    fun emptySeedInputIsNull() {
        assertNull(ColorEngine.seedFromPixels(IntArray(0), 0, 0))
    }
}

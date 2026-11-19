package app.streamvibely.theme.engine

import app.streamvibely.domain.PaletteVariant
import app.streamvibely.domain.ThemeStyle

/** The built-in starting points. The first one matches the `ThemeSpec` defaults. */
object ThemePresets {
    val all: List<ThemePreset> = listOf(
        ThemePreset("ocean", "Ocean", 0xFF8AA4FF.toInt()),
        ThemePreset("violet", "Violet", 0xFF8B5CF6.toInt()),
        ThemePreset("rose", "Rose", 0xFFF43F5E.toInt()),
        ThemePreset("sunset", "Sunset", 0xFFFB7C3C.toInt()),
        ThemePreset("amber", "Amber", 0xFFF5B301.toInt()),
        ThemePreset("mint", "Mint", 0xFF2DD4A7.toInt()),
        ThemePreset("lime", "Lime", 0xFFC1F479.toInt()),
        ThemePreset("crimson", "Crimson", 0xFFDC2626.toInt()),
        ThemePreset("sky", "Sky", 0xFF38BDF8.toInt()),
        ThemePreset("graphite", "Graphite", 0xFF9AA0A6.toInt(), PaletteVariant.Monochrome),
        ThemePreset("sakura", "Sakura", 0xFFF9A8D4.toInt(), PaletteVariant.Expressive),
        ThemePreset("midnight", "Midnight", 0xFF4F46E5.toInt(), style = ThemeStyle.Oled),
    )

    val default: ThemePreset = all.first()
}

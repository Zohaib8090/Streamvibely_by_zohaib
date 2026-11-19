package app.streamvibely.theme.engine

import app.streamvibely.domain.PaletteVariant
import app.streamvibely.domain.ThemeStyle

/** A named starting point shown in the customization screen. The list lives in `ThemePresets`. */
data class ThemePreset(
    val id: String,
    val name: String,
    val seedColor: Int,
    val variant: PaletteVariant = PaletteVariant.TonalSpot,
    /** When non-null, choosing the preset also switches the surface style. */
    val style: ThemeStyle? = null,
)

package app.streamvibely.domain

import kotlinx.coroutines.flow.StateFlow

/** Light, dark, or follow the device. The classic OLED look is a [ThemeStyle], not a mode. */
enum class ThemeMode { System, Light, Dark }

/**
 * How surfaces are drawn.
 * - Material: flat tonal surfaces.
 * - Clay: soft, inflated, rounded surfaces with a bright inner edge and a soft drop shadow.
 * - Glass: translucent surfaces with a thin light border over a blurred colorful backdrop.
 * - Oled: pure black background and near-black surfaces (saves power on OLED screens).
 */
enum class ThemeStyle { Material, Clay, Glass, Oled }

/** Where the main color comes from. */
enum class ColorSource {
    /** A fixed color chosen by the user ([ThemeSpec.seedColor]). */
    Static,
    /** The dominant color of the current song's cover (falls back to [ThemeSpec.seedColor] without art). */
    AlbumArt,
}

/** How the tonal palette is derived from the seed color. */
enum class PaletteVariant { TonalSpot, Vibrant, Expressive, Neutral, Monochrome }

/** Parts of the UI whose color the user can override. */
enum class ThemeArea { Background, Surfaces, Navigation, MiniPlayer, SidePanel, Accent }

/** Everything the user can customize about colors and surfaces. Colors are ARGB ints. */
data class ThemeSpec(
    val mode: ThemeMode = ThemeMode.Dark,
    val style: ThemeStyle = ThemeStyle.Material,
    val source: ColorSource = ColorSource.Static,
    val seedColor: Int = 0xFF8AA4FF.toInt(),
    val variant: PaletteVariant = PaletteVariant.TonalSpot,
    /** -1 (lower contrast) .. 0 (standard) .. 1 (high contrast). */
    val contrast: Float = 0f,
    /** Multiplies every corner radius: 0.5 sharper .. 1 default .. 1.5 rounder. */
    val cornerScale: Float = 1f,
    /** Clay: how strong the shadows and highlights are, 0..1. */
    val clayDepth: Float = 0.6f,
    /** Glass: blur radius in dp, 0..48. */
    val glassBlurDp: Float = 24f,
    /** Glass: how opaque the glass surfaces are, 0.2..0.9. */
    val glassOpacity: Float = 0.55f,
    /** Per-area color overrides; an area not in the map uses the generated color. */
    val areaOverrides: Map<ThemeArea, Int> = emptyMap(),
    /** Id of the preset the user started from, only for display. */
    val presetId: String? = null,
)

interface ThemeRepository {
    val spec: StateFlow<ThemeSpec>
    suspend fun update(transform: (ThemeSpec) -> ThemeSpec)
    suspend fun reset()
}

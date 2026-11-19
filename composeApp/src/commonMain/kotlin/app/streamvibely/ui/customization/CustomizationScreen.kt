package app.streamvibely.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.streamvibely.domain.ColorSource
import app.streamvibely.domain.PaletteVariant
import app.streamvibely.domain.ThemeArea
import app.streamvibely.domain.ThemeMode
import app.streamvibely.domain.ThemeSpec
import app.streamvibely.domain.ThemeStyle
import app.streamvibely.theme.AppColors
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.engine.ThemePresets
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons
import kotlin.math.roundToInt

private val modeOptions = listOf(
    SegmentOption(ThemeMode.System, "System", AppIcons.Smartphone),
    SegmentOption(ThemeMode.Light, "Light", AppIcons.LightMode),
    SegmentOption(ThemeMode.Dark, "Dark", AppIcons.DarkMode),
)

private val sourceOptions = listOf(
    SegmentOption(ColorSource.Static, "Pick a color", AppIcons.Colorize),
    SegmentOption(ColorSource.AlbumArt, "Album art", AppIcons.Image),
)

private val variantLabels = listOf(
    PaletteVariant.TonalSpot to "Tonal spot",
    PaletteVariant.Vibrant to "Vibrant",
    PaletteVariant.Expressive to "Expressive",
    PaletteVariant.Neutral to "Neutral",
    PaletteVariant.Monochrome to "Monochrome",
)

private data class StyleInfo(val style: ThemeStyle, val title: String, val subtitle: String)

private val styleInfos = listOf(
    StyleInfo(ThemeStyle.Material, "Material", "Tonal surfaces"),
    StyleInfo(ThemeStyle.Clay, "Clay", "Soft, inflated, tactile"),
    StyleInfo(ThemeStyle.Glass, "Glass", "Translucent with a blurred backdrop"),
    StyleInfo(ThemeStyle.Oled, "Classic OLED", "Pure black, saves power"),
)

private data class AreaInfo(val area: ThemeArea, val title: String, val subtitle: String, val icon: ImageVector)

private val areaInfos = listOf(
    AreaInfo(ThemeArea.Background, "Background", "Behind everything", AppIcons.FormatPaint),
    AreaInfo(ThemeArea.Surfaces, "Surfaces", "Cards and rows", AppIcons.Layers),
    AreaInfo(ThemeArea.Navigation, "Navigation", "Bottom bar and sidebar", AppIcons.Tonality),
    AreaInfo(ThemeArea.MiniPlayer, "Mini player", "The bar above the tabs", AppIcons.MusicNote),
    AreaInfo(ThemeArea.SidePanel, "Side panel", "Right-hand panel on wide screens", AppIcons.Layers),
    AreaInfo(ThemeArea.Accent, "Accent", "Buttons, sliders, highlights", AppIcons.AutoAwesome),
)

private fun AppColors.generated(area: ThemeArea): Color = when (area) {
    ThemeArea.Background -> background
    ThemeArea.Surfaces -> card
    ThemeArea.Navigation -> navigation
    ThemeArea.MiniPlayer -> miniPlayer
    ThemeArea.SidePanel -> panel
    ThemeArea.Accent -> accent
}

/**
 * Theme customization. Every change goes through [onSpecChange] and re-themes the whole app live.
 * [artSeed] is the dominant color of the playing song's cover, or null when nothing with art plays.
 */
@Composable
fun CustomizationScreen(
    spec: ThemeSpec,
    onSpecChange: ((ThemeSpec) -> ThemeSpec) -> Unit,
    onBack: () -> Unit,
    artSeed: Int?,
    modifier: Modifier = Modifier,
) {
    var editingArea by remember { mutableStateOf<ThemeArea?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    Box(modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(colors.card).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(AppIcons.ArrowBack, "Back", tint = colors.onSurface, size = 24.dp)
                }
                Text("Customization", Modifier.padding(start = 12.dp), color = colors.onBackground, style = MaterialTheme.typography.headlineMedium)
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "preview") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Preview: a small copy of the app that updates as you change the options below.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        ThemePreview(spec.cornerScale)
                    }
                }

                item(key = "mode-h") { CustomizationSectionHeader("Mode", AppIcons.Contrast) }
                item(key = "mode") {
                    SegmentedControl(modeOptions, spec.mode, { m -> onSpecChange { it.copy(mode = m) } })
                }

                item(key = "style-h") { CustomizationSectionHeader("Style", AppIcons.Brush) }
                item(key = "style") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        styleInfos.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { info ->
                                    StyleCard(
                                        info.style, info.title, info.subtitle, spec.style == info.style,
                                        onClick = { onSpecChange { it.copy(style = info.style) } },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "source-h") { CustomizationSectionHeader("Color source", AppIcons.Palette) }
                item(key = "source") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SegmentedControl(sourceOptions, spec.source, { s -> onSpecChange { it.copy(source = s) } })
                        if (spec.source == ColorSource.AlbumArt) {
                            CustomizationPanel {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (artSeed != null) {
                                        ColorSwatch(artSeed.asColor(), Modifier.padding(end = 12.dp), size = 40.dp, label = "Album art color")
                                        Text("Color from the song that is playing", color = colors.onCard, style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        AppIcon(AppIcons.Image, null, Modifier.padding(end = 12.dp), tint = colors.muted, size = 24.dp)
                                        Text("Play a song with a cover to use it", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "color-h") {
                    CustomizationSectionHeader(
                        "Colors", AppIcons.Colorize,
                        subtitle = if (spec.source == ColorSource.AlbumArt) "Used when the song has no cover" else null,
                    )
                }
                item(key = "presets") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(ThemePresets.all, key = { it.id }) { preset ->
                            ColorSwatch(
                                preset.seedColor.asColor(),
                                size = 44.dp,
                                selected = spec.presetId == preset.id,
                                label = preset.name,
                                onClick = {
                                    onSpecChange {
                                        it.copy(
                                            seedColor = preset.seedColor,
                                            variant = preset.variant,
                                            presetId = preset.id,
                                            style = preset.style ?: it.style,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
                item(key = "picker") {
                    CustomizationPanel {
                        ColorPicker(spec.seedColor, { c -> onSpecChange { it.copy(seedColor = c, presetId = null) } })
                    }
                }
                item(key = "variant-h") {
                    Text("Palette style", Modifier.padding(start = 4.dp, top = 6.dp), color = colors.onSurface, style = MaterialTheme.typography.titleSmall)
                }
                item(key = "variants") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(variantLabels, key = { it.first }) { (variant, label) ->
                            ChoiceChip(label, spec.variant == variant, { onSpecChange { it.copy(variant = variant) } })
                        }
                    }
                }

                item(key = "sliders-h") { CustomizationSectionHeader("Fine tuning", AppIcons.Tonality) }
                item(key = "sliders") {
                    CustomizationPanel {
                        SliderRow(
                            AppIcons.Contrast, "Contrast", spec.contrast,
                            { v -> onSpecChange { it.copy(contrast = v) } }, -1f..1f,
                            valueLabel = { if (it < -0.05f) "Lower" else if (it > 0.05f) "Higher" else "Standard" },
                        )
                        SliderRow(
                            AppIcons.RoundedCorner, "Corner roundness", spec.cornerScale,
                            { v -> onSpecChange { it.copy(cornerScale = v) } }, 0.5f..1.5f,
                            valueLabel = { "${(it * 100).roundToInt()}%" },
                        )
                        if (spec.style == ThemeStyle.Clay) {
                            SliderRow(
                                AppIcons.Layers, "Clay depth", spec.clayDepth,
                                { v -> onSpecChange { it.copy(clayDepth = v) } }, 0f..1f,
                                valueLabel = { "${(it * 100).roundToInt()}%" },
                            )
                        }
                        if (spec.style == ThemeStyle.Glass) {
                            SliderRow(
                                AppIcons.BlurOn, "Glass blur", spec.glassBlurDp,
                                { v -> onSpecChange { it.copy(glassBlurDp = v) } }, 0f..48f,
                                valueLabel = { "${it.roundToInt()} dp" }, step = 1f,
                            )
                            SliderRow(
                                AppIcons.Opacity, "Glass opacity", spec.glassOpacity,
                                { v -> onSpecChange { it.copy(glassOpacity = v) } }, 0.2f..0.9f,
                                valueLabel = { "${(it * 100).roundToInt()}%" },
                            )
                        }
                    }
                }

                item(key = "areas-h") {
                    CustomizationSectionHeader("Customize each area", AppIcons.FormatPaint, subtitle = "Override the color of one part of the app")
                }
                items(areaInfos, key = { it.area }) { info ->
                    AreaRow(info, override = spec.areaOverrides[info.area], generated = colors.generated(info.area), onClick = { editingArea = info.area })
                }

                item(key = "reset") {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 12.dp)
                            .clip(RoundedCornerShape(Tokens.radiusPill.dp))
                            .background(colors.cardHigh)
                            .clickable { confirmReset = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(AppIcons.RestartAlt, null, Modifier.padding(end = 8.dp), tint = colors.error, size = 20.dp)
                            Text("Reset all customization", color = colors.error, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    editingArea?.let { area ->
        val info = areaInfos.first { it.area == area }
        ColorPickerDialog(
            title = info.title,
            color = spec.areaOverrides[area] ?: colors.generated(area).toArgbOpaque(),
            onColorChange = { c -> onSpecChange { it.copy(areaOverrides = it.areaOverrides + (area to c)) } },
            onUseAutomatic = { onSpecChange { it.copy(areaOverrides = it.areaOverrides - area) } },
            onDismiss = { editingArea = null },
        )
    }

    if (confirmReset) {
        Dialog(onDismissRequest = { confirmReset = false }) {
            Column(
                Modifier.clip(RoundedCornerShape(Tokens.radiusCard.dp * 1.5f)).background(colors.surface).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Reset customization?", color = colors.onSurface, style = MaterialTheme.typography.titleLarge)
                Text("Mode, style, colors and every override go back to the defaults.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    DialogButton("Cancel", primary = false, onClick = { confirmReset = false })
                    DialogButton("Reset", primary = true, onClick = {
                        onSpecChange { ThemeSpec() }
                        confirmReset = false
                    })
                }
            }
        }
    }
}

private fun Color.toArgbOpaque(): Int = toArgb() or 0xFF000000.toInt()

@Composable
private fun AreaRow(info: AreaInfo, override: Int?, generated: Color, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.radiusCard.dp))
            .background(colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorSwatch(override?.asColor() ?: generated, Modifier.padding(end = 12.dp), size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(info.title, color = colors.onCard, style = MaterialTheme.typography.titleSmall)
            Text(info.subtitle, color = colors.muted, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            if (override == null) "Automatic" else argbToHex(override),
            Modifier.padding(end = 8.dp),
            color = if (override == null) colors.muted else colors.accent,
            style = MaterialTheme.typography.labelMedium,
        )
        AppIcon(AppIcons.Edit, null, tint = colors.muted, size = 20.dp)
    }
}

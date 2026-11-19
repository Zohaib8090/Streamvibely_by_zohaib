package app.streamvibely.ui.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqualizerState
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.appSurface
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons

private class EqActions(
    val onEnabledChange: (Boolean) -> Unit,
    val onPreampChange: (Float) -> Unit,
    val onAddBand: (Float) -> Unit,
    val onBandChange: (EqBand) -> Unit,
    val onRemoveBand: (Int) -> Unit,
    val onPreset: (String) -> Unit,
    val onSavePreset: (String) -> Unit,
    val onDeletePreset: (String) -> Unit,
    val onReset: () -> Unit,
)

private val ContentMaxWidth = 920.dp

/** Full-screen equalizer: Graphic and Parametric views of one band list, plus the live spectrum. */
@Composable
fun EqualizerScreen(
    state: EqualizerState,
    spectrum: () -> FloatArray,
    onBack: () -> Unit = {},
    onEnabledChange: (Boolean) -> Unit = {},
    onPreampChange: (Float) -> Unit = {},
    onAddBand: (frequencyHz: Float) -> Unit = {},
    onBandChange: (EqBand) -> Unit = {},
    onRemoveBand: (Int) -> Unit = {},
    onPreset: (String) -> Unit = {},
    onSavePreset: (String) -> Unit = {},
    onDeletePreset: (String) -> Unit = {},
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val actions = EqActions(onEnabledChange, onPreampChange, onAddBand, onBandChange, onRemoveBand, onPreset, onSavePreset, onDeletePreset, onReset)
    var tab by remember { mutableStateOf(0) }
    val selection = rememberBandSelection(state.bands)
    val selected = selection.resolve(state.bands)

    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        // On short screens the pinned spectrum would hide the slider labels, so it scrolls with the content.
        val pinSpectrum = maxHeight >= 820.dp
        Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxSize()) {
            Header(state, actions.onEnabledChange, Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), leading = {
                IconAction(AppIcons.ArrowBack, "Back", onBack, tint = AppTheme.colors.onSurface)
            })
            if (!state.available) {
                Unavailable(Modifier.padding(16.dp))
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .alpha(if (state.enabled) 1f else 0.5f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PresetBar(state, actions)
                    PreampCard(state, actions.onPreampChange)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Segmented(
                            options = listOf("Graphic", "Parametric"),
                            selected = tab,
                            onSelect = { tab = it },
                            modifier = Modifier.width(264.dp),
                            icons = listOf(AppIcons.Equalizer, AppIcons.Tune),
                        )
                        Box(Modifier.weight(1f))
                        AddBandButton(state.bands.size < state.maxBands) {
                            selection.expectNew(state.bands)
                            actions.onAddBand(suggestFrequency(state.bands))
                        }
                    }
                    if (tab == 0) {
                        if (state.bands.isEmpty()) EmptyBands() else {
                            Box(Modifier.fillMaxWidth().eqCard(Tokens.radiusPanel)) {
                                GraphicBands(state, selected?.id, { selection.selectedId = it }, actions.onBandChange)
                            }
                            if (selected != null) {
                                key(selected.id) { BandEditor(selected, state, actions.onBandChange, { actions.onRemoveBand(selected.id) }) }
                            }
                        }
                    } else {
                        ParametricSection(state, selection, selected, actions, 260.dp)
                    }
                    if (!pinSpectrum) SpectrumCard(spectrum, 96.dp, Modifier.padding(bottom = 8.dp))
                }
                if (pinSpectrum) SpectrumCard(spectrum, 112.dp, Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp))
            }
        }
    }
}

/** Compact equalizer for the wide-screen side panel: curve, band editor and spectrum. */
@Composable
fun EqualizerPanel(
    state: EqualizerState,
    spectrum: () -> FloatArray,
    onClose: () -> Unit = {},
    onEnabledChange: (Boolean) -> Unit = {},
    onPreampChange: (Float) -> Unit = {},
    onAddBand: (frequencyHz: Float) -> Unit = {},
    onBandChange: (EqBand) -> Unit = {},
    onRemoveBand: (Int) -> Unit = {},
    onPreset: (String) -> Unit = {},
    onSavePreset: (String) -> Unit = {},
    onDeletePreset: (String) -> Unit = {},
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val actions = EqActions(onEnabledChange, onPreampChange, onAddBand, onBandChange, onRemoveBand, onPreset, onSavePreset, onDeletePreset, onReset)
    val selection = rememberBandSelection(state.bands)
    val selected = selection.resolve(state.bands)

    Column(modifier) {
        Header(state, actions.onEnabledChange, Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp), compact = true, trailing = {
            IconAction(AppIcons.Close, "Close equalizer", onClose)
        })
        if (!state.available) {
            Unavailable(Modifier.padding(12.dp))
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (state.enabled) 1f else 0.5f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PresetBar(state, actions)
                PreampCard(state, actions.onPreampChange)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bands", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface, modifier = Modifier.weight(1f))
                    AddBandButton(state.bands.size < state.maxBands) {
                        selection.expectNew(state.bands)
                        actions.onAddBand(suggestFrequency(state.bands))
                    }
                }
                ParametricSection(state, selection, selected, actions, 170.dp)
            }
            SpectrumCard(spectrum, 68.dp, Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp))
        }
    }
}

@Composable
private fun Header(
    state: EqualizerState,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier,
    compact: Boolean = false,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val colors = AppTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        leading()
        Text(
            "Equalizer",
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (state.available) {
            Text(if (state.enabled) "On" else "Off", style = MaterialTheme.typography.labelLarge, color = if (state.enabled) colors.accent else colors.muted)
            PillSwitch(state.enabled, onEnabledChange)
        }
        trailing()
    }
}

@Composable
private fun Unavailable(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier.fillMaxWidth().eqCard(Tokens.radiusPanel).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(AppIcons.GraphicEq, null, tint = colors.muted, size = 40.dp)
        Text("Equalizer not available", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        Text(
            "Start playing a track, or check that this device supports audio effects.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyBands() {
    Box(Modifier.fillMaxWidth().eqCard(Tokens.radiusPanel).padding(24.dp), contentAlignment = Alignment.Center) {
        Text("No bands yet. Use the add button to create one.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
    }
}

@Composable
private fun AddBandButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.accent else colors.sliderTrack, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(AppIcons.Add, "Add band", tint = if (enabled) colors.onAccent else colors.muted, size = 22.dp)
    }
}

@Composable
private fun PreampCard(state: EqualizerState, onPreampChange: (Float) -> Unit) {
    val range = state.maxGainDb - state.minGainDb
    Box(Modifier.fillMaxWidth().eqCard(Tokens.radiusPanel).padding(horizontal = 16.dp, vertical = 10.dp)) {
        ParamRow(
            label = "Preamp",
            fraction = ((state.preampDb - state.minGainDb) / range).coerceIn(0f, 1f),
            text = formatDecimal(state.preampDb, 1),
            unit = "dB",
            origin = ((0f - state.minGainDb) / range).coerceIn(0f, 1f),
            onFraction = { f ->
                val db = snap(state.minGainDb + f * range, 0.1f)
                if (db != state.preampDb) onPreampChange(db)
            },
            onValue = { onPreampChange(snap(it.coerceIn(state.minGainDb, state.maxGainDb), 0.1f)) },
        )
    }
}

@Composable
private fun ParametricSection(
    state: EqualizerState,
    selection: BandSelection,
    selected: EqBand?,
    actions: EqActions,
    graphHeight: Dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().eqCard(Tokens.radiusPanel)) {
            ParametricGraph(
                state = state,
                selectedId = selected?.id,
                onSelect = { selection.selectedId = it },
                onBandChange = actions.onBandChange,
                onAddAt = { hz ->
                    if (state.bands.size < state.maxBands) {
                        selection.expectNew(state.bands)
                        actions.onAddBand(hz)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(graphHeight),
            )
            FrequencyLabels(Modifier.padding(bottom = 8.dp))
        }
        if (selected == null) {
            EmptyBands()
        } else {
            BandChips(state.bands.sortedBy { it.frequencyHz }, selected.id, onSelect = { selection.selectedId = it })
            key(selected.id) {
                BandEditor(selected, state, actions.onBandChange, { actions.onRemoveBand(selected.id) })
            }
        }
    }
}

/** Spectrum card with bars and frequency labels; sits outside the scrolling area. */
@Composable
private fun SpectrumCard(spectrum: () -> FloatArray, graphHeight: Dp, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().eqCard(Tokens.radiusPanel).padding(top = 10.dp, bottom = 6.dp)) {
        SpectrumGraph(spectrum, Modifier.fillMaxWidth().height(graphHeight))
        FrequencyLabels()
    }
}

@Composable
private fun PresetBar(state: EqualizerState, actions: EqActions) {
    val colors = rememberEqColors()
    val chipShape = eqShape(Tokens.radiusChipLarge)
    var menuOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.weight(1f)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .appSurface(SurfaceKind.Chip, chipShape)
                    .clip(chipShape)
                    .clickable(role = Role.DropdownList) { menuOpen = true }
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.selectedPreset ?: "Custom",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                AppIcon(AppIcons.KeyboardArrowDown, "Choose preset", tint = colors.muted, size = 22.dp)
            }
            app.streamvibely.ui.common.AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                state.presets.forEach { name ->
                    app.streamvibely.ui.common.AppMenuItem(
                        name,
                        { menuOpen = false; actions.onPreset(name) },
                        selected = name == state.selectedPreset,
                    )
                }
            }
        }
        IconAction(AppIcons.Save, "Save preset", { saving = true })
        IconAction(AppIcons.Delete, "Delete preset", { state.selectedPreset?.let(actions.onDeletePreset) }, enabled = state.selectedPreset != null)
        IconAction(AppIcons.RestartAlt, "Reset equalizer", actions.onReset)
    }
    if (saving) {
        SavePresetDialog(onDismiss = { saving = false }, onSave = {
            saving = false
            actions.onSavePreset(it)
        })
    }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save preset") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

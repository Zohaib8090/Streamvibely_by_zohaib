package app.streamvibely.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.ThemeStyle
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.Tokens
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons

/** Large selectable card with a tiny sample drawn in its own style. */
@Composable
internal fun StyleCard(
    style: ThemeStyle,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Tokens.radiusCard.dp)
    Column(
        modifier.clip(shape)
            .background(AppTheme.colors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) AppTheme.colors.accent else AppTheme.colors.outlineVariant, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(72.dp)) {
            StyleSample(style, Modifier.fillMaxSize())
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(AppTheme.colors.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(AppIcons.Check, null, tint = AppTheme.colors.onAccent, size = 16.dp)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = AppTheme.colors.onCard, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = AppTheme.colors.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A small rounded rectangle drawn the way the style draws its surfaces. */
@Composable
private fun StyleSample(style: ThemeStyle, modifier: Modifier) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Tokens.radiusSm.dp)
    when (style) {
        ThemeStyle.Material -> Box(modifier.clip(shape).background(colors.accentContainer))
        ThemeStyle.Clay -> Box(modifier.padding(6.dp)) {
            Box(
                Modifier.fillMaxSize()
                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = colors.accent)
                    .shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = Color.White, spotColor = Color.White)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(colors.cardHigh, colors.accentContainer)))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            )
        }
        ThemeStyle.Glass -> Box(
            modifier.clip(shape).background(Brush.linearGradient(listOf(colors.tiles[0], colors.tiles[3], colors.tiles[2]))),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxSize().padding(12.dp)
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.6f), shape),
            )
        }
        ThemeStyle.Oled -> Box(
            modifier.clip(shape).background(Color.Black).border(1.dp, colors.outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().padding(14.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0C0C0C)).border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp)))
        }
    }
}

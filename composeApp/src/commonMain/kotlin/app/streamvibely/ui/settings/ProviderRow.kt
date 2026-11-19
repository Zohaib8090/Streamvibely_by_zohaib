package app.streamvibely.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.ProviderOption
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled

/** One search source with an on/off switch. */
@Composable
internal fun ProviderRow(option: ProviderOption, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(option.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
            if (option.description.isNotEmpty()) {
                Text(option.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = option.enabled, onCheckedChange = onToggle)
    }
}

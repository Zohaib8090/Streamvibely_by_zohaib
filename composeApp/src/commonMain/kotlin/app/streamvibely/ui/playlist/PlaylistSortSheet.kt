package app.streamvibely.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons

/** Sorting options matching Spotify's playlist song sorter. */
enum class PlaylistSortOrder(val label: String) {
    Custom("Custom order"),
    Title("Title"),
    Artist("Artist"),
    Album("Album"),
    RecentlyAdded("Recently added"),
    ReleaseDate("Release date"),
    Duration("Duration"),
}

@Composable
fun PlaylistSortSheet(
    visible: Boolean,
    currentSort: PlaylistSortOrder,
    onSelectSort: (PlaylistSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF181818))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A4A4A)),
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White,
                )

                Spacer(Modifier.height(12.dp))

                val spotifyGreen = Color(0xFF1ED760)

                PlaylistSortOrder.entries.forEach { order ->
                    val selected = order == currentSort
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectSort(order)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = order.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 16.sp,
                            ),
                            color = if (selected) spotifyGreen else Color(0xFFEDEDED),
                        )
                        if (selected) {
                            AppIcon(
                                icon = AppIcons.Check,
                                contentDescription = "Selected",
                                tint = spotifyGreen,
                                size = 22.dp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


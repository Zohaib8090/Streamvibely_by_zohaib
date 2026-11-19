package app.streamvibely.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Account

/**
 * Top-left button of the home screen: the avatar when signed in, a person icon otherwise.
 * [onClick] should open the drawer, for example `{ scope.launch { drawerState.open() } }`.
 * Pass `{ drawerState.progress }` as [pull] to make the avatar shrink slightly as the drawer
 * opens; it is read in the draw phase only.
 */
@Composable
fun AccountButton(
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    pull: () -> Float = { 0f },
) {
    val label = if (account != null) "Account, ${account.displayName}" else "Account, not signed in"
    AccountAvatar(
        account,
        size,
        modifier
            .graphicsLayer {
                val s = 1f - 0.12f * pull().coerceIn(0f, 1f)
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    )
}

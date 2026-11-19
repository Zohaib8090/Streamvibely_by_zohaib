package app.streamvibely.ui.account

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Account
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val PanelMaxWidth = 300.dp
private val EdgeStrip = 24.dp
private val Overscroll = 12.dp
private const val OpenRegionFraction = 0.4f
private const val FlingVelocity = 600f
private const val ContentShift = 0.09f
private const val ContentScaleDrop = 0.04f

/**
 * Wraps the app content and adds the account side menu on top. The panel follows the finger: a
 * horizontal drag from the left edge strip opens it, a horizontal drag on the panel or scrim
 * closes it, and releasing settles with a spring that keeps the fling velocity. Behind the
 * panel the content is pushed right and shrinks slightly. While closed nothing is drawn on top
 * and only the edge strip reacts. Open it from a button with `scope.launch { state.open() }`;
 * wire the system Back to `state.close()` while `state.isOpen`.
 */
@Composable
fun AccountDrawerHost(
    state: AccountDrawerState,
    account: Account?,
    onOpenAccount: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelWidth = minOf(PanelMaxWidth, maxWidth * 0.85f)
        val panelPx = with(density) { panelWidth.toPx() }
        val edgePx = with(density) { EdgeStrip.toPx() }
        val overFrac = with(density) { Overscroll.toPx() } / panelPx

        Box(Modifier.fillMaxSize().drawerDrag(state, scope, panelPx, edgePx, overFrac, gesturesEnabled)) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val p = state.progress.coerceAtMost(1f)
                    translationX = p * panelPx * ContentShift
                    val s = 1f - ContentScaleDrop * p
                    scaleX = s
                    scaleY = s
                },
            ) { content() }

            if (state.isOpen) {
                Spacer(
                    Modifier.fillMaxSize()
                        .drawBehind { drawRect(Color.Black, alpha = 0.5f * state.progress.coerceAtMost(1f)) }
                        .pointerInput(Unit) { detectTapGestures { scope.launch { state.close() } } },
                )
                DrawerPanel(
                    state = state,
                    account = account,
                    panelWidth = panelWidth,
                    panelPx = panelPx,
                    onOpenAccount = { onOpenAccount(); scope.launch { state.close() } },
                    onOpenUsage = { onOpenUsage(); scope.launch { state.close() } },
                    onOpenSettings = { onOpenSettings(); scope.launch { state.close() } },
                )
            }
        }
    }
}

/** Soft limit: past fully open the panel travels at most [overFrac] further. */
private fun rubber(raw: Float, overFrac: Float): Float {
    if (raw <= 1f) return raw
    val over = raw - 1f
    return 1f + overFrac * (over / (over + overFrac))
}

/**
 * Horizontal drag that drives the drawer. A press that starts while the drawer is closed counts
 * only in the left 40% of the screen (which includes the [edgePx] strip), only when rightward
 * and mostly horizontal, and only while [openGestureEnabled]. Nothing is consumed before touch slop, so taps and
 * children that consume the gesture (lists, sliders) keep working.
 */
private fun Modifier.drawerDrag(
    state: AccountDrawerState,
    scope: CoroutineScope,
    panelPx: Float,
    edgePx: Float,
    overFrac: Float,
    openGestureEnabled: Boolean,
): Modifier = pointerInput(state, panelPx, edgePx, overFrac, openGestureEnabled) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var raw = state.progress
        val startedClosed = raw <= 0.001f
        if (startedClosed) {
            if (!openGestureEnabled) return@awaitEachGesture
            val region = maxOf(edgePx, size.width * OpenRegionFraction)
            if (down.position.x > region) return@awaitEachGesture
        }
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)

        fun follow(dx: Float) {
            raw = (raw + dx / panelPx).coerceAtLeast(0f)
            val value = rubber(raw, overFrac)
            scope.launch { state.snapTo(value) }
        }

        var rejected = false
        val slopChange = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
            val dx = change.position.x - down.position.x
            val dy = change.position.y - down.position.y
            if (startedClosed && (overSlop <= 0f || kotlin.math.abs(dy) > kotlin.math.abs(dx))) {
                rejected = true
            } else {
                tracker.addPointerInputChange(change)
                change.consume()
                follow(overSlop)
            }
        } ?: return@awaitEachGesture
        if (rejected) return@awaitEachGesture
        horizontalDrag(slopChange.id) { change ->
            tracker.addPointerInputChange(change)
            follow(change.positionChange().x)
            change.consume()
        }
        val velocity = tracker.calculateVelocity().x
        val target = when {
            velocity > FlingVelocity -> 1f
            velocity < -FlingVelocity -> 0f
            raw > 0.5f -> 1f
            else -> 0f
        }
        scope.launch { state.settle(target, velocity / panelPx) }
    }
}

/** Fades and slides an item in once the drawer is past halfway; later items start slightly later. */
private fun Modifier.staggered(state: AccountDrawerState, index: Int): Modifier = graphicsLayer {
    val t = ((state.progress - 0.5f) * 2f - index * 0.12f).coerceIn(0f, 1f)
    alpha = t
    translationX = (1f - t) * -16.dp.toPx()
}

@Composable
private fun DrawerPanel(
    state: AccountDrawerState,
    account: Account?,
    panelWidth: androidx.compose.ui.unit.Dp,
    panelPx: Float,
    onOpenAccount: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier.width(panelWidth + Overscroll).fillMaxHeight()
            .graphicsLayer {
                // The panel's right edge sits at progress * panelPx. Its extra left part
                // (Overscroll wide) keeps the screen edge covered while it overshoots.
                translationX = (state.progress - 1f) * panelPx - Overscroll.toPx()
            }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .padding(start = Overscroll + 12.dp, end = 12.dp, top = 20.dp, bottom = 20.dp),
    ) {
        DrawerHeader(account, Modifier.staggered(state, 0))
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DrawerRow(AppIcons.AccountCircle, "Account", onOpenAccount, Modifier.staggered(state, 1))
            DrawerRow(AppIcons.BarChart, "Listening usage", onOpenUsage, Modifier.staggered(state, 2))
            DrawerRow(AppIcons.Settings, "Settings", onOpenSettings, Modifier.staggered(state, 3))
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Streamvibely · open source (GPL-3.0)",
            Modifier.padding(horizontal = 8.dp).staggered(state, 4),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DrawerHeader(account: Account?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AccountAvatar(account, 56.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                account?.displayName ?: "Not signed in",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (account?.email != null) {
                Text(
                    account.email,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DrawerRow(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            title,
            Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

package app.streamvibely.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.streamvibely.theme.AppTheme
import kotlinx.coroutines.delay

// ── Onboarding step machine ──────────────────────────────────────────────────

private enum class Step { Connect, Syncing, Done }

/**
 * First-launch onboarding: lets the user connect Spotify and/or YouTube Music
 * and sync their library/preferences from that provider.
 *
 * @param spotifyConnected   Whether Spotify is already authenticated.
 * @param ytConnected        Whether YouTube / Google is signed in.
 * @param onConnectSpotify   Opens the Spotify login WebView (same as AccountScreen).
 * @param onConnectYouTube   Triggers Google sign-in (same as AccountScreen).
 * @param onSyncSpotify      Kicks off a full Spotify library sync.
 * @param onComplete         Called when the user taps "Let's go" or "Skip".
 */
@Composable
fun OnboardingScreen(
    spotifyConnected: Boolean,
    ytConnected: Boolean,
    onConnectSpotify: () -> Unit,
    onConnectYouTube: () -> Unit,
    onSyncSpotify: () -> Unit,
    onComplete: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.Connect) }
    // Track which providers the user chose to sync this session.
    var syncSpotify by remember { mutableStateOf(false) }
    var syncYouTube by remember { mutableStateOf(false) }

    // Auto-advance Connect → Syncing when at least one provider is linked and the user pressed sync.
    // Syncing → Done after a short wait (the real sync runs in the background).
    LaunchedEffect(step) {
        if (step == Step.Syncing) {
            if (syncSpotify) onSyncSpotify()
            delay(2800)
            step = Step.Done
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 })
                    .togetherWith(fadeOut(tween(250)))
            },
            label = "onboarding-step",
        ) { currentStep ->
            when (currentStep) {
                Step.Connect -> ConnectStep(
                    spotifyConnected = spotifyConnected,
                    ytConnected = ytConnected,
                    onConnectSpotify = onConnectSpotify,
                    onConnectYouTube = onConnectYouTube,
                    onSync = { sp, yt ->
                        syncSpotify = sp
                        syncYouTube = yt
                        step = Step.Syncing
                    },
                    onSkip = onComplete,
                )
                Step.Syncing -> SyncingStep(syncSpotify, syncYouTube)
                Step.Done    -> DoneStep(onComplete)
            }
        }
    }
}

// ── Step 1: Connect ──────────────────────────────────────────────────────────

@Composable
private fun ConnectStep(
    spotifyConnected: Boolean,
    ytConnected: Boolean,
    onConnectSpotify: () -> Unit,
    onConnectYouTube: () -> Unit,
    onSync: (spotify: Boolean, youtube: Boolean) -> Unit,
    onSkip: () -> Unit,
) {
    val anyConnected = spotifyConnected || ytConnected

    Column(
        Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Spacer(Modifier.height(52.dp))

        Text("🎵", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Welcome to Streamvibely",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Connect Spotify or YouTube Music to sync\nyour playlists and liked songs automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))

        // ── Spotify card ───────────────────────────────────────────────────
        ProviderCard(
            name = "Spotify",
            description = if (spotifyConnected) "Connected — your library will be synced" else "Sync your Liked Songs & playlists",
            emoji = "🟢",
            gradientColors = listOf(Color(0xFF1DB954), Color(0xFF158A3E)),
            isConnected = spotifyConnected,
            onClick = if (!spotifyConnected) onConnectSpotify else null,
        )
        Spacer(Modifier.height(14.dp))

        // ── YouTube Music card ─────────────────────────────────────────────
        ProviderCard(
            name = "YouTube Music",
            description = if (ytConnected) "Connected — playlists ready to import" else "Sync your YouTube Music library",
            emoji = "🔴",
            gradientColors = listOf(Color(0xFFFF0000), Color(0xFFB71C1C)),
            isConnected = ytConnected,
            onClick = if (!ytConnected) onConnectYouTube else null,
        )
        Spacer(Modifier.height(32.dp))

        // ── Primary action ─────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(
                    if (anyConnected) AppTheme.colors.accent
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(enabled = anyConnected) {
                    onSync(spotifyConnected, ytConnected)
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (anyConnected) "Sync & Get Started →" else "Connect a service above",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (anyConnected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Skip ───────────────────────────────────────────────────────────
        Text(
            "Skip for now",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSkip)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ── Step 2: Syncing ──────────────────────────────────────────────────────────

@Composable
private fun SyncingStep(spotify: Boolean, youtube: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "spin-angle",
    )
    Column(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(80.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp).rotate(angle),
            color = AppTheme.colors.accent,
            strokeWidth = 5.dp,
        )
        Text(
            "Syncing your library…",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        val providers = buildList {
            if (spotify) add("Spotify")
            if (youtube) add("YouTube Music")
        }.joinToString(" & ")
        Text(
            "Pulling playlists and liked songs from $providers",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Step 3: Done ─────────────────────────────────────────────────────────────

@Composable
private fun DoneStep(onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(80.dp))

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DB954)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(tween(400, delayMillis = 200)) { it / 3 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "You're all set!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Your library is syncing in the background.\nHead to Your Library to browse everything.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, delayMillis = 400)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(AppTheme.colors.accent)
                    .clickable(onClick = onComplete)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Let's go →",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

// ── Shared: Provider card ─────────────────────────────────────────────────────

@Composable
private fun ProviderCard(
    name: String,
    description: String,
    emoji: String,
    gradientColors: List<Color>,
    isConnected: Boolean,
    onClick: (() -> Unit)?,
) {
    val scale by animateFloatAsState(
        targetValue = if (isConnected) 1f else 1f,
        label = "card-scale",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .then(
                if (isConnected) Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon circle
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 24.sp)
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        // Status badge
        AnimatedVisibility(
            visible = isConnected,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", fontSize = 16.sp, color = gradientColors.first(), fontWeight = FontWeight.Bold)
            }
        }
        AnimatedVisibility(
            visible = !isConnected,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    "Connect",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

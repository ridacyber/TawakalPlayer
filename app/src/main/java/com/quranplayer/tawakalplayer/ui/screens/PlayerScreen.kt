package com.quranplayer.tawakalplayer.ui.screens

import android.content.ComponentName
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.quranplayer.tawakalplayer.PlaybackService
import com.quranplayer.tawakalplayer.data.Reciter
import com.quranplayer.tawakalplayer.data.ReciterRepository
import com.quranplayer.tawakalplayer.data.Surah
import com.quranplayer.tawakalplayer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val TAG = "TawakalPlayer"
private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
private const val RETRY_DELAY_MS = 4000L
private const val MAX_RETRY_DELAY_MS = 30_000L

@Composable
fun PlayerScreen(
    reciter: Reciter,
    surah: Surah,
    onSurahEnded: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Instead of owning our own ExoPlayer, we connect to the one living
    // inside PlaybackService. mediaController stays null until the async
    // connection finishes (fast, but not instant).
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
            },
            MoreExecutors.directExecutor()
        )

        onDispose {
            mediaController?.stop()
            mediaController?.release()
            mediaController = null
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var repeatEnabled by remember { mutableStateOf(false) }
    var autoNextEnabled by remember { mutableStateOf(true) }
    var retryCount by remember { mutableStateOf(0) }

    val handler = remember { Handler(Looper.getMainLooper()) }

    // rememberUpdatedState keeps the listener referencing current values
    // every time, so it doesn't get stuck on the first surah/reciter.
    val currentOnSurahEnded by rememberUpdatedState(onSurahEnded)
    val currentRepeatEnabled by rememberUpdatedState(repeatEnabled)
    val currentAutoNextEnabled by rememberUpdatedState(autoNextEnabled)
    val currentSurah by rememberUpdatedState(surah)
    val currentReciter by rememberUpdatedState(reciter)

    LaunchedEffect(reciter, surah, mediaController) {
        val controller = mediaController ?: return@LaunchedEffect
        val url = ReciterRepository.surahUrl(reciter, surah.number)
        controller.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        controller.prepare()
        controller.playWhenReady = true
    }

    LaunchedEffect(repeatEnabled, mediaController) {
        mediaController?.repeatMode =
            if (repeatEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    DisposableEffect(mediaController) {
        val controller = mediaController
        if (controller == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        retryCount = 0
                    }
                    if (state == Player.STATE_ENDED && !currentRepeatEnabled && currentAutoNextEnabled) {
                        val timestamp = logDateFormat.format(Date())
                        Log.i(
                            TAG,
                            "[$timestamp] Surah ${currentSurah.number} (${currentSurah.nameTransliteration}) ended — advancing to next."
                        )
                        currentOnSurahEnded()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val timestamp = logDateFormat.format(Date())
                    retryCount++

                    val delayMs = (RETRY_DELAY_MS * retryCount).coerceAtMost(MAX_RETRY_DELAY_MS)

                    Log.e(
                        TAG,
                        "[$timestamp] Playback error on surah ${currentSurah.number} (${currentSurah.nameTransliteration}), " +
                                "reciter=${currentReciter.displayName}, attempt=$retryCount, " +
                                "errorCode=${error.errorCode} (${error.errorCodeName}), " +
                                "message=${error.message} — retrying in ${delayMs}ms"
                    )

                    handler.postDelayed({
                        controller.prepare()
                        controller.playWhenReady = true
                    }, delayMs)
                }
            }
            controller.addListener(listener)
            onDispose {
                controller.removeListener(listener)
            }
        }
    }

    LaunchedEffect(mediaController) {
        val controller = mediaController ?: return@LaunchedEffect
        while (true) {
            currentPosition = controller.currentPosition.coerceAtLeast(0L)
            duration = controller.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaroonDark),
        contentAlignment = Alignment.Center
    ) {
        val controller = mediaController
        if (controller == null) {
            CircularProgressIndicator(color = MutedGold)
            BackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
            return@Box
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = surah.nameArabic,
                color = MutedGold,
                fontSize = 36.sp,
                fontFamily = ArefRuqaa
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${surah.number}. ${surah.nameTransliteration}",
                color = Cream,
                fontSize = 19.sp,
                fontFamily = Cormorant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reciter.displayName,
                color = GoldSoft,
                fontSize = 15.sp,
                fontFamily = Cormorant
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaybackToggle(
                    icon = Icons.Filled.Repeat,
                    label = "Repeat",
                    active = repeatEnabled,
                    onClick = {
                        repeatEnabled = !repeatEnabled
                        if (repeatEnabled) {
                            autoNextEnabled = false
                        }
                    }
                )
                PlaybackToggle(
                    icon = Icons.Filled.SkipNext,
                    label = "Auto-Next",
                    active = autoNextEnabled,
                    onClick = {
                        autoNextEnabled = !autoNextEnabled
                        if (autoNextEnabled) {
                            repeatEnabled = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(contentAlignment = Alignment.Center) {
                IslamicStarPattern(size = 220.dp, color = MutedGold)

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MutedGold),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        if (controller.isPlaying) controller.pause() else controller.play()
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaroonDark,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (duration > 0) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { controller.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MutedGold,
                        activeTrackColor = MutedGold,
                        inactiveTrackColor = Maroon
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMillis(currentPosition), color = GoldSoft, fontSize = 12.sp, fontFamily = Cormorant)
                    Text(formatMillis(duration), color = GoldSoft, fontSize = 12.sp, fontFamily = Cormorant)
                }
            } else {
                CircularProgressIndicator(color = MutedGold, modifier = Modifier.padding(16.dp))
            }
        }

        BackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun PlaybackToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (active) MutedGold else Maroon),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) MaroonDark else GoldSoft,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) MutedGold else GoldSoft,
            fontSize = 11.sp,
            fontFamily = Cormorant
        )
    }
}

@Composable
private fun IslamicStarPattern(size: Dp, color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        drawCircle(
            color = color.copy(alpha = 0.30f),
            radius = radius,
            center = center,
            style = Stroke(width = 2f)
        )

        val squareRadius = radius * 0.82f
        for (rotationOffset in listOf(0f, 45f)) {
            val path = Path()
            for (i in 0 until 4) {
                val angle = Math.toRadians((i * 90 + rotationOffset).toDouble())
                val x = center.x + squareRadius * cos(angle).toFloat()
                val y = center.y + squareRadius * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = color.copy(alpha = 0.22f), style = Stroke(width = 2f))
        }

        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = radius * 0.5f,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
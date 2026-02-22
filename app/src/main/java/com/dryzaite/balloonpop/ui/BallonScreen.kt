package com.dryzaite.balloonpop.ui

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dryzaite.balloonpop.R
import com.dryzaite.balloonpop.data.Balloon
import com.dryzaite.balloonpop.ui.theme.BalloonPopTheme
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun BalloonGameScreen(
    gameViewModel: BalloonGameViewModel = viewModel()
) {
    val uiState by gameViewModel.uiState
    val isDarkTheme = isSystemInDarkTheme()
    val context = androidx.compose.ui.platform.LocalContext.current
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    var popSoundLoaded by remember { mutableStateOf(false) }
    val popSoundId = remember(soundPool) { soundPool.load(context, R.raw.balloon_pop, 1) }
    val hoverTransition = rememberInfiniteTransition(label = "balloon-hover")
    val hoverPhase by hoverTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hover-phase"
    )
    val currentHoverPhase by rememberUpdatedState(hoverPhase)

    DisposableEffect(soundPool, popSoundId) {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == popSoundId && status == 0) {
                popSoundLoaded = true
            }
        }
        onDispose { soundPool.release() }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            gameViewModel.setViewport(screenWidthPx, screenHeightPx)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            SkyBackdrop(
                modifier = Modifier.fillMaxSize(),
                isDarkTheme = isDarkTheme
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.balloons) {
                        detectTapGestures { tap ->
                            val tapped = uiState.balloons.lastOrNull { sprite ->
                                if (sprite.isPopping) return@lastOrNull false
                                val hoveredCenterX = sprite.balloon.centerX + hoverOffsetX(
                                    sprite.balloon,
                                    currentHoverPhase
                                )
                                isTapInsideOval(tap, sprite.balloon, hoveredCenterX)
                            }

                            if (tapped != null && gameViewModel.popBalloon(tapped.balloon.id)) {
                                if (popSoundLoaded) {
                                    soundPool.play(popSoundId, 0.72f, 0.72f, 1, 0, 1.0f)
                                }
                            }
                        }
                    }
            ) {
                uiState.balloons.forEach { sprite ->
                    val balloon = sprite.balloon
                    val hoverX = hoverOffsetX(balloon, hoverPhase)
                    val scale = 1f - (sprite.popProgress * 0.6f)
                    val alpha = 1f - sprite.popProgress
                    val width = balloon.width * scale
                    val height = balloon.height * scale
                    val centerX = balloon.centerX + hoverX
                    val centerY = balloon.centerY - (sprite.popProgress * 18f)
                    val ovalTopLeft = Offset(
                        x = centerX - width / 2f,
                        y = centerY - height / 2f
                    )

                    val knotCenter = Offset(
                        x = centerX,
                        y = centerY + height / 2f
                    )

                    drawLine(
                        color = Color(0xCC7B8794).copy(alpha = alpha),
                        start = knotCenter,
                        end = Offset(
                            x = knotCenter.x,
                            y = min(size.height - 8f, knotCenter.y + height * 0.75f)
                        ),
                        strokeWidth = 2.5f
                    )

                    drawCircle(
                        color = balloon.color.copy(alpha = 0.85f * alpha),
                        radius = 6f,
                        center = knotCenter
                    )

                    drawOval(
                        color = balloon.color.copy(alpha = alpha),
                        topLeft = ovalTopLeft,
                        size = Size(width, height)
                    )

                    drawOval(
                        color = Color.White.copy(alpha = 0.42f * alpha),
                        topLeft = Offset(
                            x = ovalTopLeft.x + width * 0.18f,
                            y = ovalTopLeft.y + height * 0.18f
                        ),
                        size = Size(width * 0.22f, height * 0.2f)
                    )

                    drawOval(
                        color = balloon.color.copy(alpha = 0.85f * alpha),
                        topLeft = ovalTopLeft,
                        size = Size(width, height),
                        style = Stroke(width = 2.6f)
                    )

                    if (sprite.isPopping) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.45f * alpha),
                            radius = max(width, height) * 0.38f + sprite.popProgress * 18f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 2.5f)
                        )
                    }
                }
            }

            Text(
                text = "Popped: ${uiState.poppedCount}/${uiState.totalCount}",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
            )

            Button(
                onClick = { gameViewModel.restoreGame() },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Restore Balloons",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            if (uiState.isInitialized && uiState.balloons.isEmpty()) {
                Text(
                    text = "All balloons popped!",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 56.dp)
                )
            }
        }
    }
}

@Composable
private fun SkyBackdrop(
    modifier: Modifier,
    isDarkTheme: Boolean
) {
    val stars = remember {
        List(42) {
            val random = Random(2026 + it)
            Star(
                xFraction = random.nextFloat(),
                yFraction = random.nextFloat() * 0.62f,
                radius = 1.2f + random.nextFloat() * 2.2f,
                alpha = 0.35f + random.nextFloat() * 0.55f
            )
        }
    }

    Canvas(modifier = modifier) {
        if (isDarkTheme) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF101625),
                        Color(0xFF1E2943),
                        Color(0xFF2B2B2B)
                    )
                )
            )

            val moonCenter = Offset(size.width * 0.83f, size.height * 0.16f)
            drawCircle(
                color = Color(0xFFF7EEC7),
                radius = size.minDimension * 0.08f,
                center = moonCenter
            )
            drawCircle(
                color = Color(0xFF1A2438),
                radius = size.minDimension * 0.07f,
                center = Offset(
                    moonCenter.x + size.minDimension * 0.03f,
                    moonCenter.y - size.minDimension * 0.015f
                )
            )

            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = star.radius,
                    center = Offset(size.width * star.xFraction, size.height * star.yFraction)
                )
            }
        } else {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE7F7FF),
                        Color(0xFFD7EEFF),
                        Color(0xFFCDE7FF)
                    )
                )
            )

            drawCircle(
                color = Color(0xFFFFE394),
                radius = size.minDimension * 0.1f,
                center = Offset(size.width * 0.86f, size.height * 0.14f)
            )

            drawCloud(center = Offset(size.width * 0.22f, size.height * 0.16f), scale = size.minDimension * 0.06f)
            drawCloud(center = Offset(size.width * 0.58f, size.height * 0.24f), scale = size.minDimension * 0.07f)
            drawCloud(center = Offset(size.width * 0.80f, size.height * 0.32f), scale = size.minDimension * 0.05f)
        }
    }
}

private fun DrawScope.drawCloud(center: Offset, scale: Float) {
    val cloudColor = Color.White.copy(alpha = 0.85f)
    val puffOffsets = listOf(
        Offset(-0.95f, 0.14f),
        Offset(-0.45f, -0.18f),
        Offset(0.12f, -0.08f),
        Offset(0.64f, 0.16f)
    )
    val puffSizes = listOf(0.52f, 0.63f, 0.56f, 0.48f)

    puffOffsets.forEachIndexed { index, puff ->
        drawCircle(
            color = cloudColor,
            radius = scale * puffSizes[index],
            center = Offset(
                x = center.x + puff.x * scale,
                y = center.y + puff.y * scale
            )
        )
    }

    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(center.x - scale * 1.15f, center.y - scale * 0.02f),
        size = Size(scale * 2.25f, scale * 0.72f),
        cornerRadius = CornerRadius(scale * 0.35f, scale * 0.35f)
    )
}

private data class Star(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val alpha: Float
)

private fun hoverOffsetX(balloon: Balloon, phase: Float): Float {
    val angle = (phase * 2f * PI).toFloat() + (balloon.id * 0.75f)
    return sin(angle) * 9f
}

private fun isTapInsideOval(tap: Offset, balloon: Balloon, centerX: Float): Boolean {
    val a = balloon.width / 2f
    val b = balloon.height / 2f
    val dx = tap.x - centerX
    val dy = tap.y - balloon.centerY
    return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
}

@Preview(showBackground = true)
@Composable
fun BalloonGameScreenPreview() {
    BalloonPopTheme {
        BalloonGameScreen()
    }
}

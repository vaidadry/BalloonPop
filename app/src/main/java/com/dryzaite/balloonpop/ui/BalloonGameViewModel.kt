package com.dryzaite.balloonpop.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dryzaite.balloonpop.data.Balloon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private const val BALLOON_COUNT = 30
private const val POP_ANIMATION_MS = 170L

data class BalloonSpriteUi(
    val balloon: Balloon,
    val isPopping: Boolean = false,
    val popProgress: Float = 0f
)

data class BalloonGameUiState(
    val balloons: List<BalloonSpriteUi> = emptyList(),
    val poppedCount: Int = 0,
    val totalCount: Int = BALLOON_COUNT,
    val isInitialized: Boolean = false
)

class BalloonGameViewModel : ViewModel() {
    private val random = Random.Default
    private val _uiState = mutableStateOf(BalloonGameUiState())
    val uiState: State<BalloonGameUiState> = _uiState

    private var viewportWidth: Float = 0f
    private var viewportHeight: Float = 0f

    fun setViewport(screenWidth: Float, screenHeight: Float) {
        if (screenWidth <= 0f || screenHeight <= 0f) return

        viewportWidth = screenWidth
        viewportHeight = screenHeight

        if (!_uiState.value.isInitialized) {
            _uiState.value = _uiState.value.copy(
                balloons = generateBalloons(
                    count = BALLOON_COUNT,
                    screenWidth = viewportWidth,
                    screenHeight = viewportHeight,
                    random = random
                ).map { balloon -> BalloonSpriteUi(balloon = balloon) },
                poppedCount = 0,
                isInitialized = true
            )
        }
    }

    fun popBalloon(balloonId: Int): Boolean {
        val beforeStart = _uiState.value
        if (beforeStart.balloons.none { it.balloon.id == balloonId && !it.isPopping }) {
            return false
        }

        _uiState.value = beforeStart.copy(
            balloons = beforeStart.balloons.map { sprite ->
                if (sprite.balloon.id == balloonId) sprite.copy(isPopping = true) else sprite
            }
        )

        viewModelScope.launch {
            val frameDelayMs = 16L
            val frames = max(1, (POP_ANIMATION_MS / frameDelayMs).toInt())
            repeat(frames) { frame ->
                val progress = (frame + 1f) / frames.toFloat()
                _uiState.value = _uiState.value.copy(
                    balloons = _uiState.value.balloons.map { sprite ->
                        if (sprite.balloon.id == balloonId) sprite.copy(popProgress = progress) else sprite
                    }
                )
                delay(frameDelayMs)
            }

            val current = _uiState.value
            if (current.balloons.none { it.balloon.id == balloonId }) return@launch

            _uiState.value = current.copy(
                balloons = current.balloons.filterNot { it.balloon.id == balloonId },
                poppedCount = current.poppedCount + 1
            )
        }

        return true
    }

    fun restoreGame() {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return

        _uiState.value = _uiState.value.copy(
            balloons = generateBalloons(
                count = BALLOON_COUNT,
                screenWidth = viewportWidth,
                screenHeight = viewportHeight,
                random = random
            ).map { balloon -> BalloonSpriteUi(balloon = balloon) },
            poppedCount = 0,
            isInitialized = true
        )
    }
}

private fun generateBalloons(
    count: Int,
    screenWidth: Float,
    screenHeight: Float,
    random: Random
): List<Balloon> {
    val balloons = mutableListOf<Balloon>()
    var nextId = 0
    var attempts = 0
    val maxAttempts = 600

    while (balloons.size < count && attempts < maxAttempts) {
        attempts++
        val width = random.nextInt(138, 166).toFloat()
        val height = width * (1.24f + random.nextFloat() * 0.12f)
        val centerX = random.nextFloat() * (screenWidth - width) + width / 2f
        val minCenterY = height / 2f + 24f
        val maxCenterY = max(minCenterY, screenHeight - height / 2f - 24f)
        val centerY = random.nextFloat() * (maxCenterY - minCenterY) + minCenterY

        val candidate = Balloon(
            id = nextId++,
            centerX = centerX,
            centerY = centerY,
            width = width,
            height = height,
            color = randomPastelColor(random)
        )

        val overlapsTooMuch = balloons.any { placed ->
            overlapRatio(candidate, placed) > 0.35f
        }

        if (!overlapsTooMuch) {
            balloons.add(candidate)
        }
    }

    return balloons
}

private fun overlapRatio(a: Balloon, b: Balloon): Float {
    val rectA = Rect(
        left = a.centerX - a.width / 2f,
        top = a.centerY - a.height / 2f,
        right = a.centerX + a.width / 2f,
        bottom = a.centerY + a.height / 2f
    )
    val rectB = Rect(
        left = b.centerX - b.width / 2f,
        top = b.centerY - b.height / 2f,
        right = b.centerX + b.width / 2f,
        bottom = b.centerY + b.height / 2f
    )

    val overlapWidth = max(0f, min(rectA.right, rectB.right) - max(rectA.left, rectB.left))
    val overlapHeight = max(0f, min(rectA.bottom, rectB.bottom) - max(rectA.top, rectB.top))
    val overlapArea = overlapWidth * overlapHeight
    val smallerArea = min(rectA.width * rectA.height, rectB.width * rectB.height)

    return if (smallerArea <= 0f) 0f else overlapArea / smallerArea
}

private fun randomPastelColor(random: Random): Color {
    val red = random.nextInt(150, 236)
    val green = random.nextInt(150, 236)
    val blue = random.nextInt(150, 236)
    return Color(red = red / 255f, green = green / 255f, blue = blue / 255f)
}

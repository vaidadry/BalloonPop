package com.dryzaite.balloonpop.data

import androidx.compose.ui.graphics.Color

data class Balloon(
    val id: Int,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val color: Color
)

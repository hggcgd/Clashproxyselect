package com.zephyruso.zashboard.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TvFocusColor = Color(0xFFFFD54F)
private val TvFocusContentColor = Color(0xFF171300)

@Composable
fun FocusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (focused) 1.06f else 1f, label = "focus-button-scale")
    val containerColor by animateColorAsState(
        targetValue = if (focused) TvFocusColor else MaterialTheme.colorScheme.primary,
        label = "focus-button-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (focused) TvFocusContentColor else MaterialTheme.colorScheme.onPrimary,
        label = "focus-button-content",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource,
        border = if (focused) BorderStroke(3.dp, Color.White) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        content()
    }
}

@Composable
fun FocusOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (focused) 1.06f else 1f, label = "focus-outlined-scale")
    val containerColor by animateColorAsState(
        targetValue = if (focused) TvFocusColor else Color.Transparent,
        label = "focus-outlined-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (focused) TvFocusContentColor else MaterialTheme.colorScheme.primary,
        label = "focus-outlined-content",
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource,
        border = BorderStroke(if (focused) 3.dp else 1.dp, if (focused) Color.White else MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        content()
    }
}

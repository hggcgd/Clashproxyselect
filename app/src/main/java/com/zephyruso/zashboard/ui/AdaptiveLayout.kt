package com.zephyruso.zashboard.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DeviceLayout {
    Phone,
    Tablet,
    Tv,
}

fun deviceLayoutFor(width: Dp, height: Dp = Dp.Unspecified, isTv: Boolean = false): DeviceLayout {
    val isPortrait = height != Dp.Unspecified && height > width
    return when {
        // TV: 通过 UiMode 明确检测
        isTv -> DeviceLayout.Tv
        // Tablet: 有触摸屏 + 宽屏 + 横屏
        width >= 600.dp && !isPortrait -> DeviceLayout.Tablet
        // Phone: 其他情况（窄屏或竖屏）
        else -> DeviceLayout.Phone
    }
}

val DeviceLayout.horizontalPadding: Dp
    get() = when (this) {
        DeviceLayout.Phone -> 16.dp
        DeviceLayout.Tablet -> 24.dp
        DeviceLayout.Tv -> 48.dp
    }

val DeviceLayout.contentMaxWidth: Dp
    get() = when (this) {
        DeviceLayout.Phone -> Dp.Unspecified
        DeviceLayout.Tablet -> 760.dp
        DeviceLayout.Tv -> 1120.dp
    }

val DeviceLayout.gridColumns: Int
    get() = when (this) {
        DeviceLayout.Phone -> 1
        DeviceLayout.Tablet -> 2
        DeviceLayout.Tv -> 3
    }

val DeviceLayout.isWide: Boolean
    get() = this != DeviceLayout.Phone

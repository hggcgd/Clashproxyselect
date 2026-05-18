package com.zephyruso.zashboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Backend(
    val protocol: String,
    val host: String,
    val port: String,
    val secondaryPath: String,
    val password: String,
    val label: String = "",
    val uuid: String = "",
    val disableUpgradeCore: Boolean = false,
)

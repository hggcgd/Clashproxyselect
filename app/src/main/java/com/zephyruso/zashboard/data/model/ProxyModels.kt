package com.zephyruso.zashboard.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(
    val version: String = "",
    val message: String? = null,
)

@Serializable
data class DelayResponse(
    val delay: Int = 0,
)

@Serializable
data class SelectProxyRequest(
    val name: String,
)

@Serializable
data class UpdateConfigsRequest(
    val path: String = "",
    val payload: String = "",
)

@Serializable
data class ProxiesResponse(
    val proxies: Map<String, Proxy> = emptyMap(),
)

@Serializable
data class ProxyProvidersResponse(
    val providers: Map<String, ProxyProvider> = emptyMap(),
)

@Serializable
data class RuleProvidersResponse(
    val providers: Map<String, RuleProvider> = emptyMap(),
)

@Serializable
data class HistoryEntry(
    val time: String = "",
    val delay: Int = 0,
)

@Serializable
data class ProxyExtra(
    val alive: Boolean = true,
    val history: List<HistoryEntry> = emptyList(),
)

@Serializable
data class Proxy(
    val name: String = "",
    val type: String = "",
    val history: List<HistoryEntry> = emptyList(),
    val extra: Map<String, ProxyExtra> = emptyMap(),
    val all: List<String>? = null,
    val udp: Boolean = false,
    val xudp: Boolean? = null,
    val now: String = "",
    val fixed: String? = null,
    val icon: String = "",
    val hidden: Boolean? = null,
    val testUrl: String? = null,
    @SerialName("dialer-proxy") val dialerProxy: String? = null,
    @SerialName("provider-name") val providerName: String? = null,
)

@Serializable
data class SubscriptionInfo(
    val Download: Long? = null,
    val Upload: Long? = null,
    val Total: Long? = null,
    val Expire: Long? = null,
)

@Serializable
data class ProxyProvider(
    val subscriptionInfo: SubscriptionInfo? = null,
    val name: String = "",
    val proxies: List<Proxy> = emptyList(),
    val testUrl: String = "",
    val updatedAt: String = "",
    val vehicleType: String = "",
)

@Serializable
data class RuleProvider(
    val behavior: String = "",
    val format: String = "",
    val name: String = "",
    val ruleCount: Int = 0,
    val type: String = "",
    val updatedAt: String = "",
    val vehicleType: String = "",
)

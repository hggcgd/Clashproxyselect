package com.zephyruso.zashboard.ui

import com.zephyruso.zashboard.data.model.Backend
import com.zephyruso.zashboard.data.model.Proxy
import com.zephyruso.zashboard.data.model.ProxyProvider
import com.zephyruso.zashboard.data.model.RuleProvider

enum class AppScreen {
    Loading,
    Setup,
    Proxies,
}

data class BackendFormState(
    val protocol: String = "http",
    val host: String = "192.168.10.1",
    val port: String = "9090",
    val secondaryPath: String = "",
    val password: String = "",
    val label: String = "",
) {
    companion object {
        fun fromBackend(backend: Backend?) = backend?.let {
            BackendFormState(
                protocol = it.protocol,
                host = it.host,
                port = it.port,
                secondaryPath = it.secondaryPath,
                password = it.password,
                label = it.label,
            )
        } ?: BackendFormState()
    }
}

data class GroupTestState(
    val groupName: String,
    val done: Int = 0,
    val total: Int = 0,
)

data class ProxyScreenState(
    val proxyMap: Map<String, Proxy> = emptyMap(),
    val groupOrder: List<String> = emptyList(),
    val providers: List<ProxyProvider> = emptyList(),
    val ruleProviders: List<RuleProvider> = emptyList(),
    val isRefreshing: Boolean = false,
    val mode: String = "",
    // 正在单独测速的节点名
    val testingNodes: Set<String> = emptySet(),
    // 正在进行的组测速及其进度，非空时组测速按钮禁用
    val groupTest: GroupTestState? = null,
)

data class AppUiState(
    val screen: AppScreen = AppScreen.Loading,
    val backend: Backend? = null,
    val setup: BackendFormState = BackendFormState(),
    val proxies: ProxyScreenState = ProxyScreenState(),
    val message: String? = null,
    // 当前进行中的设置操作标识，非空时设置页按钮禁用，防止重复触发
    val pendingAction: String? = null,
)

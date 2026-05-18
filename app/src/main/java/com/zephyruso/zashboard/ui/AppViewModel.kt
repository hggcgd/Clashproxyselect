package com.zephyruso.zashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zephyruso.zashboard.data.model.Backend
import com.zephyruso.zashboard.data.model.Proxy
import com.zephyruso.zashboard.data.repository.BackendStore
import com.zephyruso.zashboard.data.repository.ProxyCatalog
import com.zephyruso.zashboard.data.repository.ProxyRepository
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException

private const val TEST_URL = "https://www.gstatic.com/generate_204"
private const val GLOBAL_GROUP = "GLOBAL"
private const val LOAD_BALANCE = "loadbalance"
private const val SELECTOR = "selector"
private const val SMART = "smart"

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val backendStore = BackendStore(application.applicationContext)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    private var initialBackendLoaded = false
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            backendStore.backendFlow.collect { backend ->
                _uiState.update { current ->
                    val nextScreen = when {
                        !initialBackendLoaded -> if (backend == null) AppScreen.Setup else AppScreen.Proxies
                        backend == null -> AppScreen.Setup
                        else -> current.screen
                    }
                    current.copy(
                        backend = backend,
                        screen = nextScreen,
                        setup = if (backend == null && current.screen != AppScreen.Loading) {
                            BackendFormState()
                        } else current.setup,
                    )
                }
                if (!initialBackendLoaded) {
                    initialBackendLoaded = true
                }
                if (backend != null && _uiState.value.screen == AppScreen.Proxies) {
                    refreshCatalog()
                }
            }
        }

        viewModelScope.launch {
            val backend = backendStore.backendFlow.first()
            if (backend == null) {
                _uiState.update { it.copy(screen = AppScreen.Setup, setup = BackendFormState()) }
            }
        }
    }

    fun onOpenSetup() {
        _uiState.update { current ->
            current.copy(
                screen = AppScreen.Setup,
                setup = BackendFormState.fromBackend(current.backend),
                message = null,
            )
        }
    }

    fun onBackToProxies() {
        if (_uiState.value.backend != null) {
            _uiState.update { it.copy(screen = AppScreen.Proxies) }
        }
    }

    fun onSetupChanged(form: BackendFormState) {
        _uiState.update { it.copy(setup = form, message = null) }
    }

    fun onSaveBackend() {
        val form = _uiState.value.setup
        val backend = form.toBackend()

        if (backend == null) {
            _uiState.update { it.copy(message = "请先填写完整的后端信息") }
            return
        }

        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).validateBackend()
            }.onSuccess {
                backendStore.save(backend)
                _uiState.update {
                    it.copy(
                        backend = backend,
                        screen = AppScreen.Proxies,
                        message = "后端连接成功",
                    )
                }
                refreshCatalog(backend)
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("后端连接失败")) }
            }
        }
    }

    fun onClearBackend() {
        viewModelScope.launch {
            backendStore.clear()
            _uiState.update {
                it.copy(
                    backend = null,
                    screen = AppScreen.Setup,
                    setup = BackendFormState(),
                    proxies = ProxyScreenState(),
                    message = "已清除后端",
                )
            }
        }
    }

    fun onRefreshCatalog() {
        refreshCatalog()
    }

    fun onSelectProxy(groupName: String, proxyName: String) {
        val backend = _uiState.value.backend ?: return
        val repository = ProxyRepository(backend)

        viewModelScope.launch {
            val group = _uiState.value.proxies.proxyMap[groupName] ?: return@launch
            if (!group.isSelectableGroup()) {
                return@launch
            }

            if (group.now == proxyName) {
                refreshCatalog(backend)
                if (_uiState.value.proxies.proxyMap[groupName]?.now == proxyName) {
                    return@launch
                }
            }

            runCatching {
                repository.selectProxy(groupName, proxyName)
            }.onSuccess {
                _uiState.update { current ->
                    val proxyMap = current.proxies.proxyMap.toMutableMap()
                    proxyMap[groupName] = group.copy(now = proxyName)
                    current.copy(
                        proxies = current.proxies.copy(proxyMap = proxyMap),
                        message = "已切换到 $proxyName",
                    )
                }
                refreshCatalog(backend)
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("切换失败")) }
            }
        }
    }

    fun onTestProxyLatency(groupName: String, proxyName: String) {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            val repository = ProxyRepository(backend)
            runCatching {
                repository.testProxyLatency(proxyName, TEST_URL)
            }.onSuccess { delay ->
                _uiState.update { current ->
                    val proxyMap = current.proxies.proxyMap.toMutableMap()
                    val target = proxyMap[proxyName]
                    if (target != null) {
                        proxyMap[proxyName] = target.withLatestDelay(delay)
                    }
                    current.copy(
                        proxies = current.proxies.copy(proxyMap = proxyMap),
                        message = "$groupName / $proxyName 延迟 ${delay}ms",
                    )
                }
                refreshCatalog(backend)
            }.onFailure { error ->
                _uiState.update { current ->
                    val proxyMap = current.proxies.proxyMap.toMutableMap()
                    proxyMap[proxyName]?.let { target ->
                        proxyMap[proxyName] = target.withInvalidDelay()
                    }
                    current.copy(
                        proxies = current.proxies.copy(proxyMap = proxyMap),
                        message = "$groupName / $proxyName：${error.toUserMessage("测速失败")}",
                    )
                }
            }
        }
    }

    fun onTestGroupLatency(groupName: String) {
        val backend = _uiState.value.backend ?: return
        val group = _uiState.value.proxies.proxyMap[groupName] ?: return
        val repository = ProxyRepository(backend)

        viewModelScope.launch {
            val testByNodes = group.isLatencyTestByNodes()
            runCatching {
                if (testByNodes) {
                    testGroupLatencyByNodes(groupName, group, repository)
                } else {
                    repository.testGroupLatency(groupName, TEST_URL)
                }
            }.onSuccess {
                if (!testByNodes) {
                    refreshCatalog(backend)
                    _uiState.update { it.copy(message = "$groupName 测速完成") }
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    val proxyMap = current.proxies.proxyMap.toMutableMap()
                    group.all.orEmpty().forEach { nodeName ->
                        proxyMap[nodeName]?.let { node ->
                            proxyMap[nodeName] = node.withInvalidDelay()
                        }
                    }
                    current.copy(
                        proxies = current.proxies.copy(proxyMap = proxyMap),
                        message = "$groupName：${error.toUserMessage("组测速失败")}",
                    )
                }
            }
        }
    }

    private suspend fun testGroupLatencyByNodes(
        groupName: String,
        group: Proxy,
        repository: ProxyRepository,
    ) {
        val nodes = group.all.orEmpty()
        val total = nodes.size
        val completed = AtomicInteger(0)
        val failed = AtomicInteger(0)

        if (total == 0) {
            _uiState.update { it.copy(message = "$groupName 没有可测速节点") }
            return
        }

        coroutineScope {
            val semaphore = Semaphore(5)
            nodes.map { nodeName ->
                launch {
                    semaphore.withPermit {
                        val result = try {
                            Result.success(repository.testProxyLatency(nodeName, TEST_URL))
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Result.failure(error)
                        }

                        val done = completed.incrementAndGet()
                        result.onSuccess { delay ->
                            _uiState.update { current ->
                                val proxyMap = current.proxies.proxyMap.toMutableMap()
                                proxyMap[nodeName]?.let { node ->
                                    proxyMap[nodeName] = node.withLatestDelay(delay)
                                }
                                current.copy(
                                    proxies = current.proxies.copy(proxyMap = proxyMap),
                                    message = "$groupName $done/$total 测试完成",
                                )
                            }
                        }.onFailure { error ->
                            failed.incrementAndGet()
                            _uiState.update { current ->
                                val proxyMap = current.proxies.proxyMap.toMutableMap()
                                proxyMap[nodeName]?.let { node ->
                                    proxyMap[nodeName] = node.withInvalidDelay()
                                }
                                current.copy(
                                    proxies = current.proxies.copy(proxyMap = proxyMap),
                                    message = "$groupName $done/$total 测试完成，$nodeName：${error.toUserMessage("测速失败")}",
                                )
                            }
                        }
                    }
                }
            }.forEach { it.join() }
        }

        val failedCount = failed.get()
        val successCount = total - failedCount
        _uiState.update {
            it.copy(
                message = if (failedCount > 0) {
                    "$groupName 测速完成：成功 $successCount/$total，失败 $failedCount"
                } else {
                    "$groupName 测速完成：成功 $successCount/$total"
                },
            )
        }
    }

    fun onProviderHealthCheck(name: String) {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).healthCheckProvider(name)
            }.onSuccess {
                refreshCatalog(backend)
                _uiState.update { it.copy(message = "$name 健康检查完成") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("健康检查失败")) }
            }
        }
    }

    fun onUpdateProvider(name: String) {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).updateProvider(name)
            }.onSuccess {
                refreshCatalog(backend)
                _uiState.update { it.copy(message = "$name 已更新") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("更新失败")) }
            }
        }
    }

    fun onUpdateAllProviders() {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).updateAllProviders()
            }.onSuccess {
                refreshCatalog(backend)
                _uiState.update { it.copy(message = "Providers 已更新") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("更新 Providers 失败")) }
            }
        }
    }

    fun onUpdateRuleProviders() {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).updateAllRuleProviders()
            }.onSuccess {
                refreshCatalog(backend)
                _uiState.update { it.copy(message = "规则 Provider 已更新") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("更新规则失败")) }
            }
        }
    }

    fun onRestartCore() {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).restartCore()
            }.onSuccess {
                _uiState.update { it.copy(message = "核心重启请求已发送") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("重启核心失败")) }
            }
        }
    }

    fun onReloadConfigs() {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).reloadConfigs()
            }.onSuccess {
                refreshCatalog(backend)
                _uiState.update { it.copy(message = "配置已重载") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("重载配置失败")) }
            }
        }
    }

    fun onSelectMode(mode: String) {
        val backend = _uiState.value.backend ?: return
        val label = when (mode) {
            "rule" -> "规则"
            "direct" -> "直连"
            "global" -> "全局"
            else -> mode
        }
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).selectMode(mode)
            }.onSuccess {
                refreshCatalog(backend)
                _uiState.update { it.copy(message = "已切换到${label}模式") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("切换模式失败")) }
            }
        }
    }

    fun onUpdateGeoData() {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).updateGeoData()
            }.onSuccess {
                _uiState.update { it.copy(message = "GEO 更新请求已发送") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("更新 GEO 失败")) }
            }
        }
    }

    fun onFlushDnsCache() {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).flushDnsCache()
            }.onSuccess {
                _uiState.update { it.copy(message = "DNS 缓存已清空") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("清空 DNS 缓存失败")) }
            }
        }
    }

    fun onFlushFakeIp() {
        val backend = _uiState.value.backend ?: return
        viewModelScope.launch {
            runCatching {
                ProxyRepository(backend).flushFakeIp()
            }.onSuccess {
                _uiState.update { it.copy(message = "FAKE IP 已清空") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("清空 FAKE IP 失败")) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshCatalog(backend: Backend? = _uiState.value.backend) {
        if (backend == null) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(proxies = it.proxies.copy(isRefreshing = true)) }
            runCatching {
                ProxyRepository(backend).loadCatalog()
            }.onSuccess { catalog ->
                applyCatalog(catalog)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        proxies = it.proxies.copy(isRefreshing = false),
                        message = error.toUserMessage("加载代理列表失败"),
                    )
                }
            }
        }
    }

    private fun applyCatalog(catalog: ProxyCatalog) {
        _uiState.update { current ->
            val proxyMap = catalog.proxyMap.mapValues { (name, proxy) ->
                proxy.withPreservedLocalDelay(current.proxies.proxyMap[name])
            }
            current.copy(
                proxies = current.proxies.copy(
                    proxyMap = proxyMap,
                    groupOrder = catalog.groupOrder,
                    providers = catalog.providers,
                    ruleProviders = catalog.ruleProviders,
                    isRefreshing = false,
                ),
            )
        }
    }

    private fun BackendFormState.toBackend(): Backend? {
        val portNumber = port.toIntOrNull() ?: return null
        if (protocol.isBlank() || host.isBlank()) return null
        if (portNumber !in 1..65535) return null

        return Backend(
            protocol = protocol.trim(),
            host = host.trim(),
            port = portNumber.toString(),
            secondaryPath = secondaryPath.trim(),
            password = password,
            label = label.trim(),
        )
    }

    private fun Proxy.isSelectableGroup(): Boolean {
        return !type.equals(LOAD_BALANCE, ignoreCase = true)
    }

    private fun Proxy.isLatencyTestByNodes(): Boolean {
        return type.equals(SELECTOR, ignoreCase = true) ||
            type.equals(LOAD_BALANCE, ignoreCase = true) ||
            type.equals(SMART, ignoreCase = true)
    }

    private fun Proxy.withLatestDelay(delay: Int): Proxy {
        val history = history + com.zephyruso.zashboard.data.model.HistoryEntry(delay = delay)
        return copy(history = history)
    }

    private fun Proxy.withInvalidDelay(): Proxy {
        val history = history + com.zephyruso.zashboard.data.model.HistoryEntry(delay = -1)
        return copy(history = history)
    }

    private fun Proxy.withPreservedLocalDelay(previous: Proxy?): Proxy {
        if (previous == null || previous.history.isEmpty()) return this

        val incomingDelay = history.lastOrNull()?.delay ?: 0
        val localDelay = previous.history.lastOrNull()?.delay ?: 0

        return if (incomingDelay == 0 && localDelay != 0) {
            copy(history = previous.history)
        } else {
            this
        }
    }

    private fun Throwable.toUserMessage(defaultMessage: String): String {
        return when (this) {
            is HttpException -> when (code()) {
                401 -> "未认证：请检查密码是否正确"
                403 -> "禁止连接：当前后端拒绝访问"
                500 -> "服务器错误：后端处理请求失败"
                502 -> "网关错误：上游服务返回异常"
                503 -> "服务不可用：后端暂时无法访问"
                504 -> "网关超时：后端响应超时"
                else -> message().takeIf { it.isNotBlank() } ?: defaultMessage
            }
            is SocketTimeoutException -> "超时：后端响应太慢或网络不稳定"
            is InterruptedIOException -> {
                if (message?.contains("timeout", ignoreCase = true) == true) {
                    "超时：后端响应太慢或网络不稳定"
                } else {
                    message?.takeIf { it.isNotBlank() } ?: defaultMessage
                }
            }
            is UnknownHostException -> "无法解析地址：请检查主机名"
            is ConnectException -> "连接失败：目标地址无法连接"
            else -> classifyMessage(message).takeIf { it.isNotBlank() } ?: defaultMessage
        }
    }

    private fun classifyMessage(rawMessage: String?): String {
        val message = rawMessage?.trim().orEmpty()
        val lower = message.lowercase()
        return when {
            lower.startsWith("server") -> "服务器错误：后端返回异常"
            lower.startsWith("gateway") -> "网关错误：中间代理返回异常"
            lower.contains("bad gateway") -> "网关错误：中间代理返回异常"
            lower.contains("gateway timeout") -> "网关超时：后端响应超时"
            lower.contains("server error") -> "服务器错误：后端处理请求失败"
            else -> message
        }
    }
}

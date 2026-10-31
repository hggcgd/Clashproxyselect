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
        if (proxyName in _uiState.value.proxies.testingNodes) return
        viewModelScope.launch {
            val repository = ProxyRepository(backend)
            _uiState.update { current ->
                current.copy(
                    proxies = current.proxies.copy(
                        testingNodes = current.proxies.testingNodes + proxyName,
                    ),
                )
            }
            runCatching {
                repository.testProxyLatency(proxyName, TEST_URL)
            }.onSuccess { delay ->
                _uiState.update { current ->
                    val proxyMap = current.proxies.proxyMap.toMutableMap()
                    proxyMap[proxyName]?.let { target ->
                        proxyMap[proxyName] = target.withLatestDelay(delay)
                    }
                    current.copy(
                        proxies = current.proxies.copy(
                            proxyMap = proxyMap,
                            testingNodes = current.proxies.testingNodes - proxyName,
                        ),
                        message = "$groupName / $proxyName 延迟 ${delay}ms",
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    val proxyMap = current.proxies.proxyMap.toMutableMap()
                    proxyMap[proxyName]?.let { target ->
                        proxyMap[proxyName] = target.withInvalidDelay()
                    }
                    current.copy(
                        proxies = current.proxies.copy(
                            proxyMap = proxyMap,
                            testingNodes = current.proxies.testingNodes - proxyName,
                        ),
                        message = "$groupName / $proxyName：${error.toUserMessage("测速失败")}",
                    )
                }
            }
        }
    }

    fun onTestGroupLatency(groupName: String) {
        val backend = _uiState.value.backend ?: return
        val group = _uiState.value.proxies.proxyMap[groupName] ?: return
        if (_uiState.value.proxies.groupTest != null) return

        val nodes = group.all.orEmpty()
        if (nodes.isEmpty()) {
            _uiState.update { it.copy(message = "$groupName 没有可测速节点") }
            return
        }

        val repository = ProxyRepository(backend)
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    proxies = current.proxies.copy(
                        groupTest = GroupTestState(groupName = groupName, total = nodes.size),
                    ),
                )
            }

            val failedCount = runCatching {
                if (group.isLatencyTestByNodes()) {
                    testGroupLatencyByNodes(group, repository)
                } else {
                    val delays = repository.testGroupLatency(groupName, TEST_URL)
                    refreshCatalog(backend)
                    nodes.size - delays.values.count { it > 0 }
                }
            }.getOrElse { error ->
                _uiState.update { it.copy(message = "$groupName：${error.toUserMessage("组测速失败")}") }
                -1
            }

            if (failedCount >= 0) {
                val successCount = nodes.size - failedCount
                _uiState.update {
                    it.copy(
                        proxies = it.proxies.copy(groupTest = null),
                        message = if (failedCount > 0) {
                            "$groupName 测速完成：成功 $successCount/${nodes.size}，失败 $failedCount"
                        } else {
                            "$groupName 测速完成：成功 $successCount/${nodes.size}"
                        },
                    )
                }
            } else {
                _uiState.update { it.copy(proxies = it.proxies.copy(groupTest = null)) }
            }
        }
    }

    private suspend fun testGroupLatencyByNodes(
        group: Proxy,
        repository: ProxyRepository,
    ): Int {
        val nodes = group.all.orEmpty()
        val completed = AtomicInteger(0)
        val failed = AtomicInteger(0)

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
                        val failedNode = result.isFailure
                        if (failedNode) {
                            failed.incrementAndGet()
                        }
                        _uiState.update { current ->
                            val proxyMap = current.proxies.proxyMap.toMutableMap()
                            proxyMap[nodeName]?.let { node ->
                                // 进度实时反映在按钮上，中间过程不再刷 Snackbar 提示
                                proxyMap[nodeName] = result.fold(
                                    onSuccess = { node.withLatestDelay(it) },
                                    onFailure = { node.withInvalidDelay() },
                                )
                            }
                            current.copy(
                                proxies = current.proxies.copy(
                                    proxyMap = proxyMap,
                                    groupTest = current.proxies.groupTest?.copy(done = done),
                                ),
                            )
                        }
                    }
                }
            }.forEach { it.join() }
        }

        return failed.get()
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
        launchBackendAction(
            actionKey = "update-providers",
            successMessage = "Providers 已更新",
            failureMessage = "更新 Providers 失败",
            refreshOnSuccess = true,
            action = { it.updateAllProviders() },
        )
    }

    fun onUpdateRuleProviders() {
        launchBackendAction(
            actionKey = "update-rules",
            successMessage = "规则 Provider 已更新",
            failureMessage = "更新规则失败",
            refreshOnSuccess = true,
            action = { it.updateAllRuleProviders() },
        )
    }

    fun onRestartCore() {
        launchBackendAction(
            actionKey = "restart-core",
            successMessage = "核心重启请求已发送",
            failureMessage = "重启核心失败",
            action = { it.restartCore() },
        )
    }

    fun onReloadConfigs() {
        launchBackendAction(
            actionKey = "reload-configs",
            successMessage = "配置已重载",
            failureMessage = "重载配置失败",
            refreshOnSuccess = true,
            action = { it.reloadConfigs() },
        )
    }

    fun onSelectMode(mode: String) {
        val backend = _uiState.value.backend ?: return
        if (_uiState.value.pendingAction != null) return
        val label = when (mode) {
            "rule" -> "规则"
            "direct" -> "直连"
            "global" -> "全局"
            else -> mode
        }
        viewModelScope.launch {
            _uiState.update { it.copy(pendingAction = "select-mode") }
            runCatching {
                ProxyRepository(backend).selectMode(mode)
            }.onSuccess {
                // 先乐观更新本地模式，随后 refreshCatalog 以服务端返回为准
                _uiState.update { current ->
                    current.copy(
                        proxies = current.proxies.copy(mode = mode),
                        message = "已切换到${label}模式",
                    )
                }
                refreshCatalog(backend)
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage("切换模式失败")) }
            }
            _uiState.update { it.copy(pendingAction = null) }
        }
    }

    fun onUpdateGeoData() {
        launchBackendAction(
            actionKey = "update-geo",
            successMessage = "GEO 更新请求已发送",
            failureMessage = "更新 GEO 失败",
            action = { it.updateGeoData() },
        )
    }

    fun onFlushDnsCache() {
        launchBackendAction(
            actionKey = "flush-dns",
            successMessage = "DNS 缓存已清空",
            failureMessage = "清空 DNS 缓存失败",
            action = { it.flushDnsCache() },
        )
    }

    fun onFlushFakeIp() {
        launchBackendAction(
            actionKey = "flush-fakeip",
            successMessage = "FAKE IP 已清空",
            failureMessage = "清空 FAKE IP 失败",
            action = { it.flushFakeIp() },
        )
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * 设置页操作的统一入口：操作期间置 pendingAction 禁用其他设置按钮，
     * 防止遥控器连按导致的重复请求与并发竞态。
     */
    private fun launchBackendAction(
        actionKey: String,
        successMessage: String,
        failureMessage: String,
        refreshOnSuccess: Boolean = false,
        action: suspend (ProxyRepository) -> Unit,
    ) {
        val backend = _uiState.value.backend ?: return
        if (_uiState.value.pendingAction != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(pendingAction = actionKey) }
            val repository = ProxyRepository(backend)
            runCatching {
                action(repository)
            }.onSuccess {
                if (refreshOnSuccess) {
                    refreshCatalog(backend)
                }
                _uiState.update { it.copy(message = successMessage) }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.toUserMessage(failureMessage)) }
            }
            _uiState.update { it.copy(pendingAction = null) }
        }
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
                    mode = catalog.mode,
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

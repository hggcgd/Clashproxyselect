package com.zephyruso.zashboard.data.repository

import com.zephyruso.zashboard.data.model.Backend
import com.zephyruso.zashboard.data.model.Proxy
import com.zephyruso.zashboard.data.model.ProxyProvider
import com.zephyruso.zashboard.data.model.RuleProvider
import com.zephyruso.zashboard.data.model.SelectProxyRequest
import com.zephyruso.zashboard.data.network.ApiClientFactory
import kotlinx.coroutines.awaitAll
import com.zephyruso.zashboard.data.network.ClashApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val GLOBAL_GROUP = "GLOBAL"
private const val COMPATIBLE_PROVIDER = "Compatible"

class ProxyRepository(
    private val backend: Backend,
) {
    private val service: ClashApiService = ApiClientFactory.create(backend)

    suspend fun validateBackend(): String {
        return service.fetchVersion().version
    }

    suspend fun loadCatalog(): ProxyCatalog {
        return coroutineScope {
            val proxiesDeferred = async { service.fetchProxies() }
            val providersDeferred = async { service.fetchProxyProviders() }
            val ruleProvidersDeferred = async { service.fetchRuleProviders() }

            val proxies = proxiesDeferred.await().proxies
            val providers = providersDeferred.await().providers.values
                .filter { it.name != "default" && it.vehicleType != COMPATIBLE_PROVIDER }
                .toList()
            val ruleProviders = ruleProvidersDeferred.await().providers.values.toList()

            val providerNodes = providers.flatMap { it.proxies }
            val merged = buildMap {
                providerNodes.forEach { put(it.name, it) }
                proxies.forEach { (name, proxy) -> put(name, proxy) }
            }

            val sortIndex = proxies[GLOBAL_GROUP]?.all.orEmpty()
            val groupOrder = proxies.values
                .filter { it.name != GLOBAL_GROUP && !it.all.isNullOrEmpty() }
                .sortedWith { left, right ->
                    val leftIndex = sortIndex.indexOf(left.name)
                    val rightIndex = sortIndex.indexOf(right.name)
                    when {
                        leftIndex == -1 && rightIndex == -1 -> 0
                        leftIndex == -1 -> 1
                        rightIndex == -1 -> -1
                        else -> leftIndex.compareTo(rightIndex)
                    }
                }
                .map { it.name }

            ProxyCatalog(
                proxyMap = merged,
                groupOrder = groupOrder,
                providers = providers,
                ruleProviders = ruleProviders,
            )
        }
    }

    suspend fun selectProxy(groupName: String, proxyName: String) {
        service.selectProxy(groupName, SelectProxyRequest(name = proxyName))
    }

    suspend fun testProxyLatency(proxyName: String, url: String): Int {
        return service.fetchProxyDelay(proxyName, url).delay
    }

    suspend fun testGroupLatency(groupName: String, url: String): Map<String, Int> {
        return service.fetchGroupDelay(groupName, url)
    }

    suspend fun healthCheckProvider(name: String): Map<String, Int> {
        return service.healthCheckProxyProvider(name)
    }

    suspend fun updateProvider(name: String) {
        service.updateProxyProvider(name)
    }

    suspend fun updateAllProviders() {
        coroutineScope {
            service.fetchProxyProviders().providers.values
                .filter { it.name != "default" && it.vehicleType != COMPATIBLE_PROVIDER }
                .map { provider -> async { service.updateProxyProvider(provider.name) } }
                .awaitAll()
        }
    }

    suspend fun updateAllRuleProviders() {
        coroutineScope {
            service.fetchRuleProviders().providers.values
                .map { provider -> async { service.updateRuleProvider(provider.name) } }
                .awaitAll()
        }
    }

    suspend fun restartCore() {
        service.restartCore()
    }

    suspend fun reloadConfigs() {
        service.reloadConfigs()
    }

    suspend fun selectMode(mode: String) {
        service.patchConfigs(mapOf("mode" to mode))
    }

    suspend fun updateGeoData() {
        service.updateGeoData()
    }

    suspend fun flushDnsCache() {
        service.flushDnsCache()
    }

    suspend fun flushFakeIp() {
        service.flushFakeIp()
    }
}

data class ProxyCatalog(
    val proxyMap: Map<String, Proxy>,
    val groupOrder: List<String>,
    val providers: List<ProxyProvider>,
    val ruleProviders: List<RuleProvider>,
)

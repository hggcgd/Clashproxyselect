package com.zephyruso.zashboard.data.network

import com.zephyruso.zashboard.data.model.DelayResponse
import com.zephyruso.zashboard.data.model.ProxyProvidersResponse
import com.zephyruso.zashboard.data.model.ProxiesResponse
import com.zephyruso.zashboard.data.model.RuleProvidersResponse
import com.zephyruso.zashboard.data.model.SelectProxyRequest
import com.zephyruso.zashboard.data.model.UpdateConfigsRequest
import com.zephyruso.zashboard.data.model.VersionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface ClashApiService {
    @GET("version")
    suspend fun fetchVersion(): VersionResponse

    @GET("proxies")
    suspend fun fetchProxies(): ProxiesResponse

    @PUT("proxies/{group}")
    suspend fun selectProxy(
        @Path("group") group: String,
        @Body body: SelectProxyRequest,
    )

    @GET("proxies/{name}/delay")
    suspend fun fetchProxyDelay(
        @Path("name") name: String,
        @Query("url") url: String,
        @Query("timeout") timeout: Int = 5000,
    ): DelayResponse

    @GET("group/{name}/delay")
    suspend fun fetchGroupDelay(
        @Path("name") name: String,
        @Query("url") url: String,
        @Query("timeout") timeout: Int = 5000,
    ): Map<String, Int>

    @GET("providers/proxies")
    suspend fun fetchProxyProviders(): ProxyProvidersResponse

    @PUT("providers/proxies/{name}")
    suspend fun updateProxyProvider(
        @Path("name") name: String,
    )

    @GET("providers/proxies/{name}/healthcheck")
    suspend fun healthCheckProxyProvider(
        @Path("name") name: String,
        @Query("timeout") timeout: Int = 15000,
    ): Map<String, Int>

    @GET("providers/rules")
    suspend fun fetchRuleProviders(): RuleProvidersResponse

    @PUT("providers/rules/{name}")
    suspend fun updateRuleProvider(
        @Path("name") name: String,
    )

    @POST("restart")
    suspend fun restartCore()

    @PUT("configs")
    suspend fun reloadConfigs(
        @Query("reload") reload: Boolean = true,
        @Body body: UpdateConfigsRequest = UpdateConfigsRequest(),
    )

    @PATCH("configs")
    suspend fun patchConfigs(
        @Body configs: Map<String, String>,
    )

    @POST("configs/geo")
    suspend fun updateGeoData()

    @POST("cache/dns/flush")
    suspend fun flushDnsCache()

    @POST("cache/fakeip/flush")
    suspend fun flushFakeIp()
}

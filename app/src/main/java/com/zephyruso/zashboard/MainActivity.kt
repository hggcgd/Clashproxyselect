package com.zephyruso.zashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.UiModeManager
import android.content.res.Configuration
import com.zephyruso.zashboard.ui.AppScreen
import com.zephyruso.zashboard.ui.AppViewModel
import com.zephyruso.zashboard.ui.deviceLayoutFor
import com.zephyruso.zashboard.ui.screens.ProxiesScreen
import com.zephyruso.zashboard.ui.screens.SetupScreen
import com.zephyruso.zashboard.ui.theme.ZashboardTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZashboardTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                Surface(color = MaterialTheme.colorScheme.background) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
                        val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
                        val layout = deviceLayoutFor(maxWidth, maxHeight, isTv)
                        when (uiState.screen) {
                            AppScreen.Loading -> LoadingScreen()
                            AppScreen.Setup -> SetupScreen(
                                layout = layout,
                                state = uiState.setup,
                                onStateChange = viewModel::onSetupChanged,
                                onSave = viewModel::onSaveBackend,
                                onClear = viewModel::onClearBackend,
                                onOpenProxies = viewModel::onBackToProxies,
                                message = uiState.message,
                                onMessageShown = viewModel::clearMessage,
                            )
                            AppScreen.Proxies -> ProxiesScreen(
                                layout = layout,
                                backend = uiState.backend,
                                state = uiState.proxies,
                                onRefresh = viewModel::onRefreshCatalog,
                                onOpenSetup = viewModel::onOpenSetup,
                                onSelectProxy = viewModel::onSelectProxy,
                                onTestProxyLatency = viewModel::onTestProxyLatency,
                                onTestGroupLatency = viewModel::onTestGroupLatency,
                                onUpdateAllProviders = viewModel::onUpdateAllProviders,
                                onUpdateRuleProviders = viewModel::onUpdateRuleProviders,
                                onRestartCore = viewModel::onRestartCore,
                                onReloadConfigs = viewModel::onReloadConfigs,
                                onSelectMode = viewModel::onSelectMode,
                                onUpdateGeoData = viewModel::onUpdateGeoData,
                                onFlushDnsCache = viewModel::onFlushDnsCache,
                                onFlushFakeIp = viewModel::onFlushFakeIp,
                                message = uiState.message,
                                onMessageShown = viewModel::clearMessage,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

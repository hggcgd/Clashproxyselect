@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zephyruso.zashboard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zephyruso.zashboard.data.model.Backend
import com.zephyruso.zashboard.data.model.Proxy
import com.zephyruso.zashboard.data.model.ProxyProvider
import com.zephyruso.zashboard.data.model.RuleProvider
import com.zephyruso.zashboard.ui.DeviceLayout
import com.zephyruso.zashboard.ui.ProxyScreenState
import com.zephyruso.zashboard.ui.contentMaxWidth
import com.zephyruso.zashboard.ui.gridColumns
import com.zephyruso.zashboard.ui.horizontalPadding
import com.zephyruso.zashboard.ui.isWide

@Composable
fun ProxiesScreen(
    layout: DeviceLayout,
    backend: Backend?,
    state: ProxyScreenState,
    onRefresh: () -> Unit,
    onOpenSetup: () -> Unit,
    onSelectProxy: (String, String) -> Unit,
    onTestProxyLatency: (String, String) -> Unit,
    onTestGroupLatency: (String) -> Unit,
    onUpdateAllProviders: () -> Unit,
    onUpdateRuleProviders: () -> Unit,
    onRestartCore: () -> Unit,
    onReloadConfigs: () -> Unit,
    onSelectMode: (String) -> Unit,
    onUpdateGeoData: () -> Unit,
    onFlushDnsCache: () -> Unit,
    onFlushFakeIp: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var tabIndex by remember { mutableIntStateOf(0) }
    var selectedGroupName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    LaunchedEffect(state.groupOrder, layout) {
        if (selectedGroupName != null && selectedGroupName !in state.groupOrder) {
            selectedGroupName = null
        }
        if (layout.isWide && selectedGroupName == null) {
            selectedGroupName = state.groupOrder.firstOrNull()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selectedGroupName ?: "节点选择")
                        Text(
                            text = backend?.let { backendLabel(it) } ?: "未连接后端",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    FocusOutlinedButton(onClick = onOpenSetup, modifier = Modifier.padding(end = 8.dp)) {
                        Text("后端")
                    }
                    FocusButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                        } else {
                            Text("刷新")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val contentVerticalPadding = if (layout.isWide) 24.dp else 16.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .adaptiveContentWidth(layout.contentMaxWidth)
                    .padding(
                        horizontal = layout.horizontalPadding,
                        vertical = contentVerticalPadding,
                    ),
            ) {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("代理组") },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("设置") },
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (tabIndex) {
                        0 -> ProxyGroupsTab(
                            layout = layout,
                            state = state,
                            selectedGroupName = selectedGroupName,
                            onGroupSelected = { selectedGroupName = it },
                            onBackToGroups = { selectedGroupName = null },
                            onSelectProxy = onSelectProxy,
                            onTestProxyLatency = onTestProxyLatency,
                            onTestGroupLatency = onTestGroupLatency,
                        )
                        else -> SettingsTab(
                            layout = layout,
                            providers = state.providers,
                            ruleProviders = state.ruleProviders,
                            onUpdateAllProviders = onUpdateAllProviders,
                            onUpdateRuleProviders = onUpdateRuleProviders,
                            onRestartCore = onRestartCore,
                            onReloadConfigs = onReloadConfigs,
                            onSelectMode = onSelectMode,
                            onUpdateGeoData = onUpdateGeoData,
                            onFlushDnsCache = onFlushDnsCache,
                            onFlushFakeIp = onFlushFakeIp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyGroupsTab(
    layout: DeviceLayout,
    state: ProxyScreenState,
    selectedGroupName: String?,
    onGroupSelected: (String) -> Unit,
    onBackToGroups: () -> Unit,
    onSelectProxy: (String, String) -> Unit,
    onTestProxyLatency: (String, String) -> Unit,
    onTestGroupLatency: (String) -> Unit,
) {
    val selectedGroup = selectedGroupName?.let { state.proxyMap[it] }

    if (layout.isWide) {
        WideProxyGroupsTab(
            layout = layout,
            state = state,
            selectedGroup = selectedGroup,
            onGroupSelected = onGroupSelected,
            onSelectProxy = onSelectProxy,
            onTestProxyLatency = onTestProxyLatency,
            onTestGroupLatency = onTestGroupLatency,
        )
        return
    }

    if (selectedGroup != null) {
        ProxyNodesList(
            group = selectedGroup,
            nodes = selectedGroup.all.orEmpty().mapNotNull { state.proxyMap[it] },
            onBackToGroups = onBackToGroups,
            onSelectProxy = onSelectProxy,
            onTestProxyLatency = onTestProxyLatency,
            onTestGroupLatency = onTestGroupLatency,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(layout.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.groupOrder) { groupName ->
            val group = state.proxyMap[groupName] ?: return@items
            ProxyGroupItem(
                layout = layout,
                group = group,
                nodeCount = group.all.orEmpty().size,
                selected = selectedGroupName == group.name,
                onClick = { onGroupSelected(group.name) },
                onTestLatency = { onTestGroupLatency(group.name) },
            )
        }
    }
}

@Composable
private fun WideProxyGroupsTab(
    layout: DeviceLayout,
    state: ProxyScreenState,
    selectedGroup: Proxy?,
    onGroupSelected: (String) -> Unit,
    onSelectProxy: (String, String) -> Unit,
    onTestProxyLatency: (String, String) -> Unit,
    onTestGroupLatency: (String) -> Unit,
) {
    val groupListSafePadding = if (layout == DeviceLayout.Tv) 40.dp else 6.dp

    Row(
        modifier = Modifier
            .fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(if (layout == DeviceLayout.Tv) 20.dp else 16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .padding(vertical = groupListSafePadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (layout == DeviceLayout.Tv) 12.dp else 0.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.groupOrder) { groupName ->
                    val group = state.proxyMap[groupName] ?: return@items
                    ProxyGroupItem(
                        layout = layout,
                        group = group,
                        nodeCount = group.all.orEmpty().size,
                        selected = selectedGroup?.name == group.name,
                        onClick = { onGroupSelected(group.name) },
                        onTestLatency = { onTestGroupLatency(group.name) },
                    )
                }
            }
        }

        if (selectedGroup == null) {
            Card(modifier = Modifier.weight(0.62f)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("选择左侧代理组")
                }
            }
        } else {
            ProxyNodesList(
                group = selectedGroup,
                nodes = selectedGroup.all.orEmpty().mapNotNull { state.proxyMap[it] },
                onBackToGroups = {},
                onSelectProxy = onSelectProxy,
                onTestProxyLatency = onTestProxyLatency,
                onTestGroupLatency = onTestGroupLatency,
                showBackButton = false,
                modifier = Modifier.weight(0.62f),
                contentPadding = 0.dp,
            )
        }
    }
}

@Composable
private fun ProxyGroupItem(
    layout: DeviceLayout,
    group: Proxy,
    nodeCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onTestLatency: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick() },
            ),
        border = BorderStroke(
            width = if (focused || selected) 4.dp else 1.dp,
            color = when {
                focused || selected -> Color(0xFFFFD54F)
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "当前：${group.now.ifBlank { "-" }} · ${group.type} · ${nodeCount} 个节点",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProxyNodesList(
    group: Proxy,
    nodes: List<Proxy>,
    onBackToGroups: () -> Unit,
    onSelectProxy: (String, String) -> Unit,
    onTestProxyLatency: (String, String) -> Unit,
    onTestGroupLatency: (String) -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    contentPadding: Dp = 16.dp,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "当前：${group.now.ifBlank { "-" }} · ${group.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FocusOutlinedButton(onClick = { onTestGroupLatency(group.name) }) {
                        Text("组测速")
                    }
                    if (showBackButton) {
                        FocusOutlinedButton(onClick = onBackToGroups) {
                            Text("返回组")
                        }
                    }
                }
            }
        }

        items(nodes) { node ->
            ProxyNodeItem(
                groupName = group.name,
                node = node,
                active = node.name == group.now,
                onSelect = onSelectProxy,
                onTest = onTestProxyLatency,
            )
        }
    }
}

@Composable
private fun ProxyNodeItem(
    groupName: String,
    node: Proxy,
    active: Boolean,
    onSelect: (String, String) -> Unit,
    onTest: (String, String) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = "${node.type} · ${node.latestDelayText()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FocusOutlinedButton(onClick = { onTest(groupName, node.name) }) {
                    Text("测速")
                }
                if (active) {
                    AssistChip(
                        onClick = {},
                        label = { Text("已选择") },
                    )
                } else {
                    FocusButton(onClick = { onSelect(groupName, node.name) }) {
                        Text("选择")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    layout: DeviceLayout,
    providers: List<ProxyProvider>,
    ruleProviders: List<RuleProvider>,
    onUpdateAllProviders: () -> Unit,
    onUpdateRuleProviders: () -> Unit,
    onRestartCore: () -> Unit,
    onReloadConfigs: () -> Unit,
    onSelectMode: (String) -> Unit,
    onUpdateGeoData: () -> Unit,
    onFlushDnsCache: () -> Unit,
    onFlushFakeIp: () -> Unit,
) {
    if (layout.isWide) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(layout.gridColumns.coerceAtMost(2)),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(layout.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProxyProvidersSection(providers, onUpdateAllProviders)
            }
            item {
                RuleProvidersSection(ruleProviders, onUpdateRuleProviders)
            }
            item {
                ModeSection(onSelectMode)
            }
            item {
                CoreSection(onReloadConfigs, onRestartCore)
            }
            item {
                CacheSection(onFlushDnsCache, onFlushFakeIp)
            }
            item {
                GeoSection(onUpdateGeoData)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(layout.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProxyProvidersSection(providers, onUpdateAllProviders) }
        item { RuleProvidersSection(ruleProviders, onUpdateRuleProviders) }
        item { ModeSection(onSelectMode) }
        item { CoreSection(onReloadConfigs, onRestartCore) }
        item { CacheSection(onFlushDnsCache, onFlushFakeIp) }
        item { GeoSection(onUpdateGeoData) }
    }
}

@Composable
private fun ProxyProvidersSection(
    providers: List<ProxyProvider>,
    onUpdateAllProviders: () -> Unit,
) {
    SettingsSection(title = "Proxy Providers") {
        FocusButton(onClick = onUpdateAllProviders, modifier = Modifier.fillMaxWidth()) {
            Text("更新全部 Proxy Providers")
        }
        providers.forEach { provider ->
            ProviderRow(
                name = provider.name,
                description = "${provider.vehicleType} · ${provider.proxies.size} 个节点 · ${provider.updatedAt.ifBlank { "未知" }}",
            )
        }
    }
}

@Composable
private fun RuleProvidersSection(
    ruleProviders: List<RuleProvider>,
    onUpdateRuleProviders: () -> Unit,
) {
    SettingsSection(title = "Rule Providers") {
        FocusButton(onClick = onUpdateRuleProviders, modifier = Modifier.fillMaxWidth()) {
            Text("更新全部规则")
        }
        ruleProviders.forEach { provider ->
            ProviderRow(
                name = provider.name,
                description = "${provider.vehicleType} · ${provider.ruleCount} 条规则 · ${provider.updatedAt.ifBlank { "未知" }}",
            )
        }
    }
}

@Composable
private fun ModeSection(onSelectMode: (String) -> Unit) {
    SettingsSection(title = "模式选择") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FocusButton(onClick = { onSelectMode("rule") }, modifier = Modifier.weight(1f)) {
                Text("规则")
            }
            FocusOutlinedButton(onClick = { onSelectMode("direct") }, modifier = Modifier.weight(1f)) {
                Text("直连")
            }
            FocusOutlinedButton(onClick = { onSelectMode("global") }, modifier = Modifier.weight(1f)) {
                Text("全局")
            }
        }
    }
}

@Composable
private fun CoreSection(
    onReloadConfigs: () -> Unit,
    onRestartCore: () -> Unit,
) {
    SettingsSection(title = "核心") {
        FocusButton(onClick = onReloadConfigs, modifier = Modifier.fillMaxWidth()) {
            Text("重载配置")
        }
        FocusOutlinedButton(onClick = onRestartCore, modifier = Modifier.fillMaxWidth()) {
            Text("重启核心")
        }
    }
}

@Composable
private fun CacheSection(
    onFlushDnsCache: () -> Unit,
    onFlushFakeIp: () -> Unit,
) {
    SettingsSection(title = "缓存") {
        FocusButton(onClick = onFlushDnsCache, modifier = Modifier.fillMaxWidth()) {
            Text("清空 DNS 缓存")
        }
        FocusOutlinedButton(onClick = onFlushFakeIp, modifier = Modifier.fillMaxWidth()) {
            Text("清空 FAKE IP")
        }
    }
}

@Composable
private fun GeoSection(onUpdateGeoData: () -> Unit) {
    SettingsSection(title = "GEO") {
        FocusButton(onClick = onUpdateGeoData, modifier = Modifier.fillMaxWidth()) {
            Text("更新 GEO")
        }
    }
}

@Composable
private fun ProviderRow(
    name: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private fun Proxy.latestDelayText(): String {
    val delay = history.lastOrNull()?.delay ?: 0
    return when {
        delay < 0 -> "无效"
        delay == 0 -> "未测速"
        else -> "${delay}ms"
    }
}

private fun backendLabel(backend: Backend): String {
    val label = backend.label.trim()
    if (label.isNotEmpty()) return label
    val path = backend.secondaryPath.trim()
    return buildString {
        append(backend.protocol)
        append("://")
        append(backend.host)
        append(":")
        append(backend.port)
        if (path.isNotEmpty()) {
            append('/')
            append(path)
        }
    }
}

private fun Modifier.adaptiveContentWidth(maxWidth: Dp): Modifier {
    return if (maxWidth == Dp.Unspecified) {
        fillMaxWidth()
    } else {
        fillMaxWidth().widthIn(max = maxWidth)
    }
}

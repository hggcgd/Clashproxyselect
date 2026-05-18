@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zephyruso.zashboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zephyruso.zashboard.ui.BackendFormState
import com.zephyruso.zashboard.ui.DeviceLayout
import com.zephyruso.zashboard.ui.contentMaxWidth
import com.zephyruso.zashboard.ui.horizontalPadding
import com.zephyruso.zashboard.ui.isWide

@Composable
fun SetupScreen(
    layout: DeviceLayout,
    state: BackendFormState,
    onStateChange: (BackendFormState) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpenProxies: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveButtonRequester = remember { FocusRequester() }
    val clearButtonRequester = remember { FocusRequester() }
    val backButtonRequester = remember { FocusRequester() }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("后端设置") })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .adaptiveContentWidth(layout.contentMaxWidth)
                    .padding(horizontal = layout.horizontalPadding, vertical = if (layout.isWide) 24.dp else 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (layout.isWide) 16.dp else 12.dp),
            ) {
                Text(
                    text = "连接现有 Clash / Mihomo 后端",
                    style = if (layout.isWide) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "填写协议、主机、端口和密码后即可进入代理选择。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(if (layout.isWide) 20.dp else 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BackendFormFields(
                            layout = layout,
                            state = state,
                            onStateChange = onStateChange,
                            saveButtonRequester = saveButtonRequester,
                            clearButtonRequester = clearButtonRequester,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FocusButton(
                                onClick = onSave,
                                modifier = Modifier
                                    .focusRequester(saveButtonRequester)
                                    .focusProperties {
                                        right = clearButtonRequester
                                        down = backButtonRequester
                                    },
                            ) {
                                Text("保存并连接")
                            }
                            FocusOutlinedButton(
                                onClick = onClear,
                                modifier = Modifier
                                    .focusRequester(clearButtonRequester)
                                    .focusProperties {
                                        left = saveButtonRequester
                                        down = backButtonRequester
                                    },
                            ) {
                                Text("清除")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FocusOutlinedButton(
                    onClick = onOpenProxies,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(backButtonRequester)
                        .focusProperties {
                            up = saveButtonRequester
                        },
                ) {
                    Text("返回代理页")
                }
            }
        }
    }
}

@Composable
private fun BackendFormFields(
    layout: DeviceLayout,
    state: BackendFormState,
    onStateChange: (BackendFormState) -> Unit,
    saveButtonRequester: FocusRequester,
    clearButtonRequester: FocusRequester,
) {
    val protocolRequester = remember { FocusRequester() }
    val hostRequester = remember { FocusRequester() }
    val portRequester = remember { FocusRequester() }
    val secondaryPathRequester = remember { FocusRequester() }
    val passwordRequester = remember { FocusRequester() }
    val labelRequester = remember { FocusRequester() }

    if (layout.isWide) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProtocolField(
                    state = state,
                    onStateChange = onStateChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(protocolRequester)
                        .focusProperties {
                            right = hostRequester
                            down = portRequester
                        },
                )
                HostField(
                    state = state,
                    onStateChange = onStateChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(hostRequester)
                        .focusProperties {
                            left = protocolRequester
                            down = secondaryPathRequester
                        },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PortField(
                    state = state,
                    onStateChange = onStateChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(portRequester)
                        .focusProperties {
                            up = protocolRequester
                            right = secondaryPathRequester
                            down = passwordRequester
                        },
                )
                SecondaryPathField(
                    state = state,
                    onStateChange = onStateChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(secondaryPathRequester)
                        .focusProperties {
                            up = hostRequester
                            left = portRequester
                            down = labelRequester
                        },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField(
                    state = state,
                    onStateChange = onStateChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(passwordRequester)
                        .focusProperties {
                            up = portRequester
                            right = labelRequester
                            down = saveButtonRequester
                        },
                )
                LabelField(
                    state = state,
                    onStateChange = onStateChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(labelRequester)
                        .focusProperties {
                            up = secondaryPathRequester
                            left = passwordRequester
                            down = clearButtonRequester
                        },
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProtocolField(
            state,
            onStateChange,
            Modifier
                .fillMaxWidth()
                .focusRequester(protocolRequester)
                .focusProperties { down = hostRequester },
        )
        HostField(
            state,
            onStateChange,
            Modifier
                .fillMaxWidth()
                .focusRequester(hostRequester)
                .focusProperties {
                    up = protocolRequester
                    down = portRequester
                },
        )
        PortField(
            state,
            onStateChange,
            Modifier
                .fillMaxWidth()
                .focusRequester(portRequester)
                .focusProperties {
                    up = hostRequester
                    down = secondaryPathRequester
                },
        )
        SecondaryPathField(
            state,
            onStateChange,
            Modifier
                .fillMaxWidth()
                .focusRequester(secondaryPathRequester)
                .focusProperties {
                    up = portRequester
                    down = passwordRequester
                },
        )
        PasswordField(
            state,
            onStateChange,
            Modifier
                .fillMaxWidth()
                .focusRequester(passwordRequester)
                .focusProperties {
                    up = secondaryPathRequester
                    down = labelRequester
                },
        )
        LabelField(
            state,
            onStateChange,
            Modifier
                .fillMaxWidth()
                .focusRequester(labelRequester)
                .focusProperties {
                    up = passwordRequester
                    down = saveButtonRequester
                },
        )
    }
}

@Composable
private fun ProtocolField(state: BackendFormState, onStateChange: (BackendFormState) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = state.protocol,
        onValueChange = { onStateChange(state.copy(protocol = it.lowercase())) },
        label = { Text("协议") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun HostField(state: BackendFormState, onStateChange: (BackendFormState) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = state.host,
        onValueChange = { onStateChange(state.copy(host = it)) },
        label = { Text("主机") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun PortField(state: BackendFormState, onStateChange: (BackendFormState) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = state.port,
        onValueChange = { onStateChange(state.copy(port = it.filter(Char::isDigit))) },
        label = { Text("端口") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SecondaryPathField(state: BackendFormState, onStateChange: (BackendFormState) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = state.secondaryPath,
        onValueChange = { onStateChange(state.copy(secondaryPath = it)) },
        label = { Text("secondaryPath") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun PasswordField(state: BackendFormState, onStateChange: (BackendFormState) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = state.password,
        onValueChange = { onStateChange(state.copy(password = it)) },
        label = { Text("密码") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun LabelField(state: BackendFormState, onStateChange: (BackendFormState) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = state.label,
        onValueChange = { onStateChange(state.copy(label = it)) },
        label = { Text("标签") },
        singleLine = true,
        modifier = modifier,
    )
}

private fun Modifier.adaptiveContentWidth(maxWidth: Dp): Modifier {
    val widthModifier = if (maxWidth == Dp.Unspecified) {
        fillMaxWidth()
    } else {
        fillMaxWidth().widthIn(max = maxWidth)
    }
    return then(widthModifier).then(Modifier)
}

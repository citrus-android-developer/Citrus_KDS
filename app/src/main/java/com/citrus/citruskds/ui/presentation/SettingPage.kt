package com.citrus.citruskds.ui.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citrus.citruskds.R
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.widget.ErrorDialog
import com.citrus.citruskds.ui.presentation.widget.SecurityDialog
import com.citrus.citruskds.ui.theme.CitrusKDSTheme
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.util.Constants
import com.citrus.citruskds.util.TextInputField
import timber.log.Timber


@Composable
fun SettingPage(
    viewModel: CentralViewModel,
    navigateTo: (Int) -> Unit,
    onVerifyCancel: () -> Unit,
) {
    SettingContent(
        state = viewModel.currentState,
        event = viewModel::setEvent,
        navigateTo = navigateTo,
        onVerifyCancel = onVerifyCancel
    )
}

@Composable
fun SettingContent(
    state: CentralContract.State,
    event: (CentralContract.Event) -> Unit,
    navigateTo: (Int) -> Unit,
    onVerifyCancel: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .padding(20.dp),
            contentAlignment = Alignment.TopStart
        ) {
            ConnectParams(state, event, navigateTo) {
                onVerifyCancel()
            }
        }

        // 測試列印/連線結果（errMsg）在設定頁直接顯示，不必切回主頁
        if (state.errMsg != null) {
            ErrorDialog(state.errMsg, onDismissRequest = {
                event(CentralContract.Event.onDismissErrorDialog)
            })
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectParams(
    state: CentralContract.State,
    event: (CentralContract.Event) -> Unit,
    navigateTo: (Int) -> Unit,
    onVerifyCancel: () -> Unit,
) {
    val isExpanded1 = remember { mutableStateOf(false) }
    val isExpanded2 = remember { mutableStateOf(false) }
    val isExpanded3 = remember { mutableStateOf(false) }

    var securityManager by remember { mutableStateOf(false) }

    var isPrinterSelectVisible by remember { mutableStateOf(prefs.printMode == 0) }

    var isPrepareEnabled by remember { mutableStateOf(prefs.isPrepareEnable) }
    var isAutoAcceptEnabled by remember { mutableStateOf(prefs.isAutoAcceptEnable) }

    // 語系切換的強制重繪 hack 已移除：locale 改由 MainActivity 的 CompositionLocalProvider 統一套用

    LaunchedEffect(Unit) {
        if (prefs.firstInstall) {
            Constants.createCode()
            securityManager = true
        }
    }


    Column {
        Text(
            text = stringResource(id = R.string.connect_params),
            color = ColorBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 30.dp, bottom = 2.dp)
        )
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .border(
                    2.dp,
                    ColorBlue.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)

        ) {

            item {
                Text(text = "KDS ID", fontSize = 14.sp, color = ColorBlue)
                TextInputField(
                    textFieldValue = state.kdsIdState,
                    placeholder = "KDS ID",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    enabled = true,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "RSNO", fontSize = 14.sp, color = ColorBlue)
                TextInputField(
                    textFieldValue = state.rsnoState,
                    placeholder = "RSNO",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    enabled = true,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "POS IP", fontSize = 14.sp, color = ColorBlue)
                TextInputField(
                    textFieldValue = state.localIpState,
                    placeholder = "POS IP",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    enabled = true,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Server URL", fontSize = 14.sp, color = ColorBlue)
                TextInputField(
                    textFieldValue = state.serverUrlState,
                    placeholder = "https://global.citrus.tw/CompassKDS/",
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                    enabled = true,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(id = R.string.language_params),
                    fontSize = 14.sp,
                    color = ColorBlue
                )
                ExposedDropdownMenuBox(
                    expanded = isExpanded1.value,
                    onExpandedChange = { newValue ->
                        isExpanded1.value = newValue
                    }
                ) {
                    TextInputField(
                        textFieldValue = state.languageState,
                        placeholder = stringResource(id = R.string.language_params_tint),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded1.value)
                        },
                        enabled = false,
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded1.value,
                        onDismissRequest = {
                            isExpanded1.value = false
                        }
                    ) {
                        listOf("English", "华文").forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = language)
                                },
                                onClick = {
                                    event(CentralContract.Event.OnLanguageChanged(language))
                                    isExpanded1.value = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(id = R.string.item_present_language),
                    fontSize = 14.sp,
                    color = ColorBlue
                )
                ExposedDropdownMenuBox(
                    expanded = isExpanded3.value,
                    onExpandedChange = { newValue ->
                        isExpanded3.value = newValue
                    }
                ) {
                    TextInputField(
                        textFieldValue = state.itemDisplayLanState,
                        placeholder = stringResource(id = R.string.language_params_tint),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded3.value)
                        },
                        enabled = false,
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded3.value,
                        onDismissRequest = {
                            isExpanded3.value = false
                        }
                    ) {
                        listOf("English", "华文", "English & 华文").forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = language)
                                },
                                onClick = {
                                    event(
                                        CentralContract.Event.OnItemDisplayLanguageChanged(
                                            language
                                        )
                                    )
                                    isExpanded3.value = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(id = R.string.is_prepare_status_enable),
                    fontSize = 14.sp,
                    color = ColorBlue
                )
                PrepareRadio {
                    isPrepareEnabled = it
                    if (!it) isAutoAcceptEnabled = false   // 連動關閉自動接單
                    event(CentralContract.Event.onPrepareModeChanged(it))
                }

                Text(
                    text = stringResource(id = R.string.auto_accept),
                    fontSize = 14.sp,
                    color = if (isPrepareEnabled) ColorBlue else ColorBlue.copy(alpha = 0.4f)
                )
                AutoAcceptRadio(
                    enabled = isPrepareEnabled,
                    checked = isAutoAcceptEnabled
                ) {
                    isAutoAcceptEnabled = it
                    event(CentralContract.Event.onAutoAcceptModeChanged(it))
                }
                Text(
                    text = stringResource(
                        id = if (isPrepareEnabled) R.string.auto_accept_hint
                        else R.string.auto_accept_disabled_hint
                    ),
                    fontSize = 12.sp,
                    color = ColorBlue.copy(alpha = 0.4f)
                )

                Text(
                    text = stringResource(id = R.string.kitchen_order),
                    fontSize = 14.sp,
                    color = ColorBlue
                )
                PrintRadio {
                    isPrinterSelectVisible = it == 0
                    event(CentralContract.Event.onPrintModeChanged(it))
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isPrinterSelectVisible) {
                    Text(text = "Printer IP", fontSize = 14.sp, color = ColorBlue)
                    TextInputField(
                        textFieldValue = state.printerIpState,
                        placeholder = "192.168.0.66",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        enabled = true,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Text(text = "Port", fontSize = 14.sp, color = ColorBlue)
                    TextInputField(
                        textFieldValue = state.printerPortState,
                        placeholder = "9100",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        enabled = true,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Button(
                            onClick = { event(CentralContract.Event.TestPrinter) },
                            colors = ButtonDefaults.buttonColors(ColorBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "測試連線", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { event(CentralContract.Event.ScanPrinters) },
                            colors = ButtonDefaults.buttonColors(ColorBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "掃描", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    if (!state.printerInfo.isNullOrEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = isExpanded2.value,
                            onExpandedChange = { newValue ->
                                isExpanded2.value = newValue
                            }
                        ) {
                            TextInputField(
                                textFieldValue = state.printerState,
                                placeholder = "選擇掃描到的印表機",
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded2.value)
                                },
                                enabled = false,
                                modifier = Modifier
                                    .padding(bottom = 20.dp)
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = isExpanded2.value,
                                onDismissRequest = {
                                    isExpanded2.value = false
                                }
                            ) {
                                state.printerInfo?.forEach { info ->
                                    DropdownMenuItem(
                                        text = { Text(text = info["Target"] ?: "") },
                                        onClick = {
                                            event(CentralContract.Event.OnPrinterSelected(info))
                                            isExpanded2.value = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }


                Text(
                    text = stringResource(id = R.string.system_mode),
                    fontSize = 14.sp,
                    color = ColorBlue
                )
                ModeRadio {
                    // 先更新模式，再直接導到對應頁（避免經由 KDS 頁 modeState 競態而彈回）
                    event(CentralContract.Event.OnModeChanged(it))
                    navigateTo(it)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(id = R.string.order_ready_orientation),
                    fontSize = 14.sp,
                    color = ColorBlue
                )
                OrientationRadio {
                    event(CentralContract.Event.onOrderReadyOrientationChanged(it))
                    // 已在 OrderReady 模式 → 直接導回取餐牆即時套用新方向；KDS 模式只存設定不跳
                    if (prefs.mode == 1) {
                        navigateTo(1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

            }

//                Text(
//                    text = stringResource(id = R.string.page_params),
//                    fontSize = 14.sp,
//                    color = ColorBlue
//                )
//                ExposedDropdownMenuBox(
//                    expanded = isExpanded2.value,
//                    onExpandedChange = { newValue ->
//                        isExpanded2.value = newValue
//                    }
//                ) {
//                    TextInputField(
//                        textFieldValue = state.defaultPageState,
//                        placeholder = stringResource(id = R.string.page_params_tint),
//                        readOnly = true,
//                        trailingIcon = {
//                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded2.value)
//                        },
//                        enabled = false,
//                        modifier = Modifier
//                            .padding(bottom = 20.dp)
//                            .menuAnchor()
//                    )
//
//                    ExposedDropdownMenu(
//                        expanded = isExpanded2.value,
//                        onDismissRequest = {
//                            isExpanded2.value = false
//                        }
//                    ) {
//                        listOf(
//                            stringResource(id = R.string.main),
//                            stringResource(id = R.string.served),
//                            stringResource(id = R.string.recall)
//                        ).forEach { pageType ->
//                            DropdownMenuItem(
//                                text = {
//                                    Text(text = pageType)
//                                },
//                                onClick = {
//                                    event(CentralContract.Event.OnDefaultPageChanged(pageType))
//                                    isExpanded2.value = false
//                                }
//                            )
//                        }
//                    }
//                }
        }
    }

    if (securityManager) {
        SecurityDialog(onDismissRequest = { securityManager = false }, onCancel = {
            onVerifyCancel()
        }, finish = {
            securityManager = false
        })
    }
}


@Composable
fun AutoAcceptRadio(
    enabled: Boolean,
    checked: Boolean,
    modeChanged: (Boolean) -> Unit = {}
) {
    val radioOptions = listOf("Yes", "No")
    val selectedOption = if (checked) radioOptions[0] else radioOptions[1]
    Row(modifier = Modifier.fillMaxWidth()) {
        radioOptions.forEach { text ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.selectable(
                    selected = (text == selectedOption),
                    enabled = enabled,
                    onClick = { modeChanged(text == "Yes") }
                )
            ) {
                RadioButton(
                    selected = (text == selectedOption),
                    enabled = enabled,
                    onClick = { modeChanged(text == "Yes") }
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.merge(),
                )
            }
        }
    }
}

@Composable
fun PrepareRadio(modeChanged: (Boolean) -> Unit = {}) {
    val radioOptions = listOf("Yes", "No")
    val (selectedOption, onOptionSelected) = remember(prefs.isPrepareEnable) {
        mutableStateOf(
            radioOptions[if (prefs.isPrepareEnable) 0 else 1]
        )
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        radioOptions.forEach { text ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier =
                Modifier
                    .selectable(
                        selected = (text == selectedOption),
                        onClick = {
                            onOptionSelected(text)
                            Timber.d("selectedOption: $text")
                            val mode =
                                text == "Yes"
                            modeChanged(mode)
                        }
                    )

            ) {
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = {
                        onOptionSelected(text)
                        val mode =
                            text == "Yes"
                        modeChanged(mode)
                    }
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.merge(),
                )

            }
        }
    }
}

@Composable
fun PrintRadio(modeChanged: (Int) -> Unit = {}) {
    val radioOptions = listOf("Yes", "No")
    val (selectedOption, onOptionSelected) = remember(prefs.printMode) { mutableStateOf(radioOptions[prefs.printMode]) }
    Row(modifier = Modifier.fillMaxWidth()) {
        radioOptions.forEach { text ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier =
                Modifier
                    .selectable(
                        selected = (text == selectedOption),
                        onClick = {
                            onOptionSelected(text)
                            Timber.d("selectedOption: $text")
                            val mode =
                                if (text == "Yes") {
                                    0
                                } else {
                                    1
                                }
                            modeChanged(mode)
                        }
                    )

            ) {
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = {
                        onOptionSelected(text)
                        val mode =
                            if (text == "Yes") {
                                0
                            } else {
                                1
                            }
                        modeChanged(mode)
                    }
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.merge(),
                )

            }
        }
    }
}

@Composable
fun ModeRadio(modeChanged: (Int) -> Unit = {}) {
    val radioOptions = listOf("KDS", "OrderReady")
    val (selectedOption, onOptionSelected) = remember(prefs.mode) { mutableStateOf(radioOptions[prefs.mode]) }
    Row(modifier = Modifier.fillMaxWidth()) {
        radioOptions.forEach { text ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier =
                Modifier
                    .selectable(
                        selected = (text == selectedOption),
                        onClick = {
                            if (prefs.localIp.isEmpty()) {
                                return@selectable
                            }
                            onOptionSelected(text)
                            Timber.d("selectedOption: $text")
                            val mode =
                                if (text == "KDS") {
                                    0
                                } else {
                                    1
                                }
                            modeChanged(mode)
                        }
                    )

            ) {
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = {
                        if (prefs.localIp.isEmpty()) {
                            return@RadioButton
                        }
                        onOptionSelected(text)
                        val mode =
                            if (text == "KDS") {
                                0
                            } else {
                                1
                            }
                        modeChanged(mode)
                    }
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.merge(),
                )
            }
        }
    }
}

@Composable
fun OrientationRadio(modeChanged: (Int) -> Unit = {}) {
    // 0=Landscape 1=Portrait
    val radioOptions = listOf("Landscape", "Portrait")
    val (selectedOption, onOptionSelected) = remember(prefs.orderReadyOrientation) {
        mutableStateOf(radioOptions[prefs.orderReadyOrientation])
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        radioOptions.forEach { text ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .selectable(
                        selected = (text == selectedOption),
                        onClick = {
                            onOptionSelected(text)
                            modeChanged(if (text == "Landscape") 0 else 1)
                        }
                    )
            ) {
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = {
                        onOptionSelected(text)
                        modeChanged(if (text == "Landscape") 0 else 1)
                    }
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.merge(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingPreview() {
    CitrusKDSTheme {

    }
}
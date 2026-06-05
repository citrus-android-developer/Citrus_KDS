package com.citrus.citruskds.ui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.R
import com.citrus.citruskds.ui.presentation.widget.StockItem
import com.citrus.citruskds.ui.presentation.widget.TextClock
import com.citrus.citruskds.ui.theme.CitrusKDSTheme
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.StockBg
import com.citrus.citruskds.ui.theme.StockGrayText
import com.citrus.citruskds.ui.theme.StockGreen
import com.citrus.citruskds.ui.theme.StockRed
import com.citrus.citruskds.util.TextInputField


@Composable
fun SetStockPage(
    viewModel: CentralViewModel
) {
    SetStockContent(
        state = viewModel.currentState,
        event = viewModel::setEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetStockContent(
    state: CentralContract.State,
    event: (CentralContract.Event) -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("no_data.json"))
    val columns = GridCells.Fixed(4)
    LaunchedEffect(Unit) {
        event(CentralContract.Event.LoadStockList)
    }

    // 損耗送出成功：畫面中央彈出綠勾卡片（比 Toast 明顯），約 1.6 秒自動消失
    var showWastageOk by remember { mutableStateOf(false) }
    LaunchedEffect(state.wastageDone) {
        if (state.wastageDone > 0) {
            showWastageOk = true
            kotlinx.coroutines.delay(1600)
            showWastageOk = false
        }
    }

    val isExpanded = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBg),
        contentAlignment = Alignment.Center
    ) {

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .padding(start = 26.dp)
                        .align(Alignment.CenterVertically),
                ) {
                    TextClock(
                        color = StockRed,
                        format = "kk:mm:ss",
                        style = TextStyle(
                            fontSize = 34.sp,
                            color = StockRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    TextClock(
                        color = StockGrayText,
                        format = "EEE, MMM d",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = StockGrayText,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Column {
                    Text(
                        text = stringResource(id = R.string.category).trim().uppercase(),
                        color = StockGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                    )
                    ExposedDropdownMenuBox(
                        modifier = Modifier.width(300.dp),
                        expanded = isExpanded.value,
                        onExpandedChange = { newValue ->
                            isExpanded.value = newValue
                        }
                    ) {
                        TextInputField(
                            textFieldValue = state.stockTypeSelect,
                            placeholder = stringResource(id = R.string.all_categories),
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded.value)
                            },
                            enabled = false,
                            modifier = Modifier
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isExpanded.value,
                            onDismissRequest = {
                                isExpanded.value = false
                            }
                        ) {
                            // 全部分類（重設）
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.all_categories)) },
                                onClick = {
                                    event(CentralContract.Event.OnStockTypeChanged(""))
                                    isExpanded.value = false
                                }
                            )
                            state.stockTypeList?.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = type)
                                    },
                                    onClick = {
                                        event(CentralContract.Event.OnStockTypeChanged(type))
                                        isExpanded.value = false
                                    }
                                )
                            }
                        }
                    }
                }

                TextInputField(
                    textFieldValue = state.stockSearchState,
                    modifier = Modifier
                        .padding(end = 26.dp)
                        .align(Alignment.CenterVertically)
                        .width(300.dp),

                    placeholder = stringResource(id = R.string.search_item),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_search_24),
                            contentDescription = null,
                            tint = ColorBlue
                        )
                    },
                )
            }


            state.stockInfoPresentList?.let { dataList ->
                if (dataList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LottieAnimation(
                                modifier = Modifier
                                    .height(300.dp)
                                    .align(Alignment.CenterHorizontally),
                                composition = composition,
                                iterations = LottieConstants.IterateForever
                            )

                            Text(
                                text = stringResource(id = R.string.no_result),
                                color = Color.LightGray,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = columns,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dataList.size, key = { index ->
                            index
                        }) { index ->

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                StockItem(dataList[index], onSelected = {
                                    event(CentralContract.Event.OnStockItemClicked(dataList[index]))
                                }, onChangeStock = {
//                                    val stock = dataList[index].copy()
//                                    stock.stock = it.toString()
//                                    event(CentralContract.Event.OnSetInventory(stock))
                                }, onWastage = { qty, status ->
                                    event(CentralContract.Event.OnSetWastage(dataList[index], qty, status))
                                })
                            }

                        }
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(id = R.string.please_select_type),
                        color = Color.LightGray,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign =
                        TextAlign.Center, modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // 損耗送出成功：中央綠勾卡片覆蓋層
        WastageSuccessOverlay(
            visible = showWastageOk,
            text = stringResource(id = R.string.wastage_submitted)
        )
    }
}

/** 損耗送出成功覆蓋層：半透明遮罩 + 中央白卡（綠色打勾動畫 + 文字），放大淡入。 */
@Composable
private fun WastageSuccessOverlay(visible: Boolean, text: String) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
    ) {
        val ok by rememberLottieComposition(LottieCompositionSpec.Asset("operation_success.json"))
        val progress by animateLottieCompositionAsState(ok, iterations = 1, speed = 1.2f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(horizontal = 56.dp, vertical = 40.dp)
            ) {
                LottieAnimation(
                    composition = ok,
                    progress = { progress },
                    modifier = Modifier.size(140.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = text,
                    color = StockGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SetStockPreview() {
    CitrusKDSTheme {

    }
}
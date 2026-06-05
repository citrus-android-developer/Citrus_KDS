package com.citrus.citruskds.ui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.displayStatus
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.widget.AllItemDialog
import com.citrus.citruskds.ui.presentation.widget.OrderItem
import com.citrus.citruskds.ui.presentation.widget.OrderItemWithOK
import com.citrus.citruskds.ui.presentation.widget.TextClock
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPickup
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.util.Constants.PREPARED
import com.citrus.citruskds.util.Constants.PROGRESSING
import com.citrus.citruskds.util.InputStateWrapper
import com.citrus.citruskds.util.TextInputField
import com.citrus.citruskds.util.pressClickEffect
import timber.log.Timber


@Composable
fun MainPage(
    viewModel: CentralViewModel,
) {
    MainContent(
        state = viewModel.currentState,
        event = viewModel::setEvent
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainContent(
    state: CentralContract.State,
    event: (CentralContract.Event) -> Unit,
) {
    val columns = GridCells.Fixed(4)
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("no_data_2.json"))


    var viewOrder: Order? by remember {
        mutableStateOf(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        Column {
            TitleRow(title = stringResource(id = R.string.main), InputStateWrapper())
            state.mainList?.let { dataList ->
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
                                text = stringResource(id = R.string.no_data),
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
                            dataList[index].orderNo
                        }) { index ->
                            Box(
                                modifier = Modifier
                                    .height(IntrinsicSize.Max)
                                    .animateItemPlacement()   // 卡片移除時鄰近卡片平滑補位
                            ) {
                                OrderItem(
                                    state = state,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    dataList[index]
                                ) { itemCount ->
                                    MainFeatureBtn(
                                        status = dataList[index].displayStatus(),
                                        size = itemCount,
                                        viewAll = {
                                            viewOrder = dataList[index]
                                        }, finish = {
                                            event(CentralContract.Event.FinishOrder(dataList[index]))
                                        }, progressing = {
                                            event(CentralContract.Event.ProgressOrder(dataList[index]))
                                        }, collected = {
                                            event(CentralContract.Event.CollectedOrder(dataList[index].orderNo))
                                        })
                                }

                                this@Column.AnimatedVisibility(
                                    visible = !dataList[index].isVisible,
                                    enter = scaleIn(initialScale = 0.8f, animationSpec = tween(220)) + fadeIn(tween(220)),
                                    exit = scaleOut(targetScale = 0.8f, animationSpec = tween(200)) + fadeOut(tween(200))
                                ) {
                                    OrderItemWithOK(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        dataList[index]
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(30.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }

    viewOrder?.let {
        AllItemDialog(order = it, onDismissRequest = { viewOrder = null }) {
            event(CentralContract.Event.FinishOrder(it))
        }
    }


}

@Composable
fun TitleRow(title: String, state: InputStateWrapper, isShowSearch: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {

        TextClock(
            modifier = Modifier
                .padding(start = 26.dp)
                .align(Alignment.CenterVertically),
            color = ColorBlue,
            format = "yyyy/MM/dd kk:mm:ss",
            style = TextStyle(
                fontSize = 22.sp,
                color = ColorBlue,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = title,
            color = ColorBlue,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        TextInputField(
            textFieldValue = state,
            modifier = Modifier
                .padding(end = 26.dp)
                .align(Alignment.CenterVertically)
                .width(300.dp)
                .alpha(if (isShowSearch) 1f else 0f),

            placeholder = stringResource(id = R.string.search_order),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_search_24),
                    contentDescription = null,
                    tint = ColorBlue
                )
            },
        )
    }
}


@Composable
private fun MainFeatureBtn(
    status: String,
    size: Int,
    viewAll: () -> Unit,
    finish: () -> Unit,
    progressing: () -> Unit,
    collected: () -> Unit,
) {

    var orderStatus by remember(status.uppercase()) {
        mutableStateOf(status.uppercase())
    }



    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
        ) {


            Button(
                onClick = {

                    Timber.d("order status click: $status")
                    if (orderStatus == PREPARED) {
                        collected()
                        return@Button
                    }

                    if (orderStatus != PROGRESSING && prefs.isPrepareEnable) {
                        orderStatus = PROGRESSING
                        progressing()
                    } else {
                        finish()
                    }
                },
                // 顏色一律依實際狀態：新單(J)=藍，製作中(W)=橘，待取(O)=綠(#4CAF50)
                colors = ButtonDefaults.buttonColors(
                    when (orderStatus) {
                        PREPARED -> ColorPickup
                        PROGRESSING -> ColorPrimary
                        else -> ColorBlue
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    4.dp,
                    when (orderStatus) {
                        PREPARED -> ColorPickup
                        PROGRESSING -> ColorPrimary
                        else -> ColorBlue
                    }
                ),
                modifier = Modifier
                    .pressClickEffect {}
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Timber.d("order status~: $status")
                    // 處理中不再顯示轉圈 loading（會被誤會成在撈資料），一律顯示一般 icon
                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_served_fill,
                        ), contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )

                    Text(
                        // 文字一律反映實際狀態（不受本機 prepareMode 影響）：
                        // W→处理中、O→待取餐、其餘(J)→新单
                        text = stringResource(
                            id = when (orderStatus) {
                                PROGRESSING -> R.string.preparing
                                PREPARED -> R.string.prepared
                                else -> R.string.prepare
                            }
                        ),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }
        }

        if (orderStatus == PROGRESSING) {
            Text(
                text = "click to finish",
                color = Color.Black.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

    }
}


@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
fun GridItemPreview() {
    MainContent(
        state = CentralContract.State(
            kdsIdState = InputStateWrapper(TextFieldState(prefs.kdsId)),
            rsnoState = InputStateWrapper(TextFieldState(prefs.rsno)),
            localIpState = InputStateWrapper(TextFieldState(prefs.localIp)),
            serverUrlState = InputStateWrapper(TextFieldState(prefs.serverUrl)),
            languageState = InputStateWrapper(TextFieldState(prefs.language)),
            itemDisplayLanState = InputStateWrapper(TextFieldState(prefs.itemDisplayLan)),
            defaultPageState = InputStateWrapper(TextFieldState("0")),
            servedSearchState = InputStateWrapper(TextFieldState("")),
            recallSearchState = InputStateWrapper(TextFieldState("")),
            stockTypeSelect = InputStateWrapper(TextFieldState("")),
            stockSearchState = InputStateWrapper(TextFieldState("")),
            printerState = InputStateWrapper(TextFieldState("s")),
            printerIpState = InputStateWrapper(TextFieldState("")),
            printerPortState = InputStateWrapper(TextFieldState("9100")),
        ),
        event = {}
    )
}
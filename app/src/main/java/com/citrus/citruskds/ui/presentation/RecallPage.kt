package com.citrus.citruskds.ui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.R
import com.citrus.citruskds.ui.presentation.widget.OrderItem
import com.citrus.citruskds.ui.presentation.widget.OrderItemWithOK
import com.citrus.citruskds.ui.presentation.widget.TextClock
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.util.TextInputField
import com.citrus.citruskds.util.pressClickEffect


@Composable
fun RecallPage(
    viewModel: CentralViewModel,
) {
    RecallContent(
        state = viewModel.currentState,
        event = viewModel::setEvent
    )
}


@Composable
fun RecallContent(
    state: CentralContract.State,
    event: (CentralContract.Event) -> Unit,
) {
    val columns = GridCells.Fixed(4)
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("no_data_2.json"))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        Column {
            TitleRow(
                title = stringResource(id = R.string.recall),
                state = state.recallSearchState,
                isShowSearch = true
            )

            state.recallFilterList?.let { dataList ->
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
                            index
                        }) { index ->

                            Box(modifier = Modifier.height(IntrinsicSize.Max)) {
                                OrderItem(
                                    state = state,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    dataList[index]
                                ) { itemCount ->
                                    RecallFeatureBtn(dataList[index].status, noShow = {

                                    }, reCall = {
                                        event(CentralContract.Event.RecallOrder(dataList[index].orderNo))
                                    })
                                }
                                this@Column.AnimatedVisibility(
                                    visible = !dataList[index].isVisible,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    OrderItemWithOK(
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
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
}

@Composable
private fun RecallFeatureBtn(status: String, noShow: () -> Unit, reCall: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
    ) {


        Button(
            onClick = {
                reCall()
            },
            colors = ButtonDefaults.buttonColors(ColorBlue),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(4.dp, ColorBlue),
            modifier = Modifier
                .pressClickEffect {

                }
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_recall_fill,
                    ), contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Text(
                    text = stringResource(id = R.string.recall),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }
    }
}

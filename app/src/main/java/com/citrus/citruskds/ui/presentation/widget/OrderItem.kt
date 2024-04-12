package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.CentralContract
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPinkBg
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.ui.theme.ColorYellowBg
import timber.log.Timber

@Composable
fun OrderItem(
    state: CentralContract.State,
    modifier: Modifier,
    order: Order,
    featureBtn: @Composable (Int) -> Unit
) {

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("operation_success.json"))

    val disPlayLan = state.displayLan

    val bgColor = if (order.status == "O") {
        ColorYellowBg
    } else if (order.status == "F") {
        ColorPinkBg
    } else {
        ColorWhiteBg
    }

    val size = order.detail.size
    val flavorSize = order.detail.filter { !it.flavor.isNullOrBlank() }.size
    val default = if (disPlayLan == "English & 华文") 50 else 40

    Card(
        colors = CardDefaults.cardColors(
            contentColor = Color.White,
            containerColor = bgColor
        ),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(
            2.dp
        ),
        modifier = modifier


    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .height((200 + ((size + flavorSize) * default)).dp)
                    .padding(10.dp)

            ) {

                Column {
                    val no = order.orderNo
                    val shortNo = no.substring(0, 3) + "-" + no.takeLast(5)
                    Text(
                        text = stringResource(id = R.string.no) + shortNo,
                        color = ColorBlue,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = stringResource(id = R.string.order_time) + order.orderTime.split(" ")[1],
                        color = Color.Black,
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    )
                }

                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .weight(2.5f)
                        .align(Alignment.Start),
                    content = {
                        items(order.detail.size) { index ->

                            val flavor =
                                if (order.detail[index].flavor.isNullOrBlank()) "" else "#${order.detail[index].flavor}"

                            when (disPlayLan) {
                                "English" -> OneLineItemInfo(
                                    order.detail[index].eName,
                                    order.detail[index].qty.toString(),
                                    "\n" + flavor,
                                    index
                                )

                                "华文" -> OneLineItemInfo(
                                    order.detail[index].cName,
                                    order.detail[index].qty.toString(),
                                    "\n" + flavor,
                                    index
                                )

                                else -> TwoLineItemInfo(
                                    order.detail[index].eName,
                                    order.detail[index].cName,
                                    order.detail[index].qty.toString(),
                                    flavor,
                                    index
                                )
                            }
                        }
                    })
                HorizontalDivider()
                featureBtn(order.detail.size)
            }
        }
    }
}


@Composable
fun TwoLineItemInfo(name1: String, name2: String, qty: String, flavor: String, index: Int) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (index % 2 != 0) it.background(Color.White.copy(alpha = 0.5f)) else it }

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "$qty x",
                color = ColorBlue,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = name1,
                    color = ColorBlue,
                )
                Text(
                    text = name2,
                    color = ColorBlue,
                )
            }
        }
        Text(
            text = flavor,
            color = ColorBlue,
        )
    }
}

@Composable
fun OneLineItemInfo(name: String, qty: String, flavor: String, index: Int) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .let { if (index % 2 != 0) it.background(Color.White.copy(alpha = 0.5f)) else it }) {
        Text(
            text = "$qty x $name$flavor",
            color = ColorBlue,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .align(Alignment.CenterStart)
        )
    }
}
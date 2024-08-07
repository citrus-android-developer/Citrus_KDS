package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.citrus.citruskds.commonData.vo.Detail
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.ui.presentation.CentralContract
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPinkBg
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.ui.theme.ColorYellowBg

@Composable
fun OrderItem(
    state: CentralContract.State,
    modifier: Modifier,
    order: Order,
    featureBtn: @Composable (Int) -> Unit,
) {

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("operation_success.json"))
    val disPlayLan = state.displayLan

    val bgColor = when (order.status) {
        "O" -> {
            ColorYellowBg
        }

        "F" -> {
            ColorPinkBg
        }

        else -> {
            ColorWhiteBg
        }
    }

    val size = order.detail.size
    val flavorSize = order.detail.filter { !it.flavor.isNullOrBlank() }.size
    val addSize = order.detail.filter { !it.addition.isNullOrBlank() }.size
    val default = if (disPlayLan == "English & 华文") 50 else 44


    val orderDetail = order.detail
    //將detail迭代，如果gType為M，則將接下來gType為S的detail加入到M的MiddleDetail中
    for (i in 0 until orderDetail.size) {
        if (orderDetail[i].gType == "M" || orderDetail[i].gType == "G") {
            orderDetail[i].middleDetail = mutableListOf()
            for (j in i + 1 until orderDetail.size) {
                if (orderDetail[j].gType == "S") {
                    orderDetail[i].middleDetail = orderDetail[i].middleDetail?.plus(orderDetail[j])
                } else {
                    break
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            contentColor = Color.White,
            containerColor = bgColor
        ),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(
            2.dp
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
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
            HorizontalDivider()

            orderDetail.filter { it.gType != "S" }.forEachIndexed { index, data ->

                val flavor = if (data.flavor.isNullOrBlank()) "" else "\n#${data.flavor}"
                val add = if (data.addition.isNullOrBlank()) "" else "\n#${data.addition}"

                OneLineItemInfo(
                    data.eName,
                    data.qty.toString(),
                    flavor,
                    add,
                    index,
                    data.middleDetail
                )
            }

            Box(modifier = Modifier.padding(top = 5.dp)) {
                featureBtn(order.detail.size)
            }
        }
    }
}


@Composable
fun OneLineItemInfo(
    name: String,
    qty: String,
    flavor: String,
    add: String,
    index: Int,
    middleList: List<Detail>?,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth() // Fill the parent container
            .padding(5.dp)
            .let { if (index % 2 != 0) it.background(Color.White.copy(alpha = 0.5f)) else it },
    ) {
        Text(
            text = "$qty x $name$flavor$add",
            color = ColorBlue,
            modifier = Modifier
                .padding(all = 5.dp)
        )



        if (middleList != null) {
            //回圈方式產生text呈現ename
            for (element in middleList) {
                Text(
                    text = " " + element.eName,
                    color = ColorBlue,
                    modifier = Modifier
                        .padding(start = 20.dp, top = 10.dp)
                )
            }


        }
    }
}

//@Composable
//@Preview
//fun OrderItemPreview() {
//    OrderItem(
//        state = CentralContract.State(),
//        modifier = Modifier.fillMaxSize(),
//        order = Order(
//            orderNo = "202107010001",
//            orderTime = "2021-07-01 12:00:00",
//    ) {}
//}
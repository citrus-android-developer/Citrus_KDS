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
import com.citrus.citruskds.commonData.vo.additionDisplay
import com.citrus.citruskds.commonData.vo.displayStatus
import com.citrus.citruskds.commonData.vo.flavorDisplay
import com.citrus.citruskds.commonData.vo.isComboMain
import com.citrus.citruskds.commonData.vo.isSideDish
import com.citrus.citruskds.commonData.vo.nameDisplay
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.CentralContract
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPinkBg
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.ui.theme.ColorYellowBg

/** 品項顯示名稱：依「訂單品項語言」itemDisplayLan 挑（支援 English & 华文 雙語並列）*/
private fun Detail.displayName(): String =
    nameDisplay(prefs.itemDisplayLan)

@Composable
fun OrderItem(
    state: CentralContract.State,
    modifier: Modifier,
    order: Order,
    featureBtn: @Composable (Int) -> Unit,
) {

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("operation_success.json"))
    val disPlayLan = state.displayLan

    val bgColor = when (order.displayStatus()) {
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
    // 套餐主項：M(附餐為 S)/ G(附餐為 R)；兩種附餐都要收進主項的 middleDetail
    //將detail迭代，套餐主項(M/G)後面接續的附餐(S/R)加入到主項的 middleDetail 中
    for (i in 0 until orderDetail.size) {
        if (orderDetail[i].isComboMain) {
            orderDetail[i].middleDetail = mutableListOf()
            for (j in i + 1 until orderDetail.size) {
                if (orderDetail[j].isSideDish) {
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
            val pager = if (order.note.isNullOrBlank()) "" else " (${order.note})"
            Text(
                text = stringResource(id = R.string.no) + shortNo + pager,
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

            orderDetail.filter { !it.isSideDish }.forEachIndexed { index, data ->

                val flavorStr = data.flavorDisplay(prefs.itemDisplayLan)
                val flavor = if (flavorStr.isBlank()) "" else "\n#$flavorStr"
                val addStr = data.additionDisplay(prefs.itemDisplayLan)
                val add = if (addStr.isBlank()) "" else "\n#$addStr"

                OneLineItemInfo(
                    data.displayName(),
                    data.qty.toString(),
                    flavor,
                    add,
                    index,
                    data.middleDetail,
                    hideQty = data.isComboMain   // 套餐主項不顯示數量
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
    hideQty: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth() // Fill the parent container
            .padding(5.dp)
            .let { if (index % 2 != 0) it.background(Color.White.copy(alpha = 0.5f)) else it },
    ) {
        Text(
            // 套餐主項(hideQty)只顯示名稱；其餘顯示「數量 x 名稱」
            text = if (hideQty) "$name$flavor$add" else "$qty x $name$flavor$add",
            color = ColorBlue,
            modifier = Modifier
                .padding(all = 5.dp)
        )



        if (middleList != null) {
            //回圈方式產生text呈現ename
            for (element in middleList) {
                Text(
                    text = " " + element.displayName(),
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
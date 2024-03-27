package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPinkBg
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.ui.theme.ColorYellowBg

@Composable
fun OrderItemWithOK(modifier: Modifier, order: Order) {

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("operation_success.json"))

    val bgColor = if (order.status == "O") {
        ColorYellowBg
    } else if (order.status == "F") {
        ColorPinkBg
    } else {
        ColorWhiteBg
    }

    val size = order.detail.size
    val flavorSize = order.detail.filter { !it.flavor.isNullOrBlank() }.size

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .height((200 + ((size + flavorSize) * 40)).dp)
                .padding(10.dp)

        ) {
            Text(
                text = "No." + order.orderNo,
                color = ColorBlue,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally)
            )
            HorizontalDivider()

            LottieAnimation(
                modifier = Modifier
                    .height(300.dp)
                    .align(Alignment.CenterHorizontally),
                composition = composition,
            )

        }
    }
}
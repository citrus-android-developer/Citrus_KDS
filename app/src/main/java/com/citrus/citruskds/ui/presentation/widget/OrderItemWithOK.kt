package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.displayStatus
import com.citrus.citruskds.ui.theme.ColorPinkBg
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.ui.theme.ColorYellowBg

@Composable
fun OrderItemWithOK(modifier: Modifier, order: Order) {

    // 依「狀態轉換」分色（兩個動畫都在主頁）：
    //  待取餐(O)被「取餐」→已完成(F)：維持綠色；其餘(製作中 W→待取餐 O，「完成」)：橘色
    val orange = order.displayStatus().uppercase() != "O"
    val asset = if (orange) "operation_success_orange.json" else "operation_success.json"
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(asset))
    // 加速播放：原始 1.5s（90 frames@60fps）÷ 1.5x ≈ 1s
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = 1,
        speed = 1.5f,
    )

    val bgColor = if (order.status == "O") {
        ColorYellowBg
    } else if (order.status == "F") {
        ColorPinkBg
    } else {
        ColorWhiteBg
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
    ) {

        LottieAnimation(
            composition = composition,
            progress = { progress },
            contentScale = ContentScale.Fit,   // 縮放至卡片範圍內，不超出卡片
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .align(Alignment.Center),
        )
    }
}

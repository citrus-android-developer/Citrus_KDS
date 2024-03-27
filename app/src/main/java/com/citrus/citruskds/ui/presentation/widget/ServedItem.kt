package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPinkBg
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.ui.theme.ColorYellowBg

@Composable
fun ServedItem(modifier: Modifier, order: Order, featureBtn: @Composable (Int) -> Unit) {

    val bgColor = if (order.status == "O") {
        ColorYellowBg
    } else if (order.status == "F") {
        ColorPinkBg
    } else {
        ColorWhiteBg
    }

    val size = order.detail.size
    val flavorSize = order.detail.filter { it.flavor != null }.size

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
                .height(  (200 + (size + flavorSize * 40)).dp)
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
            LazyColumn(
                modifier = Modifier
                    .weight(2.5f)
                    .align(Alignment.Start),
                content = {
                    items(order.detail.size) { index ->
                        val name =
                            if (prefs.language == "English") order.detail[index].eName else order.detail[index].cName
                        val flavor =
                            if (order.detail[index].flavor.isNullOrBlank()) "" else "\n#${order.detail[index].flavor}"
                        Text(
                            text = "${order.detail[index].qty} x $name" + flavor,
                            color = ColorBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp)
                                .let { if (index % 2 != 0) it.background(Color.White.copy(alpha = 0.5f)) else it }
                                .padding(5.dp)
                        )
                    }
                })
            HorizontalDivider()
            featureBtn(order.detail.size)
        }
    }
}
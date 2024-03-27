package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.util.pressClickEffect
import timber.log.Timber


@Composable
fun AllItemDialog(
    order: Order,
    onDismissRequest: () -> Unit,
    finish: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            //讓畫面可以隨著內容動態增長
        ),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                contentColor = Color.White,
                containerColor = ColorWhiteBg
            ),
            shape = RoundedCornerShape(15.dp),
            elevation = CardDefaults.cardElevation(
                2.dp
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.7f)
                .padding(horizontal = 8.dp, vertical = 4.dp)


        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(6.dp)
                    .padding(10.dp)

            ) {
                androidx.compose.material3.Text(
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
                        .fillMaxWidth()
                        .weight(2.5f)
                        .align(Alignment.Start),
                    content = {
                        items(order.detail.size) { index ->
                            val name =
                                if (prefs.language == "English") order.detail[index].eName else order.detail[index].cName
                            val flavor =
                                if (order.detail[index].flavor.isNullOrBlank()) "" else "\n#${order.detail[index].flavor}"
                            androidx.compose.material3.Text(
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth()
                ) {

                    Button(
                        onClick = {
                            finish()
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(ColorBlue),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(4.dp, ColorBlue),
                        modifier = Modifier
                            .pressClickEffect {

                            }
                            .weight(1f)
                            .padding(start = 10.dp)


                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_served_fill,
                                ), contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Text(
                                text = stringResource(id = R.string.finish),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(5.dp)
                            )
                        }
                    }
                }

            }
        }
    }
}


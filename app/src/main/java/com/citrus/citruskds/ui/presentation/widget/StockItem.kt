package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.StockInfo
import com.citrus.citruskds.di.MyApplication.Companion.prefs
import com.citrus.citruskds.ui.theme.StockGrayText
import com.citrus.citruskds.ui.theme.StockGreen
import com.citrus.citruskds.ui.theme.StockGreenBar
import com.citrus.citruskds.ui.theme.StockLogBg
import com.citrus.citruskds.ui.theme.StockNameText
import com.citrus.citruskds.ui.theme.StockPinkBar
import com.citrus.citruskds.ui.theme.StockPinkCard
import com.citrus.citruskds.ui.theme.StockRed
import com.citrus.citruskds.ui.theme.StockStepperBg

/**
 * 庫存品項卡（對齊設計圖）：
 *  上：品項名 + 分類（售完時名字紅 + 右上 SOLD OUT 標籤 + 卡底粉 + 左紅邊）
 *  中：狀態列（綠/紅底）：圓點 + On Sale/Sold Out + Switch
 *  下：Wastage（垃圾桶 icon）+ 步進器[− n +] + Log
 */
@Composable
fun StockItem(
    stockInfo: StockInfo,
    onSelected: () -> Unit,
    onChangeStock: (Int) -> Unit = {},
    onWastage: (qty: Int, status: String) -> Unit = { _, _ -> },
) {
    val isAvailable = stockInfo.sellStatus == "Available"
    var qty by remember(stockInfo.gID, stockInfo.gKID) { mutableIntStateOf(0) }
    val name = if (prefs?.language == "English") stockInfo.eName ?: stockInfo.cName ?: ""
    else stockInfo.cName ?: stockInfo.eName ?: ""
    val category = (if (prefs?.language == "English") stockInfo.gKEName else stockInfo.gKCName)
        ?.uppercase() ?: ""

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isAvailable) Color.White else StockPinkCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // 售完左側紅邊條
            if (!isAvailable) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(StockRed)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                // 名稱 + 分類 + (售完標籤)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAvailable) StockNameText else StockRed,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = StockGrayText,
                        )
                    }
                    if (!isAvailable) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StockRed)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.sold_out).uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 狀態列：圓點 + 文字 + Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAvailable) StockGreenBar else StockPinkBar)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isAvailable) StockGreen else StockRed)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = if (isAvailable) R.string.on_sale else R.string.sold_out),
                            color = if (isAvailable) StockGreen else StockRed,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { onSelected() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StockGreen,
                            checkedBorderColor = StockGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFBDBDBD),
                            uncheckedBorderColor = Color(0xFFBDBDBD),
                        ),
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                Spacer(Modifier.height(12.dp))

                // Wastage 標籤
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        tint = StockRed,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.wastage),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockNameText,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 步進器 + Log
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // 步進器
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StockStepperBg),
                    ) {
                        StepBtn("−", enabled = qty > 0) { if (qty > 0) qty-- }
                        Text(
                            text = qty.toString(),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockNameText,
                        )
                        StepBtn("+", enabled = true) { qty++ }
                    }

                    // Log
                    val logActive = qty > 0
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (logActive) StockRed else StockLogBg)
                            .then(
                                if (logActive) Modifier.clickable {
                                    onWastage(qty, "S")
                                    qty = 0
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.log),
                            color = if (logActive) Color.White else StockGrayText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBtn(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) StockGrayText else StockGrayText.copy(alpha = 0.4f),
        )
    }
}

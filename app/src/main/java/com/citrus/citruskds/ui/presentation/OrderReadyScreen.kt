package com.citrus.citruskds.ui.presentation

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.citrus.citruskds.R
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.util.pressClickEffect

/**
 * 群組名稱圖片：URL = {base}/GoodsImages/{name}.png（失敗試 .jpg），都沒有則顯示文字店名。
 * 帶 tick query 並關閉快取 → 跟著取餐牆輪詢一起刷新（中途換圖也會更新）。
 */
@Composable
private fun GroupNameImage(
    base: String,
    name: String,
    tick: Int,
    fontSp: TextUnit,
    modifier: Modifier,
) {
    val ctx = LocalContext.current
    // URL 帶 30 秒刷新 token：30 秒內 URL 不變→走快取不重載(不閃)，過 30 秒換 token→重抓一次
    fun request(ext: String) = ImageRequest.Builder(ctx)
        .data("$base/GoodsImages/$name.$ext?t=$tick")
        .crossfade(true)
        .build()

    SubcomposeAsyncImage(
        model = request("png"),
        contentDescription = name,
        contentScale = ContentScale.Fit,
        modifier = modifier,
        error = {
            // png 沒有 → 試 jpg
            SubcomposeAsyncImage(
                model = request("jpg"),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                error = {
                    // 都沒有 → 顯示文字店名
                    Text(
                        text = name,
                        fontSize = fontSp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderReadyScreen(
    viewModel: CentralViewModel,
    navigateToSetting: () -> Unit,
    askUpdate: () -> Unit
) {

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp
    // 直立畫面較窄 → 字級倍率較小（每行能塞的個數少）；每組顯示數改為依列高/寬度動態量測（見下方 capacityForRow）
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val fontMul = if (isPortrait) 0.27f else 0.36f
    val fontFloor = if (isPortrait) 10f else 13f
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    // 表頭高度與字級自適應、收小一點（整體往上，把空間讓給資料列）
    val headerH = (screenHeightDp.value * 0.075f).coerceIn(38f, 52f).dp
    val headerFontSp = (headerH.value * 0.52f).coerceIn(18f, 26f).sp

    LaunchedEffect(Unit) {
        viewModel.setEvent(CentralContract.Event.startFetchOrderReadyInfo)
    }

    val list = viewModel.currentState.orderReadyList
    val redSet = viewModel.currentState.orderReadyRedSet
    val imgTick = viewModel.currentState.orderReadyTick
    // 店家圖片 base = POS IP 去掉 port（例：http://192.168.0.162），檔名=群組名
    val imgBase = "http://" + prefs.localIp.substringBefore(":")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        list?.let { it ->
            val size = it.size
            LazyColumn(
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerH),
                        contentAlignment = Alignment.Center
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .weight(0.2f)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterVertically)
                                    .border(1.dp, Color.White)
                            ) {
                                Text(
                                    text = "Outlet",
                                    fontSize = headerFontSp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .border(1.dp, Color.White)
                                    .weight(0.8f)
                                    .padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = "Ready For Collection",
                                    fontSize = headerFontSp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        // reload(刷新圖片) + 設定齒輪，並排靠右
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 20.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_refresh_24),
                                contentDescription = "reload images",
                                modifier = Modifier
                                    .size(44.dp)
                                    .pressClickEffect {
                                        viewModel.setEvent(CentralContract.Event.ReloadOrderReadyImages)
                                    },
                                tint = Color.White,
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_settings_24),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .pressClickEffect {
                                        navigateToSetting()
                                    },
                                tint = Color.White,
                            )
                        }

                    }

                }

                // 每列高度 = (螢幕高 - 表頭 - 頁尾) / 組數，組越多每列越矮；字級隨之縮放避免裁切
                val rowHeight = (screenHeightDp - headerH - 26.dp) / size
                val fontSp = (rowHeight.value * fontMul).coerceIn(fontFloor, 32f).sp
                val chipVPad = (rowHeight.value * 0.03f).coerceIn(1f, 3f).dp
                val chipGap = (rowHeight.value * 0.10f).coerceIn(4f, 10f).dp
                items(size) { index ->
                    val orders = it.getOrNull(index)?.orderNo ?: emptyList()
                    // 動態算這一列不裁切可顯示的訂單數 = 容得下的行數 × 每行個數
                    // 用該組最寬字串量測 → 保守，保證不溢出；店少→列高→行數多→可顯示更多
                    val capacity = remember(orders, rowHeight, fontSp, chipGap, chipVPad) {
                        if (orders.isEmpty()) 0 else {
                            val style = TextStyle(fontSize = fontSp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            val widest = orders.maxByOrNull { s -> s.length } ?: ""
                            val layout = textMeasurer.measure(widest, style)
                            with(density) {
                                val usableWidthPx = (screenWidthDp * 0.8f - 36.dp).toPx()
                                val innerHeightPx = (rowHeight - 10.dp).toPx()
                                val chipGapPx = chipGap.toPx()
                                val chipVPadPx = chipVPad.toPx()
                                // 每個 chip 寬 = 文字寬 + 左右 padding(8x2) ；行內以 chipGap 相隔
                                val chipUnitPx = layout.size.width + 16.dp.toPx() + chipGapPx
                                val perLine = ((usableWidthPx + chipGapPx) / chipUnitPx).toInt().coerceAtLeast(1)
                                // 每行高 = 文字高 + 上下 padding(chipVPad x2) ；行距 chipVPad
                                val lineBlockPx = layout.size.height + chipVPadPx * 3
                                val lines = ((innerHeightPx + chipVPadPx) / lineBlockPx).toInt().coerceAtLeast(1)
                                perLine * lines
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .weight(0.2f)
                                    .fillMaxHeight()
                                    .background(Color.Gray.copy(alpha = 0.5f))
                                    .align(Alignment.CenterVertically)
                                    .border(1.dp, Color.White)
                            ) {
                                GroupNameImage(
                                    base = imgBase,
                                    name = it.getOrNull(index)?.orderName ?: "",
                                    tick = imgTick,
                                    fontSp = fontSp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .border(1.dp, Color.White)
                                    .weight(0.8f)
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart),
                                    horizontalArrangement = Arrangement.spacedBy(chipGap),
                                    verticalArrangement = Arrangement.spacedBy(chipVPad),
                                ) {
                                    orders.take(capacity).forEach { orderStr ->
                                        val isNew = orderStr in redSet
                                        Text(
                                            text = orderStr,
                                            fontSize = fontSp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNew) Color.Black else Color.White,
                                            letterSpacing = 1.sp,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isNew) Color.Red else Color.Transparent)
                                                .padding(horizontal = 8.dp, vertical = chipVPad)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(30.dp)
                    .align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier
                .clickable {
                    navigateToSetting()
                }
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // 设置水平排列方式为SpaceBetween
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Version." + LocalContext.current.packageManager.getPackageInfo(
                    LocalContext.current.packageName,
                    0
                ).versionName, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Powered by",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Image(
                painter = painterResource(id = R.drawable.citrus_logo),
                modifier = Modifier
                    .height(12.dp)
                    .padding(start = 10.dp),
                contentDescription = null
            )
        }
    }
}

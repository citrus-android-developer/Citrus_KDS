package com.citrus.citruskds.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val ColorPrimary = Color(0xFFFF7B51)

val ColorDeepGreen = Color(0xFF3B8D00)

val ColorBlue = Color(0xFF00559E)
val ColorPickup = Color(0xFF4CAF50)   // 待取餐(O) 按鈕綠

val ColorWhiteBg = Color(0xFFEBEBEB)

val ColorPinkBg = Color(0xFFF4DBFF)

val ColorYellowBg = Color(0xFFFAE5D8)

val ColorWarning = Color(0xFF8D0000)

val Gray05 = Color(0xFFF1F1F1)
val Gray10 = Color(0xFFE9E9E9)
val Gray15 = Color(0xFFD5D5D5)
val Gray20 = Color(0xFFC9C9C9)
val Gray30 = Color(0xFFBBBBBB)
val Gray40 = Color(0xFFACACAC)
val Gray50 = Color(0xFF868686)
val Gray60 = Color(0xFF636363)
val Gray70 = Color(0xFF4E4E4E)
val Gray80 = Color(0xFF3F3F3F)

val Blue40 = Color(0xFF00559E)
val BlueGrey40 = Color(0xFF5B5D71)
val BlueG40 = Color(0xFF52537D)

val ColorPrimaryDart = Color(0xFF64593B)


val ColorTextPrimaryColor = Color(0xFF3F3E3E)
val ColorAccent = Color(0xFF8D4022)

// 庫存頁設計配色（2026-06-05，依設計圖估色，可微調）
val StockBg = Color(0xFFEFEDE8)          // 頁面暖灰底
val StockGreen = Color(0xFF34A853)       // 上架中：點/文字/Switch
val StockGreenBar = Color(0xFFE7F4EA)    // 上架中狀態列底
val StockRed = Color(0xFFD6492E)         // 售完/品項名/標籤/時鐘/垃圾桶
val StockPinkCard = Color(0xFFFCEBE8)    // 售完卡底
val StockPinkBar = Color(0xFFFAE0DB)     // 售完狀態列底
val StockStepperBg = Color(0xFFF0EEE9)   // 步進器底
val StockLogBg = Color(0xFFE6E2DB)       // Log 按鈕底(未啟用)
val StockGrayText = Color(0xFF9A958C)    // 分類/次要文字
val StockNameText = Color(0xFF2B2B2B)    // 品項名


fun Color.lighten(factor: Float): Color {
    return Color(
        red = this.red + (1 - this.red) * factor,
        green = this.green + (1 - this.green) * factor,
        blue = this.blue + (1 - this.blue) * factor,
        alpha = this.alpha
    )
}
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


fun Color.lighten(factor: Float): Color {
    return Color(
        red = this.red + (1 - this.red) * factor,
        green = this.green + (1 - this.green) * factor,
        blue = this.blue + (1 - this.blue) * factor,
        alpha = this.alpha
    )
}
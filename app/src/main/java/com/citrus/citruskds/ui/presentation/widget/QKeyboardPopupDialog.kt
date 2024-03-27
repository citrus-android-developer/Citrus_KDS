package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.ui.theme.lighten
import com.citrus.citruskds.util.isKioskScreen

@Composable
fun QKeyboardPopupDialog(
    visible: Boolean,
    defaultValue: String = "",
    maxLength: Int? = null,
    isNum: Boolean = false,
    width: Int = 240,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit,
) {

    var dialogValueState by remember { mutableStateOf("") } //為了讓每次打開dialog 都可以重新輸入

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(Color.White),
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(32.dp),
            small = RoundedCornerShape(32.dp),
            medium = RoundedCornerShape(32.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(32.dp)
        )
    ) {

        DropdownMenu(
            expanded = visible,
            offset = DpOffset(y = (-8).dp, x = 0.dp),//calculateYOffset(width)
            onDismissRequest = {
                onDismiss()
                dialogValueState = ""
            },
            modifier = Modifier
                .background(color = Color.White.copy(alpha = .5f))
                .border(7.dp, ColorPrimary.lighten(.5f), shape = RoundedCornerShape(32.dp))
                .padding(17.dp)
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    LocalDensity.current.density * (if (isKioskScreen) 2 else 1).toFloat(),
//                    fontScale = if (isKioskScreen) 2f else 1f
                ),
            ) {
                QKeyboard(
                    modifier = Modifier.width(width.dp),
                    onValueChange = {
                        dialogValueState = it
                        onValueChange(it)
                    },
                    isNum = isNum,
                    value = dialogValueState,
                    maxLength = maxLength,
                    onClickOk = {
                        onDismiss()
                        dialogValueState = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun calculateYOffset(width: Int): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return ((screenWidth - width) / 2).dp
}
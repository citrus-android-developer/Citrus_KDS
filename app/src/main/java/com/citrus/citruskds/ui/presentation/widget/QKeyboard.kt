package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citrus.citruskds.R
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.ui.theme.ColorPrimaryDart
import com.citrus.citruskds.ui.theme.ColorWarning
import com.citrus.citruskds.ui.theme.lighten
import com.citrus.citruskds.util.pressClickEffect
import java.lang.Integer.min


sealed class KeyBoardEvent {
    data class AddValue(val value: String) : KeyBoardEvent()
    object DeleteValue : KeyBoardEvent()
    object ClearValue : KeyBoardEvent()
    object OnDismiss : KeyBoardEvent()

}

@Composable
fun QKeyboard(
    modifier: Modifier = Modifier,
    value: String = "",
    maxLength: Int? = null,
    isNum: Boolean = false,
    fontSize: TextUnit = 22.sp,
    onValueChange: (String) -> Unit,
    onClickOk: (() -> Unit)? = null,
) {

    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", if (onClickOk == null) "Del" else "OK")
    Column(modifier = modifier) {
        for (i in keys.indices step 3) {
            Row {
                for (j in i until min(i + 3, keys.size)) {
                    KeyBoardItem(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        value = keys[j],
                        fontSize = fontSize,
                        onClick = { clickedKey ->
                            var newValue = value
                            when (clickedKey) {
                                is KeyBoardEvent.AddValue -> {
                                    var addedString = value + clickedKey.value
                                    if (isNum) addedString = addedString.removePrefix("0")
                                    if (maxLength == null || addedString.length <= maxLength) {
                                        newValue = addedString
                                    }
                                }

                                is KeyBoardEvent.DeleteValue -> {
                                    if (value.isNotEmpty()) {
                                        newValue = value.substring(0, value.length - 1)
                                    }
                                }

                                is KeyBoardEvent.ClearValue -> {
                                    newValue = ""
                                }

                                is KeyBoardEvent.OnDismiss -> {
                                    onClickOk?.invoke()
                                    return@KeyBoardItem
                                }
                            }
                            onValueChange(newValue)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyBoardItem(
    modifier: Modifier,
    value: String,
    fontSize: TextUnit,
    onClick: (KeyBoardEvent) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .pressClickEffect {
                when (value) {
                    "Del" -> onClick(KeyBoardEvent.DeleteValue)
                    "C" -> onClick(KeyBoardEvent.ClearValue)
                    "OK" -> onClick(KeyBoardEvent.OnDismiss)
                    else -> onClick(KeyBoardEvent.AddValue(value))
                }
            }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(3.dp)
                .offset(x = 4.dp, y = 4.dp)
                .background(
                    if (value == "C") (ColorWarning.copy(.2f)) else Color.LightGray.lighten(0.3f),
                    shape = RoundedCornerShape(18.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(2.dp)
                .border(
                    2.5.dp,
                    if (value == "C") (ColorWarning.lighten(.5f)) else Color.Gray.lighten(0.3f),
                    shape = RoundedCornerShape(20.dp)
                )

        ) {
            if (value == "Del") {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete), contentDescription = "Del",
                    tint = ColorPrimary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size((fontSize.value * 1.8).dp)
                        .padding(5.dp)
                )
            } else if (value == "OK") {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_square), contentDescription = "OK",
                    tint = ColorPrimary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size((fontSize.value * 2.1).dp)
                        .padding(5.dp)
                )
            } else {
                Text(
                    fontFamily = FontFamily(Font(R.font.baloo2_medium)),
                    text = value,
                    color = if (value == "C") ColorWarning else ColorPrimaryDart,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun QKeyboardPreview() {
    QKeyboard(onValueChange = {}) {

    }
}
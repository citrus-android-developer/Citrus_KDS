package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.StockInfo
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.util.InputStateWrapper
import com.citrus.citruskds.util.TextInputField
import com.citrus.citruskds.util.pressClickEffect
import timber.log.Timber


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockItem(stockInfo: StockInfo, onSelected: () -> Unit, onChangeStock: (Int) -> Unit) {

    val stockEditState = InputStateWrapper()

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
            .pressClickEffect {
                onSelected()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .let {
                if (stockInfo.isSelect) it.border(
                    4.dp,
                    ColorPrimary,
                    RoundedCornerShape(15.dp)
                ) else it
            }


    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.6f)
            ) {
                Text(
                    text = if (prefs.language == "English") stockInfo.eName ?: stockInfo.cName
                    ?: "" else stockInfo.cName ?: stockInfo.eName ?: "",
                    fontSize = 20.sp,
                    color = ColorBlue,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.4f)
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {

                    if (!stockInfo.isSelect) {
                        Text(
                            text = stringResource(id = R.string.stock_qty) + " ",
                            fontSize = 20.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (stockInfo.stock?.isBlank() == true) "0" else stockInfo.stock
                            ?: "0",
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(2.dp)
                    )

                    if (stockInfo.isSelect) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.baseline_arrow_forward_24,
                            ), contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(2.dp),
                            tint = Color.White
                        )

                        TextInputField(
                            textFieldValue = stockEditState,
                            placeholder = "0",
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            enabled = true,
                            modifier = Modifier
                                .width(80.dp),
                            clearable = false,
                            maxLength = 5
                        )

                        Button(
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                if (stockEditState.state.text.isNotBlank()) {
                                    val num: Int
                                    try {
                                        num = (stockEditState.state.text.toString()).toInt()
                                    } catch (e: Exception) {
                                        return@Button
                                    }
                                    Timber.d("num: $num")
                                    onChangeStock(num)
                                }
                            },
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                        ) {
                            Text(text = stringResource(id = R.string.confirm))
                        }

                    }
                }
            }
        }
    }

//    QKeyboardPopupDialog(
//        visible = stockInfo.isSelect,
//        defaultValue = "0",
//        maxLength = 2,
//        isNum = true,
//        onDismiss = {
//            // onSelected()
//        },
//        onValueChange = {
//            //newStock = it
//        },
//        width = 180
//    )
}

@Preview(showBackground = true)
@Composable
fun StockItemPreview() {

}
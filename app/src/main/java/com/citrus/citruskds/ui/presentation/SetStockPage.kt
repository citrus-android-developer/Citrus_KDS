package com.citrus.citruskds.ui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.R
import com.citrus.citruskds.ui.presentation.widget.StockItem
import com.citrus.citruskds.ui.presentation.widget.TextClock
import com.citrus.citruskds.ui.theme.CitrusKDSTheme
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.util.TextInputField


@Composable
fun SetStockPage(
    viewModel: CentralViewModel
) {
    SetStockContent(
        state = viewModel.currentState,
        event = viewModel::setEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetStockContent(
    state: CentralContract.State,
    event: (CentralContract.Event) -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("no_data.json"))
    val columns = GridCells.Fixed(5)
    LaunchedEffect(Unit) {
        event(CentralContract.Event.LoadStockList)
    }

    val isExpanded = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {

                TextClock(
                    modifier = Modifier
                        .padding(start = 26.dp)
                        .align(Alignment.CenterVertically),
                    color = ColorBlue,
                    format = "kk:mm:ss",
                    style = TextStyle(
                        fontSize = 24.sp,
                        color = ColorBlue,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.category),
                        color = ColorBlue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ExposedDropdownMenuBox(
                        modifier = Modifier.width(300.dp),
                        expanded = isExpanded.value,
                        onExpandedChange = { newValue ->
                            isExpanded.value = newValue
                        }
                    ) {
                        TextInputField(
                            textFieldValue = state.stockTypeSelect,
                            placeholder = stringResource(id = R.string.select_type),
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded.value)
                            },
                            enabled = false,
                            modifier = Modifier
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isExpanded.value,
                            onDismissRequest = {
                                isExpanded.value = false
                            }
                        ) {
                            state.stockTypeList?.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = type)
                                    },
                                    onClick = {
                                        event(CentralContract.Event.OnStockTypeChanged(type))
                                        isExpanded.value = false
                                    }
                                )
                            }
                        }
                    }
                }

                TextInputField(
                    textFieldValue = state.stockSearchState,
                    modifier = Modifier
                        .padding(end = 26.dp)
                        .align(Alignment.CenterVertically)
                        .width(300.dp),

                    placeholder = stringResource(id = R.string.search_item),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_search_24),
                            contentDescription = null,
                            tint = ColorBlue
                        )
                    },
                )
            }


            state.stockInfoPresentList?.let { dataList ->
                if (dataList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LottieAnimation(
                                modifier = Modifier
                                    .height(300.dp)
                                    .align(Alignment.CenterHorizontally),
                                composition = composition,
                                iterations = LottieConstants.IterateForever
                            )

                            Text(
                                text = stringResource(id = R.string.no_result),
                                color = Color.LightGray,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = columns,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dataList.size, key = { index ->
                            index
                        }) { index ->

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                StockItem(dataList[index], onSelected = {
                                    event(CentralContract.Event.OnStockItemClicked(dataList[index]))
                                }, onChangeStock = {
                                    val stock = dataList[index].copy()
                                    stock.stock = it.toString()
                                    event(CentralContract.Event.OnSetInventory(stock))
                                })
                            }

                        }
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(id = R.string.please_select_type),
                        color = Color.LightGray,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign =
                        TextAlign.Center, modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SetStockPreview() {
    CitrusKDSTheme {

    }
}
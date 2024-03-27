package com.citrus.citruskds.ui.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citrus.citruskds.R
import com.citrus.citruskds.util.pressClickEffect

@Composable
fun OrderReadyScreen(
    viewModel: CentralViewModel,
    navigateToSetting: () -> Unit,
    askUpdate: () -> Unit
) {

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    LaunchedEffect(Unit) {
        viewModel.setEvent(CentralContract.Event.startFetchOrderReadyInfo)
    }

    val list = viewModel.currentState.orderReadyList

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
                            .height(60.dp),// 将每个项的权重设置为1，以便均分屏幕高度
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
                                    fontSize = 30.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
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
                                    fontSize = 30.sp,
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

                        Icon(
                            painter = painterResource(
                                id = R.drawable.baseline_settings_24,
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .align(Alignment.CenterEnd)
                                .padding(end = 20.dp)
                                .pressClickEffect {
                                    navigateToSetting()
                                },
                            tint = Color.White,

                            )

                    }

                }

                items(size) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((screenHeightDp - 60.dp) / size + (24 / size).dp),// 将每个项的权重设置为1，以便均分屏幕高度
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
                                Text(
                                    text = it.getOrNull(index)?.orderName ?: "",
                                    fontSize = 32.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
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
                                    text = it.getOrNull(index)?.orderNo?.take(20)
                                        ?.joinToString { orderNo ->
                                            val firstThree = orderNo.substring(0, 3)
                                            val lastFive = orderNo.takeLast(5)
                                            val result = "$firstThree-$lastFive"
                                            result
                                        }
                                        ?: "",
                                    fontSize = 32.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Start,
                                    maxLines = 4,
                                    lineHeight = 40.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .align(Alignment.TopStart)
                                )
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

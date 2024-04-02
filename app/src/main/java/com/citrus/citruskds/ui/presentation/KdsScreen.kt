package com.citrus.citruskds.ui.presentation

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citrus.citruskds.HomeTabs
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.widget.ErrorDialog
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.util.PrintStatus
import com.citrus.citruskds.util.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KdsScreen(
    navigateToOrderReady: () -> Unit,
    viewModel: CentralViewModel,
    askUpdate: () -> Unit
) {
    val sizeList = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    var errShowing by remember { mutableStateOf(false) }
    var printErrShowing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pageState = rememberPagerState(
        pageCount = { HomeTabs.entries.size },
        initialPage = if (prefs.localIp.isEmpty() || (prefs.kdsId.isEmpty() && prefs.mode == 0)) 3 else 0
    )

    val selectedTabIndex = remember {
        derivedStateOf { pageState.currentPage }
    }

    LaunchedEffect(Unit) {
        viewModel.setEvent(CentralContract.Event.startFetchKdsInfo)
    }

    LaunchedEffect(viewModel.currentState.modeState) {
        if (viewModel.currentState.modeState == 1) {
            navigateToOrderReady()
        }
    }

    LaunchedEffect(selectedTabIndex.value) {
        viewModel.updateCurrentPage(selectedTabIndex.value)
    }


    LaunchedEffect(viewModel.currentState.errMsg) {
        if (viewModel.currentState.errMsg != null) {
            errShowing = true
        }
    }

    LaunchedEffect(viewModel.currentState.printStatus) {
        when (viewModel.currentState.printStatus) {
            is PrintStatus.Idle, PrintStatus.Printing -> Unit
            is PrintStatus.Error -> {
                printErrShowing = true
            }
        }
    }


    val context = LocalContext.current
    val configuration = LocalConfiguration.current


    LaunchedEffect(viewModel.currentState.languageState.state.text) {
        val newLocale =
            if (viewModel.currentState.languageState.state.text.toString() == "English") {
                Locale("en")
            } else {
                Locale("zh")
            }

        val newConfiguration = Configuration(configuration).apply {
            setLocale(newLocale)
        }
        context.resources.updateConfiguration(newConfiguration, context.resources.displayMetrics)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex.value,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex.value]),
                    color = ColorPrimary,
                )
            }
        ) {
            HomeTabs.entries.forEachIndexed { index, currentTab ->
                Tab(
                    selected = selectedTabIndex.value == index,
                    selectedContentColor = ColorPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.outline,
                    onClick = {
                        scope.launch {
                            pageState.animateScrollToPage(currentTab.ordinal)
                        }
                    },
                    text = {
                        val name = when (currentTab.text) {
                            "Main" -> stringResource(id = R.string.main)
                            "Served" -> stringResource(id = R.string.served)
                            "ReCall" -> stringResource(id = R.string.recall)
//                            "SetStock" -> stringResource(id = R.string.set_stock)
                            "Setting" -> stringResource(id = R.string.setting)
                            else -> ""
                        }
                        Text(
                            text = name,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTabIndex.value == index) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(5.dp)
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(
                                id = if (selectedTabIndex.value == index) currentTab.selectedIcon else currentTab.unSelectedIcon,
                            ), contentDescription = null,
                            modifier = Modifier.size(35.dp),
                            tint = if (selectedTabIndex.value == index) ColorPrimary else MaterialTheme.colorScheme.outline
                        )
                    },
                    modifier = Modifier
                        .onSizeChanged {
                            sizeList[index] = Pair(it.width.toFloat(), it.height.toFloat())
                        }
                )

            }
        }

        HorizontalPager(
            state = pageState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            ReadyForPage(selectedTabIndex, viewModel)
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clickable {
                    askUpdate()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(
                text = "Version." + LocalContext.current.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).versionName, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f)
            )

            Spacer(Modifier.width(10.dp))
            Text(
                text = "Powered by",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.5f)
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


    if (errShowing) {
        if (!prefs.firstInstall && prefs.localIp.isNotBlank() && viewModel.currentState.currentPage != "else" && viewModel.currentState.errMsg != null) {
            ErrorDialog(viewModel.currentState.errMsg, onDismissRequest = {
                errShowing = false
                viewModel.setEvent(CentralContract.Event.onDismissErrorDialog)
            })
        }
    }

    if (printErrShowing) {
        ErrorDialog(
            UiText.DynamicString((viewModel.currentState.printStatus as PrintStatus.Error).errMsg),
            onDismissRequest = {
                printErrShowing = false
                viewModel.setEvent(CentralContract.Event.onDismissErrorDialog)
            })
    }
}


@Composable
fun ReadyForPage(
    selectedTabIndex: State<Int>,
    homeViewModel: CentralViewModel
) {
    when (selectedTabIndex.value) {
        0 -> MainPage(homeViewModel)
        1 -> ServedPage(homeViewModel)
        2 -> RecallPage(homeViewModel)
//        3 -> SetStockPage(homeViewModel)
        3 -> SettingPage(homeViewModel, onVerifyCancel = {
            homeViewModel.setEvent(CentralContract.Event.onVerifyCancel)
        }, navigateTo = {})
    }
}
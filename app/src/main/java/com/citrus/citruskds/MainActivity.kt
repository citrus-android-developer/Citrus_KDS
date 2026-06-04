package com.citrus.citruskds

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.CentralContract
import com.citrus.citruskds.ui.presentation.CentralViewModel
import com.citrus.citruskds.ui.presentation.KdsScreen
import com.citrus.citruskds.ui.presentation.OrderReadyScreen
import com.citrus.citruskds.ui.presentation.SettingPage
import com.citrus.citruskds.ui.presentation.widget.DownloadApkProgressDialog
import com.citrus.citruskds.ui.presentation.widget.UpdateDialog
import com.citrus.citruskds.ui.theme.CitrusKDSTheme
import com.citrus.citruskds.util.PrintUtil
import com.citrus.citruskds.util.scanner.SunmiScanReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


enum class HomeTabs(
    val selectedIcon: Int,
    val unSelectedIcon: Int,
    val text: String,
) {
    Main(
        selectedIcon = R.drawable.ic_main_fill,
        unSelectedIcon = R.drawable.ic_main_outline,
        text = "Main",
    ),

    Served(
        selectedIcon = R.drawable.ic_served_fill,
        unSelectedIcon = R.drawable.ic_served_outline,
        text = "Served",
    ),

    ReCall(
        selectedIcon = R.drawable.ic_recall_fill,
        unSelectedIcon = R.drawable.ic_recall_outline,
        text = "ReCall",
    ),

    SetStock(
        selectedIcon = R.drawable.ic_setstock_fill,
        unSelectedIcon = R.drawable.ic_setstock_outline,
        text = "SetStock",
    ),

    Setting(
        selectedIcon = R.drawable.ic_setting_fill,
        unSelectedIcon = R.drawable.ic_setting_outline,
        text = "Setting",
    ),
}

const val APK_FILE_NAME = "compass_kds.apk"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var printUtil: PrintUtil

    // 與 setContent 內 hiltViewModel<CentralViewModel>() 為同一實例（皆 Activity 範圍）
    private val viewModel: CentralViewModel by viewModels()

    /** SUNMI 掃描廣播：掃到取餐 QR(=訂單號) → 震動 + 派發 ScanOrderNo（全頁面生效） */
    private val scanReceiver = SunmiScanReceiver { orderNo ->
        vibrate()
        viewModel.setEvent(CentralContract.Event.ScanOrderNo(orderNo))
    }

    private lateinit var mProgressDialog: ProgressDialog

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SunmiScanReceiver.ACTION_DATA_CODE_RECEIVED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(scanReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(scanReceiver) }
    }

    /** 掃描成功觸覺回饋 */
    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VibratorManager::class.java))?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120)
        }
    }

    override fun onResume() {
        super.onResume()
        setFullScreen()
    }

    private fun setFullScreen() {
        val decorView = setSystemUiVisibilityMode()
        decorView.setOnSystemUiVisibilityChangeListener {
            setSystemUiVisibilityMode() // Needed to avoid exiting immersive_sticky when keyboard is displayed
        }
    }

    //kiosk working (下方虛擬按鈕)
    private fun setSystemUiVisibilityMode(): View {
        val decorView = window.decorView
        val options = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = options
        return decorView
    }


    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OrderReady 模式跟著裝置轉（支援直向），KDS 模式維持橫向
        requestedOrientation = if (prefs.mode == 1) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        setContent {
            CitrusKDSTheme {

                val navController = rememberNavController()
                val homeViewModel = hiltViewModel<CentralViewModel>()

                val updateAsk = remember { mutableStateOf(false) }

                // 語系單一來源：以 languageState 提供更新後的 LocalConfiguration/LocalContext，
                // 讓整棵 Compose 樹（stringResource 與品項名稱）一致重組
                //（取代 KdsScreen 的 updateConfiguration 與 SettingPage 的 alpha hack）
                val baseConfiguration = LocalConfiguration.current
                val baseContext = LocalContext.current
                val langText = homeViewModel.currentState.languageState.state.text.toString()
                val localizedConfiguration = remember(langText, baseConfiguration) {
                    Configuration(baseConfiguration).apply {
                        setLocale(if (langText == "English") Locale("en") else Locale("zh"))
                    }
                }
                val localizedContext = remember(localizedConfiguration) {
                    baseContext.createConfigurationContext(localizedConfiguration)
                }

                CompositionLocalProvider(
                    LocalConfiguration provides localizedConfiguration,
                    LocalContext provides localizedContext,
                ) {

                LaunchedEffect(homeViewModel.currentState.isVerifyCancel) {
                    if (homeViewModel.currentState.isVerifyCancel) {
                        this@MainActivity.finish()
                    }
                }

                // key 在 printRequestId：重印同一張(含加點)也會重觸發
                LaunchedEffect(homeViewModel.currentState.printRequestId) {
                    if (homeViewModel.currentState.printRequestId > 0) {
                        homeViewModel.currentState.printOrder?.let {
                            printUtil.setOrderPrint(it)
                        }
                    }
                }


                NavHost(
                    navController = navController,
                    startDestination = if (prefs.mode == 0) "kds" else "orderReady"
                ) {

                    composable("kds") {
                        LaunchedEffect(Unit) {
                            this@MainActivity.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            val scope = rememberCoroutineScope()

                            KdsScreen(viewModel = homeViewModel,
//                                sendToPrint = {
//                                scope.launch {
//                                    Timber.d("Printer issue trace: step 3")
//                                    initializeObject()
//                                    runPrintReceiptSequence(it)
//                                }
//                            },
                                navigateToOrderReady = {
                                    navController.navigate("orderReady")
                                }) {
                                intentToUpdate(updateAsk)
                            }
                        }
                    }

                    composable("orderReady") {
                        LaunchedEffect(Unit) {
                            // 取餐牆方向依設定：0=橫向 1=直向
                            this@MainActivity.requestedOrientation =
                                if (prefs.orderReadyOrientation == 1) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                        }
                        OrderReadyScreen(viewModel = homeViewModel, navigateToSetting = {
                            navController.navigate("setting")
                        }) {
                            intentToUpdate(updateAsk)
                        }
                    }

                    composable("setting") {
                        SettingPage(homeViewModel, onVerifyCancel = {}, navigateTo = { mode ->
                            // 依選擇的模式直接導到對應頁（0=KDS / 1=OrderReady），不再統一導到 kds
                            val dest = if (mode == 1) "orderReady" else "kds"
                            navController.navigate(dest) {
                                popUpTo("setting") { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    }
                }

                homeViewModel.currentState.downloadStatus?.let {
                    DownloadApkProgressDialog(
                        downloadStatus = it,
                        onDialogDismiss = {
                            homeViewModel.setEvent(CentralContract.Event.OnDismissDownloadApkDialog)
                        }
                    )
                }

                if (updateAsk.value) {
                    UpdateDialog(
                        onDismissRequest = {
                            updateAsk.value = false
                        },
                        finish = {
                            homeViewModel.setEvent(CentralContract.Event.IntentToUpdateVersion(it))
                        },
                        onCancel = {
                            updateAsk.value = false
                        })
                }

                } // CompositionLocalProvider（語系）
            }
        }


    }

    private fun intentToUpdate(updateAsk: MutableState<Boolean>) {
        // API 29+ 下載寫入 app 專屬目錄(getExternalFilesDir)，免權限 → 直接開對話框
        // （WRITE_EXTERNAL_STORAGE 在 Android 11+ 無法授予，舊邏輯會讓新機點版號沒反應）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            updateAsk.value = true
            return
        }

        // 舊版(API<29)寫入公用 Downloads，仍需 WRITE_EXTERNAL_STORAGE
        val permissionCheck = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                888
            )
        } else {
            updateAsk.value = true
        }
    }


}



package com.citrus.citruskds

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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

    private lateinit var mProgressDialog: ProgressDialog

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
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


    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CitrusKDSTheme {

                val navController = rememberNavController()
                val homeViewModel = hiltViewModel<CentralViewModel>()

                val updateAsk = remember { mutableStateOf(false) }

//                LaunchedEffect(Unit) {
//                    downloadApk("citrus_kds.apk", "citrus_kds.apk")
//                }

                LaunchedEffect(homeViewModel.currentState.isVerifyCancel) {
                    if (homeViewModel.currentState.isVerifyCancel) {
                        this@MainActivity.finish()
                    }
                }

                LaunchedEffect(homeViewModel.currentState.printOrder) {
                    homeViewModel.currentState.printOrder?.let {
                        printUtil.setOrderPrint(it)
                    }
                }


                NavHost(
                    navController = navController,
                    startDestination = if (prefs.mode == 0) "kds" else "orderReady"
                ) {

                    composable("kds") {
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
                        OrderReadyScreen(viewModel = homeViewModel, navigateToSetting = {
                            navController.navigate("setting")
                        }) {
                            intentToUpdate(updateAsk)
                        }
                    }

                    composable("setting") {
                        SettingPage(homeViewModel, onVerifyCancel = {}, navigateTo = {
                            navController.navigate("kds")
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

            }
        }


    }

    private fun intentToUpdate(updateAsk: MutableState<Boolean>) {
        val permissionCheck =
            this@MainActivity.let { it1 ->
                ContextCompat.checkSelfPermission(
                    it1,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            this@MainActivity.let { it1 ->
                ActivityCompat.requestPermissions(
                    it1,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    888
                )
            }
        } else {
            updateAsk.value = true
        }
    }


}



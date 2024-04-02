package com.citrus.citruskds

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.CentralViewModel
import com.citrus.citruskds.ui.presentation.KdsScreen
import com.citrus.citruskds.ui.presentation.OrderReadyScreen
import com.citrus.citruskds.ui.presentation.SettingPage
import com.citrus.citruskds.ui.presentation.widget.UpdateDialog
import com.citrus.citruskds.ui.theme.CitrusKDSTheme
import com.citrus.citruskds.util.Constants
import com.citrus.citruskds.util.PrintUtil
import com.citrus.citruskds.util.PrinterDetecter
import com.citrus.citruskds.util.apkDownload.DownloadDetecter
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.citrus.citruskds.util.apkDownload.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

//    SetStock(
//        selectedIcon = R.drawable.ic_setstock_fill,
//        unSelectedIcon = R.drawable.ic_setstock_outline,
//        text = "SetStock",
//    ),

    Setting(
        selectedIcon = R.drawable.ic_setting_fill,
        unSelectedIcon = R.drawable.ic_setting_outline,
        text = "Setting",
    ),
}


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var printUtil: PrintUtil

    @Inject
    lateinit var downloadDetecter: DownloadDetecter

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

                if (updateAsk.value) {
                    UpdateDialog(
                        onDismissRequest = {
                            updateAsk.value = false
                        },
                        finish = {
                            downloadApk(it.first, it.second)
                        },
                        onCancel = {
                            updateAsk.value = false
                        })
                }

            }
        }


        initProgressDialog()

        lifecycleScope.launchWhenStarted {
            downloadDetecter.downloadStatus.collectLatest { event ->
                when (event) {
                    is DownloadStatus.Success -> {
                        mProgressDialog.dismiss()


                        Timber.d("TEST:" + this@MainActivity.packageName)


//                        val apkUri = FileProvider.getUriForFile(
//                            this@MainActivity,
//                            "kds.provider",
//                            getFile()
//                        )
//
//                        MainScope().launch {
//                            try {
//                                val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
//                                install.data = apkUri
//                                install.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                                install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//                                install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
//                                startActivity(install)
//                            } catch (e: Exception) {
//                            }
//
//                        }


                    }

                    is DownloadStatus.Error -> {
                        mProgressDialog.dismiss()
                        Toast.makeText(this@MainActivity, "Download failed", Toast.LENGTH_SHORT)
                            .show()
                    }

                    is DownloadStatus.Progress -> {
                        mProgressDialog.isIndeterminate = false
                        mProgressDialog.progress = event.progress
                    }
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


    private fun downloadApk(name: String, current: String) {
        if (name == current) {
            Toast.makeText(this, "No need to update", Toast.LENGTH_SHORT).show()
            return
        }

        downloadDetecter.intentUpdate(
            getFile(),
            Constants.DOWNLOAD_URL + "citrus_kds_v$name.apk"
        )
        mProgressDialog.show()

        //http://hq.citrus.tw/apk/citrus_kds_v1.0.4.apk
    }

    private fun getFile(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "citrus_kds.apk"
            )
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "citrus_kds.apk"
            )
        }
    }

    private fun initProgressDialog() {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog.setMessage("download progressing..")
        mProgressDialog.isIndeterminate = true
        mProgressDialog.max = 100
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog.setCancelable(false)
        mProgressDialog.setButton(
            Dialog.BUTTON_NEGATIVE,
            "取消"
        ) { _: DialogInterface?, _: Int ->
            downloadDetecter.cancelUpdateJob()
        }
    }


}



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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
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
class MainActivity : ComponentActivity(), ReceiveListener {

    private val REQUEST_PERMISSION = 100
    private val DISCONNECT_INTERVAL = 500

    private var mPrinter: Printer? = null
    private val mPrinterList = ArrayList<Map<String, String>>()

    private var mFilterOption: FilterOption? = null

    @Inject
    lateinit var printerDetecter: PrinterDetecter

    @Inject
    lateinit var downloadDetecter: DownloadDetecter

    private lateinit var mProgressDialog: ProgressDialog


    private val mDiscoveryListener =
        DiscoveryListener { deviceInfo ->
            val item = HashMap<String, String>()
            item["PrinterName"] = deviceInfo.deviceName
            item["Target"] = deviceInfo.target
            Timber.d("PrinterName: ${deviceInfo.deviceName}, Target: ${deviceInfo.target}")
            mPrinterList.add(item)
            MainScope().launch {
                printerDetecter.setValue(mPrinterList)
            }
        }


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

                            KdsScreen(viewModel = homeViewModel, sendToPrint = {
                                scope.launch {
                                    Timber.d("Printer issue trace: step 3")
                                    initializeObject()
                                    runPrintReceiptSequence(it)
                                }
                            }, navigateToOrderReady = {
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

        mFilterOption = FilterOption()
        mFilterOption?.deviceType = Discovery.TYPE_PRINTER
        mFilterOption?.epsonFilter = Discovery.FILTER_NAME
        mFilterOption?.usbDeviceName = Discovery.TRUE

        try {
            Discovery.start(this, mFilterOption, mDiscoveryListener)
        } catch (e: java.lang.Exception) {
            Timber.e(e)
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


    @RequiresApi(Build.VERSION_CODES.O)
    private fun runPrintReceiptSequence(order: Order): Boolean {

        if (!createReceiptData(order)) {
            Timber.d("Printer issue trace: step 4 error")
            return false
        }
        Timber.d("Printer issue trace: step 4")
        return printData()
    }

    private fun String.orCh(cStr: String): String {
        return if (prefs.language == "English") this else cStr
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createReceiptData(order: Order): Boolean {
        val textData: StringBuilder? = StringBuilder()
        if (mPrinter == null) {
            return false
        }
        try {
            mPrinter?.addFeedLine(1)
            mPrinter?.addTextSize(2, 2)
            mPrinter?.addText("${prefs.kdsId}\n")
            mPrinter?.addText("No. ".orCh("单号 ") + order.orderNo + "\n")
            mPrinter?.addFeedLine(1)
            mPrinter?.addTextSize(1, 1)
            //mPrinter?.addTextLang(Printer.LANG_ZH_TW)
            textData!!.append("Order time:".orCh("点单时间:") + order.orderTime + "\n")
            textData.append(
                "Print time:".orCh("打印时间:") + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n"
            )
            textData.append("------------------------------\n")
            mPrinter?.addText(textData.toString())
            textData.delete(0, textData.length)
            mPrinter?.addTextSize(2, 2)
            for (i in order.detail.indices) {
                val name = when (prefs.itemDisplayLan) {
                    "English" -> order.detail[i].eName
                    "华文" -> order.detail[i].cName
                    else -> order.detail[i].eName + " [" + order.detail[i].cName + "]"
                }

                val flavor =
                    if (order.detail[i].flavor.isNullOrBlank()) "" else "\n#${order.detail[i].flavor}"
                textData.append("${order.detail[i].qty} x " + name + flavor + "\n")
            }
            mPrinter?.addText(textData.toString())
            textData.delete(0, textData.length)
            mPrinter?.addTextSize(1, 1)
            textData.append("------------------------------\n")
            textData.append("Total sum: ".orCh("总计: ") + order.detail.size + "\n")
            mPrinter?.addText(textData.toString())
            textData.delete(0, textData.length)
            mPrinter?.addFeedLine(5)
            mPrinter?.addCut(Printer.CUT_FEED)
        } catch (e: java.lang.Exception) {
            mPrinter?.clearCommandBuffer()
            Timber.e(e)
            return false
        }

        return true
    }


    private fun printData(): Boolean {
        if (mPrinter == null) {
            Timber.d("Printer issue trace: step 5 err mPrinter null")
            return false
        }
        if (!connectPrinter()) {
            Timber.d("Printer issue trace: step 5 err connectPrinter")
            mPrinter?.clearCommandBuffer()
            return false
        }
        try {
            mPrinter?.sendData(Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            Timber.d("Printer issue trace: step 5 err ${e.message})")
            mPrinter?.clearCommandBuffer()
            try {
                mPrinter?.disconnect()
            } catch (ex: java.lang.Exception) {
                // Do nothing
            }
            return false
        }
        return true
    }

    private fun connectPrinter(): Boolean {
        if (mPrinter == null) {
            return false
        }
        try {
            mPrinter?.connect(
                prefs.printerTarget,
                Printer.PARAM_DEFAULT
            )
        } catch (e: java.lang.Exception) {
            Timber.e(e)
            return false
        }
        return true
    }


    override fun onPtrReceive(
        printerObj: Printer,
        code: Int,
        status: PrinterStatusInfo?,
        printJobId: String?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            printerDetecter.resetPrintTask()
            disconnectPrinter()
        }

        status?.let {
            Timber.d("" + makeErrorMessage(it))
        }

    }

    private fun makeErrorMessage(status: PrinterStatusInfo): String {
        var msg = ""
        if (status.online == Printer.FALSE) {
            msg += getString(R.string.handlingmsg_err_offline)
        }
        if (status.connection == Printer.FALSE) {
            msg += getString(R.string.handlingmsg_err_no_response)
        }
        if (status.coverOpen == Printer.TRUE) {
            msg += getString(R.string.handlingmsg_err_cover_open)
        }
        if (status.paper == Printer.PAPER_EMPTY) {
            msg += getString(R.string.handlingmsg_err_receipt_end)
        }
        if (status.paperFeed == Printer.TRUE || status.panelSwitch == Printer.SWITCH_ON) {
            msg += getString(R.string.handlingmsg_err_paper_feed)
        }
        if (status.errorStatus == Printer.MECHANICAL_ERR || status.errorStatus == Printer.AUTOCUTTER_ERR) {
            msg += getString(R.string.handlingmsg_err_autocutter)
            msg += getString(R.string.handlingmsg_err_need_recover)
        }
        if (status.errorStatus == Printer.UNRECOVER_ERR) {
            msg += getString(R.string.handlingmsg_err_unrecover)
        }
        if (status.errorStatus == Printer.AUTORECOVER_ERR) {
            if (status.autoRecoverError == Printer.HEAD_OVERHEAT) {
                msg += getString(R.string.handlingmsg_err_overheat)
                msg += getString(R.string.handlingmsg_err_head)
            }
            if (status.autoRecoverError == Printer.MOTOR_OVERHEAT) {
                msg += getString(R.string.handlingmsg_err_overheat)
                msg += getString(R.string.handlingmsg_err_motor)
            }
            if (status.autoRecoverError == Printer.BATTERY_OVERHEAT) {
                msg += getString(R.string.handlingmsg_err_overheat)
                msg += getString(R.string.handlingmsg_err_battery)
            }
            if (status.autoRecoverError == Printer.WRONG_PAPER) {
                msg += getString(R.string.handlingmsg_err_wrong_paper)
            }
        }
        if (status.batteryLevel == Printer.BATTERY_LEVEL_0) {
            msg += getString(R.string.handlingmsg_err_battery_real_end)
        }
        if (status.removalWaiting == Printer.REMOVAL_WAIT_PAPER) {
            msg += getString(R.string.handlingmsg_err_wait_removal)
        }
        if (status.unrecoverError == Printer.HIGH_VOLTAGE_ERR ||
            status.unrecoverError == Printer.LOW_VOLTAGE_ERR
        ) {
            msg += getString(R.string.handlingmsg_err_voltage)
        }
        return msg
    }

    private fun initializeObject(): Boolean {
        try {
            Timber.d("Printer issue trace: step 3")
            mPrinter = Printer(
                22,
                if (prefs.language == "English") 0 else 2,
                this
            )
        } catch (e: java.lang.Exception) {
            Timber.d("Printer issue trace: step 3 error")
            Timber.e(e)
            return false
        }
        mPrinter?.setReceiveEventListener(this)
        return true
    }

    private fun disconnectPrinter() {
        if (mPrinter == null) {
            return
        }
        while (true) {
            try {
                mPrinter?.disconnect()
                break
            } catch (e: Exception) {
                if (e is Epos2Exception) {
                    //Note: If printer is processing such as printing and so on, the disconnect API returns ERR_PROCESSING.
                    if (e.errorStatus == Epos2Exception.ERR_PROCESSING) {
                        try {
                            Thread.sleep(DISCONNECT_INTERVAL.toLong())
                        } catch (ex: Exception) {
                            Timber.e(ex)
                        }
                    } else {
                        runOnUiThread {

                        }
                        break
                    }
                } else {
                    runOnUiThread {

                    }
                    break
                }
            }
        }
        mPrinter?.clearCommandBuffer()
    }
}



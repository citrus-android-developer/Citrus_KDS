package com.citrus.citruskds.util

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class PrintUtil(private val mContext: Context, private val printerDetecter: PrinterDetecter) :
    ReceiveListener {

    private val REQUEST_PERMISSION = 100
    private val DISCONNECT_INTERVAL = 500

    private var mPrinter: Printer? = null
    private val mPrinterList = ArrayList<Map<String, String>>()

    private var mFilterOption: FilterOption? = null

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


    init {
        Timber.d("PrintUtil init")
        mFilterOption = FilterOption()
        mFilterOption?.deviceType = Discovery.TYPE_PRINTER
        mFilterOption?.epsonFilter = Discovery.FILTER_NAME
        mFilterOption?.usbDeviceName = Discovery.TRUE

        try {
            Discovery.start(mContext, mFilterOption, mDiscoveryListener)
        } catch (e: java.lang.Exception) {
            Timber.e(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun setOrderPrint(data: Order) {
        printerDetecter.sendPrintStatus(PrintStatus.Printing)
        if (initializeObject()) {
            runPrintReceiptSequence(data)
        }
    }


    private suspend fun initializeObject(): Boolean {
        try {
            mPrinter = Printer(
                22,
                if (prefs.language == "English") 0 else 2,
                mContext
            )
        } catch (e: java.lang.Exception) {
            printerDetecter.sendPrintStatus(PrintStatus.Error(e.message.toString()))
            return false
        }
        mPrinter?.setReceiveEventListener(this)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun runPrintReceiptSequence(order: Order): Boolean {

        if (!createReceiptData(order)) {
            return false
        }

        return printData()
    }

    private fun String.orCh(cStr: String): String {
        return if (prefs.language == "English") this else cStr
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createReceiptData(order: Order): Boolean {
        val textData: StringBuilder? = StringBuilder()
        if (mPrinter == null) {
            printerDetecter.sendPrintStatus(PrintStatus.Error("Printer Not Initialized"))
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
            printerDetecter.sendPrintStatus(PrintStatus.Error(e.message.toString()))
            return false
        }

        return true
    }

    private suspend fun printData(): Boolean {
        if (mPrinter == null) {
            return false
        }
        if (!connectPrinter()) {
            mPrinter?.clearCommandBuffer()
            return false
        }
        try {
            mPrinter?.sendData(Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            mPrinter?.clearCommandBuffer()
            try {
                mPrinter?.disconnect()
            } catch (ex: java.lang.Exception) {
                printerDetecter.sendPrintStatus(PrintStatus.Error(e.message.toString()))
            }
            printerDetecter.sendPrintStatus(PrintStatus.Error(e.message.toString()))
            return false
        }
        return true
    }

    private suspend fun connectPrinter(): Boolean {
        if (mPrinter == null) {
            return false
        }
        try {
            mPrinter?.connect(
                prefs.printerTarget,
                Printer.PARAM_DEFAULT
            )
        } catch (e: java.lang.Exception) {
            printerDetecter.sendPrintStatus(PrintStatus.Error(e.message.toString()))
            return false
        }
        return true
    }

    private suspend fun disconnectPrinter() {
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
                        break
                    }
                } else {
                    break
                }
            }
        }
        mPrinter?.clearCommandBuffer()
        printerDetecter.sendPrintStatus(PrintStatus.Idle)
    }

    override fun onPtrReceive(
        printerObj: Printer,
        code: Int,
        status: PrinterStatusInfo?,
        printJobId: String?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            disconnectPrinter()
        }

        status?.let {
            Timber.d("" + makeErrorMessage(it))
        }

    }


    private fun makeErrorMessage(status: PrinterStatusInfo): String {
        var msg = ""
        if (status.online == Printer.FALSE) {
            msg += mContext.getString(R.string.handlingmsg_err_offline)
        }
        if (status.connection == Printer.FALSE) {
            msg += mContext.getString(R.string.handlingmsg_err_no_response)
        }
        if (status.coverOpen == Printer.TRUE) {
            msg += mContext.getString(R.string.handlingmsg_err_cover_open)
        }
        if (status.paper == Printer.PAPER_EMPTY) {
            msg += mContext.getString(R.string.handlingmsg_err_receipt_end)
        }
        if (status.paperFeed == Printer.TRUE || status.panelSwitch == Printer.SWITCH_ON) {
            msg += mContext.getString(R.string.handlingmsg_err_paper_feed)
        }
        if (status.errorStatus == Printer.MECHANICAL_ERR || status.errorStatus == Printer.AUTOCUTTER_ERR) {
            msg += mContext.getString(R.string.handlingmsg_err_autocutter)
            msg += mContext.getString(R.string.handlingmsg_err_need_recover)
        }
        if (status.errorStatus == Printer.UNRECOVER_ERR) {
            msg += mContext.getString(R.string.handlingmsg_err_unrecover)
        }
        if (status.errorStatus == Printer.AUTORECOVER_ERR) {
            if (status.autoRecoverError == Printer.HEAD_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat)
                msg += mContext.getString(R.string.handlingmsg_err_head)
            }
            if (status.autoRecoverError == Printer.MOTOR_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat)
                msg += mContext.getString(R.string.handlingmsg_err_motor)
            }
            if (status.autoRecoverError == Printer.BATTERY_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat)
                msg += mContext.getString(R.string.handlingmsg_err_battery)
            }
            if (status.autoRecoverError == Printer.WRONG_PAPER) {
                msg += mContext.getString(R.string.handlingmsg_err_wrong_paper)
            }
        }
        if (status.batteryLevel == Printer.BATTERY_LEVEL_0) {
            msg += mContext.getString(R.string.handlingmsg_err_battery_real_end)
        }
        if (status.removalWaiting == Printer.REMOVAL_WAIT_PAPER) {
            msg += mContext.getString(R.string.handlingmsg_err_wait_removal)
        }
        if (status.unrecoverError == Printer.HIGH_VOLTAGE_ERR ||
            status.unrecoverError == Printer.LOW_VOLTAGE_ERR
        ) {
            msg += mContext.getString(R.string.handlingmsg_err_voltage)
        }
        return msg
    }
}
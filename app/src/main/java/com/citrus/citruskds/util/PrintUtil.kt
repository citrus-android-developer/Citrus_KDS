package com.citrus.citruskds.util

import android.content.Context
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.util.lanprint.EscPosReceiptBuilder
import com.citrus.citruskds.util.lanprint.LanPrinter
import com.citrus.citruskds.util.lanprint.NetworkScanner
import com.citrus.citruskds.util.lanprint.PrinterResult
import timber.log.Timber

/**
 * 列印工具（LAN 版）。
 * 已從 EPSON ePos USB 探索/連線改為 ESC/POS over TCP（RAW socket 9100）。
 * 對外介面維持不變：[setOrderPrint] + 透過 [PrinterDetecter] 回報 [PrintStatus]。
 */
class PrintUtil(
    @Suppress("unused") private val mContext: Context,
    private val printerDetecter: PrinterDetecter,
) {

    /** 列印收據（內容由 [EscPosReceiptBuilder] 1:1 重現舊版） */
    suspend fun setOrderPrint(data: Order) {
        Timber.d("PRINT_TRACE setOrderPrint 觸發: orderNo=${data.orderNo} printMode=${prefs.printMode} ip=${prefs.printerIp}:${prefs.printerPort}")
        printerDetecter.sendPrintStatus(PrintStatus.Printing)

        val ip = prefs.printerIp
        if (ip.isBlank()) {
            printerDetecter.sendPrintStatus(PrintStatus.Error("印表機 IP 未設定，請至設定頁輸入"))
            return
        }

        val bytes = try {
            EscPosReceiptBuilder.build(data)
        } catch (e: Exception) {
            Timber.e(e, "建立收據失敗")
            printerDetecter.sendPrintStatus(PrintStatus.Error("收據建立失敗：${e.message}"))
            return
        }

        when (val result = LanPrinter(ip = ip, port = prefs.printerPort).print(bytes)) {
            is PrinterResult.Success -> printerDetecter.sendPrintStatus(PrintStatus.Idle)
            is PrinterResult.Failure -> printerDetecter.sendPrintStatus(PrintStatus.Error(result.message))
        }
    }

    /** 測試印表機連線（設定頁「測試連線」用） */
    suspend fun testConnection(ip: String, port: Int): Boolean =
        LanPrinter(ip = ip, port = port).ping()

    /** 掃描區網印表機（設定頁「掃描」用） */
    suspend fun scanPrinters(subnet: String): List<String> =
        NetworkScanner.scan(subnet)
}

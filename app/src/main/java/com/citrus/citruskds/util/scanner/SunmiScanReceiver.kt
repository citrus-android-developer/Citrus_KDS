package com.citrus.citruskds.util.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * SUNMI 掃描廣播接收器（內建紅外線掃描頭 Serial + 外接 USB 掃描槍共用）。
 * 對齊官方《Barcode Scanner User Guide》：掃描器需設為「廣播輸出」模式。
 *
 * 掃描成功 → 系統發出 [ACTION_DATA_CODE_RECEIVED] 廣播，
 * 字串資料在 extra `"data"`（不含結束符 / CodeID）。
 */
class SunmiScanReceiver(
    private val onScan: (text: String) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DATA_CODE_RECEIVED) return
        val text = intent.getStringExtra(EXTRA_DATA).orEmpty().trim()
        Timber.d("SunmiScan received: $text")
        if (text.isNotEmpty()) onScan(text)
    }

    companion object {
        const val ACTION_DATA_CODE_RECEIVED = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        const val EXTRA_DATA = "data"
    }
}

package com.citrus.citruskds.util.lanprint

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/** 列印結果 */
sealed class PrinterResult {
    object Success : PrinterResult()
    data class Failure(val message: String) : PrinterResult()
}

/**
 * LAN 熱感印表機（RAW Socket / Port 9100）。
 * 直接把 ESC/POS bytes 送到印表機，取代原本 EPSON ePos USB 連線。
 */
class LanPrinter(
    private val ip: String,
    private val port: Int = 9100,
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 10_000,
) {

    suspend fun print(data: ByteArray): PrinterResult = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), connectTimeoutMs)
                socket.soTimeout = readTimeoutMs
                socket.getOutputStream().apply {
                    write(data)
                    flush()
                }
                PrinterResult.Success
            }
        } catch (e: SocketTimeoutException) {
            PrinterResult.Failure("連線逾時：$ip:$port")
        } catch (e: ConnectException) {
            PrinterResult.Failure("無法連線：$ip:$port")
        } catch (e: IOException) {
            PrinterResult.Failure("資料傳送失敗：${e.message}")
        } catch (e: Exception) {
            PrinterResult.Failure(e.message ?: "未知錯誤")
        }
    }

    /** 存活檢測：能連上 9100 即視為在線 */
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), connectTimeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

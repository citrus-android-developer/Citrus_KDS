package com.citrus.citruskds.util.lanprint

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/** 掃描區網內 9100 埠可達的印表機 */
object NetworkScanner {

    suspend fun scan(
        subnet: String,
        port: Int = 9100,
        timeoutMs: Int = 250,
    ): List<String> = withContext(Dispatchers.IO) {
        // supervisorScope：單一 IP 失敗不會取消整批掃描
        supervisorScope {
            (1..254).map { host ->
                async {
                    val ip = "$subnet.$host"
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(ip, port), timeoutMs)
                            ip
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }
}

package com.citrus.citruskds.util.lanprint

import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.flavorDisplay
import com.citrus.citruskds.commonData.vo.isComboMain
import com.citrus.citruskds.di.prefs
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把訂單組成 ESC/POS bytes（熱感收據）。
 * 內容 1:1 對應原本 EPSON 版本（[com.citrus.citruskds.util.PrintUtil] 舊版 createReceiptData）：
 *   放大字單號 → 時間 → 分隔線 → 放大字品項 → 分隔線 → 總計 → 進紙 → 切紙。
 *
 * 中文：原機型為簡中（EPSON model lang=2），故文字以 GBK 編碼並啟用 FS & 中文模式。
 */
object EscPosReceiptBuilder {

    private const val ESC = 0x1B
    private const val GS = 0x1D
    private const val FS = 0x1C

    private fun String.orCh(cStr: String): String =
        if (prefs.language == "English") this else cStr

    fun build(order: Order): ByteArray {
        val out = ByteArrayOutputStream()

        // 初始化 + 進入中文模式（讓 2-byte GBK 中文被正確辨識）
        out.write(byteArrayOf(ESC.toByte(), '@'.code.toByte()))   // ESC @ 重置
        out.write(byteArrayOf(FS.toByte(), '&'.code.toByte()))    // FS & 中文模式

        feed(out, 1)

        // 放大字：KDS ID + 單號
        size(out, double = true)
        text(out, "${prefs.kdsId}\n")
        text(out, "No. ".orCh("单号 ") + order.orderNo + "\n")

        // 加點單：最上方標註「加點」（跟品項名稱同一套語言設定 itemDisplayLan）
        if (order.addonPrint) {
            val addonLabel = when (prefs.itemDisplayLan) {
                "English" -> "ADD ORDER"
                "华文" -> "加点"
                else -> "加点 / ADD ORDER"
            }
            text(out, "** $addonLabel **\n")
        }

        feed(out, 1)

        // 正常字：時間 + 分隔線
        size(out, double = false)
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        text(out, "Order time:".orCh("点单时间:") + order.orderTime + "\n")
        text(out, "Print time:".orCh("打印时间:") + now + "\n")
        text(out, "------------------------------\n")

        // 放大字：品項
        size(out, double = true)
        for (d in order.detail) {
            val name = when (prefs.itemDisplayLan) {
                "English" -> d.eName
                "华文" -> d.cName
                else -> d.eName + " [" + d.cName + "]"
            }
            val flavorStr = d.flavorDisplay(prefs.itemDisplayLan)
            val flavor = if (flavorStr.isBlank()) "" else "\n#$flavorStr"
            // 套餐主項(GType G/M)只印名稱、不印數量；其餘正常印「數量 x 名稱」
            val line = if (d.isComboMain) name + flavor else "${d.qty} x " + name + flavor
            text(out, line + "\n")
        }

        // 正常字：分隔線 + 總計
        size(out, double = false)
        text(out, "------------------------------\n")
        text(out, "Total sum: ".orCh("总计: ") + order.detail.size + "\n")

        // 進紙 + 切紙
        feed(out, 5)
        out.write(byteArrayOf(GS.toByte(), 'V'.code.toByte(), 66, 0))  // GS V 66 0：進紙到切點並半切

        return out.toByteArray()
    }

    /**
     * 編碼診斷表：同一段中文用多種編碼/中文模式各印一行，前面有純英數標籤。
     * 用來找出此印表機正確的中文編碼。確認後即可移除。
     */
    fun buildEncodingTest(): ByteArray {
        val out = ByteArrayOutputStream()
        val zh = "单号 小菜 川香牛腱"

        fun reset() = out.write(byteArrayOf(ESC.toByte(), '@'.code.toByte()))
        fun ascii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun lf() = out.write('\n'.code)
        fun zhBytes(cs: String) = try {
            out.write(zh.toByteArray(charset(cs)))
        } catch (e: Exception) {
            ascii("($cs fail)")
        }
        fun fsAnd() = out.write(byteArrayOf(FS.toByte(), '&'.code.toByte()))

        reset(); ascii("== ENCODING TEST =="); lf(); lf()

        // 1 GBK + FS&
        reset(); fsAnd(); ascii("1 GBK+FS&: "); zhBytes("GBK"); lf()
        // 2 GBK only
        reset(); ascii("2 GBK: "); zhBytes("GBK"); lf()
        // 3 GBK + FS C 1 + FS&
        reset(); out.write(byteArrayOf(FS.toByte(), 'C'.code.toByte(), 0x01)); fsAnd()
        ascii("3 FSC1: "); zhBytes("GBK"); lf()
        // 4 UTF-8
        reset(); ascii("4 UTF8: "); zhBytes("UTF-8"); lf()
        // 5 Big5 + FS&
        reset(); fsAnd(); ascii("5 Big5: "); zhBytes("Big5"); lf()

        out.write(byteArrayOf(ESC.toByte(), 'd'.code.toByte(), 4))
        out.write(byteArrayOf(GS.toByte(), 'V'.code.toByte(), 66, 0))
        return out.toByteArray()
    }

    /** GBK 編碼（目標：簡體 + 英文；印表機請設為簡中/GBK 模式。ASCII 1 byte、簡中 2 byte） */
    private fun text(out: ByteArrayOutputStream, s: String) =
        out.write(s.toByteArray(charset("GBK")))

    /** GS ! n：0x11=寬高各 2 倍，0x00=正常 */
    private fun size(out: ByteArrayOutputStream, double: Boolean) =
        out.write(byteArrayOf(GS.toByte(), '!'.code.toByte(), if (double) 0x11 else 0x00))

    /** ESC d n：印出並進 n 行 */
    private fun feed(out: ByteArrayOutputStream, n: Int) =
        out.write(byteArrayOf(ESC.toByte(), 'd'.code.toByte(), n.toByte()))
}

package com.citrus.citruskds

import com.citrus.citruskds.util.lanprint.receiptDashLine
import org.junit.Assert.assertEquals
import org.junit.Test

/** 收據分隔線填滿紙寬：80mm=48 字、58mm=32 字（正常字級每行字數）。 */
class ReceiptDashLineTest {

    @Test
    fun width80mm() {
        assertEquals(48, receiptDashLine(true).length)
        assertEquals("-".repeat(48), receiptDashLine(true))
    }

    @Test
    fun width58mm() {
        assertEquals(32, receiptDashLine(false).length)
    }
}

package com.citrus.citruskds

import com.citrus.citruskds.commonData.vo.stockNameWithSize
import org.junit.Assert.assertEquals
import org.junit.Test

/** 庫存名稱接 Size：有實際規格才加「(S)」，"." / 空白不加（後端無規格時填 "."）。 */
class StockNameWithSizeTest {

    @Test
    fun appendsRealSize() {
        assertEquals("咖啡 (M)", stockNameWithSize("咖啡", "M"))
        assertEquals("Milk Tea (S)", stockNameWithSize("Milk Tea", "S"))
    }

    @Test
    fun dotPlaceholderNotAppended() {
        assertEquals("Pilot Stick", stockNameWithSize("Pilot Stick", "."))
    }

    @Test
    fun emptyOrNullNotAppended() {
        assertEquals("Sundae", stockNameWithSize("Sundae", ""))
        assertEquals("Sundae", stockNameWithSize("Sundae", null))
        assertEquals("Sundae", stockNameWithSize("Sundae", "  "))
    }
}

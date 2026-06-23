package com.citrus.citruskds

import com.citrus.citruskds.commonData.vo.stockDisplaySize
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

    // --- 依語言挑規格 ---
    @Test
    fun englishUsesSizeEn() {
        assertEquals("M", stockDisplaySize("English", "中", "M"))   // 英文模式 → 英文規格
    }

    @Test
    fun chineseUsesRawSize() {
        assertEquals("中", stockDisplaySize("华文", "中", "M"))      // 中文模式 → 原始規格
    }

    @Test
    fun englishFallsBackWhenNoSizeEn() {
        assertEquals("中", stockDisplaySize("English", "中", null)) // 英文規格缺 → fallback 原始
        assertEquals("中", stockDisplaySize("English", "中", ""))
    }
}

package com.citrus.citruskds

import com.citrus.citruskds.commonData.vo.middleItemLine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 附餐（套餐 middleDetail）行格式：一律 "- {qty} x {名稱}"。
 * 迴歸點：原 OrderItem.kt:187 只印 "- 名稱"，附餐 Qty>1 時廚房會少做（見 [[ISSUE-附餐顯示缺陷]] 缺陷1）。
 */
class SideDishLineTest {

    @Test
    fun qtyOneShowsCount() {
        assertEquals("- 1 x 蘑菇義大利麵", middleItemLine(1, "蘑菇義大利麵"))
    }

    @Test
    fun qtyTwoShowsCount() {
        assertEquals("- 2 x 奶茶 (S)", middleItemLine(2, "奶茶 (S)"))
    }
}

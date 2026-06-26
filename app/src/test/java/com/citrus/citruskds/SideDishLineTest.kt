package com.citrus.citruskds

import com.citrus.citruskds.commonData.vo.middleItemLine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 附餐（套餐 middleDetail）行格式："- {qty} x {名稱}"，後接可選 "\n#{調味}" / "\n#{加料}"。
 * 迴歸點1：原只印 "- 名稱"，附餐 Qty>1 時廚房會少做（缺陷1）。
 * 迴歸點3：附餐不顯示調味加料（缺陷3）——主項有印、附餐沒印，與列印不一致（見 [[ISSUE-附餐顯示缺陷]]）。
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

    @Test
    fun emptyFlavorAdditionUnchanged() {
        // 缺陷3 不回歸：無調味加料時格式與舊版完全相同
        assertEquals("- 2 x 蘑菇義大利麵", middleItemLine(2, "蘑菇義大利麵", "", ""))
    }

    @Test
    fun flavorShown() {
        assertEquals("- 2 x 蘑菇義大利麵\n#辣", middleItemLine(2, "蘑菇義大利麵", "辣", ""))
    }

    @Test
    fun additionShown() {
        assertEquals("- 2 x 蘑菇義大利麵\n#多麵*2,少麵*1", middleItemLine(2, "蘑菇義大利麵", "", "多麵*2,少麵*1"))
    }

    @Test
    fun flavorAndAdditionShown() {
        assertEquals(
            "- 2 x 蘑菇義大利麵\n#辣\n#多麵*2,少麵*1",
            middleItemLine(2, "蘑菇義大利麵", "辣", "多麵*2,少麵*1")
        )
    }
}

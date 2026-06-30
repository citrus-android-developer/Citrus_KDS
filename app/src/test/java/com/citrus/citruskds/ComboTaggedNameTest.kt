package com.citrus.citruskds

import com.citrus.citruskds.commonData.vo.comboTaggedName
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 套餐主餐名稱前綴 "□"（顯示與列印一致；卡片/已出餐/全品項/收據共用）。
 * 主餐(G/M)=isComboMain true 加 "□"；一般品項與附餐不加。
 */
class ComboTaggedNameTest {

    @Test
    fun comboMainGetsBoxPrefix() {
        assertEquals("□G套餐", comboTaggedName(true, "G套餐"))
        assertEquals("□M Set", comboTaggedName(true, "M Set"))
    }

    @Test
    fun normalItemUnchanged() {
        assertEquals("蘑菇義大利麵", comboTaggedName(false, "蘑菇義大利麵"))
    }

    @Test
    fun bilingualComboNameTagged() {
        assertEquals("□G套餐 [G Set]", comboTaggedName(true, "G套餐 [G Set]"))
    }
}

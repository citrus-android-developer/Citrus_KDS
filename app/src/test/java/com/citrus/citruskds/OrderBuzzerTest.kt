package com.citrus.citruskds

import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.buzzerNo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 綁定合約：KDS 卡片的震動器號碼來自後端 OrdersList 回傳的 "OrderNote" 欄位。
 * 真實 API（192.168.0.162:8099 /KDS/OrdersList）回傳的 key 是 "OrderNote"，值形如 "123\"。
 * 迴歸點：曾誤標 @Json(name="Note") 導致永遠對不到 key → note=null → 卡片不顯示（自 ab02278 從未生效）。
 */
class OrderBuzzerTest {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(Order::class.java)

    private fun parse(orderNoteJson: String): Order = adapter.fromJson(
        """{"Detail":[],"OrderTime":"2026-06-17 16:07:26","OrderNo":"W012026061700008",""" +
            """"ServiceType":"D","Status":"W",$orderNoteJson}"""
    )!!

    @Test
    fun orderNoteKeyMapsToBuzzerNo() {
        val order = parse(""""OrderNote":"123\\"""")
        assertEquals("123\\", order.note)   // 原始值含尾綴反斜線
        assertEquals("123", order.buzzerNo) // 顯示用：去尾綴
    }

    @Test
    fun backslashOnlyIsNoBuzzer() {
        assertNull(parse(""""OrderNote":"\\"""").buzzerNo)
    }

    @Test
    fun emptyIsNoBuzzer() {
        assertNull(parse(""""OrderNote":""""").buzzerNo)
    }

    @Test
    fun missingOrderNoteIsNoBuzzer() {
        assertNull(parse(""""x":0""").buzzerNo)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        // 去尾綴反斜線後再 trim：" 123\" → "123"
        assertEquals("123", parse(""""OrderNote":" 123\\"""").buzzerNo)
    }
}

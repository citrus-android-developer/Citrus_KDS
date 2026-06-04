package com.citrus.citruskds.commonData.vo

import com.citrus.citruskds.di.prefs
import com.squareup.moshi.Json

data class SetOrderStatusRequest(
    @Json(name = "OrderNo")
    val orderNo: String,
    @Json(name = "Status")
    val status: String,
    @Json(name = "KDS_ID")
    val kdsId: String = prefs.kdsId,
    /** 來源狀態(選配，逗號分隔如 "j,J")：只更新目前為這些狀態的品項；空=更新整單(向後相容) */
    @Json(name = "FromStatus")
    val fromStatus: String? = null,
)
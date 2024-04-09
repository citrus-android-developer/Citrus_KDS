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
)
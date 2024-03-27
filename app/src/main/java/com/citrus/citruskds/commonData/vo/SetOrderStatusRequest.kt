package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json

data class SetOrderStatusRequest(
    @Json(name = "OrderNo")
    val orderNo: String,
    @Json(name = "Status")
    val status: String,
)
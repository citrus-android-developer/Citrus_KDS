package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json

data class OrderRequest(
    @Json(name = "KDS_ID")
    val kdsId: String,
    @Json(name = "OrderStatus")
    val type: String,
)
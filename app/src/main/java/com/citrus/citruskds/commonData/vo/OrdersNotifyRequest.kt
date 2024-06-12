package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json

data class OrdersNotifyRequest(
    @Json(name = "StoreNo")
    var storeNo: String,
    @Json(name = "OrderNo")
    var orderNo: String
)
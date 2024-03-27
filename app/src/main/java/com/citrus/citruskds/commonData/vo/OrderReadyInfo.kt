package com.citrus.citruskds.commonData.vo


import com.squareup.moshi.Json

data class OrderReadyInfo(
    @Json(name = "OrderName")
    var orderName: String,
    @Json(name = "OrderNo")
    var orderNo: List<String>
)
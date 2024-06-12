package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json


data class Order(
    @Json(name = "Detail")
    var detail: List<Detail>,
    @Json(name = "OrderTime")
    var orderTime: String,
    @Json(name = "OrderNo")
    var orderNo: String,
    @Json(name = "ServiceType")
    var serviceType: String,
    @Json(name = "Status")
    var status: String,
    @Json(ignore = true)
    var isVisible: Boolean = true
)

data class Detail(
    @Json(name = "GType")
    var gType: String,
    @Json(name = "CName")
    var cName: String,
    @Json(name = "EName")
    var eName: String,
    @Json(name = "Flavor")
    var flavor: String?,
    @Json(name = "Addition")
    var addition: String?,
    @Json(name = "GID")
    var gID: String,
    @Json(name = "GKID")
    var gKID: String,
    @Json(name = "Qty")
    var qty: Int,
    @Json(name = "Price")
    var price: Double,
    @Json(name = "middleDetail")
    var middleDetail: List<Detail>?,
)



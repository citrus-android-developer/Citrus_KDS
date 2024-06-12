package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json


data class StockInfo(
    @Json(name = "CName")
    var cName: String?,
    @Json(name = "EName")
    var eName: String?,
    @Json(name = "GID")
    var gID: String?,
    @Json(name = "GK_CName")
    var gKCName: String?,
    @Json(name = "GK_EName")
    var gKEName: String?,
    @Json(name = "GKID")
    var gKID: String?,
    @Json(name = "SellStatus")
    var sellStatus: String?,
    @Json(name = "Size")
    var size: String?,
)
package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json

data class SetItemSellStatusRequest(
    @Json(name = "StoreNo")
    var storeNo: String,
    @Json(name = "GKID")
    var gKID: String,
    @Json(name = "GID")
    var gID: String,
    @Json(name = "Gname")
    var gname: String,
    @Json(name = "Status")
    var status: String,
    @Json(name = "Size")
    var size: String
)
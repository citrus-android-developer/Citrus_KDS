package com.citrus.citruskds.commonData.vo


import com.squareup.moshi.Json

data class SetInventoryRequest(
    @Json(name = "GID")
    var gID: String,
    @Json(name = "GKID")
    var gKID: String,
    @Json(name = "Stock")
    var stock: Int
)
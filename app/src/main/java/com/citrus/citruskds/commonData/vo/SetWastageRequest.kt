package com.citrus.citruskds.commonData.vo

import com.citrus.citruskds.di.prefs
import com.squareup.moshi.Json

/**
 * 損耗/報廢請求。GKID/GID/Qty/CreateUser/Status 由前端帶；
 * 其餘(Gname/sizedesc/GPrice/CreateDate/Flag)由後端依 Goods 主檔填。
 * Status：W=報廢、S=損耗。CreateUser 預設帶 KDS 編號。
 */
data class SetWastageRequest(
    @Json(name = "GKID")
    val gKID: String,
    @Json(name = "GID")
    val gID: String,
    @Json(name = "Qty")
    val qty: Int,
    @Json(name = "Status")
    val status: String,
    @Json(name = "CreateUser")
    val createUser: String = prefs.kdsId,
)

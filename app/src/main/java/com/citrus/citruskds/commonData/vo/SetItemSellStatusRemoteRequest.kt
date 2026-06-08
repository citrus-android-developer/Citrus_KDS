package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json

/**
 * 雲端(remote) soldout 請求。對齊 CompassKDS 後端精簡 contract：
 * 僅帶 StoreNo/GKID/GID/Status，Gname/Size 由後端依 Goods 主檔補齊。
 * 本地(local) POS 仍沿用含 Gname/Size 的 [SetItemSellStatusRequest]，不受影響。
 */
data class SetItemSellStatusRemoteRequest(
    @Json(name = "StoreNo")
    val storeNo: String,
    @Json(name = "GKID")
    val gKID: String,
    @Json(name = "GID")
    val gID: String,
    @Json(name = "Status")
    val status: String
)

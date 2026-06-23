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

/**
 * 庫存品項顯示名稱：名稱後接 Size「名稱 (S)」，讓同名不同 Size 的品項可分辨。
 * Size 為空或佔位符「.」時不附（後端無實際規格時用 "." 填）。
 */
fun stockNameWithSize(name: String, size: String?): String {
    val s = size?.trim().orEmpty()
    return if (s.isNotEmpty() && s != ".") "$name ($s)" else name
}
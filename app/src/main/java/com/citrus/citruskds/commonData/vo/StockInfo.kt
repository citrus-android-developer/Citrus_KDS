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
    @Json(name = "SizeEN")
    var sizeEn: String? = null,
)

/**
 * 依系統語言挑規格顯示值：英文模式用 sizeEn(後端 SizeDesc.GName2)、否則用 size(中文/原始)。
 * 英文 size 為空時 fallback 回原始 size。
 */
fun stockDisplaySize(language: String?, size: String?, sizeEn: String?): String? =
    if (language == "English") sizeEn?.takeIf { it.isNotBlank() } ?: size else size

/**
 * 依系統語言挑品項名稱，**空白(非僅 null)時 fallback 另一語系**。
 * 英文模式：eName 有值用 eName、空則用 cName；中文模式反之。(原 `?:` 只擋 null,空字串會顯示空白)
 */
fun stockName(language: String?, eName: String?, cName: String?): String {
    val en = eName?.trim().orEmpty()
    val zh = cName?.trim().orEmpty()
    return if (language == "English") en.ifBlank { zh } else zh.ifBlank { en }
}

/**
 * 庫存品項顯示名稱：名稱後接 Size「名稱 (S)」，讓同名不同 Size 的品項可分辨。
 * Size 為空或佔位符「.」時不附（後端無實際規格時用 "." 填）。
 */
fun stockNameWithSize(name: String, size: String?): String {
    val s = size?.trim().orEmpty()
    return if (s.isNotEmpty() && s != ".") "$name ($s)" else name
}
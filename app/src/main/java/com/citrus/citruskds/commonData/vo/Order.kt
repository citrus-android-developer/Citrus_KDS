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
    @Json(name = "Note")
    var note: String? = null,
    @Json(ignore = true)
    var isVisible: Boolean = true,
    /** 列印標記：此份為「加點單」(只含新增品項，標題印「加點」)。非 API 欄位 */
    @Json(ignore = true)
    var addonPrint: Boolean = false
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
    @Json(name = "Flavor2")
    var flavor2: String? = null,
    @Json(name = "Addition")
    var addition: String?,
    @Json(name = "Addition2")
    var addition2: String? = null,
    @Json(name = "GID")
    var gID: String,
    @Json(name = "GKID")
    var gKID: String,
    @Json(name = "Qty")
    var qty: Int,
    @Json(name = "Price")
    var price: Double,
    @Json(name = "ItemStatus")
    var itemStatus: String? = null,
    @Json(name = "middleDetail")
    var middleDetail: List<Detail>?,
)

/** 該品項是否為「未接的新品項」(j/J)。itemStatus 為 null（後端未提供）時視為非未接 */
val Detail.isPending: Boolean get() = itemStatus?.uppercase() == "J"

/**
 * 加點：同一張單同時有「未接(j)品項」與「已接(W/O)品項」。
 * 全新單(全 j)不算加點；itemStatus 全 null（後端未部署）時恆為 false（無回歸）。
 */
val Order.isAddon: Boolean
    get() {
        val s = detail.mapNotNull { it.itemStatus?.uppercase() }
        return s.any { it == "J" } && s.any { it == "W" || it == "O" }
    }

/** 加點時要升級/列印的新增品項（未接 j/J） */
val Order.addonItems: List<Detail> get() = detail.filter { it.isPending }

/**
 * 卡片顯示用派生狀態：有任何未接(j)→J(新單)；否則有 W→W(製作中)；否則有 O→O(待取)；
 * 否則回退整單 status。讓加點(混合狀態)顯示為新單，且「有 W 就製作中」。
 * 後端未提供 per-item itemStatus 時，全部回退到整單 status（無回歸）。
 */
fun Order.displayStatus(): String {
    val s = detail.mapNotNull { it.itemStatus?.uppercase() }
    return when {
        s.any { it == "J" } -> "J"
        s.any { it == "W" } -> "W"
        s.any { it == "O" } -> "O"
        else -> status.uppercase()
    }
}

/** 套餐主項（GType G/M）：顯示與列印時只印名稱、不印數量 */
val Detail.isComboMain: Boolean get() = gType == "G" || gType == "M"

/** 套餐附餐（GType S/R）：M 套餐附餐為 S、G 套餐附餐為 R */
val Detail.isSideDish: Boolean get() = gType == "S" || gType == "R"

/**
 * 依語言設定挑調味顯示字串。`flavor`=第一語言(中)、`flavor2`=第二語言(英)。
 * 第二語言為空時 fallback 第一語言（後端資料常缺第二語言），反之亦然。
 * lan: "English" / "华文" / "English & 华文"（後者中英並列）
 */
fun Detail.flavorDisplay(lan: String): String = pickLang(flavor, flavor2, lan)

/** 依語言設定挑加料顯示字串（規則同 [flavorDisplay]）。 */
fun Detail.additionDisplay(lan: String): String = pickLang(addition, addition2, lan)

private fun pickLang(primaryZh: String?, secondEn: String?, lan: String): String {
    val zh = primaryZh?.trim().orEmpty()
    val en = secondEn?.trim().orEmpty()
    return when (lan) {
        "English" -> en.ifBlank { zh }
        "English & 华文" -> listOf(zh, en).filter { it.isNotEmpty() }.joinToString(" / ")
        else -> zh.ifBlank { en }   // 华文 / 預設
    }
}



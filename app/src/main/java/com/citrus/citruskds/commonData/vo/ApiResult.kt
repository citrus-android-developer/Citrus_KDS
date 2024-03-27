package com.citrus.citruskds.commonData.vo

import com.squareup.moshi.Json

data class ApiResult<T>(
    @Json(name = "ApiStatus")
    val status: String,
    @Json(name = "Data")
    val data: T? = null,
    @Json(name = "Error")
    val error: CodeMessage? = null,
)

data class CodeMessage(
    @Json(name = "Code") var code: String?,
    @Json(name = "Message") var message: String?,
)

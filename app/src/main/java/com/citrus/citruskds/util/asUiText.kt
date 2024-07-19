package com.citrus.citruskds.util

import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.NetworkError
import com.citrus.citruskds.commonData.RootError
import com.citrus.citruskds.commonData.Result

fun NetworkError.asUiText(): UiText {
    return when (this) {
        is NetworkError.DataFetchFailed -> UiText.MultiUiText(
            listOf(
                UiText.StringResource(
                    R.string.data_fetch_error
                ), UiText.DynamicString(errMsg ?: "")
            )
        )

        is NetworkError.HttpError -> UiText.MultiUiText(
            listOf(
                UiText.DynamicString(errMsg ?: ""),
                UiText.StringResource(
                    R.string.http_error, arrayOf(code)
                )
            )
        )

        is NetworkError.NoInternet -> UiText.MultiUiText(
            listOf(
                UiText.DynamicString(errMsg ?: ""),
                UiText.StringResource(
                    R.string.no_internet_error
                )
            )
        )

        is NetworkError.RequestTimeout -> UiText.MultiUiText(
            listOf(
                UiText.DynamicString(errMsg ?: ""),
                UiText.StringResource(
                    R.string.the_request_timed_out
                )
            )
        )

        is NetworkError.Serialization -> UiText.MultiUiText(
            listOf(
                UiText.DynamicString(errMsg ?: ""),
                UiText.StringResource(
                    R.string.json_serialization_error
                )
            )
        )

        is NetworkError.ServerError -> UiText.MultiUiText(
            listOf(
                UiText.DynamicString(errMsg ?: ""),
                UiText.StringResource(
                    R.string.server_error
                )
            )
        )

        is NetworkError.ConnectionError -> UiText.MultiUiText(
            listOf(
                UiText.DynamicString(errMsg ?: ""),
                UiText.StringResource(
                    R.string.connection_error
                )
            )
        )

        is NetworkError.UnknownError -> UiText.MultiUiText(
            listOf(
                UiText.DynamicString(errMsg ?: ""),
                UiText.StringResource(
                    R.string.unknown_error
                )
            )
        )
    }
}

fun Result.Error<*, RootError>.asErrorUiText(): UiText {
    return (error as NetworkError).asUiText()
}
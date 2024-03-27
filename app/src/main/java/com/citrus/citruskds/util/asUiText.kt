package com.citrus.citruskds.util

import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.NetworkError
import com.citrus.citruskds.commonData.RootError
import com.citrus.citruskds.commonData.Result

fun NetworkError.asUiText(): UiText {
    return when (this) {
        is NetworkError.DataFetchFailed -> UiText.StringResource(
            R.string.data_fetch_error
        )

        NetworkError.HttpError -> UiText.StringResource(
            R.string.http_error
        )

        NetworkError.NoInternet -> UiText.StringResource(
            R.string.no_internet_error
        )

        NetworkError.RequestTimeout -> UiText.StringResource(
            R.string.the_request_timed_out
        )

        NetworkError.Serialization -> UiText.StringResource(
            R.string.json_serialization_error
        )

        NetworkError.ServerError -> UiText.StringResource(
            R.string.server_error
        )

        NetworkError.UnknownError -> UiText.StringResource(
            R.string.unknown_error
        )
    }
}

fun Result.Error<*, RootError>.asErrorUiText(): UiText {
    return (error as NetworkError).asUiText()
}
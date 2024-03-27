package com.citrus.citruskds.commonData

sealed interface DataError : Error

sealed class NetworkError : DataError {
    data object UnknownError : NetworkError()
    data object ServerError : NetworkError()
    data object RequestTimeout : NetworkError()
    data object HttpError : NetworkError()
    data object NoInternet : NetworkError()
    data object Serialization : NetworkError()
    data class DataFetchFailed(val errMsg: String?) : NetworkError()
}

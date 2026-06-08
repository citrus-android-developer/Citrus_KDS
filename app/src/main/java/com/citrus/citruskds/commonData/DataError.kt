package com.citrus.citruskds.commonData

sealed interface DataError : Error

sealed class NetworkError : DataError {
    data class UnknownError(val errMsg: String?) : NetworkError()
    data class ServerError(val errMsg: String?) : NetworkError()
    data class RequestTimeout(val errMsg: String?) : NetworkError()
    data class HttpError(val code: String, val errMsg: String?) : NetworkError()
    data class NoInternet(val errMsg: String?) : NetworkError()
    data class ConnectionError(val errMsg: String?) : NetworkError()
    data class Serialization(val errMsg: String?) : NetworkError()
    data class DataFetchFailed(val errMsg: String?) : NetworkError()

    /** 未設定 Server URL（遠端呼叫前的設定檢查）；訊息由 UI 依語系顯示 */
    data object ServerUrlNotSet : NetworkError()
}

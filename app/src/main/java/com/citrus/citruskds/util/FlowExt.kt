package com.citrus.citruskds.util

import com.citrus.citruskds.commonData.NetworkError
import com.citrus.citruskds.commonData.Result
import com.citrus.citruskds.commonData.RootError
import com.citrus.citruskds.commonData.vo.ApiResult
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException

/**基於sandwich進一步封裝含retry功能、error錯誤處理,僅抽出success各自實作*/
/**crossInline：讓函數類型的參數可以被間接調用，但無法return*/
/**noInline：函數類型的參數在inline時會無法被當成對象來使用，需用noinline局部關閉inline效果*/
fun <T> resultFlowData(apiAction: suspend () -> ApiResult<T>, isNeedLoading: Boolean = true) =
    flow {
        val apiActionResult = apiAction()
        emit(if (apiActionResult.status != "1") {
            Result.Error<T, RootError>(
                NetworkError.DataFetchFailed(
                    errMsg = apiActionResult.error?.message
                )
            )
        } else {
            apiActionResult.data?.let {
                Result.Success<T, RootError>(data = it)
            } ?: Result.Success(Unit)
        })
    }.retryWithPolicy(isNeedLoading)


fun <T> Flow<Result<T, RootError>>.retryWithPolicy(isNeedLoading: Boolean): Flow<Result<T, RootError>> {
    return this.retryWhen { cause, attempt ->
        val delayTime = when (attempt) {
            0L -> 3000L
            1L -> 9000L
            2L -> 15000L
            else -> 3000L
        }

        if (cause is IOException && attempt < 2) {
            delay(delayTime)
            return@retryWhen true
        } else {
            emit(
                Result.Error(
                    NetworkError.RequestTimeout
                )
            )
            return@retryWhen false
        }
    }.onIfLoadingAndCatch(isNeedLoading)
}

fun <T> Flow<Result<T, RootError>>.onIfLoadingAndCatch(isNeedLoading: Boolean): Flow<Result<T, RootError>> {
    return this.onStart {
        if (isNeedLoading) {
            emit(Result.Loading(true))
        }
    }.catch { e ->
        Timber.d("api error: ${e.message}")
        when (e) {
            is UnknownHostException -> {
                emit(Result.Error(NetworkError.NoInternet))
            }

            is HttpException -> {
                val error = when (e.code()) {
                    408 -> NetworkError.RequestTimeout
                    else -> NetworkError.HttpError
                }
                emit(Result.Error(error))
            }

            is JsonParseException -> {
                emit(Result.Error(NetworkError.Serialization))
            }

            else -> {
                emit(Result.Error(NetworkError.UnknownError))
            }
        }
    }.flowOn(Dispatchers.IO)
}






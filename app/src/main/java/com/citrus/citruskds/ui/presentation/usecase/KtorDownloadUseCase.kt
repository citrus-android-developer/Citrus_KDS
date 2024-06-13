package com.citrus.citruskds.ui.presentation.usecase

import android.content.Context
import android.os.Build
import android.os.Environment
import com.citrus.citruskds.APK_FILE_NAME
import com.citrus.citruskds.util.UiText
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadStatus {
    object Success : DownloadStatus()
    data class Error(val message: UiText) : DownloadStatus()
    data class Progress(val progress: Int) : DownloadStatus()
}

@Singleton
class KtorDownloadUseCase @Inject constructor(@ApplicationContext private val appContext: Context) {
    suspend fun downloadApk(version: String, onProgressCallBack: (progress: Int) -> Unit): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Progress(0))
        val url =
            "http://hq.citrus.tw/apk/compass_kds_v$version.apk"

        Timber.d("downloadApk: $url")
        val httpResponse: HttpResponse = HttpClient().get(url) {
            onDownload { bytesSentTotal, contentLength ->
                val progress = (bytesSentTotal * 100f / contentLength).toInt()
                onProgressCallBack(progress)
            }
        }
        val responseBody: ByteArray = httpResponse.body()
        getFile().writeBytes(responseBody)

        if (httpResponse.status.isSuccess()) {
            getFile().writeBytes(responseBody)
            emit(DownloadStatus.Success)
        } else {
            emit(DownloadStatus.Error(UiText.DynamicString(httpResponse.status.description)))
        }
        return@flow

    }.catch { e ->
        val url = "compass_kds_v$version"
        emit(DownloadStatus.Error(UiText.DynamicString("${e.message.toString()}\n${url}")))
    }.flowOn(Dispatchers.IO)


    fun getFile(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + APK_FILE_NAME)
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)
        }
    }

}
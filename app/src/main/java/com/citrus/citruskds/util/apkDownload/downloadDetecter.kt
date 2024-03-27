package com.citrus.citruskds.util.apkDownload

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DownloadDetecter @Inject constructor() {

    /**更版用*/
    private var updateJob: Job? = null
    private val _downloadStatus = MutableSharedFlow<DownloadStatus>()
    val downloadStatus: SharedFlow<DownloadStatus>
        get() = _downloadStatus


    fun intentUpdate(file: File, url: String) {
        updateJob = CoroutineScope(Dispatchers.IO).launch {
            HttpClient().downloadFile(file, url).collect {
                if (isActive) {
                    _downloadStatus.emit(it)
                }
            }
        }
    }

    fun cancelUpdateJob() {
        if (updateJob != null) {
            updateJob?.cancel()
        }
    }


}
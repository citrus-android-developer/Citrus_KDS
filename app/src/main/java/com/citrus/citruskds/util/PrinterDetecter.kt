package com.citrus.citruskds.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PrinterDetecter @Inject constructor() {

    private val _scannerValue = MutableSharedFlow<ArrayList<Map<String, String>>>()
    val scannerValue = _scannerValue.asSharedFlow()

    private val _resetNotice = MutableSharedFlow<Unit>()
    val resetNotice = _resetNotice.asSharedFlow()


    suspend fun setValue(value: ArrayList<Map<String, String>>) {
        Timber.d("set scanner value: $value")
        _scannerValue.emit(value)
    }

    suspend fun resetPrintTask() {
        Timber.d("reset scanner value")
        _resetNotice.emit(Unit)
    }



}
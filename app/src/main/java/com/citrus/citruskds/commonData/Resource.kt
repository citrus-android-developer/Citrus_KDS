package com.citrus.citruskds.commonData

sealed class Resource<out T>(val data: T? = null, val message: String? = null, val isLoading: Boolean = false) {
    class Success<T>(data: T) : Resource<T>(data = data)
    class Error<T>(message: String) : Resource<T>(message = message)
    class Loading<T>(isLoading: Boolean) : Resource<T>(isLoading = isLoading)
}

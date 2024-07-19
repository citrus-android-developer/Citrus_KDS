package com.citrus.citruskds.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(@StringRes val id: Int, val arg: Array<Any> = arrayOf()) : UiText()
    data class MultiUiText(val uiTextList: List<UiText>) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> LocalContext.current.getString(id, *arg)
            is MultiUiText -> uiTextList.fold("") { prev, uiText ->
                prev + "\n${uiText.asString()}"
            }
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(id, *arg)
            is MultiUiText -> uiTextList.fold("") { prev, uiText ->
                prev + "\n${uiText.asString(context)}"
            }
        }
    }
}


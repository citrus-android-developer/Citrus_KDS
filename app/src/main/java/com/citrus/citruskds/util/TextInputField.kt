package com.citrus.citruskds.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.InputTransformation
import androidx.compose.foundation.text2.input.TextFieldBuffer
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.clearText
import androidx.compose.foundation.text2.input.delete
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text2.input.setTextAndSelectAll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citrus.citruskds.ui.*
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.ui.theme.ColorWarning
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.ui.theme.Gray05
import com.citrus.citruskds.ui.theme.Gray10
import com.citrus.citruskds.ui.theme.Gray20
import com.citrus.citruskds.ui.theme.Gray30
import com.citrus.citruskds.ui.theme.Gray40
import com.citrus.citruskds.ui.theme.Gray70
import com.citrus.citruskds.ui.theme.lighten

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class InputStateWrapper @OptIn(ExperimentalFoundationApi::class) constructor(
    var state: TextFieldState = TextFieldState(),
    var errorId: Int? = null
)


@OptIn(
    ExperimentalFoundationApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun TextInputField(
    modifier: Modifier = Modifier,
    textFieldValue: InputStateWrapper = InputStateWrapper(),
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    imeAction: ImeAction? = null,
    keyboardActions: KeyboardActions? = null,
    fontSize: TextUnit = 16.sp,
    shape: Shape = MaterialTheme.shapes.medium,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    maxLength: Int? = null,
    isShowKeyboard: Boolean = true,
    clearable: Boolean = true,
    onPress: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val interactionSourceState = interactionSource.collectIsFocusedAsState()
    val interactionSourcePressedState = interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible

    val keyboardController = LocalSoftwareKeyboardController.current

    // Bring the composable into view (visible to user).
    LaunchedEffect(isImeVisible, interactionSourceState.value) {
        if (isImeVisible && interactionSourceState.value) {
            scope.launch {
                delay(200)
                bringIntoViewRequester.bringIntoView()
            }
        }
    }

    LaunchedEffect(interactionSourcePressedState.value) {
        scope.launch {
            onPress()
        }
    }


    val border = remember { mutableStateOf(2.dp) }

    Column(modifier = modifier) {
        BasicTextField2(
            state = textFieldValue.state,
            modifier = Modifier
                .fillMaxWidth()
                .border(border.value, ColorBlue.copy(0.3f), shape)
                .onFocusEvent {
                    border.value = if (it.isFocused) 2.dp else 0.dp
                    if (!isShowKeyboard && (it.isFocused || it.hasFocus || it.isCaptured)) {
                        keyboardController?.hide()
                    }
                }
                .bringIntoViewRequester(bringIntoViewRequester),
            enabled = enabled,
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
            textStyle = TextStyle(
                fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onBackground
            ),
            keyboardActions = keyboardActions ?: KeyboardActions(
                onDone = { focusManager.clearFocus() },
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onSearch = { focusManager.clearFocus() }
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction ?: if (singleLine) ImeAction.Done else ImeAction.Default
            ),
            interactionSource = interactionSource,
            readOnly = readOnly,
            inputTransformation = if (maxLength != null) AndroidInputTransformation(maxLength) else null,
            decorator = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = textFieldValue.state.text.toString(),
                    visualTransformation = VisualTransformation.None,
                    innerTextField = innerTextField,
                    contentPadding = PaddingValues(6.dp),
                    placeholder = {
                        Text(placeholder, style = TextStyle(color = Gray40))
                    },
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon ?: {
                        if (textFieldValue.state.text.isNotEmpty() && clearable) {
                            IconButton(
                                enabled = enabled,
                                onClick = {
                                    textFieldValue.state.setTextAndPlaceCursorAtEnd("")
                                }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    },

                    prefix = prefix,
                    suffix = suffix,
                    shape = shape,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = textFieldValue.errorId != null,
                    interactionSource = interactionSource,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ColorWhiteBg.lighten(0.8f),
                        unfocusedContainerColor = Gray10,
                        disabledContainerColor = Gray05.copy(alpha = 0.8f),
                        disabledLeadingIconColor = Gray30,
                        disabledPlaceholderColor = Gray30,
                        disabledTextColor = Gray20,
                        cursorColor = Color.DarkGray,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedLeadingIconColor = ColorPrimary,
                        unfocusedLeadingIconColor = Gray70,
                    )
                )

            }
        )

        textFieldValue.errorId?.let {
            Text(
                text = stringResource(id = it),
                color = ColorWarning,
                textAlign = TextAlign.Start,
                fontSize = 13.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            ) // error message
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
class AndroidInputTransformation(private val maxLength: Int) : InputTransformation {
    override fun transformInput(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBuffer,
    ) {
        if (valueWithChanges.length > maxLength) {
            valueWithChanges.delete(maxLength, valueWithChanges.length)
        }
    }
}
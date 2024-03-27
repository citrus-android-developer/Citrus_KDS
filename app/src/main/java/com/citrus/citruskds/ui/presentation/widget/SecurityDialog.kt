package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.textAsFlow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.theme.ColorBlue
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.ui.theme.ColorWhiteBg
import com.citrus.citruskds.util.Constants.code
import com.citrus.citruskds.util.Constants.finalCode
import com.citrus.citruskds.util.InputStateWrapper
import com.citrus.citruskds.util.TextInputField
import com.citrus.citruskds.util.pressClickEffect
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SecurityDialog(
    onDismissRequest: () -> Unit,
    finish: () -> Unit,
    onCancel: () -> Unit
) {

    val state = remember { mutableStateOf(InputStateWrapper(TextFieldState(""))) }
    var isCodeCorrect by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        state.value.state.textAsFlow().collectLatest {
            isCodeCorrect = true
        }
    }


    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            //讓畫面可以隨著內容動態增長
        ),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                contentColor = Color.White,
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(15.dp),
            elevation = CardDefaults.cardElevation(
                2.dp
            ),
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .padding(horizontal = 8.dp, vertical = 4.dp)


        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(6.dp)
                    .padding(10.dp)

            ) {

                Text(
                    text = stringResource(id = R.string.verify_hint),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )

                Text(
                    text = code,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = ColorBlue,
                    modifier = Modifier.padding(10.dp)
                )

                TextInputField(
                    textFieldValue = state.value,
                    placeholder = "Verification Code",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    enabled = true,
                    leadingIcon = {
                        if(isCodeCorrect) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_security_24),
                                contentDescription = null,
                                tint = ColorBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }else {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_error_24),
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                    },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(0.5f)
                )


                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth()
                ) {

                    Box(
                        modifier = Modifier
                            .pressClickEffect {
                                onCancel()
                            }
                            .fillMaxHeight()
                            .background(Color.White, shape = RoundedCornerShape(10.dp))
                            .border(
                                BorderStroke(2.dp, ColorBlue),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .weight(0.3f)

                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            color = ColorBlue,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .align(Alignment.Center)
                        )
                    }



                    Button(
                        onClick = {
                            if (state.value.state.text.toString() == finalCode) {
                                prefs.firstInstall = false
                                finish()
                                onDismissRequest()
                            } else {
                                isCodeCorrect = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(ColorBlue),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(4.dp, ColorBlue),
                        modifier = Modifier
                            .pressClickEffect {

                            }
                            .weight(0.7f)
                            .padding(start = 10.dp)


                    ) {
                        Text(
                            text = stringResource(id = R.string.verify),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
        }
    }
}


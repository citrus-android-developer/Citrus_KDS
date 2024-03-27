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
import com.citrus.citruskds.util.UiText
import com.citrus.citruskds.util.pressClickEffect
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber


@Composable
fun ErrorDialog(
    errorMsg: UiText?,
    onDismissRequest: () -> Unit,
) {


    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
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

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_error_24),
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier
                            .size(30.dp)
                    )

                    Text(
                        text = errorMsg?.asString() ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(10.dp)
                    )
                }

            }
        }
    }
}


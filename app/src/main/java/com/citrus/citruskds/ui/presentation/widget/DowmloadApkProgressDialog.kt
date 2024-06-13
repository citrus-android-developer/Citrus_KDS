package com.citrus.citruskds.ui.presentation.widget


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.citrus.citruskds.R
import com.citrus.citruskds.ui.presentation.usecase.DownloadStatus
import com.citrus.citruskds.ui.theme.ColorAccent
import com.citrus.citruskds.ui.theme.ColorPrimary
import com.citrus.citruskds.ui.theme.ColorTextPrimaryColor
import com.citrus.citruskds.util.bounceClick


@Composable
fun DownloadApkProgressDialog(
    downloadStatus: DownloadStatus,
    onDialogDismiss: () -> Unit,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                indication = null, // 禁用水波紋效果
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.78f)
                .border(
                    5.dp,
                    ColorPrimary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(34.dp)
                ),
            shape = RoundedCornerShape(34.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)

        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                when (downloadStatus) {
                    is DownloadStatus.Progress -> {
                        Text(
                            text = "downloading...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColorTextPrimaryColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        LinearProgressIndicator(
                            progress = { downloadStatus.progress.toFloat() / 100 },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(20.dp),
                            strokeCap = StrokeCap.Round,
                        )
                        Text(
                            text = "${downloadStatus.progress}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorAccent,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                            )
                        )
                    }

                    is DownloadStatus.Success -> {
                        LottieAnimation(composition = composition, modifier = Modifier.size(150.dp))
                        Text(
                            text = "download success",
                            style = MaterialTheme.typography.titleLarge,
                            color = ColorTextPrimaryColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                top = 30.dp,
                                start = 20.dp,
                                end = 20.dp,
                            )
                        )
                    }

                    is DownloadStatus.Error -> {
                        Text(
                            text = downloadStatus.message.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextPrimaryColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                top = 30.dp,
                                start = 20.dp,
                                end = 20.dp,
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 25.dp, horizontal = 25.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .bounceClick {
                                onDialogDismiss()
                            }
                            .weight(1f)
                            .background(ColorPrimary.copy(.3f), MaterialTheme.shapes.medium)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "close",
                            color = ColorPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}


package com.citrus.citruskds.ui.presentation.widget

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.core.os.ConfigurationCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 時鐘元件。
 *
 * 改用 Compose 原生 + [SimpleDateFormat]，並以 [LocalConfiguration] 的 locale 格式化，
 * 讓星期/月份等文字跟隨 App 語系設定即時切換
 * （locale 由 MainActivity 依 languageState 提供，與 stringResource、品項名稱同一來源）。
 *
 * 原本以 AndroidView 包 android.widget.TextClock，其日期格式固定吃 Locale.getDefault()
 * 且 factory 只建立一次，導致切換語系後 "EEE, MMM d" 不會更新。
 */
@Composable
fun TextClock(
    modifier: Modifier = Modifier,
    format: String = "kk:mm:ss",
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current
        }
    }

    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration).get(0) ?: Locale.getDefault()
    val formatter = remember(format, locale) { SimpleDateFormat(format, locale) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            // 對齊下一秒邊界，避免長時間累積漂移
            delay(1000L - now % 1000L)
        }
    }

    Text(
        text = formatter.format(Date(now)),
        modifier = modifier,
        color = textColor,
        style = style,
    )
}

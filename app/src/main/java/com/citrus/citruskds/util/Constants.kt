package com.citrus.citruskds.util


import android.annotation.SuppressLint
import android.util.Log
import com.citrus.citruskds.di.MyApplication.Companion.prefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    //const val BASE_URL = "http://cms.citrus.tw/soramenLAB"

    const val USB_NO_PERMISSION = "USB_NO_PERMISSION"
    const val SHARED_PREFERENCES_NAME = "sharedPref"
    const val TWO_MINUTES = 120

    const val POS_GET_ORDER = "/KDS/OrdersList"
    const val POS_SET_ORDER_STATUS = "/KDS/SetOrderStatus"
    const val POS_GET_STOCK_INFO = "/KDS/InventoryList"
    const val POS_SET_INVENTORY = "/KDS/SetInventory"
    const val POS_SET_SELL_STATUS = "/KDS/SetSellStatus"

    const val SERVER_SET_SELL_STATUS = "KDS/SetSellStatus"
    const val SERVER_SET_ORDERS_NOTIFY = "KDS/OrdersNotify"

    const val POS_GET_ORDER_READY_INFO = "/controller/OrdersList"


    const val BASE_URL = "https://global.citrus.tw/CompassKDS/"
    //const val BASE_URL = "https://lab.citrus.tw/CompassKDS/"


    var df = DecimalFormat("#,###,##0.###")
    var dfShow = DecimalFormat("###,###,###,##0.##")
    var dateTimeFormatSql = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    var timeFormat = SimpleDateFormat("HH:mm")

    const val ACTION_USB_PERMISSION = "com.citrus.kiosk.USB_PERMISSION"

    /**Prefs*/
    const val KEY_TAX = "KEY_TAX"
    const val KEY_METHOD_OF_OPERATION = "KEY_METHOD_OF_OPERATION"
    const val KEY_DECIMAL_PLACES = "KEY_DECIMAL_PLACES"
    const val KEY_TAX_FUNCTION = "KEY_TAX_FUNCTION"
    const val KEY_IDLE_TIME = "KEY_IDLE_TIME"
    const val KEY_SEVER_IP = "KEY_SEVER_IP"
    const val KEY_RSNO = "KEY_RSNO"
    const val KEY_STORE_NAME = "KEY_STORE_NAME"
    const val KEY_STORE_NO = "KEY_STORE_NO"
    const val KEY_PRINTER_IS80MM = "KEY_PRINTER_IS80MM"
    const val KEY_LANGUAGE_POS = "KEY_LANGUAGE_POS"
    const val KEY_KDS_ID = "KEY_KDS_ID"
    const val KEY_STORE_ADDRESS = "KEY_STORE_ADDRESS"
    const val KEY_HEADER = "KEY_HEADER"
    const val KEY_FOOTER = "KEY_FOOTER"
    const val KEY_ORDER_STRING = "KEY_ORDER_STRING"
    const val KEY_PORT_NAME = "KEY_PORT_NAME"
    const val KEY_BG_COLOR = "KEY_BG_COLOR"

    const val DOWNLOAD_URL = "http://hq.citrus.tw/apk/"


    /**status:J*/
    const val NEW = "J"
    /**status:O*/
    const val PREPARED = "O"
    /**status:W*/
    const val PROGRESSING = "W"
    /**status:F*/
    const val COLLECTED = "F"




    /**Fail Message Type*/
    const val RefundSuccess = "RefundSuccess"
    const val OrderUploadFail = "OrderUploadFail"

    var screenW = 0
    var screenH = 0

    var code = ""
    var finalCode = ""

    sealed class LanguageType {
        object SimpleChinese : LanguageType()
        object English : LanguageType()
    }

    sealed class PayWayType(values: String) {
        object CreditCard : PayWayType("01")
        object Cash : PayWayType("02")
    }


    fun String.trimSpace(): String {
        return this.replace("\\s".toRegex(), "")
    }


    @SuppressLint("SimpleDateFormat")
    fun getCurrentTime(): String {
        val currentDate = Calendar.getInstance().time
        val sdf = dateTimeFormatSql
        return sdf.format(currentDate)
    }

    inline fun <T> List<T>.forEachReversedByIndex(action: (T) -> Unit) {
        val initialSize = size
        for (i in lastIndex downTo 0) {
            if (size != initialSize) throw ConcurrentModificationException()
            action(get(i))
        }
    }

    inline fun <T> List<T>.forEachReversedWithIndex(
        allowSafeModifications: Boolean = false,
        action: (Int, T) -> Unit
    ) {
        val initialSize = size
        for (i in lastIndex downTo 0) {
            when {
                allowSafeModifications && i > lastIndex -> {
                    throw ConcurrentModificationException()
                }

                allowSafeModifications.not() && size != initialSize -> {
                    throw ConcurrentModificationException()
                }
            }
            action(i, get(i))
        }
    }

    fun <T> turnObjIntoString(type: Class<T>, obj: T): String {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter = moshi.adapter(type)
        return jsonAdapter.toJson(obj)
    }

    fun <T> turnStringToObj(type: Class<T>, json: String): T? {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter = moshi.adapter(type)
        return jsonAdapter.fromJson(json)
    }


    private fun getDecimalFormat(): DecimalFormat {
        return when (prefs?.decimalPlace) {
            0 -> DecimalFormat("0")
            1 -> DecimalFormat("0.0")
            2 -> DecimalFormat("0.00")
            else -> DecimalFormat("0.00")
        }
    }


    //SHA3-256字串加密
//    fun String.sha3_256(): String {
//        val digest = java.security.MessageDigest.getInstance("SHA3-256")
//        val hashBytes = digest.digest(this.toByteArray())
//        return hashBytes.joinToString("") {
//            "%02x".format(it)
//        }
//    }


    fun String.sha3_256(): String {

        return "5EC8433D25C759DD6BB965090F6835C77BB569CE86F3713B2D364E642F693280"
    }


    fun createCode() {
        //產生安全碼
        val date = Date()
        val bartDateFormat = SimpleDateFormat("MMdd")
        val dateStr = bartDateFormat.format(date)
        //        Log.d("TEST","##"+ dateStr);
        code = (Math.random() * 99999 + 100001).toInt().toString()
        //        Log.d("TEST","##1==>"+ code); //100001~199999 的亂碼
        var sb: StringBuffer = StringBuffer(code)
        sb = sb.reverse()
        //        Log.d("TEST","##2==>"+ String.valueOf(sb));

        //finalCede = (反轉的亂數 + 525111)*3 + 日期(月日)
        finalCode = ((sb.toString().toInt() + 525111) * 3 + dateStr.toInt()).toString()
        Log.d("TEST", "##3==>$finalCode")
    }


    fun getValByMathWay(orgValue: Double): String {
        return when (prefs?.methodOfOperation) {
            0 -> {
                val value =
                    BigDecimal.valueOf(orgValue)
                        .setScale(prefs?.decimalPlace ?: 0, BigDecimal.ROUND_HALF_UP)
                        .toDouble()
                getDecimalFormat().format(value)
            }

            1 -> {
                val value =
                    BigDecimal.valueOf(orgValue)
                        .setScale(prefs?.decimalPlace ?: 0, BigDecimal.ROUND_UP)
                        .toDouble()
                getDecimalFormat().format(value)
            }

            2 -> {
                val value =
                    BigDecimal.valueOf(orgValue)
                        .setScale(prefs?.decimalPlace ?: 0, BigDecimal.ROUND_DOWN)
                        .toDouble()
                getDecimalFormat().format(value)
            }

            3 -> {
                val priceString: String = getDecimalFormat().format(orgValue)
                if (priceString.contains(".")) {
                    val s = priceString.split("\\.".toRegex()).toTypedArray()
                    if (s[1].length == 2) {
                        var a = s[1].substring(1, 2)
                        when (a) {
                            "0", "1", "2" -> {
                                a = "0"
                                val value = (s[0] + "." + s[1].substring(0, 1) + a).toDouble()
                                getDecimalFormat().format(value)
                            }

                            "3", "4", "5", "6", "7" -> {
                                a = "5"
                                val value = (s[0] + "." + s[1].substring(0, 1) + a).toDouble()
                                getDecimalFormat().format(value)
                            }

                            "8", "9" -> {
                                val value = (s[0] + "." + s[1].substring(0, 1)).toDouble() + 0.1
                                getDecimalFormat().format(value)
                            }

                            else -> getDecimalFormat().format(orgValue)
                        }
                    } else getDecimalFormat().format(orgValue)
                } else getDecimalFormat().format(orgValue)
            }

            else -> getDecimalFormat().format(orgValue)
        }
    }
}

var isKioskScreen = false


object GlobalConstants {
    const val ECR_SALE = "C200"
    const val ECR_PRE_AUTH = "C201"
    const val ECR_PRE_AUTH_CAPTURE = "C202"
    const val ECR_REFUND = "C203"
    const val ECR_QUASI_CASH_ADVANCE = "C204"
    const val ECR_GET_CARD_DATA = "C209"
    const val ECR_VOID = "C300"
    const val ECR_PRE_AUTH_CANCEL =
        "C301" // 2017032101 Rajesh: Added PRE AUTH CANCEL DEV TMS-13131313
    const val ECR_INQUIRY = "C400"
    const val ECR_ECHO = "C902"
    const val ECR_BEGIN_SHIFT = "C800"
    const val ECR_GET_MAGSTRIPE = "C810"
    const val ECR_TIP_ADJUST = "C500"
    const val ECR_CASH_ADVANCE = "C205"
    const val ECR_SETTLEMENT = "C700"
    const val ECR_WAIT = "C920"
    const val ECR_RETRIEVE_TERMINAL_INFO =
        "C900" // Retrieve terminal info ie. TID, Serial Number Etc..
    const val ECR_EZLINK_SALE = "C610" //Ezlink sale
    const val ECR_CUSTOM_RECEIPT = "C620" //Ezlink Blacklist Download
    const val ECR_WALLET_SALE = "C640"
}


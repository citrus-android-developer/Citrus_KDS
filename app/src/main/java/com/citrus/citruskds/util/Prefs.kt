package com.citrus.citruskds.util

import android.content.Context
import android.content.SharedPreferences


class Prefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    var isNavigate: Boolean
        get() = prefs.getBoolean("isNavigate", false)
        set(value) = prefs.edit().putBoolean("isNavigate", value).apply()

    var firstInstall: Boolean
        get() = prefs.getBoolean("firstInstall", true)
        set(value) = prefs.edit().putBoolean("firstInstall", value).apply()

    var mode: Int
        get() = prefs.getInt("mode", 0)
        set(value) = prefs.edit().putInt("mode", value).apply()

    var language: String
        get() = prefs.getString("language", "English") ?: "English"
        set(value) = prefs.edit().putString("language", value).apply()

    var itemDisplayLan: String
        get() = prefs.getString("itemDisplayLan", "English") ?: "English"
        set(value) = prefs.edit().putString("itemDisplayLan", value).apply()

    var defaultPage: String
        get() = prefs.getString("defaultPage", "Main") ?: "Main"
        set(value) = prefs.edit().putString("defaultPage", value).apply()

    var charSet: String
        get() = prefs.getString("charSet", "UTF-8") ?: ""
        set(value) = prefs.edit().putString("charSet", value).apply()

    var printer: String
        get() = prefs.getString("printer", "") ?: ""
        set(value) = prefs.edit().putString("printer", value).apply()

    var isLargeLineSpacing: Boolean
        get() = prefs.getBoolean("isLargeLineSpacing", false)
        set(value) = prefs.edit().putBoolean("isLargeLineSpacing", value).apply()


    var rsno: String
        get() = prefs.getString(Constants.KEY_RSNO, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_RSNO, value).apply()


    var header: String
        get() = prefs.getString(Constants.KEY_HEADER, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_HEADER, value).apply()

    var footer: String
        get() = prefs.getString(Constants.KEY_FOOTER, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_FOOTER, value).apply()

    var kdsId: String
        get() = prefs.getString(Constants.KEY_KDS_ID, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_KDS_ID, value).apply()

    var storeName: String
        get() = prefs.getString(Constants.KEY_STORE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_STORE_NAME, value).apply()

    var storeAddress: String
        get() = prefs.getString(Constants.KEY_STORE_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_STORE_ADDRESS, value).apply()

    var storeNo: String
        get() = prefs.getString(Constants.KEY_STORE_NO, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_STORE_NO, value).apply()

    var bgColor: String
        get() = prefs.getString(Constants.KEY_BG_COLOR, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_BG_COLOR, value).apply()

    var serverIp: String
        get() = prefs.getString(Constants.KEY_SEVER_IP, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_SEVER_IP, value).apply()

    var localIp: String
        get() = prefs.getString("localIp", "") ?: ""
        set(value) = prefs.edit().putString("localIp", value).apply()

    var printerTarget: String
        get() = prefs.getString("printerTarget", "") ?: ""
        set(value) = prefs.edit().putString("printerTarget", value).apply()

    var printerName: String
        get() = prefs.getString("printerName", "") ?: ""
        set(value) = prefs.edit().putString("printerName", value).apply()

    var portName: String
        get() = prefs.getString(Constants.KEY_PORT_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_PORT_NAME, value).apply()

    var idleTime: Int
        get() = prefs.getInt(Constants.KEY_IDLE_TIME, 120)
        set(value) = prefs.edit().putInt(Constants.KEY_IDLE_TIME, value).apply()

    var decimalPlace: Int
        get() = prefs.getInt(Constants.KEY_DECIMAL_PLACES, 0)
        set(value) = prefs.edit().putInt(Constants.KEY_DECIMAL_PLACES, value).apply()

    var taxFunction: Int
        get() = prefs.getInt(Constants.KEY_TAX_FUNCTION, 0)
        set(value) = prefs.edit().putInt(Constants.KEY_TAX_FUNCTION, value).apply()

    var methodOfOperation: Int
        get() = prefs.getInt(Constants.KEY_METHOD_OF_OPERATION, 0)
        set(value) = prefs.edit().putInt(Constants.KEY_METHOD_OF_OPERATION, value).apply()

    var printerIs80mm: Boolean
        get() = prefs.getBoolean(Constants.KEY_PRINTER_IS80MM, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_PRINTER_IS80MM, value).apply()

    var tax: Int
        get() = prefs.getInt(Constants.KEY_TAX, 0)
        set(value) = prefs.edit().putInt(Constants.KEY_TAX, value).apply()


    var orderStr: String
        get() = prefs.getString(Constants.KEY_ORDER_STRING, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_ORDER_STRING, value).apply()


}

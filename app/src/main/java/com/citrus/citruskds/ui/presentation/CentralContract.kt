package com.citrus.citruskds.ui.presentation


import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.OrderReadyInfo
import com.citrus.citruskds.commonData.vo.StockInfo
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.presentation.usecase.DownloadStatus
import com.citrus.citruskds.util.InputStateWrapper
import com.citrus.citruskds.util.PrintStatus
import com.citrus.citruskds.util.UiEffect
import com.citrus.citruskds.util.UiEvent
import com.citrus.citruskds.util.UiState
import com.citrus.citruskds.util.UiText

class CentralContract {

    sealed class Event : UiEvent {
        data class OnLanguageChanged(val lan: String) : Event()
        data class OnItemDisplayLanguageChanged(val lan: String) : Event()
        data class IntentToUpdateVersion(val version: String) : Event()
        data class OnDefaultPageChanged(val page: String) : Event()
        data class OnStockTypeChanged(val type: String) : Event()
        data class FinishOrder(val order: Order, val status: String = "O") : Event()
        data class ProgressOrder(val order: Order, val status: String = "W") : Event()
        data class CollectedOrder(val orderNo: String, val status: String = "F") : Event()
        data class RecallOrder(val orderNo: String, val status: String = "O") : Event()
        data class OnStockItemClicked(val stockInfo: StockInfo) : Event()
        data class OnSetInventory(val stock: StockInfo) : Event()
        data class OnPrinterSelected(val info: Map<String, String>) : Event()
        data class ReprintOrder(val order: Order) : Event()
        data object LoadStockList : Event()
        data class OnModeChanged(val mode: Int) : Event()
        data object startFetchKdsInfo : Event()
        data object startFetchOrderReadyInfo : Event()
        data object onVerifyCancel : Event()
        data object OnDismissDownloadApkDialog : Event()
        data object onDismissErrorDialog : Event()
        data class onPrintModeChanged(val mode: Int) : Event()
        data class onPrepareModeChanged(val mode: Boolean) : Event()
    }

    data class State(
        var isVerifyCancel: Boolean = false,
        var modeState: Int = 0,
        var kdsIdState: InputStateWrapper,
        var rsnoState: InputStateWrapper,
        var localIpState: InputStateWrapper,
        var languageState: InputStateWrapper,
        var itemDisplayLanState: InputStateWrapper,
        var defaultPageState: InputStateWrapper,
        var servedSearchState: InputStateWrapper,
        var recallSearchState: InputStateWrapper,
        var printerState: InputStateWrapper,
        var displayLan: String = prefs.itemDisplayLan,
        var currentPage: String = prefs.defaultPage.lowercase(),
        var orderReadyList: List<OrderReadyInfo>? = null,
        var mainList: List<Order>? = null,
        var servedList: List<Order>? = null,
        var recallList: List<Order>? = null,
        var servedFilterList: List<Order>? = null,
        var recallFilterList: List<Order>? = null,
        var mainListError: String = "",
        var servedListError: String = "",
        var recallListError: String = "",
        var stockInfoList: List<StockInfo>? = null,
        var stockInfoPresentList: List<StockInfo>? = null,
        var stockTypeList: List<String>? = null,
        var stockTypeSelect: InputStateWrapper,
        var stockTypeSelected: String = "All",
        var stockSearchState: InputStateWrapper,
        var errMsg: UiText? = null,
        var printOrder: Order? = null,
        var printerInfo: ArrayList<Map<String, String>>? = null,
        var printStatus: PrintStatus = PrintStatus.Idle,
        var setStatusGkidGid: Pair<String, String>? = null,   //setSellStatus之後紀錄改變了哪一個，在filter list中找到並更新
        var downloadStatus: DownloadStatus? = null,
    ) : UiState

    sealed class Effect : UiEffect {
        data object DownloadApkSuccess : Effect()
    }

}
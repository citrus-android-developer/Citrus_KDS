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
        // fromStatus：只搬目前為這些來源狀態的品項（支援混合狀態/加點）
        data class FinishOrder(val order: Order, val status: String = "O", val fromStatus: String = "W") : Event()
        data class ProgressOrder(val order: Order, val status: String = "W", val fromStatus: String = "j,J") : Event()
        data class CollectedOrder(val orderNo: String, val status: String = "F", val fromStatus: String = "O") : Event()
        data class RecallOrder(val orderNo: String, val status: String = "O", val fromStatus: String = "O,F") : Event()
        data class OnStockItemClicked(val stockInfo: StockInfo) : Event()
        data class OnSetInventory(val stock: StockInfo) : Event()
        /** 損耗/報廢：對品項輸入數量送本地。status：W=報廢、S=損耗 */
        data class OnSetWastage(val stockInfo: StockInfo, val qty: Int, val status: String) : Event()
        data class OnPrinterSelected(val info: Map<String, String>) : Event()
        data object TestPrinter : Event()
        data object ScanPrinters : Event()
        data class ScanOrderNo(val orderNo: String) : Event()
        data object ReloadOrderReadyImages : Event()
        data class ReprintOrder(val order: Order) : Event()
        /** 列印失敗後重印「上一張列印快照」(含加點子集與標記)，不受狀態已升級影響 */
        data object RetryPrintOrder : Event()
        data object LoadStockList : Event()
        data class OnModeChanged(val mode: Int) : Event()
        data object startFetchKdsInfo : Event()
        data object startFetchOrderReadyInfo : Event()
        data object onVerifyCancel : Event()
        data object OnDismissDownloadApkDialog : Event()
        data object onDismissErrorDialog : Event()
        data class onPrintModeChanged(val mode: Int) : Event()
        data class onPrepareModeChanged(val mode: Boolean) : Event()
        data class onAutoAcceptModeChanged(val mode: Boolean) : Event()
        data class onOrderReadyOrientationChanged(val mode: Int) : Event()
    }

    data class State(
        var isVerifyCancel: Boolean = false,
        var modeState: Int = 0,
        var kdsIdState: InputStateWrapper,
        var rsnoState: InputStateWrapper,
        var localIpState: InputStateWrapper,
        var serverUrlState: InputStateWrapper,
        var languageState: InputStateWrapper,
        var itemDisplayLanState: InputStateWrapper,
        var defaultPageState: InputStateWrapper,
        var servedSearchState: InputStateWrapper,
        var recallSearchState: InputStateWrapper,
        var printerState: InputStateWrapper,
        var printerIpState: InputStateWrapper,
        var printerPortState: InputStateWrapper,
        var displayLan: String = prefs.itemDisplayLan,
        var currentPage: String = prefs.defaultPage.lowercase(),
        var orderReadyList: List<OrderReadyInfo>? = null,
        var orderReadyRedSet: Set<String> = emptySet(),
        var orderReadyTick: Int = 0,
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
        // 每次列印請求遞增；列印觸發改 key 在此，確保「重印同一張(含加點)」也能重觸發
        var printRequestId: Int = 0,
        var printerInfo: ArrayList<Map<String, String>>? = null,
        var printStatus: PrintStatus = PrintStatus.Idle,
        // 損耗送出成功計數（UI 觀察以提示「已送出」）
        var wastageDone: Int = 0,
        var setStatusGkidGid: Pair<String, String>? = null,   //setSellStatus之後紀錄改變了哪一個，在filter list中找到並更新
        var downloadStatus: DownloadStatus? = null,
    ) : UiState

    sealed class Effect : UiEffect {
        data object DownloadApkSuccess : Effect()
    }

}
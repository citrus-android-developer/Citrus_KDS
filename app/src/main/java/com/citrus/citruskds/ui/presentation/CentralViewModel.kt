package com.citrus.citruskds.ui.presentation

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text2.input.textAsFlow
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.citrus.citruskds.R
import com.citrus.citruskds.commonData.DataError
import com.citrus.citruskds.commonData.NetworkError
import com.citrus.citruskds.commonData.Resource
import com.citrus.citruskds.commonData.Result
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.OrderReadyInfo
import com.citrus.citruskds.commonData.vo.addonItems
import com.citrus.citruskds.commonData.vo.displayStatus
import com.citrus.citruskds.commonData.vo.isAddon
import com.citrus.citruskds.commonData.vo.isPending
import com.citrus.citruskds.commonData.vo.OrdersNotifyRequest
import com.citrus.citruskds.commonData.vo.SetInventoryRequest
import com.citrus.citruskds.commonData.vo.SetItemSellStatusRequest
import com.citrus.citruskds.commonData.vo.SetOrderStatusRequest
import com.citrus.citruskds.commonData.vo.StockInfo
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.domain.ApiRepositoryImpl
import com.citrus.citruskds.ui.presentation.usecase.DownloadStatus
import com.citrus.citruskds.ui.presentation.usecase.KtorDownloadUseCase
import com.citrus.citruskds.util.BaseViewModel
import com.citrus.citruskds.util.Constants.COLLECTED
import com.citrus.citruskds.util.Constants.NEW
import com.citrus.citruskds.util.Constants.PREPARED
import com.citrus.citruskds.util.Constants.PROGRESSING
import com.citrus.citruskds.util.InputStateWrapper
import com.citrus.citruskds.util.PrintStatus
import com.citrus.citruskds.util.PrinterDetecter
import com.citrus.citruskds.util.lanprint.LanPrinter
import com.citrus.citruskds.util.lanprint.NetworkScanner
import com.citrus.citruskds.util.UiText
import com.citrus.citruskds.util.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@OptIn(ExperimentalFoundationApi::class)
@HiltViewModel
class CentralViewModel @Inject constructor(
    private val repository: ApiRepositoryImpl,
    private val printerDetecter: PrinterDetecter,
    private val downloadApkUseCase: KtorDownloadUseCase,
) : BaseViewModel<CentralContract.Event, CentralContract.State, CentralContract.Effect>() {

    private var orderInfoJob: Job? = null

    private var orderReadyJob: Job? = null

    private var animateBufferGap = false

    private var updateJob: Job? = null

    /** 自動接單基準：開啟（或重啟）當下已存在的「未接(有 pending j)」訂單，不自動接（避免一開就全接走）*/
    private val autoAcceptBaseline = mutableSetOf<String>()

    /** 自動接單派發中的訂單（防止網路延遲窗口內重複派發；完成後移除）*/
    private val autoAcceptInFlight = mutableSetOf<String>()

    /** 自動接單是否已建立基準 */
    private var autoAcceptBaselined = false

    /** OrderReady 取餐牆：上一次輪詢的單號集合（用來算「新進來的」）*/
    private var orderReadyPrevSet = emptySet<String>()
    private var orderReadyInitialized = false
    /** OrderReady 要標紅的單號集合：最新一批進來的新單，直到下一批新單進來才換 */
    private var orderReadyRedSet = emptySet<String>()

    /** 店家圖片刷新 token：只在按下 reload 按鈕時 +1（平常不變→走快取不閃）*/
    private var imgReloadTick = 0


    init {
        prefs.defaultPage = "Main"
        viewModelScope.launch {
            printerDetecter.scannerValue.collectLatest {
                setState {
                    copy(printerInfo = it)
                }
            }
        }

        viewModelScope.launch {
            printerDetecter.printStatus.collectLatest {
                setState {
                    copy(printStatus = it)
                }
            }
        }

        viewModelScope.launch {
            printerDetecter.resetNotice.collectLatest {
                setState {
                    copy(printerInfo = null)
                }
            }
        }


        viewModelScope.launch {
            snapshotFlow { currentState.stockInfoList }.collectLatest { stocks ->
                stocks?.let { info ->
                    val typeList =
                        info.groupBy { if (prefs.language == "English") it.gKEName else it.gKCName }
                            .map { it.key ?: "" }
                    setState { copy(stockTypeList = typeList) }
                }
            }
        }

        viewModelScope.launch {
            currentState.stockSearchState.state.textAsFlow()
                .combine(snapshotFlow { currentState.stockInfoPresentList }) { name, list ->
                    name to list
                }.collectLatest { (name, list) ->

                    val stockInfoFilterPresentList = currentState.stockInfoList?.filter {
                        if (prefs.language == "English") {
                            it.gKEName?.contains(
                                currentState.stockTypeSelected,
                                ignoreCase = true
                            ) == true
                        } else {
                            it.gKCName?.contains(
                                currentState.stockTypeSelected,
                                ignoreCase = true
                            ) == true
                        }
                    }?.filter {
                        if (prefs.language == "English") {
                            it.eName?.contains(name, ignoreCase = true) == true
                        } else {
                            it.cName?.contains(name, ignoreCase = true) == true
                        }
                    }.let { stockInfo ->
                        if (currentState.setStatusGkidGid != null) {
                            stockInfo?.map {
                                if (it.gID == currentState.setStatusGkidGid?.first && it.gKID == currentState.setStatusGkidGid?.second) {
                                    it.copy(sellStatus = if (it.sellStatus == "Available") "Unavailable" else "Available")
                                } else {
                                    it
                                }
                            }
                        } else {
                            stockInfo
                        }
                    }

                    val nowList = if (currentState.setStatusGkidGid != null) {
                        currentState.stockInfoList?.map {
                            if (it.gID == currentState.setStatusGkidGid?.first && it.gKID == currentState.setStatusGkidGid?.second) {
                                it.copy(sellStatus = if (it.sellStatus == "Available") "Unavailable" else "Available")
                            } else {
                                it
                            }
                        }
                    } else {
                        currentState.stockInfoList
                    }

                    setState {
                        copy(
                            stockInfoList = nowList,
                            stockInfoPresentList = stockInfoFilterPresentList,
                            setStatusGkidGid = null
                        )
                    }
                }
        }


        viewModelScope.launch {
            currentState.kdsIdState.state.textAsFlow().collectLatest {
                prefs.kdsId = it.toString()
            }
        }

        viewModelScope.launch {
            currentState.rsnoState.state.textAsFlow().collectLatest {
                prefs.rsno = it.toString()
            }
        }

        viewModelScope.launch {
            currentState.localIpState.state.textAsFlow().collectLatest {
                prefs.localIp = it.toString()
            }
        }

        viewModelScope.launch {
            currentState.serverUrlState.state.textAsFlow().collectLatest {
                prefs.serverUrl = it.toString()
            }
        }

        viewModelScope.launch {
            currentState.printerIpState.state.textAsFlow().collectLatest {
                prefs.printerIp = it.toString()
            }
        }

        viewModelScope.launch {
            currentState.printerPortState.state.textAsFlow().collectLatest {
                prefs.printerPort = it.toString().toIntOrNull() ?: 9100
            }
        }

        viewModelScope.launch {
            currentState.servedSearchState.state.textAsFlow()
                .combine(snapshotFlow { currentState.servedList }) { searchStr, servedList ->
                    searchStr to servedList
                }.collectLatest { (search, list) ->
                    Timber.d("servedSearchState: $search")
                    Timber.d("servedSearchState: $list")
                    setState {
                        copy(servedFilterList = list?.filter {
                            it.orderNo.contains(
                                search,
                                ignoreCase = true
                            )
                        })
                    }
                }
        }

        viewModelScope.launch {
            currentState.recallSearchState.state.textAsFlow()
                .combine(snapshotFlow { currentState.recallList }) { searchStr, recallList ->
                    searchStr to recallList
                }.collectLatest { (search, list) ->
                    setState {
                        copy(recallFilterList = list?.filter {
                            it.orderNo.contains(
                                search,
                                ignoreCase = true
                            )
                        })
                    }
                }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun createInitialState(): CentralContract.State {
        var printer = prefs.printerName + " " + prefs.printerTarget
        if (printer.trim().replace(" ", "").isEmpty()) {
            printer = ""
        }
        return CentralContract.State(
            kdsIdState = InputStateWrapper(TextFieldState(prefs.kdsId)),
            rsnoState = InputStateWrapper(TextFieldState(prefs.rsno)),
            localIpState = InputStateWrapper(TextFieldState(prefs.localIp)),
            serverUrlState = InputStateWrapper(TextFieldState(prefs.serverUrl)),
            languageState = InputStateWrapper(TextFieldState(prefs.language)),
            itemDisplayLanState = InputStateWrapper(TextFieldState(prefs.itemDisplayLan)),
            defaultPageState = InputStateWrapper(TextFieldState(getDefaultPageByLan())),
            servedSearchState = InputStateWrapper(TextFieldState("")),
            recallSearchState = InputStateWrapper(TextFieldState("")),
            stockTypeSelect = InputStateWrapper(TextFieldState("")),
            stockSearchState = InputStateWrapper(TextFieldState("")),
            printerState = InputStateWrapper(TextFieldState(printer)),
            printerIpState = InputStateWrapper(TextFieldState(prefs.printerIp)),
            printerPortState = InputStateWrapper(TextFieldState(prefs.printerPort.toString())),
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun handleEvent(event: CentralContract.Event) {
        when (event) {
            is CentralContract.Event.onPrepareModeChanged -> {
                prefs.isPrepareEnable = event.mode
                // PrepareMode 關閉時強制連動關掉自動接單（避免 J 瞬間→O 進 Served）
                if (!event.mode) {
                    prefs.isAutoAcceptEnable = false
                }
            }

            is CentralContract.Event.onAutoAcceptModeChanged -> {
                prefs.isAutoAcceptEnable = event.mode
            }

            is CentralContract.Event.onPrintModeChanged -> {
                prefs.printMode = event.mode
            }

            is CentralContract.Event.onOrderReadyOrientationChanged -> {
                prefs.orderReadyOrientation = event.mode
            }

            is CentralContract.Event.onDismissErrorDialog -> {
                setState {
                    copy(errMsg = null, printStatus = PrintStatus.Idle)
                }
            }

            is CentralContract.Event.IntentToUpdateVersion -> {
                intentDownloadApk(event.version)
            }

            is CentralContract.Event.OnDismissDownloadApkDialog -> {
                setState {
                    copy(downloadStatus = null)
                }
                cancelDownloadApkJob()
            }

            is CentralContract.Event.onVerifyCancel -> {
                setState {
                    copy(isVerifyCancel = true)
                }
            }

            is CentralContract.Event.startFetchKdsInfo -> {
                startFetchOrders()
            }

            is CentralContract.Event.startFetchOrderReadyInfo -> {
                startFetchOrderReady()
            }

            /**系統模式變更*/
            is CentralContract.Event.OnModeChanged -> {
                if (prefs.mode != event.mode) {
                    prefs.mode = event.mode
                    setState {
                        copy(modeState = event.mode)
                    }
                }
            }

            /**語系變更*/
            is CentralContract.Event.OnLanguageChanged -> {
                if (prefs.language != event.lan) {
                    setState {
                        prefs.language = event.lan
                        copy(languageState = InputStateWrapper(state = TextFieldState(event.lan)))
                    }

                    setState {
                        copy(
                            defaultPageState = InputStateWrapper(
                                state = TextFieldState(
                                    getDefaultPageByLan()
                                )
                            )
                        )
                    }
                }
            }

            /**品項名稱語系變更*/
            is CentralContract.Event.OnItemDisplayLanguageChanged -> {
                if (prefs.itemDisplayLan != event.lan) {
                    setState {
                        prefs.itemDisplayLan = event.lan
                        copy(
                            itemDisplayLanState = InputStateWrapper(state = TextFieldState(event.lan)),
                            displayLan = event.lan
                        )
                    }
                }
            }

            /**預設頁面(未開放)*/
            is CentralContract.Event.OnDefaultPageChanged -> {
                setState {
                    if (event.page == "Main" || event.page == "主页") {
                        prefs.defaultPage = "Main"
                    }

                    if (event.page == "Served" || event.page == "已完成") {
                        prefs.defaultPage = "Served"
                    }

                    if (event.page == "Recall" || event.page == "召回") {
                        prefs.defaultPage = "Recall"
                    }

                    copy(defaultPageState = InputStateWrapper(state = TextFieldState(event.page)))
                }
            }

            /**Main Finish按鍵觸發*/
            is CentralContract.Event.FinishOrder -> {
                Timber.d("FinishOrder: ${event.order.orderNo}")
                setOrderStatus(orderNo = event.order.orderNo, status = event.status, fromStatus = event.fromStatus)
            }

            is CentralContract.Event.ProgressOrder -> {
                val isAddon = event.order.isAddon
                setOrderStatus(orderNo = event.order.orderNo, status = event.status, fromStatus = event.fromStatus)

                setState {
                    copy(mainList = currentState.mainList?.map {
                        if (it.orderNo == event.order.orderNo) {
                            it.copy(status = PROGRESSING)
                        } else {
                            it
                        }
                    })
                }
                // 接單(變W)即印廚房單（Print Kitchen Order=Yes，printMode==0）
                // 加點：只印新增品項並標「加點」；全新單：印整張
                if (prefs.printMode == 0) {
                    val toPrint = if (isAddon)
                        event.order.copy(detail = event.order.addonItems, addonPrint = true)
                    else event.order
                    requestPrint(toPrint)
                }
            }

            /**Served Collected按鍵觸發*/
            is CentralContract.Event.CollectedOrder -> {
                setOrderStatus(orderNo = event.orderNo, status = event.status, fromStatus = event.fromStatus)
            }

            /**Recall Recall按鍵觸發*/
            is CentralContract.Event.RecallOrder -> {
                setOrderStatus(orderNo = event.orderNo, status = event.status, fromStatus = event.fromStatus)
            }

            /**載入庫存列表*/
            is CentralContract.Event.LoadStockList -> {
                fetchStockInfo()
            }

            /**選擇庫存類別*/
            is CentralContract.Event.OnStockTypeChanged -> {
                currentState.stockSearchState.state.setTextAndPlaceCursorAtEnd("")
                setState {
                    copy(
                        stockInfoPresentList = currentState.stockInfoList?.filter {
                            if (prefs.language == "English") {
                                it.gKEName == event.type
                            } else {
                                it.gKCName == event.type
                            }
                        },
                        stockTypeSelected = event.type,
                        stockTypeSelect = InputStateWrapper(TextFieldState(event.type))
                    )
                }
            }

            /**選擇庫存品項*/
            is CentralContract.Event.OnStockItemClicked -> {

                if (event.stockInfo.gID?.isBlank() == true || event.stockInfo.gKID?.isBlank() == true) {
                    setState {
                        copy(errMsg = UiText.StringResource(R.string.item_abnormal))
                    }
                    return
                }

                setSellStatus(
                    SetItemSellStatusRequest(
                        gID = event.stockInfo.gID ?: "",
                        gKID = event.stockInfo.gKID ?: "",
                        status = if (event.stockInfo.sellStatus == "Available") "Sold Out" else "Available",
                        storeNo = prefs.rsno,
                        gname = event.stockInfo.eName ?: event.stockInfo.cName ?: "",
                        size = event.stockInfo.size ?: ""
                    )
                )
            }

            /**設定品項庫存*/
            is CentralContract.Event.OnSetInventory -> {
                //setInventory(event.stock)
            }

            /**選擇掃描到的印表機 → 填入 IP*/
            is CentralContract.Event.OnPrinterSelected -> {
                val ip = event.info["Target"] ?: ""
                prefs.printerIp = ip
                currentState.printerIpState.state.setTextAndPlaceCursorAtEnd(ip)
            }

            /**測試印表機連線*/
            is CentralContract.Event.TestPrinter -> {
                viewModelScope.launch {
                    val ip = prefs.printerIp
                    if (ip.isBlank()) {
                        setState { copy(errMsg = UiText.DynamicString("請先輸入印表機 IP")) }
                        return@launch
                    }
                    val ok = LanPrinter(ip = ip, port = prefs.printerPort).ping()
                    setState {
                        copy(
                            errMsg = UiText.DynamicString(
                                if (ok) "印表機連線成功 ✓ ($ip:${prefs.printerPort})"
                                else "印表機連線失敗 ✗ ($ip:${prefs.printerPort})"
                            )
                        )
                    }
                }
            }

            /**掃描區網印表機*/
            is CentralContract.Event.ScanPrinters -> {
                viewModelScope.launch {
                    val base = prefs.printerIp.ifBlank { prefs.localIp }.substringBefore(":")
                    val subnet = base.split(".").take(3).joinToString(".").ifBlank { "192.168.0" }
                    val found = NetworkScanner.scan(subnet)
                    val list = ArrayList(found.map {
                        mapOf("PrinterName" to "Printer", "Target" to it)
                    })
                    printerDetecter.setValue(list)
                }
            }

            /**手動刷新取餐牆店家圖片*/
            is CentralContract.Event.ReloadOrderReadyImages -> {
                imgReloadTick++
                val tickSnapshot = imgReloadTick
                setState { copy(orderReadyTick = tickSnapshot) }
            }

            /**掃描取餐 QR（=訂單號）→ 即時抓 served 清單比對 → 自動 Collect*/
            is CentralContract.Event.ScanOrderNo -> {
                val orderNo = event.orderNo.trim()
                if (orderNo.isNotEmpty()) {
                    viewModelScope.launch {
                        // 掃碼取餐：要收的是「待取(O)」的單。後端 served 回的是已取(F)，
                        // 故改查 main（含 J/W/O），且僅在該單為 PREPARED(O) 時才 Collect。
                        repository.getOrders(type = "main").collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    val match = result.data.firstOrNull { it.orderNo == orderNo }
                                    if (match != null && match.status.equals(PREPARED, ignoreCase = true)) {
                                        setOrderStatus(orderNo, COLLECTED)
                                    } else {
                                        setState {
                                            copy(errMsg = UiText.StringResource(R.string.order_not_in_served))
                                        }
                                    }
                                }

                                is Result.Error -> {
                                    when (result.error) {
                                        is NetworkError -> setState {
                                            copy(errMsg = result.error.asUiText())
                                        }

                                        else -> Unit
                                    }
                                }

                                else -> Unit
                            }
                        }
                    }
                }
            }

            /**重印廚房單*/
            is CentralContract.Event.ReprintOrder -> {

                Timber.d("Printer issue trace: ${prefs.printMode}")
                if (currentState.printStatus != PrintStatus.Idle) {
                    return
                }


                if (prefs.printMode == 0) {
                    requestPrint(event.order)
                }
            }

            /**列印失敗後重印上一張快照（含加點子集；不受狀態已升級影響）*/
            is CentralContract.Event.RetryPrintOrder -> {
                val last = currentState.printOrder ?: return
                setState { copy(printStatus = PrintStatus.Idle) }
                requestPrint(last)
            }
        }

    }


    /** 更版 **/
    private fun intentDownloadApk(version: String) {
        updateJob = viewModelScope.launch {
            downloadApkUseCase.downloadApk(version, onProgressCallBack = {
                setState {
                    copy(downloadStatus = DownloadStatus.Progress(it))
                }
            }).collect {
                setState {
                    copy(downloadStatus = it)
                }
                if (it == DownloadStatus.Success) {
                    setEffect { CentralContract.Effect.DownloadApkSuccess }
                }
            }
        }
    }

    private fun getDefaultPageByLan(): String {
        var defaultPage = prefs.defaultPage
        if (prefs.language == "华文") {
            defaultPage = when (prefs.defaultPage) {
                "Main" -> "主页"
                "Served" -> "已完成"
                "Recall" -> "召回"
                else -> "主页"
            }
        }
        return defaultPage
    }

    private fun startFetchOrders() {
        stopFetchOrderReady()
        orderInfoJob = viewModelScope.launch {
            fetchOrdersJob().collect()
        }
    }

    private fun startFetchOrderReady() {
        stopFetchOrders()
        orderReadyJob = viewModelScope.launch {
            fetchOrderReadyJob().collect()
        }
    }

    private fun stopFetchOrders() {
        orderInfoJob?.cancel()
    }

    private fun stopFetchOrderReady() {
        orderReadyJob?.cancel()
    }

    private fun setSellStatus(
        setItemSellStatusRequest: SetItemSellStatusRequest,
        isUpdateServer: Boolean = true
    ) {
        viewModelScope.launch {
            repository.setSellStatus(setItemSellStatusRequest).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        Timber.d("setSellStatus: ${result.isLoading}")
                    }

                    is Result.Success -> {
                        Timber.d("setSellStatus: success")
                        val list = currentState.stockInfoPresentList?.map {
                            if (it.gID == setItemSellStatusRequest.gID && it.gKID == setItemSellStatusRequest.gKID) {
                                Timber.d("setSellStatus: success  ${it.sellStatus}")
                                it.copy(sellStatus = if (it.sellStatus == "Available") "Sold out" else "Available")
                            } else {
                                it
                            }
                        }

                        Timber.d("setSellStatus: success  ${list?.find { it.gID == setItemSellStatusRequest.gID && it.gKID == setItemSellStatusRequest.gKID }}")

                        setState {
                            copy(
                                setStatusGkidGid = setItemSellStatusRequest.gID to setItemSellStatusRequest.gKID,
                                stockInfoPresentList = list
                            )
                        }

                        if (isUpdateServer) {
                            setSellStatusRemote(setItemSellStatusRequest)
                        }

                    }

                    is Result.Error -> {
                        Timber.d("setSellStatus: err occur")
                        Timber.d("setSellStatus: ${result.error}")


                        when (result.error) {
                            is NetworkError -> {
                                Timber.d("setSellStatus 123: ${result.error.asUiText()}")
                                setState {
                                    copy(errMsg = result.error.asUiText())
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }


    private fun setSellStatusRemote(setItemSellStatusRequest: SetItemSellStatusRequest) {
        viewModelScope.launch {
            repository.setSellStatusRemote(setItemSellStatusRequest).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        Timber.d("setSellStatus Remote: ${result.isLoading}")
                    }

                    is Result.Success -> {
                        Timber.d("setSellStatus Remote: success")
                    }

                    is Result.Error -> {
                        val status = setItemSellStatusRequest.status
                        setItemSellStatusRequest.status =
                            if (status == "Available") "Sold out" else "Available"

                        setSellStatus(
                            setItemSellStatusRequest = setItemSellStatusRequest,
                            isUpdateServer = false
                        )


                        when (result.error) {
                            is NetworkError -> {
                                Timber.d("setSellStatus 123: ${result.error.asUiText()}")
                                setState {
                                    copy(errMsg = result.error.asUiText())
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun cancelDownloadApkJob() {
        updateJob?.cancel()
    }


    private fun setOrdersNotify(orderNo: String) = viewModelScope.launch {
        Timber.d("setOrdersNotify: $orderNo")
        repository.setOrdersNotifyRemote(
            ordersNotifyRequest = OrdersNotifyRequest(
                storeNo = prefs.rsno,
                orderNo = orderNo
            )
        ).collect { result ->
            when (result) {
                is Result.Loading -> {
                    Timber.d("setOrdersNotify: ${result.isLoading}")
                }

                is Result.Success -> {
                    Timber.d("setOrdersNotify: ${result.data}")
                }

                is Result.Error -> {
                    when (result.error) {
                        is NetworkError -> {
                            setState {
                                copy(errMsg = result.error.asUiText())
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    /** 發出一次列印請求：更新快照並遞增 printRequestId，讓 MainActivity 即使重印同一張也會重觸發 */
    private fun requestPrint(order: Order) {
        setState { copy(printOrder = order, printRequestId = printRequestId + 1) }
    }

    private fun setOrderStatus(orderNo: String, status: String, fromStatus: String? = null) = viewModelScope.launch {
        repository.setOrderStatus(
            setOrderStatusRequest = SetOrderStatusRequest(
                orderNo = orderNo,
                status = status,
                fromStatus = fromStatus
            )
        ).collect { result ->
            when (result) {
                is Result.Loading -> {
                    Timber.d("setOrderStatus: ${result.isLoading}")
                }

                is Result.Success -> {
                    Timber.d("setOrderStatus: ${result.data}")
                    if (status == PREPARED || status == COLLECTED) {

                        // 目前不需要呼叫 server 推播通知，暫時註解
                        // if (status == PREPARED && orderNo.startsWith("E")) {
                        //     setOrdersNotify(orderNo)
                        // }

                        setState {
                            copy(mainList = currentState.mainList?.map {
                                if (it.orderNo == orderNo) {
                                    it.copy(isVisible = false)
                                } else {
                                    it
                                }
                            }, recallList = currentState.recallList?.map {
                                if (it.orderNo == orderNo) {
                                    it.copy(isVisible = false)
                                } else {
                                    it
                                }
                            })
                        }
                    } else {
                        setState {
                            copy(servedList = currentState.servedList?.map {
                                if (it.orderNo == orderNo) {
                                    it.copy(isVisible = false)
                                } else {
                                    it
                                }
                            })
                        }
                    }

                    animateBufferGap = true
                    Timber.d("setOrderStatus: ${result.data}")
                }

                is Result.Error -> {
                    when (result.error) {
                        is NetworkError -> {
                            setState {
                                copy(errMsg = result.error.asUiText())
                            }
                        }

                        else -> Unit
                    }
                }

            }
        }
    }

    private fun fetchStockInfo() = viewModelScope.launch {
        repository.getStockInfo().collectLatest { result ->
            when (result) {
                is Result.Loading -> {
                    Timber.d("fetchStockInfo: ${result.isLoading}")
                }

                is Result.Success -> {
                    Timber.d("fetchStockInfo: ${result.data}")
                    setState {
                        copy(stockInfoList = result.data,
                            stockInfoPresentList = result.data.filter {
                                if (prefs.language == "English") {
                                    it.gKEName?.contains(
                                        currentState.stockTypeSelected,
                                        ignoreCase = true
                                    ) == true
                                } else {
                                    it.gKCName?.contains(
                                        currentState.stockTypeSelected,
                                        ignoreCase = true
                                    ) == true
                                }
                            }
                        )
                    }


                }

                is Result.Error -> {
                    when (result.error) {
                        is NetworkError -> {
                            setState {
                                copy(errMsg = result.error.asUiText())
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun fetchOrdersJob(): Flow<Job> = flow {
        while (currentCoroutineContext().isActive) {
            if (!animateBufferGap) {
                fetchOrders()
                delay(3000)
            } else {
                // 延後刷新至完成動畫播完再移除卡片，避免動畫被砍掉
                delay(OK_ANIMATION_BUFFER_MS)
                animateBufferGap = false
                fetchOrders()
            }

        }
    }

    private fun fetchOrderReadyJob(): Flow<Job> = flow {
        while (currentCoroutineContext().isActive) {
            fetchOrderReady()
            delay(3000)
        }
    }


    private fun fetchOrderReady() = viewModelScope.launch {
        repository.getOrderReadyInfo().collectLatest { result ->
            when (result) {
                is Result.Loading -> {
                    Timber.d("fetchOrderReady: ${result.isLoading}")
                }

                is Result.Success -> {
                    Timber.d("fetchOrderReady: ${result.data}")
                    val currentNos = result.data.flatMap { it.orderNo }.toSet()
                    if (orderReadyInitialized) {
                        // 這次才出現的新單 → 成為新的紅色集合（取代上一批）
                        val newNos = currentNos - orderReadyPrevSet
                        if (newNos.isNotEmpty()) {
                            orderReadyRedSet = newNos
                        }
                    }
                    // 已被取餐/離開牆上的，從紅色集合移除
                    orderReadyRedSet = orderReadyRedSet.intersect(currentNos)
                    orderReadyPrevSet = currentNos
                    orderReadyInitialized = true
                    val redSnapshot = orderReadyRedSet   // 避免 setState lambda 內被 State 同名屬性遮蔽
                    val tickSnapshot = imgReloadTick
                    setState {
                        copy(
                            orderReadyList = result.data,
                            orderReadyRedSet = redSnapshot,
                            orderReadyTick = tickSnapshot,
                            errMsg = null
                        )
                    }
                }

                is Result.Error -> {
                    Timber.d("fetchOrderReady: ${result.error}")
                    when (result.error) {
                        is NetworkError -> {
                            result.error.asUiText()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    /** 訂單相關頁面（主頁/已完成/召回）才需要輪詢；設定頁等其他頁面不輪詢。 */
    private val orderPages = setOf("main", "served", "recall")

    /**
     * 是否該進行輪詢。設定是逐字即時儲存的，無法用「欄位有沒有字」判斷是否設定完成，
     * 因此改用「使用者目前在哪一頁」當訊號：
     *  - 還停在設定頁（currentPage 非訂單頁）= 仍在設定中 → 不輪詢
     *  - 切到訂單頁 = 視為設定完成 → 才輪詢
     * 另外仍要求必要參數有填（POS 位址；KDS 模式還需 KDS 編號），
     * 避免完全沒設定就被切到訂單頁時送出無效請求一直跳錯誤訊息。
     */
    private fun shouldPollOrders(): Boolean {
        if (currentState.currentPage.lowercase() !in orderPages) return false
        return prefs.localIp.isNotEmpty() && (prefs.kdsId.isNotEmpty() || prefs.mode != 0)
    }

    private fun fetchOrders() = viewModelScope.launch {
        if (!shouldPollOrders()) return@launch   // 還在設定中或參數未填，先不輪詢
        repository.getOrders(
            type = currentState.currentPage.lowercase()
        ).collectLatest { result ->
            when (result) {
                is Result.Loading -> {
                    Timber.d("fetchOrders: ${result.isLoading}")
                }

                is Result.Success -> {
                    when (currentState.currentPage.lowercase()) {
                        "main" -> {
                            setState {
                                copy(mainList = result.data, errMsg = null)
                            }
                            autoAcceptNewOrders(result.data)
                        }

                        "served" -> {
                            setState {
                                copy(servedList = result.data, errMsg = null)
                            }
                        }

                        "recall" -> {
                            setState {
                                copy(recallList = result.data, errMsg = null)
                            }
                        }
                    }
                }

                is Result.Error -> {
                    when (result.error) {
                        is NetworkError -> {
                            setState {
                                copy(errMsg = result.error.asUiText())
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * 自動接單：開啟且 prepareMode 開時，把「有未接 pending(j)」的單自動推進到 W(製作中)。
     * - 候選 = displayStatus()=="J"（有 pending j；後端未提供 per-item itemStatus 時自動回退到整單 status==NEW，無回歸）。
     * - 開啟（或重啟）當下已存在的未接單設為基準、不自動接；只套用之後進來的。
     * - 加點(isAddon：已接 W/O 又冒出新 j)可越過基準/去重 → 支援「再加點」重複觸發。
     * - InFlight 防止延遲窗口內重複派發；派發後 j→W，下次輪詢不再是候選（自然去重）。
     */
    private fun autoAcceptNewOrders(orders: List<Order>) {
        if (!prefs.isAutoAcceptEnable || !prefs.isPrepareEnable) {
            autoAcceptBaselined = false   // 關閉時重置，下次開啟重新建立基準
            autoAcceptBaseline.clear()
            return
        }

        val pendingOrders = orders.filter { it.displayStatus() == NEW }

        if (!autoAcceptBaselined) {
            // 開啟（或重啟）後第一次：現有未接單設為基準，不自動接
            autoAcceptBaseline.clear()
            pendingOrders.forEach { autoAcceptBaseline.add(it.orderNo) }
            autoAcceptBaselined = true
            return
        }

        pendingOrders
            .filter { it.orderNo !in autoAcceptInFlight }
            // 非基準訂單；或雖在基準但已是加點(代表已被部分接過)→ 也要接
            .filter { it.orderNo !in autoAcceptBaseline || it.isAddon }
            .forEach { order ->
                autoAcceptInFlight.add(order.orderNo)
                autoAcceptDispatch(order)
            }
    }

    /**
     * 自動接單派發：樂觀更新 —— 立即把該單畫面狀態改成 W(製作中)，不等下一輪輪詢；
     * 若 setOrderStatus 失敗，再改回 J(新單) 並提示（不自動重試）。
     */
    private fun autoAcceptDispatch(order: Order) = viewModelScope.launch {
        val orderNo = order.orderNo
        val isAddon = order.isAddon
        // 樂觀更新：把該單的 pending(j) 品項就地改成 W（同時更新整單 status，兼容後端未提供 itemStatus 的情況）
        setState {
            copy(mainList = currentState.mainList?.map { o ->
                if (o.orderNo == orderNo)
                    o.copy(
                        status = PROGRESSING,
                        detail = o.detail.map { d -> if (d.isPending) d.copy(itemStatus = PROGRESSING) else d }
                    )
                else o
            })
        }

        repository.setOrderStatus(
            setOrderStatusRequest = SetOrderStatusRequest(
                orderNo = orderNo,
                status = PROGRESSING,
                fromStatus = "j,J"   // 只升級未接的新品項
            )
        ).collect { result ->
            when (result) {
                is Result.Success -> {
                    // 接單(變W)成功 → 印廚房單（Print Kitchen Order=Yes，printMode==0）
                    // 加點只印新增品項並標「加點」；全新單印整張
                    if (prefs.printMode == 0) {
                        val toPrint = if (isAddon)
                            order.copy(detail = order.addonItems, addonPrint = true)
                        else order
                        requestPrint(toPrint)
                    }
                    autoAcceptInFlight.remove(orderNo)
                }

                is Result.Error -> {
                    Timber.d("autoAccept 派發失敗，還原 $orderNo: ${result.error}")
                    // 失敗：還原整單 status（相容無 itemStatus 的舊路徑）；
                    // 有 itemStatus 時 detail 交給下一輪輪詢校正（伺服器仍為 j），避免誤翻原本已 W 的品項
                    setState {
                        copy(mainList = currentState.mainList?.map { o ->
                            if (o.orderNo == orderNo) o.copy(status = NEW) else o
                        })
                    }
                    autoAcceptInFlight.remove(orderNo)
                    when (result.error) {
                        is NetworkError -> setState { copy(errMsg = result.error.asUiText()) }
                        else -> Unit
                    }
                }

                else -> Unit
            }
        }
    }

    fun updateCurrentPage(pageIndex: Int) {
        when (pageIndex) {
            0 -> {
                setState {
                    copy(currentPage = "main")
                }
            }

            1 -> {
                setState {
                    copy(currentPage = "served")
                }
            }

            2 -> {
                setState {
                    copy(currentPage = "recall")
                }
            }

            3 -> {
                setState {
                    copy(currentPage = "setStock")
                }
            }

            else -> {
                setState {
                    copy(currentPage = "else")
                }
            }
        }
    }

    companion object {
        /** 完成動畫(operation_success.json 1.5s，以 1.5x 播放 ≈ 1s)的緩衝時間，含少量餘裕。 */
        private const val OK_ANIMATION_BUFFER_MS = 1100L
    }
}
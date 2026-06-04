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

    /** 自動接單已派發過的訂單（記憶體防重派；重啟 App 自動清空）*/
    private val autoAcceptedThisSession = mutableSetOf<String>()

    /** 自動接單是否已建立基準：開啟（或重啟）當下的舊 J 單設為基準不自動接，只套用之後進來的新單 */
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
//                if (currentState.printStatus != PrintStatus.Idle) {
//                    return
//                }
                setOrderStatus(orderNo = event.order.orderNo, status = event.status)

//                if (prefs.printMode == 1) {
//                    setState {
//                        copy(printOrder = event.order)
//                    }
//                }
            }

            is CentralContract.Event.ProgressOrder -> {
                setOrderStatus(orderNo = event.order.orderNo, status = event.status)

                setState {
                    copy(mainList = currentState.mainList?.map {
                        if (it.orderNo == event.order.orderNo) {
                            it.copy(status = PROGRESSING)
                        } else {
                            it
                        }
                    })
                }
            }

            /**Served Collected按鍵觸發*/
            is CentralContract.Event.CollectedOrder -> {
                setOrderStatus(orderNo = event.orderNo, status = event.status)
            }

            /**Recall Recall按鍵觸發*/
            is CentralContract.Event.RecallOrder -> {
                setOrderStatus(orderNo = event.orderNo, status = event.status)
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
                    setState {
                        Timber.d("Printer issue trace: step 1")
                        copy(printOrder = event.order)
                    }
                }
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

    private fun setOrderStatus(orderNo: String, status: String) = viewModelScope.launch {
        repository.setOrderStatus(
            setOrderStatusRequest = SetOrderStatusRequest(
                orderNo = orderNo,
                status = status
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
                delay(1000)
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

    private fun fetchOrders() = viewModelScope.launch {
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
     * 自動接單：開啟且 prepareMode 開時，把新進的 J(NEW) 單自動推進到 W(製作中)。
     * 只套用「開啟之後才進來的新單」；開啟（或重啟）當下已存在的舊 J 單設為基準、不自動接。
     */
    private fun autoAcceptNewOrders(orders: List<Order>) {
        if (!prefs.isAutoAcceptEnable || !prefs.isPrepareEnable) {
            autoAcceptBaselined = false   // 關閉時重置，下次開啟重新建立基準
            return
        }

        val newOrders = orders.filter { it.status.uppercase() == NEW }

        if (!autoAcceptBaselined) {
            // 開啟（或重啟）後第一次：現有 J 單設為基準，舊單不派，只套用之後進來的新單
            newOrders.forEach { autoAcceptedThisSession.add(it.orderNo) }
            autoAcceptBaselined = true
            return
        }

        newOrders
            .filter { it.orderNo !in autoAcceptedThisSession }
            .forEach { order ->
                autoAcceptedThisSession.add(order.orderNo)   // 先標記再派發，避免下次輪詢重派
                autoAcceptDispatch(order.orderNo)
            }
    }

    /**
     * 自動接單派發：樂觀更新 —— 立即把該單畫面狀態改成 W(製作中)，不等下一輪輪詢；
     * 若 setOrderStatus 失敗，再改回 J(新單) 並提示（不自動重試）。
     */
    private fun autoAcceptDispatch(orderNo: String) = viewModelScope.launch {
        // 樂觀更新：先把畫面改成製作中（不等下一輪輪詢）
        setState {
            copy(mainList = currentState.mainList?.map {
                if (it.orderNo == orderNo) it.copy(status = PROGRESSING) else it
            })
        }

        repository.setOrderStatus(
            setOrderStatusRequest = SetOrderStatusRequest(
                orderNo = orderNo,
                status = PROGRESSING
            )
        ).collect { result ->
            if (result is Result.Error) {
                Timber.d("autoAccept 派發失敗，還原 $orderNo 為 NEW: ${result.error}")
                // 失敗：改回 J(新單)
                setState {
                    copy(mainList = currentState.mainList?.map {
                        if (it.orderNo == orderNo) it.copy(status = NEW) else it
                    })
                }
                when (result.error) {
                    is NetworkError -> setState { copy(errMsg = result.error.asUiText()) }
                    else -> Unit
                }
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
}
package com.citrus.citruskds.ui.presentation

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text2.input.textAsFlow
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.citrus.citruskds.commonData.DataError
import com.citrus.citruskds.commonData.NetworkError
import com.citrus.citruskds.commonData.Resource
import com.citrus.citruskds.commonData.Result
import com.citrus.citruskds.commonData.vo.SetInventoryRequest
import com.citrus.citruskds.commonData.vo.SetOrderStatusRequest
import com.citrus.citruskds.commonData.vo.StockInfo
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.ui.domain.ApiRepositoryImpl
import com.citrus.citruskds.util.BaseViewModel
import com.citrus.citruskds.util.InputStateWrapper
import com.citrus.citruskds.util.PrinterDetecter
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
    private val printerDetecter: PrinterDetecter
) : BaseViewModel<CentralContract.Event, CentralContract.State, CentralContract.Effect>() {

    private var orderInfoJob: Job? = null

    private var orderReadyJob: Job? = null

    private var animateBufferGap = false


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
                    }.let { info ->
                        if (list?.find { it.isSelect } != null) {
                            val target = list.find { it.isSelect }

                            info?.map {
                                if (it.gID == target!!.gID && it.gKID == target.gKID) {
                                    it.copy(isSelect = true)
                                } else {
                                    it
                                }
                            }
                        } else {
                            info
                        }
                    }


                    setState {
                        copy(stockInfoPresentList = stockInfoFilterPresentList)
                    }
                }
        }


        viewModelScope.launch {
            currentState.kdsIdState.state.textAsFlow().collectLatest {
                prefs.kdsId = it.toString()
            }
        }

        viewModelScope.launch {
            currentState.localIpState.state.textAsFlow().collectLatest {
                prefs.localIp = it.toString()
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
            localIpState = InputStateWrapper(TextFieldState(prefs.localIp)),
            languageState = InputStateWrapper(TextFieldState(prefs.language)),
            itemDisplayLanState = InputStateWrapper(TextFieldState(prefs.itemDisplayLan)),
            defaultPageState = InputStateWrapper(TextFieldState(getDefaultPageByLan())),
            servedSearchState = InputStateWrapper(TextFieldState("")),
            recallSearchState = InputStateWrapper(TextFieldState("")),
            stockTypeSelect = InputStateWrapper(TextFieldState("")),
            stockSearchState = InputStateWrapper(TextFieldState("")),
            printerState = InputStateWrapper(TextFieldState(printer)),
        )
    }

    override fun handleEvent(event: CentralContract.Event) {
        when (event) {
            is CentralContract.Event.onDismissErrorDialog -> {
                setState {
                    copy(errMsg = null)
                }
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
                setOrderStatus(orderNo = event.order.orderNo, status = event.status)
                setState {
                    copy(printOrder = event.order)
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
                val list = currentState.stockInfoPresentList?.map {
                    if (it.gID == event.stockInfo.gID && it.gKID == event.stockInfo.gKID) {
                        it.copy(isSelect = !it.isSelect)
                    } else {
                        it.copy(isSelect = false)
                    }
                }

                setState {
                    copy(stockInfoPresentList = list)
                }
            }

            /**設定品項庫存*/
            is CentralContract.Event.OnSetInventory -> {
                setInventory(event.stock)
            }

            /**選擇印表機*/
            is CentralContract.Event.OnPrinterSelected -> {
                setState {
                    copy(
                        printerState = InputStateWrapper(
                            state = TextFieldState(
                                (event.info["PrinterName"] ?: "") + " " + (event.info["Target"]
                                    ?: "")
                            )
                        )
                    )
                }
                prefs.printerTarget = event.info["Target"] ?: ""
                prefs.printerName = event.info["PrinterName"] ?: ""
            }

            /**重印廚房單*/
            is CentralContract.Event.ReprintOrder -> {
                setState {
                    Timber.d("Printer issue trace: step 1")
                    copy(printOrder = event.order)
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


    private fun setInventory(stockInfo: StockInfo) = viewModelScope.launch {
        repository.setInventory(
            setInventoryRequest = SetInventoryRequest(
                gID = stockInfo.gID!!,
                gKID = stockInfo.gKID!!,
                stock = stockInfo.stock!!.toInt()
            )
        ).collect { result ->
            when (result) {
                is Result.Loading -> {
                    Timber.d("setInventory: ${result.isLoading}")
                }

                is Result.Success -> {
                    Timber.d("setInventory: Success")
                    fetchStockInfo()
                }

                is Result.Error -> {
                    when (result.error) {
                        is NetworkError -> {
                            setState {
                                copy(errMsg = result.error.asUiText())
                            }
                        }
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
                    if (status == "O") {
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
                    setState {
                        copy(orderReadyList = result.data, errMsg = null)
                    }
                }

                is Result.Error -> {
                    Timber.d("fetchOrderReady: ${result.error}")
                    when (result.error) {
                        is NetworkError -> {
                            result.error.asUiText()
                        }
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
                    Timber.d("fetchOrders: ${result.data}")
                    Timber.d("fetchOrders: ${currentState.currentPage.lowercase()}")
                    when (currentState.currentPage.lowercase()) {
                        "main" -> {
                            setState {
                                copy(mainList = result.data, errMsg = null)
                            }
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
                    }
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

            else -> {
                setState {
                    copy(currentPage = "else")
                }
            }
        }
    }
}
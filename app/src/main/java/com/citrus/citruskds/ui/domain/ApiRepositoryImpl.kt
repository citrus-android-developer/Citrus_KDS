package com.citrus.citruskds.ui.domain


import com.citrus.citruskds.commonData.ApiService
import com.citrus.citruskds.commonData.NetworkError
import com.citrus.citruskds.commonData.Result
import com.citrus.citruskds.commonData.RootError
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.OrderReadyInfo
import com.citrus.citruskds.commonData.vo.OrderRequest
import com.citrus.citruskds.commonData.vo.OrdersNotifyRequest
import com.citrus.citruskds.commonData.vo.SetInventoryRequest
import com.citrus.citruskds.commonData.vo.SetItemSellStatusRemoteRequest
import com.citrus.citruskds.commonData.vo.SetItemSellStatusRequest
import com.citrus.citruskds.commonData.vo.SetWastageRequest
import com.citrus.citruskds.commonData.vo.SetOrderStatusRequest
import com.citrus.citruskds.commonData.vo.StockInfo
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.util.Constants.POS_GET_ORDER
import com.citrus.citruskds.util.Constants.POS_GET_ORDER_READY_INFO
import com.citrus.citruskds.util.Constants.POS_GET_STOCK_INFO
import com.citrus.citruskds.util.Constants.POS_SET_INVENTORY
import com.citrus.citruskds.util.Constants.POS_SET_ORDER_STATUS
import com.citrus.citruskds.util.Constants.POS_SET_SELL_STATUS
import com.citrus.citruskds.util.Constants.POS_SET_WASTAGE
import com.citrus.citruskds.util.Constants.SERVER_SET_ORDERS_NOTIFY
import com.citrus.citruskds.util.Constants.SERVER_SET_SELL_STATUS
import com.citrus.citruskds.util.resultFlowData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ApiRepositoryImpl @Inject constructor(private val apiService: ApiService) : ApiRepository {
    override suspend fun getOrders(type: String): Flow<Result<List<Order>, RootError>> =
        resultFlowData(apiAction = {
            apiService.getOrdersByType(
                url = "http://" + prefs.localIp + POS_GET_ORDER, orderRequest = OrderRequest(
                    kdsId = prefs.kdsId, type = type
                )
            )
        }, feature = "GetOrder: ") as Flow<Result<List<Order>, RootError>>

    override suspend fun setOrderStatus(setOrderStatusRequest: SetOrderStatusRequest): Flow<Result<Int, RootError>> =
        resultFlowData(apiAction = {
            apiService.setOrderStatus(
                url = "http://" + prefs.localIp + POS_SET_ORDER_STATUS,
                setOrderStatusRequest = setOrderStatusRequest
            )
        }, feature = "SetOrderStatus: ") as Flow<Result<Int, RootError>>

    override suspend fun getStockInfo(): Flow<Result<List<StockInfo>, RootError>> =
        resultFlowData(apiAction = {
            apiService.getStockInfo(
                url = "http://" + prefs.localIp + POS_GET_STOCK_INFO
            )
        }, feature = "GetStockInfo: ") as Flow<Result<List<StockInfo>, RootError>>

    override suspend fun getOrderReadyInfo(): Flow<Result<List<OrderReadyInfo>, RootError>> =
        resultFlowData(apiAction = {
            apiService.getOrderReadyInfo(
                url = "http://" + prefs.localIp + POS_GET_ORDER_READY_INFO
            )
        }, feature = "GetOrderReadyInfo: ") as Flow<Result<List<OrderReadyInfo>, RootError>>

    override suspend fun setInventory(setInventoryRequest: SetInventoryRequest): Flow<Result<Unit, RootError>> =
        resultFlowData(apiAction = {
            apiService.setInventory(
                url = "http://" + prefs.localIp + POS_SET_INVENTORY,
                setInventoryRequest = setInventoryRequest
            )
        }, feature = "SetInventory: ") as Flow<Result<Unit, RootError>>

    override suspend fun setSellStatus(setItemSellStatusRequest: SetItemSellStatusRequest): Flow<Result<Unit, RootError>> =
        resultFlowData(apiAction = {
            apiService.setSellStatusLocal(
                url = "http://" + prefs.localIp + POS_SET_SELL_STATUS,
                setItemSellStatusRequest = setItemSellStatusRequest
            )
        }, feature = "SetSellStatus: ") as Flow<Result<Unit, RootError>>

    override suspend fun setSellStatusRemote(setItemSellStatusRequest: SetItemSellStatusRequest): Flow<Result<Unit, RootError>> {
        // 未設定 Server URL → 直接擋下，避免 @Url 變相對路徑 fallback 到 Retrofit BASE_URL（誤打到非預期後端）
        if (prefs.serverBaseUrl.isBlank()) return serverUrlNotConfigured()
        return resultFlowData(apiAction = {
            apiService.setSellStatusRemote(
                url = prefs.serverBaseUrl + SERVER_SET_SELL_STATUS,
                // 雲端僅需 StoreNo/GKID/GID/Status；Gname/Size 由後端補齊。
                // 雲端 Status 僅允許 Available / Not Available，故將本地的 "Sold Out" 對應為 "Not Available"
                setItemSellStatusRemoteRequest = SetItemSellStatusRemoteRequest(
                    storeNo = setItemSellStatusRequest.storeNo,
                    gKID = setItemSellStatusRequest.gKID,
                    gID = setItemSellStatusRequest.gID,
                    status = if (setItemSellStatusRequest.status == "Available") "Available" else "Not Available"
                )
            )
        }, feature = "SetSellStatusRemote: ") as Flow<Result<Unit, RootError>>
    }

    override suspend fun setWastage(setWastageRequest: SetWastageRequest): Flow<Result<Unit, RootError>> =
        resultFlowData(apiAction = {
            apiService.setWastage(
                url = "http://" + prefs.localIp + POS_SET_WASTAGE,
                setWastageRequest = setWastageRequest
            )
        }, feature = "SetWastage: ") as Flow<Result<Unit, RootError>>

    override suspend fun setOrdersNotifyRemote(ordersNotifyRequest: OrdersNotifyRequest): Flow<Result<Unit, RootError>> {
        // 未設定 Server URL → 直接擋下，避免 @Url 變相對路徑 fallback 到 Retrofit BASE_URL（誤打到非預期後端）
        if (prefs.serverBaseUrl.isBlank()) return serverUrlNotConfigured()
        return resultFlowData(apiAction = {
            apiService.setOrdersNotify(
                url = prefs.serverBaseUrl + SERVER_SET_ORDERS_NOTIFY,
                ordersNotifyRequest = ordersNotifyRequest
            )
        }, feature = "SetOrdersNotify: ") as Flow<Result<Unit, RootError>>
    }

    /** Server URL 未設定時的統一錯誤流：阻止遠端請求送出；訊息由 UI 依語系顯示（[NetworkError.ServerUrlNotSet]）。 */
    private fun serverUrlNotConfigured(): Flow<Result<Unit, RootError>> =
        flowOf(Result.Error(NetworkError.ServerUrlNotSet))
}
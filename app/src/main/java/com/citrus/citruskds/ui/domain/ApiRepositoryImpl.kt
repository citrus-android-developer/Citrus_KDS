package com.citrus.citruskds.ui.domain


import com.citrus.citruskds.commonData.ApiService
import com.citrus.citruskds.commonData.Result
import com.citrus.citruskds.commonData.RootError
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.OrderReadyInfo
import com.citrus.citruskds.commonData.vo.OrderRequest
import com.citrus.citruskds.commonData.vo.OrdersNotifyRequest
import com.citrus.citruskds.commonData.vo.SetInventoryRequest
import com.citrus.citruskds.commonData.vo.SetItemSellStatusRequest
import com.citrus.citruskds.commonData.vo.SetOrderStatusRequest
import com.citrus.citruskds.commonData.vo.StockInfo
import com.citrus.citruskds.di.prefs
import com.citrus.citruskds.util.Constants
import com.citrus.citruskds.util.Constants.POS_GET_ORDER
import com.citrus.citruskds.util.Constants.POS_GET_ORDER_READY_INFO
import com.citrus.citruskds.util.Constants.POS_GET_STOCK_INFO
import com.citrus.citruskds.util.Constants.POS_SET_INVENTORY
import com.citrus.citruskds.util.Constants.POS_SET_ORDER_STATUS
import com.citrus.citruskds.util.Constants.POS_SET_SELL_STATUS
import com.citrus.citruskds.util.Constants.SERVER_SET_ORDERS_NOTIFY
import com.citrus.citruskds.util.Constants.SERVER_SET_SELL_STATUS
import com.citrus.citruskds.util.resultFlowData
import kotlinx.coroutines.flow.Flow
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
        }) as Flow<Result<List<Order>, RootError>>

    override suspend fun setOrderStatus(setOrderStatusRequest: SetOrderStatusRequest): Flow<Result<Int, RootError>> =
        resultFlowData(apiAction = {
            apiService.setOrderStatus(
                url = "http://" + prefs.localIp + POS_SET_ORDER_STATUS,
                setOrderStatusRequest = setOrderStatusRequest
            )
        }) as Flow<Result<Int, RootError>>

    override suspend fun getStockInfo(): Flow<Result<List<StockInfo>, RootError>> =
        resultFlowData(apiAction = {
            apiService.getStockInfo(
                url = "http://" + prefs.localIp + POS_GET_STOCK_INFO
            )
        }) as Flow<Result<List<StockInfo>, RootError>>

    override suspend fun getOrderReadyInfo(): Flow<Result<List<OrderReadyInfo>, RootError>> =
        resultFlowData(apiAction = {
            apiService.getOrderReadyInfo(
                url = "http://" + prefs.localIp + POS_GET_ORDER_READY_INFO
            )
        }) as Flow<Result<List<OrderReadyInfo>, RootError>>

    override suspend fun setInventory(setInventoryRequest: SetInventoryRequest): Flow<Result<Unit, RootError>> =
        resultFlowData(apiAction = {
            apiService.setInventory(
                url = "http://" + prefs.localIp + POS_SET_INVENTORY,
                setInventoryRequest = setInventoryRequest
            )
        }) as Flow<Result<Unit, RootError>>

    override suspend fun setSellStatus(setItemSellStatusRequest: SetItemSellStatusRequest): Flow<Result<Unit, RootError>> =
        resultFlowData(apiAction = {
            apiService.setSellStatusLocal(
                url = "http://" + prefs.localIp + POS_SET_SELL_STATUS,
                setItemSellStatusRequest = setItemSellStatusRequest
            )
        }) as Flow<Result<Unit, RootError>>

    override suspend fun setSellStatusRemote(setItemSellStatusRequest: SetItemSellStatusRequest): Flow<Result<Unit, RootError>> =
        resultFlowData(apiAction = {
            apiService.setSellStatusRemote(
                url = Constants.BASE_URL + SERVER_SET_SELL_STATUS,
                setItemSellStatusRequest = setItemSellStatusRequest
            )
        }) as Flow<Result<Unit, RootError>>

    override suspend fun setOrdersNotifyRemote(ordersNotifyRequest: OrdersNotifyRequest): Flow<Result<Unit, RootError>> =
        resultFlowData(apiAction = {
            apiService.setOrdersNotify(
                url = Constants.BASE_URL + SERVER_SET_ORDERS_NOTIFY,
                ordersNotifyRequest = ordersNotifyRequest
            )
        }) as Flow<Result<Unit, RootError>>


}
package com.citrus.citruskds.ui.domain

import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.SetOrderStatusRequest
import kotlinx.coroutines.flow.Flow
import com.citrus.citruskds.commonData.Result
import com.citrus.citruskds.commonData.RootError
import com.citrus.citruskds.commonData.vo.OrderReadyInfo
import com.citrus.citruskds.commonData.vo.OrdersNotifyRequest
import com.citrus.citruskds.commonData.vo.SetInventoryRequest
import com.citrus.citruskds.commonData.vo.SetItemSellStatusRequest
import com.citrus.citruskds.commonData.vo.StockInfo

interface ApiRepository {
    suspend fun getOrders(type: String): Flow<Result<List<Order>, RootError>>
    suspend fun setOrderStatus(setOrderStatusRequest: SetOrderStatusRequest): Flow<Result<Int, RootError>>
    suspend fun getStockInfo(): Flow<Result<List<StockInfo>, RootError>>
    suspend fun getOrderReadyInfo(): Flow<Result<List<OrderReadyInfo>, RootError>>
    suspend fun setInventory(setInventoryRequest: SetInventoryRequest): Flow<Result<Unit, RootError>>
    suspend fun setSellStatus(setItemSellStatusRequest: SetItemSellStatusRequest): Flow<Result<Unit, RootError>>
    suspend fun setSellStatusRemote(setItemSellStatusRequest: SetItemSellStatusRequest): Flow<Result<Unit, RootError>>
    suspend fun setOrdersNotifyRemote(ordersNotifyRequest: OrdersNotifyRequest): Flow<Result<Unit, RootError>>
}

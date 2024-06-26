package com.citrus.citruskds.commonData

import com.citrus.citruskds.commonData.vo.ApiResult
import com.citrus.citruskds.commonData.vo.Order
import com.citrus.citruskds.commonData.vo.OrderReadyInfo
import com.citrus.citruskds.commonData.vo.OrderRequest
import com.citrus.citruskds.commonData.vo.OrdersNotifyRequest
import com.citrus.citruskds.commonData.vo.SetInventoryRequest
import com.citrus.citruskds.commonData.vo.SetItemSellStatusRequest
import com.citrus.citruskds.commonData.vo.SetOrderStatusRequest
import com.citrus.citruskds.commonData.vo.StockInfo
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url


interface ApiService {
    @POST
    suspend fun getOrdersByType(
        @Url url: String,
        @Body orderRequest: OrderRequest
    ): ApiResult<List<Order>>


    @POST
    suspend fun setOrderStatus(
        @Url url: String,
        @Body setOrderStatusRequest: SetOrderStatusRequest
    ): ApiResult<Int>


    @POST
    suspend fun getStockInfo(
        @Url url: String,
    ): ApiResult<List<StockInfo>>


    @POST
    suspend fun setInventory(
        @Url url: String,
        @Body setInventoryRequest: SetInventoryRequest
    ): ApiResult<Int?>

    @POST
    suspend fun getOrderReadyInfo(
        @Url url: String,
    ): ApiResult<List<OrderReadyInfo>>


    @POST
    suspend fun setSellStatusLocal(
        @Url url: String,
        @Body setItemSellStatusRequest: SetItemSellStatusRequest
    ): ApiResult<Int>

    @POST
    suspend fun setSellStatusRemote(
        @Url url: String,
        @Body setItemSellStatusRequest: SetItemSellStatusRequest
    ): ApiResult<Int>


    @POST
    suspend fun setOrdersNotify(
        @Url url: String,
        @Body ordersNotifyRequest: OrdersNotifyRequest
    ): ApiResult<Int>

}
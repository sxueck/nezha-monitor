package com.sxueck.monitor.data.network

import com.sxueck.monitor.data.model.LoginRequest
import com.sxueck.monitor.data.model.LoginResponse
import com.sxueck.monitor.data.model.ServerData
import com.sxueck.monitor.data.model.ServerGroup
import com.sxueck.monitor.data.model.ServerGroupResponseItem
import com.sxueck.monitor.data.model.ServerResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NezhaApi {
    @POST("api/v1/login")
    suspend fun login(@Body request: LoginRequest): Response<ServerResponse<LoginResponse>>

    @GET("api/v1/server")
    suspend fun getServerList(@Query("id") id: Long? = null): Response<ServerResponse<List<ServerData>>>

    @GET("api/v1/server-group")
    suspend fun getServerGroups(): Response<ServerResponse<List<ServerGroupResponseItem>>>
}

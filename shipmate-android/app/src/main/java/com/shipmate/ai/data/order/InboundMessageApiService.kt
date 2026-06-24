package com.shipmate.ai.data.order

import retrofit2.http.Body
import retrofit2.http.POST

interface InboundMessageApiService {
    @POST("api/inbound-messages")
    suspend fun create(@Body request: CreateInboundMessageRequest): InboundMessageResponse
}

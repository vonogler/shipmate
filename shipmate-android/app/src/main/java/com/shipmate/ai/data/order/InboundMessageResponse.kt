package com.shipmate.ai.data.order

data class InboundMessageResponse(
    val inboundMessageId: String,
    val merchantId: String,
    val customerId: String?,
    val sourceChannel: String,
    val businessChannel: String,
    val rawText: String,
    val inboundMessageStatus: String
)

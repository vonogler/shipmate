package com.shipmate.ai.data.order

data class CreateInboundMessageRequest(
    val merchantId: String,
    val merchantStaffId: String?,
    val customerId: String?,
    val sourceChannel: String,
    val businessChannel: String,
    val rawText: String
)

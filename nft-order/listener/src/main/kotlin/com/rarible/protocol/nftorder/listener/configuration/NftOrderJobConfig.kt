package com.rarible.protocol.nftorder.listener.configuration

class ReconciliationJobConfig(
    val order: OrderReconciliationConfig
)

class OrderReconciliationConfig(
    val batchSize: Int = 50
)
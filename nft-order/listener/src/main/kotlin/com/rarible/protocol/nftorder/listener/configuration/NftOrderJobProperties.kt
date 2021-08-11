package com.rarible.protocol.nftorder.listener.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("listener.job")
class NftOrderJobProperties(
    val reconciliation: ReconciliationJobConfig
)
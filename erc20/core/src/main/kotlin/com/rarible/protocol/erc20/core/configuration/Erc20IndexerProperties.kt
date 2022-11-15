package com.rarible.protocol.erc20.core.configuration

import com.rarible.blockchain.scanner.ethereum.reduce.MetricProperties
import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("common")
data class Erc20IndexerProperties(
    override val blockchain: Blockchain,
    override val metricRootPath: String = "erc20-metrics",
    val confirmationBlocks: Int = 12,
    val kafkaReplicaSet: String,
) : MetricProperties
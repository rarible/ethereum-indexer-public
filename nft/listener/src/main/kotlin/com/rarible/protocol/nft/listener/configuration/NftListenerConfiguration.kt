package com.rarible.protocol.nft.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.monitoring.BlockchainMonitoringWorker
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.listener.NftListenerApplication
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EnableMongock
@Configuration
@EnableScaletherMongoConversions
@EnableLogListeners(scanPackage = [NftListenerApplication::class])
@EnableConfigurationProperties(NftListenerProperties::class)
class NftListenerConfiguration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val blockRepository: BlockRepository
) {
    @Bean
    fun reduceSkipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens(nftListenerProperties.skipReduceTokens.map { ItemId.parseId(it) })
    }

    @Bean
    fun blockchainMonitoringWorker(): BlockchainMonitoringWorker {
        return BlockchainMonitoringWorker(
            properties = nftListenerProperties.monitoringWorker,
            blockchain = nftIndexerProperties.blockchain,
            meterRegistry = meterRegistry,
            blockRepository = blockRepository
        )
    }
}
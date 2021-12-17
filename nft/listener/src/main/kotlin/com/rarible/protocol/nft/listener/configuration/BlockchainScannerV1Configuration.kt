package com.rarible.protocol.nft.listener.configuration

import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.monitoring.BlockchainMonitoringWorker
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.listener.NftListenerApplication
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["common.feature-flags.scanner-version"], havingValue = "V1")
@EnableLogListeners(scanPackage = [NftListenerApplication::class])
class BlockchainScannerV1Configuration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val blockRepository: BlockRepository
) {
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

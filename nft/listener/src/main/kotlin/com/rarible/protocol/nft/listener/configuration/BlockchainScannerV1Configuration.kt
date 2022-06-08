package com.rarible.protocol.nft.listener.configuration

import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.monitoring.BlockchainMonitoringWorker
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.listener.NftListenerApplication
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableOnScannerV1
@EnableLogListeners(scanPackage = [NftListenerApplication::class])
class BlockchainScannerV1Configuration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val blockRepository: BlockRepository
) {
    // TODO: this bean is apparently configured in the ethereum-core (BlockchainMonitoringConfiguration), no need to configure here.
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

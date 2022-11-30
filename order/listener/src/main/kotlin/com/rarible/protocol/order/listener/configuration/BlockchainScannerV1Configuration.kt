package com.rarible.protocol.order.listener.configuration

import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.monitoring.BlockchainMonitoringWorker
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.OrderListenerApplication
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableOnScannerV1
@EnableLogListeners(scanPackage = [OrderListenerApplication::class])
class BlockchainScannerV1Configuration(
    private val commonProperties: OrderIndexerProperties,
    private val listenerProperties: OrderListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val blockRepository: BlockRepository
) {
    @Bean
    fun blockchainMonitoringWorker(): BlockchainMonitoringWorker {
        return BlockchainMonitoringWorker(
            properties = listenerProperties.monitoringWorker,
            blockchain = commonProperties.blockchain,
            meterRegistry = meterRegistry,
            blockRepository = blockRepository
        )
    }
}
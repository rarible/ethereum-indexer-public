package com.rarible.protocol.nft.listener.configuration

import com.rarible.core.task.TaskRepository
import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.listener.log.LogListenService
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.monitoring.BlockchainMonitoringWorker
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.NftListenerApplication
import com.rarible.protocol.nft.listener.admin.ReindexTokenItemsTaskHandler
import com.rarible.protocol.nft.listener.admin.ReindexTokenTaskHandler
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import scalether.core.MonoEthereum

@Configuration
@ConditionalOnProperty(name = ["common.feature-flags.scanner-version"], havingValue = "V1")
@EnableLogListeners(scanPackage = [NftListenerApplication::class])
class BlockchainScannerV1Configuration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
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

    @Bean
    fun reindexTokenItemsTaskHandler(
        taskRepository: TaskRepository,
        logListenService: LogListenService,
        tokenRegistrationService: TokenRegistrationService,
        ethereum: MonoEthereum
    ) : ReindexTokenItemsTaskHandler {
        return ReindexTokenItemsTaskHandler(
            taskRepository = taskRepository,
            logListenService = logListenService,
            tokenRegistrationService = tokenRegistrationService,
            ethereum = ethereum
        )
    }

    @Bean
    fun reindexTokenTaskHandler(
        logListenService: LogListenService,
        tokenRegistrationService: TokenRegistrationService,
        ethereum: MonoEthereum,
        nftListenerProperties: NftListenerProperties,
    ) : ReindexTokenTaskHandler {
        return ReindexTokenTaskHandler(
            logListenService = logListenService,
            tokenRegistrationService = tokenRegistrationService,
            ethereum = ethereum,
            nftListenerProperties = nftListenerProperties,
        )
    }
}

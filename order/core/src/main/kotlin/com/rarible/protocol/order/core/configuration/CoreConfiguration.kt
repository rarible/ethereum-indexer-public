package com.rarible.protocol.order.core.configuration

import com.rarible.core.reduce.blockchain.BlockchainSnapshotStrategy
import com.rarible.core.reduce.service.ReduceService
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.StringToAddressConverter
import com.rarible.ethereum.converters.StringToBinaryConverter
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.order.core.converters.ConvertersPackage
import com.rarible.protocol.order.core.event.EventPackage
import com.rarible.protocol.order.core.metric.MetricsPackage
import com.rarible.protocol.order.core.producer.ProducerPackage
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionSnapshotRepository
import com.rarible.protocol.order.core.service.Package
import com.rarible.protocol.order.core.service.auction.AuctionReduceService
import com.rarible.protocol.order.core.service.auction.AuctionReducer
import com.rarible.protocol.order.core.service.auction.AuctionUpdateService
import com.rarible.protocol.order.core.trace.TracePackage
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import scalether.transaction.MonoTransactionSender

@Configuration
@EnableContractService
@ComponentScan(basePackageClasses = [
    Package::class,
    ConvertersPackage::class,
    EventPackage::class,
    TracePackage::class,
    ProducerPackage::class,
    MetricsPackage::class]
)
@Import(
    RepositoryConfiguration::class,
    ProducerConfiguration::class,
    ApiClientConfiguration::class,
    OrderIndexerPropertiesConfiguration::class,
    MetricsConfiguration::class
)
class CoreConfiguration {

    @Bean
    @ConfigurationPropertiesBinding
    fun stringToAddressConverter() = StringToAddressConverter()

    @Bean
    @ConfigurationPropertiesBinding
    fun stringToBinaryConverter() = StringToBinaryConverter()

    @Bean
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    fun erc1271SignService(sender: MonoTransactionSender): ERC1271SignService {
        return ERC1271SignService(sender)
    }

    @Bean
    fun auctionReduceService(
        balanceReducer: AuctionReducer,
        eventRepository: AuctionHistoryRepository,
        snapshotRepository: AuctionSnapshotRepository,
        auctionUpdateService: AuctionUpdateService,
        properties: OrderIndexerProperties
    ): AuctionReduceService {
        return ReduceService(
            reducer = balanceReducer,
            eventRepository = eventRepository,
            snapshotRepository = snapshotRepository,
            updateService = auctionUpdateService,
            snapshotStrategy = BlockchainSnapshotStrategy(properties.blockCountBeforeSnapshot)
        )
    }
}

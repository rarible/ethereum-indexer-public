package com.rarible.protocol.nft.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.safeSplit
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.meta.resource.http.DefaultHttpClient
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.http.OpenseaHttpClient
import com.rarible.core.meta.resource.http.ProxyHttpClient
import com.rarible.core.meta.resource.http.builder.DefaultWebClientBuilder
import com.rarible.core.meta.resource.http.builder.ProxyWebClientBuilder
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.ConstantGatewayProvider
import com.rarible.core.meta.resource.resolver.GatewayProvider
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.RandomGatewayProvider
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.nft.core.converters.ConvertersPackage
import com.rarible.protocol.nft.core.event.EventListenerPackage
import com.rarible.protocol.nft.core.model.CollectionEventType
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.HistoryTopics
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.Package
import com.rarible.protocol.nft.core.service.item.meta.ipfs.EthereumCustomIpfsGatewayResolver
import com.rarible.protocol.nft.core.service.item.meta.ipfs.LazyItemIpfsGatewayResolver
import net.logstash.logback.util.StringUtils
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.http.HttpHeaders

@Configuration
@EnableConfigurationProperties(NftIndexerProperties::class)
@Import(RepositoryConfiguration::class, ProducerConfiguration::class, MetricsCountersConfiguration::class)
@ComponentScan(
    basePackageClasses = [
        Package::class,
        ConvertersPackage::class,
        EventListenerPackage::class
    ]
)
class CoreConfiguration(
    private val properties: NftIndexerProperties,
    private val application: ApplicationEnvironmentInfo
) {

    @Bean
    fun featureFlags(): FeatureFlags {
        return properties.featureFlags
    }

    @Bean
    fun reduceProperties(): NftIndexerProperties.ReduceProperties {
        return properties.reduce
    }

    @Bean
    fun scannerProperties(): NftIndexerProperties.ScannerProperties {
        return properties.scannerProperties
    }

    @Bean
    fun ipfsProperties(): NftIndexerProperties.IpfsProperties {
        return properties.ipfs
    }

    @Bean
    fun scamByteCodeProperties(): NftIndexerProperties.ScamByteCodeProperties {
        return properties.scamByteCodes
    }

    @Bean
    fun collectionProperties(): NftIndexerProperties.CollectionProperties {
        return properties.collection
    }

    @Bean
    fun historyTopics(): HistoryTopics {
        val nftItemHistoryTopics = (ItemType.TRANSFER.topic + ItemType.ROYALTY.topic + ItemType.CREATORS.topic)
            .associateWith { NftItemHistoryRepository.COLLECTION }

        val nftHistoryTopics = CollectionEventType.values().flatMap { it.topic }
            .associateWith { NftHistoryRepository.COLLECTION }

        return HistoryTopics(nftItemHistoryTopics + nftHistoryTopics)
    }

    @Bean
    fun logEventService(mongo: ReactiveMongoOperations, historyTopics: HistoryTopics): LogEventService {
        return LogEventService(historyTopics, mongo)
    }

    @Bean
    fun contractAddresses(): NftIndexerProperties.ContractAddresses {
        return properties.contractAddresses
    }

    @Bean
    fun urlParser() = UrlParser()

    @Bean
    fun internalGatewayProvider(): GatewayProvider {
        return RandomGatewayProvider(
            properties.ipfs.ipfsGateway.safeSplit().map { it.trimEnd('/') }
        )
    }

    @Bean
    fun urlResolver(
        internalGatewayProvider: GatewayProvider,
        ipfs: NftIndexerProperties.IpfsProperties
    ): UrlResolver {
        val publicGatewayProvider = ConstantGatewayProvider(
            ipfs.ipfsPublicGateway.trimEnd('/')
        )

        val lazyGateway = StringUtils.trimToNull(ipfs.ipfsLazyGateway)
        val customGatewaysResolver = EthereumCustomIpfsGatewayResolver(listOfNotNull(
            lazyGateway?.let { LazyItemIpfsGatewayResolver(it) }
        ))

        return UrlResolver(
            ipfsGatewayResolver = IpfsGatewayResolver(
                publicGatewayProvider = publicGatewayProvider,
                internalGatewayProvider = internalGatewayProvider,
                customGatewaysResolver = customGatewaysResolver
            )
        )
    }

    @Bean
    fun externalHttpClient(
        properties: NftIndexerProperties,
    ): ExternalHttpClient {
        val defaultHeaders = HttpHeaders()
        defaultHeaders.set(HttpHeaders.USER_AGENT, "rarible-protocol")

        val defaultWebClientBuilder = DefaultWebClientBuilder(
            followRedirect = properties.followRedirect,
            defaultHeaders = defaultHeaders
        )
        val proxyWebClientBuilder = ProxyWebClientBuilder(
            readTimeout = properties.opensea.readTimeout,
            connectTimeout = properties.opensea.connectTimeout,
            proxyUrl = properties.proxyUrl,
            followRedirect = properties.followRedirect,
            defaultHeaders = defaultHeaders
        )
        val defaultHttpClient = DefaultHttpClient(
            builder = defaultWebClientBuilder,
            requestTimeout = properties.requestTimeout
        )
        val proxyHttpClient = ProxyHttpClient(
            builder = proxyWebClientBuilder,
            requestTimeout = properties.opensea.requestTimeout
        )
        val openseaHttpClient = OpenseaHttpClient(
            builder = proxyWebClientBuilder,
            requestTimeout = properties.opensea.requestTimeout,
            openseaUrl = properties.opensea.url,
            openseaApiKey = properties.opensea.apiKey,
            proxyUrl = properties.proxyUrl,
        )
        return ExternalHttpClient(
            defaultClient = defaultHttpClient,
            proxyClient = proxyHttpClient,
            customClients = listOf(openseaHttpClient)
        )
    }

    @Bean
    fun raribleKafkaConsumerFactory() = RaribleKafkaConsumerFactory(
        env = application.name,
        host = application.host
    )
}

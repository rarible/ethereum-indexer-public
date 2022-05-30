package com.rarible.protocol.nft.core.configuration

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.ConstantGatewayProvider
import com.rarible.core.meta.resource.GatewayProvider
import com.rarible.core.meta.resource.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.RandomGatewayProvider
import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.detector.embedded.DefaultEmbeddedContentDecoderProvider
import com.rarible.core.meta.resource.detector.embedded.EmbeddedBase64Decoder
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetectProcessor
import com.rarible.core.meta.resource.detector.embedded.EmbeddedSvgDecoder
import com.rarible.core.meta.resource.parser.ArweaveUrlResourceParser
import com.rarible.core.meta.resource.parser.CidUrlResourceParser
import com.rarible.core.meta.resource.parser.DefaultUrlResourceParserProvider
import com.rarible.core.meta.resource.parser.HttpUrlResourceParser
import com.rarible.core.meta.resource.parser.UrlResourceParsingProcessor
import com.rarible.core.meta.resource.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.meta.resource.parser.ipfs.ForeignIpfsUrlResourceParser
import com.rarible.core.meta.resource.resolver.ArweaveGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsCidGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.SimpleHttpGatewayResolver
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.configuration.EnableRaribleCacheLoader
import com.rarible.protocol.nft.core.converters.ConvertersPackage
import com.rarible.protocol.nft.core.event.EventListenerPackage
import com.rarible.protocol.nft.core.model.CollectionEventType
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.HistoryTopics
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.Package
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaCacheLoader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@Configuration
@EnableRaribleCacheLoader
@EnableConfigurationProperties(NftIndexerProperties::class)
@Import(RepositoryConfiguration::class, ProducerConfiguration::class, MetricsCountersConfiguration::class)
@ComponentScan(basePackageClasses = [
    Package::class,
    ConvertersPackage::class,
    EventListenerPackage::class
])
class CoreConfiguration(
    private val properties: NftIndexerProperties
) {

    @Bean
    fun featureFlags(): FeatureFlags {
        return properties.featureFlags
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
    @Qualifier("meta.cache.loader.service")
    fun metaCacheLoaderService(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderService<ItemMeta> =
        @Suppress(
            "UNCHECKED_CAST"
        ) (cacheLoaderServices.find { it.type == ItemMetaCacheLoader.TYPE } as CacheLoaderService<ItemMeta>)

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
    fun urlResourceProcessor(): UrlResourceParsingProcessor {
        val cidOneValidator = CidV1Validator()
        val foreignIpfsUrlResourceParser = ForeignIpfsUrlResourceParser(
            cidOneValidator = cidOneValidator
        )

        val defaultUrlResourceParserProvider = DefaultUrlResourceParserProvider(
            arweaveUrlParser = ArweaveUrlResourceParser(),
            foreignIpfsUrlResourceParser = foreignIpfsUrlResourceParser,
            abstractIpfsUrlResourceParser = AbstractIpfsUrlResourceParser(),
            cidUrlResourceParser = CidUrlResourceParser(cidOneValidator),
            httpUrlParser = HttpUrlResourceParser()
        )

        return UrlResourceParsingProcessor(
            provider = defaultUrlResourceParserProvider
        )
    }

    @Bean
    fun publicGatewayProvider() : GatewayProvider {
        return ConstantGatewayProvider(
            properties.ipfs.ipfsPublicGateway.trimEnd('/')
        )
    }

    @Bean
    fun innerGatewaysProvider() : GatewayProvider {
        return RandomGatewayProvider(
            properties.ipfs.ipfsGateway.split(",").map { it.trimEnd('/') }
        )
    }

    @Bean
    fun urlResolver(
        publicGatewayProvider: GatewayProvider,
        innerGatewaysProvider: GatewayProvider
    ): UrlResolver {
        val customGatewaysResolver = LegacyIpfsGatewaySubstitutor(emptyList()) // For handle Legacy gateways
        val arweaveGatewayProvider = ConstantGatewayProvider(ArweaveUrl.ARWEAVE_GATEWAY)     // TODO Move to properties

        val ipfsGatewayResolver = IpfsGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            innerGatewaysProvider = innerGatewaysProvider,
            customGatewaysResolver = customGatewaysResolver
        )

        val ipfsCidGatewayResolver = IpfsCidGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            innerGatewaysProvider = innerGatewaysProvider,
        )

        val arweaveGatewayResolver = ArweaveGatewayResolver(
            arweaveGatewayProvider = arweaveGatewayProvider
        )

        return UrlResolver(
            ipfsGatewayResolver = ipfsGatewayResolver,
            ipfsCidGatewayResolver = ipfsCidGatewayResolver,
            arweaveGatewayResolver = arweaveGatewayResolver,
            simpleHttpGatewayResolver = SimpleHttpGatewayResolver()
        )
    }

    @Bean
    fun embeddedContentDetectProcessor() : EmbeddedContentDetectProcessor =
        EmbeddedContentDetectProcessor(
            provider = DefaultEmbeddedContentDecoderProvider(
                embeddedBase64Decoder = EmbeddedBase64Decoder,
                embeddedSvgDecoder = EmbeddedSvgDecoder
            )
        )
}

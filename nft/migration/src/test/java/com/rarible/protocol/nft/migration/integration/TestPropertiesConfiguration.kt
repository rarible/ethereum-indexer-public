package com.rarible.protocol.nft.migration.integration

import com.rarible.core.lock.LockService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.service.CryptoPunksMetaService
import com.rarible.protocol.nft.core.service.item.meta.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.*
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import scalether.transaction.ReadOnlyMonoTransactionSender


@TestConfiguration
class TestPropertiesConfiguration {

    @Autowired
    protected lateinit var cryptoPunksMetaService: CryptoPunksMetaService

    @Autowired
    protected lateinit var properties: NftIndexerProperties

    private val tokenRepository = mockk<TokenRepository>()
    private val lazyNftItemHistoryRepository = mockk<LazyNftItemHistoryRepository>()
    private val temporaryItemPropertiesRepository = mockk<TemporaryItemPropertiesRepository>()
    private val sender = ReadOnlyMonoTransactionSender(MonoEthereum(WebClientTransport("https://dark-solitary-resonance.quiknode.pro/c0b7c629520de6c3d39261f6417084d71c3f8791/", MonoEthereum.mapper(), 10000, 10000)), Address.ZERO())
    private val ipfsService = IpfsService(IpfsService.IPFS_NEW_URL, mockk())
    private val propertiesCacheDescriptor = PropertiesCacheDescriptor(sender, tokenRepository, lazyNftItemHistoryRepository, ipfsService, 86400, 20000)
    private val kittiesCacheDescriptor = KittiesCacheDescriptor(86400)
    private val yInsureCacheDescriptor = YInsureCacheDescriptor(sender, "0x181aea6936b407514ebfc0754a37704eb8d98f91", 86400, "0x1776651F58a17a50098d31ba3C3cD259C1903f7A", "http://localhost:8080")
    private val hegicCacheDescriptor = HegicCacheDescriptor(sender, "0xcb9ebae59738d9dadc423adbde66c018777455a4", 86400, "http://localhost:8080")
    private val hashmasksCacheDescriptor = HashmasksCacheDescriptor(sender, "0xc2c747e0f7004f9e8817db2ca4997657a7746928", 86400)
    private val waifusionCacheDescriptor = WaifusionCacheDescriptor(sender, "0x2216d47494e516d8206b70fca8585820ed3c4946", 86400, "https://ipfs.io/ipfs/QmQuzMGqHxSXugCUvWQjDCDHWhiGB75usKbrZk6Ec6pFvw")

    @Bean
    fun itemPropService(): ItemPropertiesService {
        return ItemPropertiesService(
            propertiesCacheDescriptor,
            kittiesCacheDescriptor,
            mockk(),
            yInsureCacheDescriptor,
            hegicCacheDescriptor,
            hashmasksCacheDescriptor,
            waifusionCacheDescriptor,
            cryptoPunksMetaService,
            OpenSeaCacheDescriptor("https://api.opensea.io/api/v1", "", 10000, 3000, 86400, 20000, "", null),
            ipfsService,
            temporaryItemPropertiesRepository,
            properties,
            "0x181aea6936b407514ebfc0754a37704eb8d98f91",
            "0xcb9ebae59738d9dadc423adbde66c018777455a4",
            "0xc2c747e0f7004f9e8817db2ca4997657a7746928",
            "0x2216d47494e516d8206b70fca8585820ed3c4946",
            null
        )
    }

    @Bean
    fun transactionSender() : MonoTransactionSender {
        return mockk()
    }

    @Bean
    fun lockService(): LockService {
        return mockk()
    }

    @Bean
    fun propertyCacheDescriptor(): PropertiesCacheDescriptor {
        return propertiesCacheDescriptor
    }
}

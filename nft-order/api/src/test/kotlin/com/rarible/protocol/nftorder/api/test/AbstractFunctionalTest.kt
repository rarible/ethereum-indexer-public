package com.rarible.protocol.nftorder.api.test

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.api.test.mock.*
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import io.mockk.clearMocks
import io.mockk.coEvery
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestTemplate
import java.net.URI

abstract class AbstractFunctionalTest {

    @Autowired
    @Qualifier("testLocalhostUri")
    protected lateinit var baseUri: URI

    @Autowired
    protected lateinit var testRestTemplate: RestTemplate

    @Autowired
    private lateinit var nftItemControllerApi: NftItemControllerApi

    @Autowired
    private lateinit var nftOwnershipControllerApi: NftOwnershipControllerApi

    @Autowired
    private lateinit var nftCollectionControllerApi: NftCollectionControllerApi

    @Autowired
    private lateinit var nftLazyMintControllerApi: NftLazyMintControllerApi

    @Autowired
    private lateinit var orderControllerApi: OrderControllerApi

    @Autowired
    private lateinit var lockControllerApi: LockControllerApi

    @Autowired
    protected lateinit var testItemEventProducer: RaribleKafkaProducer<NftOrderItemEventDto>

    @Autowired
    protected lateinit var testOwnershipEventProducer: RaribleKafkaProducer<NftOrderOwnershipEventDto>

    lateinit var nftItemControllerApiMock: NftItemControllerApiMock
    lateinit var nftOwnershipControllerApiMock: NftOwnershipControllerApiMock
    lateinit var nftCollectionControllerApiMock: NftCollectionControllerApiMock
    lateinit var nftLazyMintControllerApiMock: NftLazyMintControllerApiMock
    lateinit var orderControllerApiMock: OrderControllerApiMock
    lateinit var lockControllerApiMock: LockControllerApiMock

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            nftItemControllerApi,
            nftOwnershipControllerApi,
            nftCollectionControllerApi,
            nftLazyMintControllerApi,
            orderControllerApi,
            lockControllerApi,
            testItemEventProducer,
            testOwnershipEventProducer
        )
        nftItemControllerApiMock = NftItemControllerApiMock(nftItemControllerApi)
        nftOwnershipControllerApiMock = NftOwnershipControllerApiMock(nftOwnershipControllerApi)
        nftCollectionControllerApiMock = NftCollectionControllerApiMock(nftCollectionControllerApi)
        nftLazyMintControllerApiMock = NftLazyMintControllerApiMock(nftLazyMintControllerApi)
        orderControllerApiMock = OrderControllerApiMock(orderControllerApi)
        lockControllerApiMock = LockControllerApiMock(lockControllerApi)

        coEvery {
            testItemEventProducer.send(any() as KafkaMessage<NftOrderItemEventDto>)
        } returns KafkaSendResult.Success("")

        coEvery {
            testOwnershipEventProducer.send(any() as KafkaMessage<NftOrderOwnershipEventDto>)
        } returns KafkaSendResult.Success("")

    }
}

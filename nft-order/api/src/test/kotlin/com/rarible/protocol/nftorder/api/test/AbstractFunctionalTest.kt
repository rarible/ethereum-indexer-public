package com.rarible.protocol.nftorder.api.test

import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.api.test.mock.*
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import io.mockk.clearMocks
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractFunctionalTest {

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
            lockControllerApi
        )
        nftItemControllerApiMock = NftItemControllerApiMock(nftItemControllerApi)
        nftOwnershipControllerApiMock = NftOwnershipControllerApiMock(nftOwnershipControllerApi)
        nftCollectionControllerApiMock = NftCollectionControllerApiMock(nftCollectionControllerApi)
        nftLazyMintControllerApiMock = NftLazyMintControllerApiMock(nftLazyMintControllerApi)
        orderControllerApiMock = OrderControllerApiMock(orderControllerApi)
        lockControllerApiMock = LockControllerApiMock(lockControllerApi)

    }
}

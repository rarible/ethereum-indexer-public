package com.rarible.protocol.nftorder.listener.test

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.listener.test.mock.LockControllerApiMock
import com.rarible.protocol.nftorder.listener.test.mock.NftItemControllerApiMock
import com.rarible.protocol.nftorder.listener.test.mock.NftOwnershipControllerApiMock
import com.rarible.protocol.nftorder.listener.test.mock.OrderControllerApiMock
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import io.mockk.clearMocks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var nftItemControllerApi: NftItemControllerApi

    @Autowired
    protected lateinit var nftOwnershipControllerApi: NftOwnershipControllerApi

    @Autowired
    protected lateinit var orderControllerApi: OrderControllerApi

    @Autowired
    protected lateinit var lockControllerApi: LockControllerApi

    @Autowired
    lateinit var itemConsumer: RaribleKafkaConsumer<NftOrderItemEventDto>
    var itemEvents: MutableList<KafkaMessage<NftOrderItemEventDto>>? = null
    private var itemJob: Deferred<Unit>? = null

    @Autowired
    lateinit var ownershipConsumer: RaribleKafkaConsumer<NftOrderOwnershipEventDto>
    var ownershipEvents: MutableList<KafkaMessage<NftOrderOwnershipEventDto>>? = null
    private var ownershipJob: Deferred<Unit>? = null

    lateinit var nftItemControllerApiMock: NftItemControllerApiMock
    lateinit var nftOwnershipControllerApiMock: NftOwnershipControllerApiMock
    lateinit var orderControllerApiMock: OrderControllerApiMock
    lateinit var lockControllerApiMock: LockControllerApiMock

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            nftItemControllerApi,
            nftOwnershipControllerApi,
            orderControllerApi,
            lockControllerApi
        )
        orderControllerApiMock = OrderControllerApiMock(orderControllerApi)
        nftItemControllerApiMock = NftItemControllerApiMock(nftItemControllerApi)
        nftOwnershipControllerApiMock = NftOwnershipControllerApiMock(nftOwnershipControllerApi)
        lockControllerApiMock = LockControllerApiMock(lockControllerApi)
    }

    fun <T> runWithKafka(block: suspend CoroutineScope.() -> T): T =
        runBlocking<T> {
            ownershipEvents = Collections.synchronizedList(ArrayList<KafkaMessage<NftOrderOwnershipEventDto>>())
            ownershipJob = async { ownershipConsumer.receive().collect { ownershipEvents?.add(it) } }

            itemEvents = Collections.synchronizedList(ArrayList<KafkaMessage<NftOrderItemEventDto>>())
            itemJob = async { itemConsumer.receive().collect { itemEvents?.add(it) } }

            val result = block()
            ownershipJob?.cancel()
            itemJob?.cancel()
            result
        }

}
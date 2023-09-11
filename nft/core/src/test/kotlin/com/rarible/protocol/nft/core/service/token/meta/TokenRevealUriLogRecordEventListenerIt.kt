package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.common.EventTimeMarks
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.parser.ItemIdParser
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.model.TokenUriReveal
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import com.rarible.protocol.nft.core.test.TestLogRecordEventPublisher
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

@IntegrationTest
class TokenRevealUriLogRecordEventListenerIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var publisher: TestLogRecordEventPublisher

    @Test
    fun name() = runBlocking {
        val contract = Address.ONE()

        publisher.publish(
            groupId = SubscriberGroups.TOKEN_REVEAL,
            logRecordEvents = listOf(
                LogRecordEvent(
                    record = ReversedEthereumLogRecord(
                        id = ObjectId().toHexString(),
                        data = TokenUriReveal(
                            contract = contract,
                            tokenIdFrom = BigInteger.ZERO,
                            tokenIdTo = BigInteger.TEN,
                        ),
                        address = contract,
                        topic = Word.apply(randomWord()),
                        transactionHash = randomWord(),
                        status = EthereumBlockStatus.CONFIRMED,
                        blockNumber = 1,
                        logIndex = 0,
                        minorLogIndex = 0,
                        index = 0
                    ),
                    reverted = false,
                    eventTimeMarks = EventTimeMarks(source = "test")
                )
            )
        )

        Wait.waitAssert {
            val events = itemMetaEventHandler.events.toList()
            assertThat(events).hasSize(11)
            val itemsByContract = events.map { ItemIdParser.parse(it.itemId) }
                .groupBy({ it.token }, { BigDecimal(it.tokenId) })
            assertThat(itemsByContract).hasSize(1).containsKey(contract)
            val tokenIds = itemsByContract[contract]!!.sorted()
            assertThat(tokenIds.first()).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(tokenIds.last()).isEqualByComparingTo(BigDecimal.TEN)
        }
    }
}

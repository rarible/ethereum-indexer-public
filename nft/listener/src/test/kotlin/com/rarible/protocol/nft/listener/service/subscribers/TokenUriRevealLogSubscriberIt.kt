package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftItemMetaRefreshEventDto
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.test.TestKafkaHandler
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@IntegrationTest
internal class TokenUriRevealLogSubscriberIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var testItemMetaHandler: TestKafkaHandler<NftItemMetaEventDto>

    @Test
    fun `parse reveal`() = runBlocking<Unit> {
        val (_, sender) = newSender()

        val token = TestERC721.deployAndWait(sender, poller, "TEST", "TST").awaitFirst()

        testItemMetaHandler.events.clear()
        token.reveal(BigInteger.ZERO).withSender(sender).execute().verifySuccess()

        for (i in 0..9) {
            val event = testItemMetaHandler.events.poll(10, TimeUnit.SECONDS)
            assertThat(event).isInstanceOf(NftItemMetaRefreshEventDto::class.java)
            assertThat(event.itemId).isEqualTo(ItemId(token.address(), i.toBigInteger()).toString())
        }
        assertThat(testItemMetaHandler.events).isEmpty()
    }
}

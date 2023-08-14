package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionSetBaseUriEventDto
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.test.TestKafkaHandler
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

@IntegrationTest
internal class SetBaseUriTransactionRecordEventSubscriberIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var testCollectionHandler: TestKafkaHandler<NftCollectionEventDto>

    @Test
    fun setBaseUri() = runBlocking<Unit> {
        val (_, sender) = newSender()

        val token = TestERC721.deployAndWait(sender, poller, "TEST", "TST").awaitFirst()
        tokenRepository.save(
            Token(
                id = token.address(),
                name = "TEST",
                standard = TokenStandard.ERC721
            )
        ).awaitSingle()
        testCollectionHandler.events.clear()

        token.setBaseURI("ipfs://QmVAgjZpTLyebeQrrBTL6d8QD1geATxc1Vha296Hu7aUnP/").withSender(sender).execute()
            .verifySuccess()

        val event = testCollectionHandler.events.poll(10, TimeUnit.SECONDS)
        assertThat(event).isInstanceOf(NftCollectionSetBaseUriEventDto::class.java)
        assertThat(event!!.id).isEqualTo(token.address())
    }
}

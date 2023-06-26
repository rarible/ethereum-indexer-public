package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@IntegrationTest
internal class SetBaseUriTransactionRecordEventSubscriberIt : AbstractIntegrationTest() {

    @Disabled
    @Test
    fun setBaseUri() = runBlocking<Unit> {
        val (_, sender, _) = newSender()

        val token = TestERC721.deployAndWait(sender, poller, "TEST", "TST").awaitFirst()
//        token.setBaseURI("ipfs://QmVAgjZpTLyebeQrrBTL6d8QD1geATxc1Vha296Hu7aUnP/").withSender(sender).execute()
//            .verifySuccess()

//        checkCollectionSetBaseUriWasPublished(token.address())
    }
}
package com.rarible.protocol.nft.migration.mongock.mongo

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.erc721.rarible.user.CreateERC721RaribleUserEvent
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@IntegrationTest
class ChangeLog00018SetRaribleContractFieldTest : AbstractIntegrationTest() {

    private val changeLog = ChangeLog00018SetRaribleContractField()

    @Test
    fun setRaribleContractField() = runBlocking {
        val collectionId = randomAddress()
        tokenRepository.save(
            Token(
                id = collectionId,
                owner = randomAddress(),
                name = "Name",
                symbol = "Symbol",
                standard = TokenStandard.ERC721
            )
        ).awaitFirst()

        nftHistoryRepository.save(
            LogEvent(
                data = CreateCollection(
                    id = collectionId,
                    owner = randomAddress(),
                    name = "Name",
                    symbol = "Symbol"
                ),
                address = collectionId,
                topic = CreateERC721RaribleUserEvent.id(),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                index = 0,
                minorLogIndex = 0
            )
        ).awaitFirst()

        assertFalse(tokenRepository.findById(collectionId).awaitFirst().isRaribleContract)

        changeLog.setRaribleContractField(nftHistoryRepository, tokenRepository)

        assertTrue(tokenRepository.findById(collectionId).awaitFirst().isRaribleContract)
    }
}
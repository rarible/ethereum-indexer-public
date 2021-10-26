package com.rarible.protocol.nft.migration.mongock.mongo

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.erc721.rarible.user.CreateERC721RaribleUserEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class ChangeLog00019UpdateCollectionOwnersTest : AbstractIntegrationTest() {

    private val changeLog = ChangeLog00019UpdateCollectionOwners()

    @Test
    fun `update collection owners`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        val oldOwner = randomAddress()
        val newOwner = randomAddress()
        tokenRepository.save(
            Token(
                id = collectionId,
                owner = oldOwner,
                name = "Name",
                symbol = "Symbol",
                standard = TokenStandard.ERC721
            )
        ).awaitFirst()

        nftHistoryRepository.save(
            LogEvent(
                data = CollectionOwnershipTransferred(
                    id = collectionId,
                    previousOwner = oldOwner,
                    newOwner = newOwner
                ),
                address = collectionId,
                topic = CreateERC721RaribleUserEvent.id(),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                index = 0,
                minorLogIndex = 0
            )
        ).awaitFirst()

        assertThat(tokenRepository.findById(collectionId).awaitFirst().owner).isEqualTo(oldOwner)

        changeLog.updateCollectionOwners(nftHistoryRepository, tokenRepository)

        assertThat(tokenRepository.findById(collectionId).awaitFirst().owner).isEqualTo(newOwner)
    }
}
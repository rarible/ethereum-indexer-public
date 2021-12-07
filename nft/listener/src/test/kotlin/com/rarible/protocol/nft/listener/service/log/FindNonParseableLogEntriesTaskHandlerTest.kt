package com.rarible.protocol.nft.listener.service.log

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.listener.admin.FindNonParseableLogEntriesTaskHandler
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo

/**
 * Test for [FindNonParseableLogEntriesTaskHandler].
 */
@FlowPreview
@IntegrationTest
class FindNonParseableLogEntriesTaskHandlerTest : AbstractIntegrationTest() {

    private val collectionName = NftItemHistoryRepository.COLLECTION

    @Test
    fun findNonParseableLogEntries() {
        runBlocking<Unit> {
            // Good entries.
            save(
                ItemTransfer(
                    owner = randomAddress(),
                    token = randomAddress(),
                    tokenId = EthUInt256.of(randomInt()),
                    date = nowMillis(),
                    from = randomAddress(),
                    value = EthUInt256.of(randomInt())
                ),
                ItemRoyalty(
                    token = randomAddress(),
                    tokenId = EthUInt256.of(randomInt()),
                    date = nowMillis(),
                    royalties = listOf(Part(randomAddress(), randomInt()))
                ),
                ItemCreators(
                    token = randomAddress(),
                    tokenId = EthUInt256.of(randomInt()),
                    date = nowMillis(),
                    creators = listOf(Part(randomAddress(), randomInt()))
                )
            )

            val badIds = listOf(
                run {
                    // Royalty with too big value (out of int range) (as in RPN-1316).
                    val royaltyId = save(
                        ItemRoyalty(
                            token = randomAddress(),
                            tokenId = EthUInt256.of(randomInt()),
                            date = nowMillis(),
                            royalties = listOf(Part(randomAddress(), randomInt()))
                        )
                    ).single()
                    mongo.updateFirst(
                        Query(LogEvent::id isEqualTo royaltyId),
                        Update().set("data.royalties.0.value", Long.MAX_VALUE),
                        LogEvent::class.java,
                        collectionName
                    ).awaitFirst()
                    royaltyId
                },
                run {
                    // Just some random doc
                    val id = ObjectId()
                    mongo.getCollection(collectionName).awaitFirst().insertOne(
                        Document(
                            mapOf(
                                "_id" to id,
                                "randomField" to randomString()
                            )
                        )
                    ).awaitFirst()
                    id
                },
                run {
                    val wrongClassId = save(
                        ItemTransfer(
                            owner = randomAddress(),
                            token = randomAddress(),
                            tokenId = EthUInt256.of(randomInt()),
                            date = nowMillis(),
                            from = randomAddress(),
                            value = EthUInt256.of(randomInt())
                        )
                    ).single()
                    mongo.updateFirst(
                        Query(LogEvent::id isEqualTo wrongClassId),
                        Update().set("data.type", "suchClassDoesNotExist"),
                        LogEvent::class.java,
                        collectionName
                    ).awaitFirst()
                    wrongClassId
                }
            )

            val nonParseableIds = mutableSetOf<ObjectId>()
            val callback = object : FindNonParseableLogEntriesTaskHandler.OnNonParseableEntryCallback {
                override fun onEntryFound(objectId: ObjectId) {
                    nonParseableIds += objectId
                }
            }
            val findNonParseableLogEntriesTaskHandler = FindNonParseableLogEntriesTaskHandler(mongo, mongoConverter)
            findNonParseableLogEntriesTaskHandler
                .findNonParseableLogEntries(
                    null,
                    callback,
                    ItemHistory::class.java,
                    collectionName
                ).collect {  }
            assertThat(nonParseableIds).isEqualTo(badIds.toSet())
        }
    }

    private suspend fun save(vararg eventDataList: EventData): List<ObjectId> {
        return eventDataList.map { eventData ->
            nftItemHistoryRepository.save(logEvent(eventData)).awaitFirst()
        }.map { it.id }
    }

    private fun logEvent(eventData: EventData) = LogEvent(
        data = eventData,
        address = randomAddress(),
        topic = Word.apply(randomWord()),
        transactionHash = Word.apply(randomWord()),
        status = LogEventStatus.CONFIRMED,
        index = randomInt(),
        minorLogIndex = randomInt(),
        blockNumber = randomLong(),
        blockHash = Word.apply(randomWord())
    )
}

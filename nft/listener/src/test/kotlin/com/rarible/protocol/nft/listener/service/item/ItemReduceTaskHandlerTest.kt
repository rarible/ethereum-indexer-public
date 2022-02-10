package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.ScannerVersion
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
@FlowPreview
internal class ItemReduceTaskHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var itemReduceTaskHandler: ItemReduceTaskHandler

    @BeforeEach
    fun setup() = runBlocking<Unit> {
        nftItemHistoryRepository.createIndexes()
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should make reduce all items`(version: ReduceVersion) = withReducer(version) {
        when (featureFlags.scannerVersion) {
            ScannerVersion.V2 -> {
                assertThat(itemReduceTaskHandler.isAbleToRun("")).isTrue()
            }
            ScannerVersion.V1 -> {

            }
        }
        val log1 = createMintLog(blockNumber = 1)
        val mint1 = log1.data as ItemTransfer

        val log2 = createMintLog(blockNumber = 2)
        val mint2 = log2.data as ItemTransfer
        listOf(log1, log2).forEach { nftItemHistoryRepository.save(it).awaitFirst() }

        Wait.waitAssert {
            itemReduceTaskHandler.runLongTask(null, "").toList()

            val item1 = itemRepository.findById(ItemId(mint1.token, mint1.tokenId)).awaitFirstOrNull()
            assertThat(item1).isNotNull

            val item2 = itemRepository.findById(ItemId(mint2.token, mint2.tokenId)).awaitFirstOrNull()
            assertThat(item2).isNotNull
        }
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should make reduce target token`(version: ReduceVersion) = withReducer(version) {
        when (featureFlags.scannerVersion) {
            ScannerVersion.V2 -> {
                assertThat(itemReduceTaskHandler.isAbleToRun("")).isTrue()
            }
            ScannerVersion.V1 -> {

            }
        }
        val token = Address.THREE()
        val log1 = createMintLog(token, blockNumber = 1)
        val mint1 = log1.data as ItemTransfer

        // This address following after first token, so it should NOT be handled in our test
        val nextToken = Address.FOUR()
        val log2 = createMintLog(nextToken, blockNumber = 2)
        val mint2 = log2.data as ItemTransfer

        val log3 = createMintLog(token, blockNumber = 3)
        val mint3 = log3.data as ItemTransfer

        listOf(log1, log2, log3).forEach { nftItemHistoryRepository.save(it).awaitFirst() }

        Wait.waitAssert {
            itemReduceTaskHandler.runLongTask(null, nextToken.hex()).toList()

            val item1 = itemRepository.findById(ItemId(mint1.token, mint1.tokenId)).awaitFirstOrNull()
            assertThat(item1).isNotNull

            val item2 = itemRepository.findById(ItemId(mint2.token, mint2.tokenId)).awaitFirstOrNull()
            assertThat(item2).isNull()

            val item3 = itemRepository.findById(ItemId(mint3.token, mint3.tokenId)).awaitFirstOrNull()
            assertThat(item3).isNotNull
        }
    }

    private fun createMintLog(
        token: Address = randomAddress(),
        blockNumber: Long = 1
    ): LogEvent {
        val transfer = ItemTransfer(
            owner = randomAddress(),
            token = token,
            tokenId = EthUInt256.of(randomBigInt()),
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        return LogEvent(
            data = transfer,
            address = token,
            topic = WordFactory.create(),
            transactionHash = Word.apply(randomWord()) ,
            status = LogEventStatus.CONFIRMED,
            from = randomAddress(),
            index = 0,
            logIndex = 1,
            blockNumber = blockNumber,
            minorLogIndex = 0,
            blockTimestamp = nowMillis().epochSecond
        )
    }
}
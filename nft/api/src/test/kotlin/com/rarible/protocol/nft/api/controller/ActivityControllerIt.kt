package com.rarible.protocol.nft.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByItemAndOwnerDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.api.e2e.data.createItemBurn
import com.rarible.protocol.nft.api.e2e.data.createItemLazyMint
import com.rarible.protocol.nft.api.e2e.data.createItemMint
import com.rarible.protocol.nft.api.e2e.data.createItemTransfer
import com.rarible.protocol.nft.api.e2e.data.createLogEvent
import com.rarible.protocol.nft.api.test.AbstractIntegrationTest
import com.rarible.protocol.nft.api.test.End2EndTest
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.time.Instant

@End2EndTest
class ActivityControllerIt : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var historyRepository: NftItemHistoryRepository

    @Autowired
    protected lateinit var activityController: ActivityController

    @BeforeEach
    override fun setupDatabase() = runBlocking {
        mongo.dropCollection(NftItemHistoryRepository.COLLECTION).awaitFirstOrNull()
        historyRepository.createIndexes()
    }

    @Test
    fun `activity controller test`() = runBlocking<Unit> {
        repeat(20) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            delay(1)
        }
        val result = activityController.getNftActivitiesSync(null, null, 20, SyncSortDto.DB_UPDATE_ASC)

        assertThat(result.body!!.items).hasSize(20)
        assertThat(result.body!!.items).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
    }

    @Test
    fun `activity controller test - not only transfer`() = runBlocking<Unit> {
        val amountNft = 40
        val size = 20
        repeat(amountNft / 2) {
            historyRepository.save(createItemTransfer().copy(data = createItemLazyMint())).awaitFirst()
            historyRepository.save(createItemTransfer()).awaitFirst()
        }
        val result = activityController.getNftActivitiesSync(null, null, size, SyncSortDto.DB_UPDATE_ASC)

        assertThat(result.body!!.items).hasSize(size)
        assertThat(result.body!!.items).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
    }

    @Test
    fun `activity controller pagination earlies first test`() = runBlocking<Unit> {
        repeat(95) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            delay(1)
        }

        var continuation: String? = null
        val activities = mutableListOf<ActivityDto>()
        var totalPages = 0

        do {
            val result = activityController.getNftActivitiesSync(null, continuation, 20, SyncSortDto.DB_UPDATE_ASC)
            result.body?.let { activities.addAll(it.items) }
            continuation = result.body?.continuation
            totalPages += 1
        } while (continuation != null)

        assertThat(totalPages).isEqualTo(5)
        assertThat(activities).hasSize(95)
        assertThat(activities).isSortedAccordingTo { o1, o2 -> compareValues(o1.lastUpdatedAt, o2.lastUpdatedAt) }
    }

    @Test
    fun `activity controller pagination latest first test`() = runBlocking<Unit> {
        repeat(95) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            delay(1)
        }

        var continuation: String? = null
        val activities = mutableListOf<ActivityDto>()
        var totalPages = 0

        do {
            val result = activityController.getNftActivitiesSync(false, continuation, 20, SyncSortDto.DB_UPDATE_DESC)
            result.body?.let { activities.addAll(it.items) }
            continuation = result.body?.continuation
            totalPages += 1
        } while (continuation != null)

        assertThat(totalPages).isEqualTo(5)
        assertThat(activities).hasSize(95)
        assertThat(activities).isSortedAccordingTo { o1, o2 -> compareValues(o2.lastUpdatedAt, o1.lastUpdatedAt) }
    }

    @Test
    fun `activity controller by item and owner`() = runBlocking<Unit> {
        val owner = randomAddress()
        val token = randomAddress()
        val tokenId = EthUInt256.of(randomLong())
        val date = Instant.now()

        val transfer = createItemTransfer(owner, token, tokenId).copy(date = date)
        val mint = createItemTransfer(owner, token, tokenId).copy(date = date.minusMillis(1000), from = Address.ZERO())

        listOf(transfer, mint)
            .map { createLogEvent(it) }
            .onEach { historyRepository.save(it).awaitFirst() }
        repeat(10) { historyRepository.save(createItemTransfer()).awaitFirst() }

        val filter = NftActivityFilterByItemAndOwnerDto(
            token,
            tokenId.value,
            owner,
            NftActivityFilterByItemAndOwnerDto.Types.values().asList()
        )
        val result = activityController.getNftActivities(filter, null, 10, ActivitySortDto.LATEST_FIRST)

        assertThat(result.body).isNotNull
        assertThat(result.body!!.items).hasSize(2)
        assertThat(result.body!!.items[0]).isInstanceOf(TransferDto::class.java)
        assertThat(result.body!!.items[1]).isInstanceOf(MintDto::class.java)
    }

    @Test
    fun `all activities and sync activities result should be the same`() {
        runBlocking {
            val activities = mutableListOf<LogEvent>()
            repeat(3) {
                activities.add(createItemMint())
            }
            repeat(3) {
                activities.add(createItemBurn())
            }
            repeat(14) {
                activities.add(createItemTransfer())
            }
            repeat(5) {
                activities.add(createItemTransfer().copy(status = LogEventStatus.REVERTED))
            }

            activities.forEach {
                historyRepository.save(it).awaitFirstOrNull()
            }

            val all = activityController.getNftActivities(
                NftActivityFilterAllDto(
                    NftActivityFilterAllDto.Types.values().toList()
                ), null, null, null
            ).body!!.items.map { it.id }.sorted()

            val sync = activityController.getNftActivitiesSync(false, null, null, null)
                .body!!.items.map { it.id }.sorted()

            assertThat(all).isEqualTo(sync)
        }
    }

    @Test
    fun `sync - reverted events`() = runBlocking<Unit> {

        val confirmed = historyRepository.save(createItemTransfer()).awaitFirst()
        val reverted = historyRepository.save(createItemTransfer().copy(status = LogEventStatus.REVERTED)).awaitFirst()

        val revertedResult = activityController.getNftActivitiesSync(true, null, 20, SyncSortDto.DB_UPDATE_ASC)
        assertThat(revertedResult.body!!.items).hasSize(1)
        assertThat(revertedResult.body!!.items.first().id).isEqualTo(reverted.id.toString())

        val confirmedResult = activityController.getNftActivitiesSync(false, null, 20, SyncSortDto.DB_UPDATE_ASC)
        assertThat(confirmedResult.body!!.items).hasSize(1)
        assertThat(confirmedResult.body!!.items.first().id).isEqualTo(confirmed.id.toString())
    }

    @Test
    fun `get activities - ok, burns`() = runBlocking<Unit> {
        val deadAddress = Address.apply("0x000000000000000000000000000000000000dead")
        val zeroAddressBurn = historyRepository.save(createItemTransfer(owner = Address.ZERO())).awaitFirst()
        val deadAddressBurn = historyRepository.save(createItemTransfer(owner = deadAddress)).awaitFirst()
        historyRepository.save(createItemTransfer()).awaitFirst()

        val result = activityController.getNftActivities(
            NftActivityFilterAllDto(listOf(NftActivityFilterAllDto.Types.BURN)),
            null,
            5,
            ActivitySortDto.LATEST_FIRST
        ).body!!.items

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(
            zeroAddressBurn.id.toString(),
            deadAddressBurn.id.toString()
        )
    }
}

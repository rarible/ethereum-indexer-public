package com.rarible.protocol.nft.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftActivityFilterByItemAndOwnerDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItemTransfer
import com.rarible.protocol.nft.api.e2e.data.createLogEvent
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
class ActivityControllerIt : SpringContainerBaseTest() {

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
    fun `activity controller test`() = runBlocking<Unit>  {
        repeat(20) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            delay(1)
        }
        val result = activityController.getNftActivitiesSync(null, 20, SyncSortDto.DB_UPDATE_ASC)

        assertThat(result.body!!.items).hasSize(20)
        assertThat(result.body!!.items).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
    }

    @Test
    fun `activity controller pagination earlies first test`() = runBlocking<Unit>  {
        repeat(95) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            delay(1)
        }

        var continuation: String? = null
        val activities = mutableListOf<ActivityDto>()
        var totalPages = 0

        do{
            val result = activityController.getNftActivitiesSync(continuation, 20, SyncSortDto.DB_UPDATE_ASC)
            result.body?.let { activities.addAll(it.items) }
            continuation = result.body?.continuation
            totalPages += 1
        }while (continuation != null)

        assertThat(totalPages).isEqualTo(5)
        assertThat(activities).hasSize(95)
        assertThat(activities).isSortedAccordingTo { o1, o2 -> compareValues(o1.lastUpdatedAt , o2.lastUpdatedAt) }
    }

    @Test
    fun `activity controller pagination latest first test`() = runBlocking<Unit>  {
        repeat(95) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            delay(1)
        }

        var continuation: String? = null
        val activities = mutableListOf<ActivityDto>()
        var totalPages = 0

        do {
            val result = activityController.getNftActivitiesSync(continuation, 20, SyncSortDto.DB_UPDATE_DESC)
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
        val tokenId = EthUInt256.of(randomLong())
        val date = Instant.now()

        val transfer = createItemTransfer(owner, tokenId).copy(date = date)
        val mint = createItemTransfer(owner, tokenId).copy(date = date.minusMillis(1000), from = Address.ZERO())

        listOf(transfer, mint)
            .map { createLogEvent(it) }
            .onEach { historyRepository.save(it).awaitFirst() }
        repeat(10) { historyRepository.save(createItemTransfer()).awaitFirst() }

        val filter = NftActivityFilterByItemAndOwnerDto(tokenId.value, owner)
        val result = activityController.getNftActivities(filter, null, 10, ActivitySortDto.LATEST_FIRST)

        assertThat(result.body).isNotNull
        assertThat(result.body.items).hasSize(2)
        assertThat(result.body.items[0]).isInstanceOf(TransferDto::class.java)
        assertThat(result.body.items[1]).isInstanceOf(MintDto::class.java)
    }
}
package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.NftActivitiesSyncTypesDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItemBurn
import com.rarible.protocol.nft.api.e2e.data.createItemMint
import com.rarible.protocol.nft.api.e2e.data.createItemTransfer
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
        val result = activityController.getNftActivitiesSync(null, 20, SyncSortDto.DB_UPDATE_ASC, null)

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
            val result = activityController.getNftActivitiesSync(continuation, 20, SyncSortDto.DB_UPDATE_ASC, null)
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

        do{
            val result = activityController.getNftActivitiesSync(continuation, 20, SyncSortDto.DB_UPDATE_DESC, null)
            result.body?.let { activities.addAll(it.items) }
            continuation = result.body?.continuation
            totalPages += 1
        }while (continuation != null)

        assertThat(totalPages).isEqualTo(5)
        assertThat(activities).hasSize(95)
        assertThat(activities).isSortedAccordingTo { o1, o2 -> compareValues(o2.lastUpdatedAt , o1.lastUpdatedAt) }
    }

    @Test
    fun `activity controller sync test transfer`() = runBlocking<Unit> {
        val itemHistoryQuantity = 20
        repeat(itemHistoryQuantity) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            historyRepository.save(createItemBurn()).awaitFirst()
            historyRepository.save(createItemMint()).awaitFirst()
        }
        val result = activityController.getNftActivitiesSync(
            null,
            itemHistoryQuantity*3,
            SyncSortDto.DB_UPDATE_ASC,
            listOf(NftActivitiesSyncTypesDto.TRANSFER)
        )

        assertThat(result.body!!.items).hasSize(itemHistoryQuantity)
        assertThat(result.body!!.items).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
    }

    @Test
    fun `activity controller sync test transfer and mint`() = runBlocking<Unit> {
        val itemHistoryQuantity = 20
        repeat(itemHistoryQuantity) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            historyRepository.save(createItemBurn()).awaitFirst()
            historyRepository.save(createItemMint()).awaitFirst()
        }
        val result = activityController.getNftActivitiesSync(
            null,
            itemHistoryQuantity*3,
            SyncSortDto.DB_UPDATE_ASC,
            listOf(NftActivitiesSyncTypesDto.TRANSFER, NftActivitiesSyncTypesDto.MINT)
        )

        assertThat(result.body!!.items).hasSize(itemHistoryQuantity*2)
        assertThat(result.body!!.items).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
    }


    @Test
    fun `activity controller sync test burn`() = runBlocking<Unit> {
        val itemHistoryQuantity = 25
        repeat(itemHistoryQuantity) {
            historyRepository.save(createItemTransfer()).awaitFirst()
            historyRepository.save(createItemBurn()).awaitFirst()
            historyRepository.save(createItemMint()).awaitFirst()
        }
        val result = activityController.getNftActivitiesSync(
            null,
            itemHistoryQuantity*3,
            SyncSortDto.DB_UPDATE_ASC,
            listOf(NftActivitiesSyncTypesDto.BURN)
        )

        assertThat(result.body!!.items).hasSize(itemHistoryQuantity)
        assertThat(result.body!!.items).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
    }
}
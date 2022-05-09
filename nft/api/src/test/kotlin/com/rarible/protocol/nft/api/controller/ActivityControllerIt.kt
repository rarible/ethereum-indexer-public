package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
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
        val result = activityController.getNftActivitiesSync(null, 20, ActivitySortDto.EARLIEST_FIRST)

        assertThat(result.body!!.items).hasSize(20)
        assertThat(result.body!!.items).isSortedAccordingTo { i1, i2 -> compareValues(i1.updatedAt , i2.updatedAt) }
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
            val result = activityController.getNftActivitiesSync(continuation, 20, ActivitySortDto.EARLIEST_FIRST)
            result.body?.let { activities.addAll(it.items) }
            continuation = result.body?.continuation
            totalPages += 1
        }while (continuation != null)

        assertThat(totalPages).isEqualTo(5)
        assertThat(activities).hasSize(95)
        assertThat(activities).isSortedAccordingTo { i1, i2 -> compareValues(i1.updatedAt , i2.updatedAt) }
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
            val result = activityController.getNftActivitiesSync(continuation, 20, ActivitySortDto.LATEST_FIRST)
            result.body?.let { activities.addAll(it.items) }
            continuation = result.body?.continuation
            totalPages += 1
        }while (continuation != null)

        assertThat(totalPages).isEqualTo(5)
        assertThat(activities).hasSize(95)
        assertThat(activities).isSortedAccordingTo { i1, i2 -> compareValues(i2.updatedAt , i1.updatedAt) }
    }

}
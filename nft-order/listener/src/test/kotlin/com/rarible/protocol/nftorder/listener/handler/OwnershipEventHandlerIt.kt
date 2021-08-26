package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.OwnershipService
import com.rarible.protocol.nftorder.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class OwnershipEventHandlerIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipEventHandler: OwnershipEventHandler

    @Autowired
    private lateinit var ownershipService: OwnershipService

    @Test
    fun `update event - ownership doesn't exist`() = runWithKafka {
        val itemId = randomItemId()
        val ownershipId = randomOwnershipId(itemId)

        val ownershipDto = randomNftOwnershipDto(ownershipId)
        ownershipEventHandler.handle(createOwnershipUpdateEvent(ownershipDto))

        val created = ownershipService.get(ownershipId)
        // Ownership should not be updated since it wasn't in DB before update
        assertThat(created).isNull()

        // But there should be single Ownership event "as is"
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            assertUpdateOwnershipEvent(ownershipId, ownershipEvents!![0])
        }
    }

    @Test
    fun `update event - existing ownership updated`() = runWithKafka {
        val itemId = randomItemId()
        val bestSell = randomLegacyOrderDto(itemId)
        val ownership = randomOwnership(itemId).copy(bestSellOrder = bestSell)

        ownershipService.save(ownership)

        val ownershipDto = randomNftOwnershipDto(ownership.id)
        ownershipEventHandler.handle(createOwnershipUpdateEvent(ownershipDto))

        val updated = ownershipService.get(ownership.id)!!

        // Entity should be completely replaced by update data, except enrich data - it should be the same
        assertOwnershipAndNftDtoEquals(updated, ownershipDto)
        assertThat(updated.bestSellOrder).isEqualTo(bestSell)
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            assertUpdateOwnershipEvent(ownership.id, ownershipEvents!![0])
        }
    }

    @Test
    fun `delete event - existing ownership deleted`() = runWithKafka {
        val ownership = ownershipService.save(randomOwnership())
        assertThat(ownershipService.get(ownership.id)).isNotNull()

        // No enrichment data fetched
        ownershipEventHandler.handle(randomOwnershipDeleteEvent(ownership.id))

        // Entity not created due to absence of enrichment data
        assertThat(ownershipService.get(ownership.id)).isNull()
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            assertDeleteOwnershipEvent(ownership.id, ownershipEvents!![0])
        }
    }

    @Test
    fun `delete event - ownership doesn't exist`() = runWithKafka {
        val ownershipId = randomOwnershipId()

        ownershipEventHandler.handle(randomOwnershipDeleteEvent(ownershipId))

        assertThat(ownershipService.get(ownershipId)).isNull()
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            assertDeleteOwnershipEvent(ownershipId, ownershipEvents!![0])
        }
    }

    private fun assertDeleteOwnershipEvent(ownershipId: OwnershipId, message: KafkaMessage<NftOrderOwnershipEventDto>) {
        val event = message.value
        assertThat(event is NftOrderOwnershipDeleteEventDto)
        assertThat(event.ownershipId).isEqualTo(ownershipId.decimalStringValue)
    }

    private fun assertUpdateOwnershipEvent(ownershipId: OwnershipId, message: KafkaMessage<NftOrderOwnershipEventDto>) {
        val event = message.value
        assertThat(event is NftOrderOwnershipUpdateEventDto)
        assertThat(event.ownershipId).isEqualTo(ownershipId.decimalStringValue)
    }

    private fun createOwnershipUpdateEvent(nftOwnership: NftOwnershipDto): NftOwnershipUpdateEventDto {
        return NftOwnershipUpdateEventDto(
            randomString(),
            nftOwnership.id,
            nftOwnership
        )
    }
}
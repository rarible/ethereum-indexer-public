package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferFromEvent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitLessThanOffset
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class OwnershipEventDtoFromOwnershipConverterTest {

    private val timeDelta = TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS)

    @Test
    fun `convert - ok, with event`() {
        val ownership = createRandomOwnership()
        val transfer = createRandomOwnershipTransferFromEvent()

        val dto = OwnershipEventDtoFromOwnershipConverter.convert(ownership, transfer) as NftOwnershipUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.ownership).isEqualTo(OwnershipDtoConverter.convert(ownership))
        assertThat(timeMarks.source).isEqualTo("blockchain")
        assertThat(timeMarks.marks[0].name).isEqualTo("source")
        assertThat(timeMarks.marks[0].date.epochSecond).isEqualTo(transfer.log.blockTimestamp)
        assertThat(timeMarks.marks[1].name).isEqualTo("indexer-in_nft")
        assertThat(timeMarks.marks[1].date).isEqualTo(transfer.eventTimeMarks!!.marks[1].date)

        assertThat(timeMarks.marks[2].name).isEqualTo("indexer-out_nft")
        assertThat(timeMarks.marks[2].date).isCloseTo(nowMillis(), timeDelta)
    }

    @Test
    fun `convert - ok, without event`() {
        val ownership = createRandomOwnership()

        val dto = OwnershipEventDtoFromOwnershipConverter.convert(ownership, null) as NftOwnershipUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.ownership).isEqualTo(OwnershipDtoConverter.convert(ownership))

        assertThat(timeMarks.source).isEqualTo("offchain")
        assertThat(timeMarks.marks[0].name).isEqualTo("indexer-out_nft")
        assertThat(timeMarks.marks[0].date).isCloseTo(nowMillis(), timeDelta)
    }
}
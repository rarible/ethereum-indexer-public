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

    @Test
    fun `convert - ok, with event`() {
        val ownership = createRandomOwnership()
        val transfer = createRandomOwnershipTransferFromEvent()

        val dto = OwnershipEventDtoFromOwnershipConverter.convert(ownership, transfer) as NftOwnershipUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.ownership).isEqualTo(OwnershipDtoConverter.convert(ownership))
        assertThat(timeMarks.indexer).isCloseTo(nowMillis(), TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS))
        assertThat(timeMarks.source!!.date.epochSecond).isEqualTo(transfer.log.blockTimestamp)
    }

    @Test
    fun `convert - ok, without event`() {
        val ownership = createRandomOwnership()

        val dto = OwnershipEventDtoFromOwnershipConverter.convert(ownership, null) as NftOwnershipUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.ownership).isEqualTo(OwnershipDtoConverter.convert(ownership))
        assertThat(timeMarks.indexer).isCloseTo(nowMillis(), TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS))
        assertThat(timeMarks.source).isNull()
    }
}
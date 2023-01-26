package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitLessThanOffset
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class ItemEventDtoConverterTest {

    @Test
    fun `convert - ok, with event`() {
        val item = createRandomItem()
        val mint = createRandomMintItemEvent()

        val dto = ItemEventDtoConverter.convert(item, mint) as NftItemUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.item).isEqualTo(ItemDtoConverter.convert(item))
        assertThat(timeMarks.indexer).isCloseTo(nowMillis(), TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS))
        assertThat(timeMarks.source!!.date.epochSecond).isEqualTo(mint.log.blockTimestamp)
    }

    @Test
    fun `convert - ok, without event`() {
        val item = createRandomItem()

        val dto = ItemEventDtoConverter.convert(item, null) as NftItemUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.item).isEqualTo(ItemDtoConverter.convert(item))
        assertThat(timeMarks.indexer).isCloseTo(nowMillis(), TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS))
        assertThat(timeMarks.source).isNull()
    }

}
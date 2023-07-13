package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.misc.nftStubEventMarks
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitLessThanOffset
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class ItemEventDtoConverterTest {

    private val timeDelta = TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS)

    @Test
    fun `convert - ok`() {
        val item = createRandomItem()

        val inputTimeMarks = nftStubEventMarks()
        val dto = ItemEventDtoConverter.convert(item, inputTimeMarks) as NftItemUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.item).isEqualTo(ItemDtoConverter.convert(item))

        assertThat(timeMarks.source).isEqualTo("stub")
        assertThat(timeMarks.marks[0].name).isEqualTo("source")
        assertThat(timeMarks.marks[0].date).isEqualTo(inputTimeMarks.marks[0].date)
        assertThat(timeMarks.marks[1].name).isEqualTo("indexer-in_nft")
        assertThat(timeMarks.marks[1].date).isEqualTo(inputTimeMarks.marks[1].date)

        assertThat(timeMarks.marks[2].name).isEqualTo("indexer-out_nft")
        assertThat(timeMarks.marks[2].date).isCloseTo(nowMillis(), timeDelta)
    }

}
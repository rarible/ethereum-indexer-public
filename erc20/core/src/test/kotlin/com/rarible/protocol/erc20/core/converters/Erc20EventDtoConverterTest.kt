package com.rarible.protocol.erc20.core.converters

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import com.rarible.protocol.erc20.core.randomIncomeTransferEvent
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitLessThanOffset
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class Erc20EventDtoConverterTest {

    @Test
    fun `convert - ok, with event`() {
        val event = Erc20UpdateEvent(randomIncomeTransferEvent(), randomBalance())

        val dto = Erc20EventDtoConverter.convert(event) as Erc20BalanceUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.balance).isEqualTo(Erc20BalanceDtoConverter.convert(event.balance))
        assertThat(timeMarks.indexer).isCloseTo(nowMillis(), TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS))
        assertThat(timeMarks.source!!.date.epochSecond).isEqualTo(event.event!!.log.blockTimestamp)
    }

    @Test
    fun `convert - ok, without event`() {
        val event = Erc20UpdateEvent(null, randomBalance())

        val dto = Erc20EventDtoConverter.convert(event) as Erc20BalanceUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.balance).isEqualTo(Erc20BalanceDtoConverter.convert(event.balance))
        assertThat(timeMarks.indexer).isCloseTo(nowMillis(), TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS))
        assertThat(timeMarks.source).isNull()
    }

}

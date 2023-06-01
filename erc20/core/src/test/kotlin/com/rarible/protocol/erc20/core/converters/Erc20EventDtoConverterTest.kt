package com.rarible.protocol.erc20.core.converters

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.misc.erc20OffchainEventMarks
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import com.rarible.protocol.erc20.core.randomIncomeTransferEvent
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitLessThanOffset
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class Erc20EventDtoConverterTest {

    private val timeDelta = TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS)

    @Test
    fun `convert - ok, with event`() {
        val event = Erc20UpdateEvent(
            randomIncomeTransferEvent(),
            erc20OffchainEventMarks(),
            randomBalance()
        )

        val dto = Erc20EventDtoConverter.convert(event) as Erc20BalanceUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.balance).isEqualTo(Erc20BalanceDtoConverter.convert(event.balance))

        assertThat(timeMarks.source).isEqualTo("offchain")
        assertThat(timeMarks.marks[0].name).isEqualTo("source")
        assertThat(timeMarks.marks[0].date).isCloseTo(nowMillis(), timeDelta)

        assertThat(timeMarks.marks[1].name).isEqualTo("indexer-in_erc20")
        assertThat(timeMarks.marks[1].date).isCloseTo(nowMillis(), timeDelta)

        assertThat(timeMarks.marks[2].name).isEqualTo("indexer-out_erc20")
        assertThat(timeMarks.marks[2].date).isCloseTo(nowMillis(), timeDelta)
    }

    @Test
    fun `convert - ok, without event`() {
        val event = Erc20UpdateEvent(null, null, randomBalance())

        val dto = Erc20EventDtoConverter.convert(event) as Erc20BalanceUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.balance).isEqualTo(Erc20BalanceDtoConverter.convert(event.balance))

        assertThat(timeMarks.source).isEqualTo("offchain")

        assertThat(timeMarks.marks[0].name).isEqualTo("source")
        assertThat(timeMarks.marks[0].date).isCloseTo(nowMillis(), timeDelta)

        assertThat(timeMarks.marks[1].name).isEqualTo("indexer-in_erc20")
        assertThat(timeMarks.marks[1].date).isCloseTo(nowMillis(), timeDelta)

        assertThat(timeMarks.marks[2].name).isEqualTo("indexer-out_erc20")
        assertThat(timeMarks.marks[2].date).isCloseTo(nowMillis(), timeDelta)
    }

}

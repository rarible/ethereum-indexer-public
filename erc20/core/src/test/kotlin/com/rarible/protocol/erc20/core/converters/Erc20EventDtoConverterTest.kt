package com.rarible.protocol.erc20.core.converters

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.Erc20AllowanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.erc20.core.misc.erc20OffchainEventMarks
import com.rarible.protocol.erc20.core.model.Erc20AllowanceEvent
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import com.rarible.protocol.erc20.core.randomIncomeTransferEvent
import com.rarible.protocol.erc20.core.repository.data.randomAllowance
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

        verifyTimeMarks(timeMarks)
    }

    @Test
    fun `convert - ok, without event`() {
        val event = Erc20UpdateEvent(null, null, randomBalance())

        val dto = Erc20EventDtoConverter.convert(event) as Erc20BalanceUpdateEventDto
        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.balance).isEqualTo(Erc20BalanceDtoConverter.convert(event.balance))

        verifyTimeMarks(timeMarks)
    }

    private fun verifyTimeMarks(timeMarks: EventTimeMarksDto) {
        assertThat(timeMarks.source).isEqualTo("offchain")

        assertThat(timeMarks.marks[0].name).isEqualTo("source")
        assertThat(timeMarks.marks[0].date).isCloseTo(nowMillis(), timeDelta)

        assertThat(timeMarks.marks[1].name).isEqualTo("indexer-in_erc20")
        assertThat(timeMarks.marks[1].date).isCloseTo(nowMillis(), timeDelta)

        assertThat(timeMarks.marks[2].name).isEqualTo("indexer-out_erc20")
        assertThat(timeMarks.marks[2].date).isCloseTo(nowMillis(), timeDelta)
    }

    @Test
    fun `convert - allowance`() {
        val event = Erc20AllowanceEvent(null, null, randomAllowance())

        val dto = Erc20EventDtoConverter.convert(event) as Erc20AllowanceEventDto

        val timeMarks = dto.eventTimeMarks!!

        assertThat(dto.allowance.allowance).isEqualTo(event.allowance.allowance.value)
        assertThat(dto.allowance.contract).isEqualTo(event.allowance.token)
        assertThat(dto.allowance.owner).isEqualTo(event.allowance.owner)
        assertThat(dto.allowance.createdAt).isEqualTo(event.allowance.createdAt)
        assertThat(dto.allowance.lastUpdatedAt).isEqualTo(event.allowance.lastUpdatedAt)

        assertThat(dto.eventId).isEqualTo(event.id)
        assertThat(dto.balanceId).isEqualTo(event.allowance.id.stringValue)
        assertThat(dto.createdAt).isEqualTo(event.allowance.createdAt)
        assertThat(dto.lastUpdatedAt).isEqualTo(event.allowance.lastUpdatedAt)

        verifyTimeMarks(timeMarks)
    }
}

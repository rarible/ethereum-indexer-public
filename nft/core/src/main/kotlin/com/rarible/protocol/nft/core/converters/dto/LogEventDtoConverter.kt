package com.rarible.protocol.nft.core.converters.dto

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.LogEventDto
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object LogEventDtoConverter : Converter<LogEvent, LogEventDto> {
    override fun convert(source: LogEvent): LogEventDto {
        return LogEventDto(
            transactionHash = source.transactionHash,
            status = convert(source.status),
            address = source.address,
            topic = source.topic
        )
    }

    private fun convert(source: LogEventStatus): LogEventDto.Status {
        return when (source) {
            LogEventStatus.PENDING -> LogEventDto.Status.PENDING
            LogEventStatus.CONFIRMED -> LogEventDto.Status.CONFIRMED
            LogEventStatus.REVERTED -> LogEventDto.Status.REVERTED
            LogEventStatus.DROPPED -> LogEventDto.Status.DROPPED
            LogEventStatus.INACTIVE -> LogEventDto.Status.INACTIVE
        }
    }
}

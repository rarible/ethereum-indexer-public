package com.rarible.protocol.nft.core.converters.dto

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.dto.LogEventDto
import io.daonomic.rpc.domain.Word
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object LogEventDtoConverter : Converter<ReversedEthereumLogRecord, LogEventDto> {
    override fun convert(source: ReversedEthereumLogRecord): LogEventDto {
        return LogEventDto(
            transactionHash = Word.apply(source.transactionHash),
            status = convert(source.status),
            address = source.address,
            topic = source.topic
        )
    }

    private fun convert(source: EthereumLogStatus): LogEventDto.Status {
        return when (source) {
            EthereumLogStatus.PENDING -> LogEventDto.Status.PENDING
            EthereumLogStatus.CONFIRMED -> LogEventDto.Status.CONFIRMED
            EthereumLogStatus.REVERTED -> LogEventDto.Status.REVERTED
            EthereumLogStatus.DROPPED -> LogEventDto.Status.DROPPED
            EthereumLogStatus.INACTIVE -> LogEventDto.Status.INACTIVE
        }
    }
}
package com.rarible.protocol.order.core.converters.dto

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.dto.LogEventDto
import io.daonomic.rpc.domain.Word
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object TransactionDtoConverter : Converter<ReversedEthereumLogRecord, LogEventDto> {
    override fun convert(source: ReversedEthereumLogRecord): LogEventDto {
        return LogEventDto(
            transactionHash = Word.apply(source.transactionHash),
            status = convert(source.status),
            address = source.address,
            topic = source.topic
        )
    }

    private fun convert(source: EthereumBlockStatus): LogEventDto.Status {
        return when (source) {
            EthereumBlockStatus.PENDING -> LogEventDto.Status.PENDING
            EthereumBlockStatus.CONFIRMED -> LogEventDto.Status.CONFIRMED
            EthereumBlockStatus.REVERTED -> LogEventDto.Status.REVERTED
            EthereumBlockStatus.DROPPED -> LogEventDto.Status.DROPPED
            EthereumBlockStatus.INACTIVE -> LogEventDto.Status.INACTIVE
        }
    }
}

package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.log.domain.TransactionDto
import com.rarible.protocol.dto.CreateTransactionRequestDto
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ListenerTransactionConverter : Converter<CreateTransactionRequestDto, TransactionDto> {
    override fun convert(source: CreateTransactionRequestDto): TransactionDto {
        return TransactionDto(
            hash = source.hash,
            from = source.from,
            nonce = source.nonce,
            to = source.to,
            input = source.input
        )
    }
}


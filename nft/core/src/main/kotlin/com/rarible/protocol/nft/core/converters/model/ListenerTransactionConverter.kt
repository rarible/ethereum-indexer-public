package com.rarible.protocol.nft.core.converters.model

import com.rarible.protocol.dto.CreateTransactionRequestDto
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import com.rarible.ethereum.log.domain.TransactionDto as Transaction

@Component
object ListenerTransactionConverter : Converter<CreateTransactionRequestDto, Transaction> {
    override fun convert(source: CreateTransactionRequestDto): Transaction {
        return Transaction(
            hash = source.hash,
            from = source.from,
            nonce = source.nonce,
            to = source.to,
            input = source.input
        )
    }
}


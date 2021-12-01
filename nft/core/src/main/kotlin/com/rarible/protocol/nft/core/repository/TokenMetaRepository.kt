package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFilter
import com.rarible.protocol.nft.core.model.TokenStandard
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.*
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.DB)
class TokenMetaRepository(
    private val mongo: ReactiveMongoOperations
) {

    fun save(token: Token): Mono<Token> {
        return mongo.save(token)
    }

    fun findById(id: Address): Mono<Token> {
        return mongo.findById(id)
    }
}

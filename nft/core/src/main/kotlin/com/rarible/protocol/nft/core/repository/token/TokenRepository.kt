package com.rarible.protocol.nft.core.repository.token

import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFilter
import com.rarible.protocol.nft.core.model.TokenStandard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
class TokenRepository(
    private val mongo: ReactiveMongoOperations
) {

    suspend fun createIndexes() {
        TokenRepositoryIndexes.ALL_INDEXES.forEach { index ->
            mongo.indexOps(Token.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun save(token: Token): Mono<Token> {
        return mongo.save(token.withDbUpdatedAt())
    }

    fun remove(token: Address): Mono<Void> {
        val criteria = Criteria.where(ID).isEqualTo(token)
        return mongo.remove<Token>(Query(criteria)).then()
    }

    fun findAll(): Flux<Token> {
        return mongo.findAll()
    }

    fun findAllFrom(from: Address?): Flow<Token> {
        val criteria = from?.let { Criteria.where(ID).gt(it) } ?: Criteria()
        return mongo.find(Query.query(criteria).with(ID_ASC_SORT), Token::class.java).asFlow()
    }

    suspend fun findNone(limit: Int, retries: Int): List<Token> {
        val query = Query(Token::standard isEqualTo TokenStandard.NONE)
            .addCriteria(Criteria().orOperator(
                Token::standardRetries exists false,
                Token::standardRetries lt retries)
            )
            .limit(limit)

        return mongo.find(query, Token::class.java).collectList().awaitFirst()
    }

    suspend fun incrementRetry(id: Address) {
        mongo.findAndModify(
            Query(Criteria.where(ID).isEqualTo(id)),
            Update().inc(Token::standardRetries.name, 1),
            Token::class.java).awaitSingle()
    }

    fun count(): Mono<Long> {
        return mongo.count<Token>()
    }

    fun findById(id: Address): Mono<Token> {
        return mongo.findById(id)
    }

    fun findByIds(ids: Collection<Address>): Flow<Token> {
        val criteria = Criteria.where(ID).`in`(ids)
        return mongo.find(Query.query(criteria), Token::class.java).asFlow()
    }

    fun search(filter: TokenFilter): Flux<Token> {
        return mongo.find(filter.toQuery().with(ID_ASC_SORT))
    }

    private fun TokenFilter.toQuery(): Query {
        val criteria = when (this) {
            is TokenFilter.All -> all()
            is TokenFilter.ByOwner -> byOwner(owner)
        }.and(Token::standard).nin(TokenStandard.IGNORABLE).and(Token::status).ne(ContractStatus.ERROR) scrollTo continuation

        return Query.query(criteria).limit(size)
    }

    private fun all() = Criteria()

    private fun byOwner(user: Address): Criteria =
        Criteria(Token::owner.name).`is`(user)

    private infix fun Criteria.scrollTo(continuation: String?): Criteria =
        if (continuation == null) {
            this
        } else {
            and(ID).gt(continuation)
        }

    private companion object {
        const val ID = "_id"
        val ID_ASC_SORT: Sort = Sort.by(Sort.Direction.ASC, ID)
    }
}

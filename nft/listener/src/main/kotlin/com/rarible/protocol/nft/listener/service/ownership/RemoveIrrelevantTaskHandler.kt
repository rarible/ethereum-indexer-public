package com.rarible.protocol.nft.listener.service.ownership

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.misc.div
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.ne
import org.springframework.stereotype.Component

@Component
class RemoveIrrelevantTaskHandler(
    private val mongo: ReactiveMongoOperations,
    private val ownershipRepository: OwnershipRepository
) : TaskHandler<String> {

    override val type: String
        get() = REMOVE_IRRELEVANT

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val criteria = from?.let { Ownership::id gt OwnershipId.parseId(from) } ?: Criteria()
        val query = Query(Criteria().andOperator(
            criteria,
            Ownership::value ne EthUInt256.ZERO.toString(),
            Ownership::lazyValue inValues listOf(null, EthUInt256.ZERO.toString()),
            Ownership::pending isEqualTo listOf()
        )).with(Sort.by(Sort.Direction.ASC, Ownership::id.name))
        return mongo.query<Ownership>().matching(query).all().asFlow()
            .onEach(this::processOwnership)
            .map { it.id.toString() }
    }

    suspend fun processOwnership(ownership: Ownership) {
        val criteria = Criteria().andOperator(
            LogEvent::data / ItemHistory::token isEqualTo ownership.token,
            LogEvent::data / ItemHistory::tokenId isEqualTo ownership.tokenId,
            LogEvent::data / ItemHistory::owner isEqualTo ownership.owner
        )
        val tx = mongo.find(Query(criteria), LogEvent::class.java, NftItemHistoryRepository.COLLECTION).awaitFirstOrNull()
        if (null == tx) {
            ownershipRepository.deleteById(ownership.id).awaitFirstOrNull()
            logger.info("Deleted ownership: ${ownership.id}")
        }
    }

    companion object {
        const val REMOVE_IRRELEVANT = "REMOVE_IRRELEVANT_OWNERSHIPS"
        private val logger: Logger = LoggerFactory.getLogger(RemoveIrrelevantTaskHandler::class.java)
    }
}

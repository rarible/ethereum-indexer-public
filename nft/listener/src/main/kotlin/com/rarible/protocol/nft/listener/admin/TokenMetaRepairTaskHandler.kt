package com.rarible.protocol.nft.listener.admin

import com.rarible.core.cache.Cache
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.TOKEN_METADATA_COLLECTION
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenMetaRepairTaskHandler(
    private val tokenPropertiesService: TokenPropertiesService,
    private val mongo: ReactiveMongoOperations,
    private val ff: FeatureFlags
) : TaskHandler<String> {

    override val type = "TOKEN_META_REPAIR"

    override suspend fun isAbleToRun(param: String): Boolean {
        return ff.enableTokenMetaSelfRepair
    }

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        //val filter = Criteria("data.properties.name").isEqualTo("Untitled")
        val filter = Criteria()
        val criteria = from?.let { filter.and("_id").gt(from) } ?: filter

        val query = Query(criteria).with(Sort.by(Sort.Direction.ASC, "_id"))

        return mongo.find<Cache>(query, TOKEN_METADATA_COLLECTION).asFlow().map {
            val properties = (it.data as TokenPropertiesService.CachedTokenProperties?)?.properties

            if (properties == null || properties.name == TokenMeta.EMPTY.properties.name || properties.content.imageOriginal == null) {
                val address = Address.apply(it.id)
                tokenPropertiesService.reset(address)
                tokenPropertiesService.resolve(address)
            }

            it.id
        }
    }

}
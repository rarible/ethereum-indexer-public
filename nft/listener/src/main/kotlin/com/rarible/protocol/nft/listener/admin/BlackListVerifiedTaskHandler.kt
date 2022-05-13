package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class BlackListVerifiedTaskHandler(
    private val itemRepository: ItemRepository,
    private val reduceEventListenerListener: ReduceEventListenerListener
) : TaskHandler<String> {

    override val type: String
        get() = NAME

    override fun runLongTask(from: String?, paramStr: String): Flow<String> {
        val params = parseParam(paramStr)
        val fromTokenId = from?.let { EthUInt256.of(it) }
        return itemRepository.findTokenItems(params.address, fromTokenId).map { item ->
            logger.info("Handling item: ${item.id} with feature: ${params.feature}")
            val savedItem = when (params.feature) {
                Feature.BLACKLIST -> itemRepository.save(item.copy(deleted = true)).awaitSingle()
                Feature.VERIFIED -> item
            }
            reduceEventListenerListener.onItemChanged(savedItem)
            item.id.token.prefixed()
        }
    }

    companion object {
        const val NAME = "BLACKLIST_VERIFIED"
        val logger: Logger = LoggerFactory.getLogger(BlackListVerifiedTaskHandler::class.java)
        fun parseParam(param: String): Param {
            val slices = param.split("_")
            return Param(Address.apply(slices[0]), Feature.valueOf(slices[1]))
        }
    }

    data class Param(
        val address: Address,
        val feature: Feature
    ) {
        override fun toString(): String {
            return "${address}_${feature}"
        }
    }

    enum class Feature {
        BLACKLIST, VERIFIED
    }
}

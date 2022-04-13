package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.EnsDomainsPropertiesResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class EnsDomainAutoBurnTaskHandler(
    private val ensDomainsPropertiesResolver: EnsDomainsPropertiesResolver,
    private val nftIndexerProperties: NftIndexerProperties,
    private val itemRepository: ItemRepository,
    private val ensDomainService: EnsDomainService
) : TaskHandler<String> {

    override val type: String
        get() = ENN_DOMAIN_AUTO_BURN


    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val token = Address.apply(nftIndexerProperties.ensDomainsContractAddress)
        val tokenId = from?.let { EthUInt256.of(from) }
        return itemRepository.findTokenItems(token, tokenId).map { item ->
            val properties = ensDomainsPropertiesResolver.resolve(item.id)
            val expirationProperty = properties?.let { ensDomainService.getExpirationProperty(it) }
            if (expirationProperty != null) {
                logger.info("Got properties for ${item.id.decimalStringValue}, burn at $expirationProperty")
            } else {
                logger.warn("Can't get properties for ${item.id.decimalStringValue} for auto burn")
            }
            item.tokenId.toString()
        }
    }

    companion object {
        const val ENN_DOMAIN_AUTO_BURN = "ENN_DOMAIN_AUTO_BURN"
        private val logger = LoggerFactory.getLogger(EnsDomainAutoBurnTaskHandler::class.java)
    }
}

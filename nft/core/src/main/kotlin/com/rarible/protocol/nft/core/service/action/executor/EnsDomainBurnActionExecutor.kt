package com.rarible.protocol.nft.core.service.action.executor

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.nft.core.model.ActionType
import com.rarible.protocol.nft.core.model.BurnItemAction
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.EnsDomainsPropertiesProvider
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnsDomainBurnActionExecutor(
    private val reducer: ItemReduceService,
    private val ensDomainsPropertiesProvider: EnsDomainsPropertiesProvider,
    private val ensDomainService: EnsDomainService,
    private val executedBurnActionMetric: RegisteredCounter,
    private val errorBurnActionMetric: RegisteredCounter,
) : ActionExecutor<BurnItemAction> {
    override val type: ActionType = ActionType.BURN

    override suspend fun execute(action: BurnItemAction) {
        val itemId = action.itemId()
        val properties = ensDomainsPropertiesProvider.get(itemId)
            ?: run {
                errorBurnActionMetric.increment()
                throw IllegalStateException("Can't get properties for ens ${itemId.decimalStringValue}")
            }

        val expiration = ensDomainService.getExpirationProperty(properties)
        if (expiration == null || action.actionAt >= expiration) {
            if (reducer.update(token = action.token, tokenId = action.tokenId).awaitFirstOrNull() != null) {
                logger.info("Action burn for ${itemId.decimalStringValue} was executed")
                executedBurnActionMetric.increment()
            } else {
                logger.error("Can't execute action for ${itemId.decimalStringValue}")
            }
        } else {
            logger.info(
                "New expiration date detected for ${itemId.decimalStringValue}, expected to burn at ${action.actionAt} but expired at $expiration"
            )
            ensDomainService.onGetProperties(itemId, properties)
        }
    }

    private companion object {

        val logger: Logger = LoggerFactory.getLogger(EnsDomainBurnActionExecutor::class.java)
    }
}

package com.rarible.protocol.erc20.listener.listener

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.toModel
import com.rarible.protocol.erc20.core.misc.addIndexerIn
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.service.Erc20AllowanceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderActivityEventHandler(
    private val erc20AllowanceService: Erc20AllowanceService,
) : RaribleKafkaEventHandler<EthActivityEventDto> {

    override suspend fun handle(event: EthActivityEventDto) {
        val activity = event.activity
        if (activity !is OrderActivityMatchDto) {
            return
        }
        logger.info("Handle event: $event")
        if (activity.type == OrderActivityMatchDto.Type.ACCEPT_BID) {
            handleActivity(activity, event)
        }
    }

    private suspend fun handleActivity(
        activity: OrderActivityMatchDto,
        event: EthActivityEventDto
    ) {
        val sideDto = if (activity.left.asset.assetType is Erc20AssetTypeDto) {
            activity.left
        } else if (activity.right.asset.assetType is Erc20AssetTypeDto) {
            activity.right
        } else {
            return
        }
        val assetType = sideDto.asset.assetType as Erc20AssetTypeDto
        val owner = sideDto.maker
        val token = assetType.contract
        logger.info(
            "Will recalculate erc20 balance owner=$owner, token=$token. After bid execution id=${sideDto.hash}"
        )
        erc20AllowanceService.onChainUpdate(
            balanceId = BalanceId(token = token, owner = owner),
            eventTimeMarks = event.eventTimeMarks.toModel().addIndexerIn(),
            event = null,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OrderActivityEventHandler::class.java)
    }
}

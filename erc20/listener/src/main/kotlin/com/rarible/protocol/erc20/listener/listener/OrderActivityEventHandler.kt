package com.rarible.protocol.erc20.listener.listener

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
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
        logger.info("Handle event: $event")
        val activity = event.activity
        if (activity is OrderActivityMatchDto && activity.type == OrderActivityMatchDto.Type.ACCEPT_BID) {
            val sideDto = if (activity.left.type == OrderActivityMatchSideDto.Type.BID) {
                activity.left
            } else {
                activity.right
            }
            val assetType = sideDto.asset.assetType
            if (assetType is Erc20AssetTypeDto) {
                logger.info("Will recalculate erc20 balance")
                val owner = sideDto.maker
                val token = assetType.contract
                erc20AllowanceService.onChainUpdate(
                    balanceId = BalanceId(token = token, owner = owner),
                    eventTimeMarks = event.eventTimeMarks.toModel().addIndexerIn(),
                    event = null,
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OrderActivityEventHandler::class.java)
    }
}

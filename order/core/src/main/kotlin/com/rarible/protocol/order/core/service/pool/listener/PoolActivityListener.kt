package com.rarible.protocol.order.core.service.pool.listener

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.model.PoolActivityResult
import com.rarible.protocol.order.core.model.PoolCreate
import com.rarible.protocol.order.core.model.PoolDataUpdate
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolNftDeposit
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.stereotype.Component

@Component
class PoolActivityListener(
    private val orderPublisher: ProtocolOrderPublisher,
    private val orderActivityConverter: OrderActivityConverter
) : PoolEventListener {

    override suspend fun onPoolEvent(event: ReversedEthereumLogRecord, eventTimeMarks: EventTimeMarks) {
        val reverted = event.status == EthereumBlockStatus.REVERTED
        val activity = when (event.data as PoolHistory) {
            is PoolTargetNftIn,
            is PoolTargetNftOut -> PoolActivityResult.History(event)

            is PoolDataUpdate,
            is PoolCreate,
            is PoolNftDeposit,
            is PoolNftWithdraw -> return
        }
        orderActivityConverter.convert(activity, reverted)?.let {
            orderPublisher.publish(it, eventTimeMarks)
        }
    }
}

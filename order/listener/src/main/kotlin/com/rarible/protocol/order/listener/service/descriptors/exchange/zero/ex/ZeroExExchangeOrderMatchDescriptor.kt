package com.rarible.protocol.order.listener.service.descriptors.exchange.zero.ex

import com.rarible.protocol.contracts.exchange.zero.ex.FillEvent
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderEventConverter
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderParser
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class ZeroExExchangeOrderMatchDescriptor(
    contractsProvider: ContractsProvider,
    private val zeroExOrderEventConverter: ZeroExOrderEventConverter,
    private val zeroExOrderParser: ZeroExOrderParser,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderSideMatch>(
    name = "0x_fill",
    topic = FillEvent.id(),
    contracts = contractsProvider.zeroEx(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Instant,
        index: Int,
        totalLogs: Int
    ): List<OrderSideMatch> {
        val event = FillEvent.apply(log)
        val matchOrdersDataList = zeroExOrderParser.parseMatchOrdersData(
            txHash = transaction.hash(),
            txInput = transaction.input(),
            txFrom = transaction.from(),
            event = event,
            index = index,
            totalLogs = totalLogs
        )
        return matchOrdersDataList.map { matchOrdersData ->
            zeroExOrderEventConverter.convert(
                matchOrdersData = matchOrdersData,
                from = transaction.from(),
                date = timestamp,
                orderHash = Word.apply(event.orderHash()),
                makerAddress = event.makerAddress(),
                makerAssetFilledAmount = event.makerAssetFilledAmount(),
                takerAssetFilledAmount = event.takerAssetFilledAmount(),
                input = transaction.input(),
            )
        }.flatten()
    }
}

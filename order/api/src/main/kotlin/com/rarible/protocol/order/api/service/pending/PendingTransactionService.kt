package com.rarible.protocol.order.api.service.pending

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.ethereum.log.service.AbstractPendingTransactionService
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties.ExchangeContractAddresses
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.BlockProcessor
import com.rarible.protocol.order.core.service.asset.AssetTypeService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.Address
import java.math.BigInteger
import com.rarible.protocol.contracts.exchange.v1.CancelEvent as CancelEventV1

@Service
class PendingTransactionService(
    private val assetTypeService: AssetTypeService,
    private val orderRepository: OrderRepository,
    exchangeContractAddresses: ExchangeContractAddresses,
    logEventService: LogEventService,
    blockProcessor: BlockProcessor
) : AbstractPendingTransactionService(logEventService, blockProcessor) {

    private val exchangeContracts = listOfNotNull(
        exchangeContractAddresses.v1,
        exchangeContractAddresses.v1Old,
        exchangeContractAddresses.v2
    ).toSet()

    override suspend fun process(
        hash: Word,
        from: Address,
        nonce: Long,
        to: Address?,
        id: Binary,
        data: Binary
    ): List<LogEvent> {
        return if (to != null && exchangeContracts.contains(to)) {
            processTxToExchange(from, id, data).map { (event, topic) ->
                LogEvent(
                    data = event,
                    address = to,
                    topic = topic,
                    transactionHash = hash,
                    from = from,
                    nonce = nonce,
                    status = LogEventStatus.PENDING,
                    index = 0,
                    minorLogIndex = 0
                )
            }
        } else {
            emptyList()
        }
    }

    private suspend fun processTxToExchange(from: Address, id: Binary, data: Binary): List<PendingLog> {
        logger.info("Process tx to exchange: from=$from, id=$id, data=$data")

        val pendingLog = when (id.prefixed()) {
            ExchangeV1.cancelSignature().id().prefixed() -> {
                val it = ExchangeV1.cancelSignature().`in`().decode(data, 0).value()
                //it = (owner, salt, sellAsset, buyAsset). asset = token, tokenId, ercType

                val makeToken = it._3()._1()
                val makeTokenId = EthUInt256.of(it._3()._2())

                val takeToken = it._4()._1()
                val takeTokenId = EthUInt256.of(it._4()._2())

                val owner = it._1()
                val salt = it._2()

                val makeAssetType = assetTypeService.toAssetType(makeToken, makeTokenId)
                val takeAssetType = assetTypeService.toAssetType(takeToken, takeTokenId)
                val order = findOrder(makeAssetType, takeAssetType, owner, salt)

                order?.let {
                    val event = OrderCancel(
                        hash = order.hash,
                        maker = order.maker,
                        make = order.make,
                        take = order.take,
                        source = HistorySource.RARIBLE
                    )
                    PendingLog(event, CancelEventV1.id())
                }
            }
            ExchangeV1.exchangeSignature().id().prefixed() -> {
                val it = ExchangeV1.exchangeSignature().`in`().decode(data, 0).value()
                //it = (((owner, salt, sellAsset, buyAsset), selling, buying, fee), signature, amount)

                val makeToken = it._1()._1()._3()._1()
                val makeTokenId = EthUInt256.of(it._1()._1()._3()._2())

                val takeToken = it._1()._1()._4()._1()
                val takeTokenId = EthUInt256.of(it._1()._1()._4()._2())

                val owner = it._1()._1()._1()
                val salt = it._1()._1()._2()

                val amount = it._3()
                val buyValue = it._1()._2()
                val sellValue = it._1()._3()

                val makeAssetType = assetTypeService.toAssetType(makeToken, makeTokenId)
                val takeAssetType = assetTypeService.toAssetType(takeToken, takeTokenId)

                val order = findOrder(makeAssetType, takeAssetType, owner, salt)
                val counterHash = Order.hashKey(from, takeAssetType, makeAssetType, BigInteger.ZERO)

                order?.let {
                    val event = OrderSideMatch(
                        hash = it.hash,
                        counterHash = counterHash,
                        fill = EthUInt256.of(amount.multiply(buyValue).div(sellValue)),
                        make = order.make,
                        take = order.take,
                        maker = owner,
                        taker = from,
                        side = OrderSide.LEFT,
                        makeValue = null,
                        takeValue = null,
                        makeUsd = null,
                        takeUsd = null,
                        makePriceUsd = null,
                        takePriceUsd = null,
                        source = HistorySource.RARIBLE
                    )
                    PendingLog(event, BuyEvent.id())
                }
            }
            ExchangeV2.cancelSignature().id().prefixed() -> {
                null //TODO: need to support
            }
            ExchangeV2.matchOrdersSignature().id().prefixed() -> {
                null //TODO: need to support
            }
            else -> null
        }

        return listOfNotNull(pendingLog)
    }

    private suspend fun findOrder(
        makeAssetType: AssetType,
        takeAssetType: AssetType,
        owner: Address,
        salt: BigInteger
    ): Order? {
        val hash = Order.hashKey(owner, makeAssetType, takeAssetType, salt)
        return orderRepository.findById(hash)
    }

    private data class PendingLog(
        val eventData: OrderExchangeHistory,
        val topic: Word
    )
}

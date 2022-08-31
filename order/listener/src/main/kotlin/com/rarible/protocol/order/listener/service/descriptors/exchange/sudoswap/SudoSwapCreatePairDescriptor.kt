package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.AmmNftAssetType
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OnChainAmmOrder
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.SudoSwapErc20PairDetail
import com.rarible.protocol.order.core.model.SudoSwapEthPairDetail
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import java.time.Instant
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger

@Service
@CaptureSpan(type = SpanType.EVENT)
class SudoSwapCreatePairDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val priceUpdateService: PriceUpdateService,
    private val priceNormalizer: PriceNormalizer,
): LogEventDescriptor<OnChainAmmOrder> {

    override val collection: String = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = NewPairEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = listOf(exchangeContractAddresses.sudoswapPairFactoryV1).toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<OnChainAmmOrder> {
        return mono { listOfNotNull(convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): OnChainAmmOrder? {
        val event = NewPairEvent.apply(log)
        val details = sudoSwapEventConverter.getTransactionDetails(transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val nft = Asset(
            type = AmmNftAssetType(details.nft),
            value = EthUInt256.of(details.inNft.size)
        )
        val currency = when (details) {
            is SudoSwapEthPairDetail ->
                Asset(EthAssetType, EthUInt256.of(details.ethBalance))
            is SudoSwapErc20PairDetail ->
                Asset(Erc20AssetType(details.token), EthUInt256.of(details.tokenBalance))
        }
        if (details.poolType !in SUPPORTED_POOL_TYPES) {
            return null
        }
        val (make, take) = when (details.poolType) {
            SudoSwapPoolType.NFT,
            SudoSwapPoolType.TRADE -> {
                nft to currency
            }
            SudoSwapPoolType.TOKEN -> {
                currency to nft
            }
        }
        val data = OrderSudoSwapAmmDataV1(
            poolAddress = event.poolAddress(),
            bondingCurve = details.bondingCurve,
            assetRecipient = details.assetRecipient,
            poolType = details.poolType,
            delta = details.delta,
            fee = details.fee
        )
        return OnChainAmmOrder(
            hash = keccak256(event.poolAddress()),
            maker = event.poolAddress(),
            make = make,
            take = take,
            inNft = details.inNft.map { EthUInt256.of(it) },
            createdAt = date,
            platform = Platform.SUDOSWAP,
            data = data,
            price = EthUInt256.of(details.spotPrice),
            priceValue = priceNormalizer.normalize(take.type, details.spotPrice),
            priceUsd = priceUpdateService.getAssetUsdValue(take.type, details.spotPrice, date)
        )
    }

    private companion object {
        val SUPPORTED_POOL_TYPES = listOf(SudoSwapPoolType.NFT, SudoSwapPoolType.TRADE)
    }
}

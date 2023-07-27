package com.rarible.protocol.order.core.service.pool

import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.PoolInfo
import com.rarible.protocol.order.core.model.currency
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.sudoswap.SudoSwapProtocolFeeProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.math.BigInteger

@Component
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class PoolInfoProvider(
    private val sender: ReadOnlyMonoTransactionSender,
    private val orderRepository: OrderRepository,
    private val sudoSwapProtocolFeeProvider: SudoSwapProtocolFeeProvider,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
) {
    suspend fun getPollInfo(order: Order): PoolInfo? {
        return getInfoFromOrder(order)
    }

    suspend fun getPollInfo(hash: Word, poolAddress: Address): PoolInfo? {
        return orderRepository.findById(hash)
            ?.let {
                getInfoFromOrder(it)
            }
            ?: run {
                if (featureFlags.getPoolInfoFromChain)
                    getInfoFromChain(poolAddress)
                else null
            }
    }

    private suspend fun getInfoFromOrder(order: Order): PoolInfo? {
        return if (order.type == OrderType.AMM) {
            when (val data = order.data as OrderAmmData) {
                is OrderSudoSwapAmmDataV1 -> {
                    PoolInfo(
                        collection = order.token,
                        curve = data.bondingCurve,
                        spotPrice = data.spotPrice,
                        delta = data.delta,
                        fee = data.fee,
                        protocolFee = sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory),
                        token = order.currency.token
                    )
                }
            }
        } else null
    }

    private suspend fun getInfoFromChain(poolAddress: Address): PoolInfo {
        val contract = LSSVMPairV1(poolAddress, sender)
        return coroutineScope {
            val collection = async { contract.nft().call().awaitSingle() }
            val spotPrice = async { contract.spotPrice().call().awaitSingle() }
            val delta = async { contract.delta().call().awaitSingle() }
            val curve = async { contract.bondingCurve().call().awaitSingle() }
            val fee = async { contract.fee().call().awaitSingle() }
            val protocolFee = async {
                val factory = contract.factory().call().awaitSingle()
                sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(factory)
            }
            val pairVariant = async { contract.pairVariant().call().awaitSingle() }

            PoolInfo(
                collection = collection.await(),
                curve = curve.await(),
                spotPrice = spotPrice.await(),
                delta = delta.await(),
                fee = fee.await(),
                protocolFee = protocolFee.await(),
                token = contract.getAddressByPairVariant(pairVariant.await())
            )
        }
    }

    private suspend fun LSSVMPairV1.getAddressByPairVariant(pairVariant: BigInteger): Address {
        return when (val id = pairVariant.toInt()) {
            // ENUMERABLE_ETH, MISSING_ENUMERABLE_ETH
            0, 1 -> Address.ZERO()
            // ENUMERABLE_ERC20, MISSING_ENUMERABLE_ERC20
            2, 3 -> token().call().awaitSingle()
            else -> error("Unknown pairVariant: $id")
        }
    }
}

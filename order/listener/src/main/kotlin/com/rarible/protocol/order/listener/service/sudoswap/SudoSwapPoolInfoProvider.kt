package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.PoolInfo
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.sudoswap.SudoSwapProtocolFeeProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender

@Component
class SudoSwapPoolInfoProvider(
    private val sender: ReadOnlyMonoTransactionSender,
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val orderRepository: OrderRepository,
    private val sudoSwapProtocolFeeProvider: SudoSwapProtocolFeeProvider,
) {
    suspend fun gePollInfo(poolAddress: Address): PoolInfo {
        val contract = LSSVMPairV1(poolAddress, sender)
        val hash = sudoSwapEventConverter.getPoolHash(poolAddress)
        val order = orderRepository.findById(hash)

        return if (order != null && order.type == OrderType.AMM) {
            when (val data = order.data as OrderAmmData) {
                is OrderSudoSwapAmmDataV1 -> {
                    PoolInfo(
                        collection = order.token,
                        curve = data.bondingCurve,
                        spotPrice = data.spotPrice,
                        delta = data.delta,
                        fee = data.fee,
                        protocolFee = sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory)
                    )
                }
            }
        } else {
            coroutineScope {
                val collection = async { contract.nft().call().awaitSingle() }
                val spotPrice = async { contract.spotPrice().call().awaitSingle() }
                val delta = async { contract.delta().call().awaitSingle() }
                val curve = async { contract.bondingCurve().call().awaitSingle() }
                val fee = async { contract.fee().call().awaitSingle() }
                val protocolFee = async {
                    val factory = contract.factory().call().awaitSingle()
                    sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(factory)
                }
                PoolInfo(
                    collection = collection.await(),
                    curve = curve.await(),
                    spotPrice = spotPrice.await(),
                    delta = delta.await(),
                    fee =fee.await(),
                    protocolFee = protocolFee.await()
                )
            }
        }
    }
}
package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.LSSVMPairFactoryV1
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.SudoSwapErc20PairDetail
import com.rarible.protocol.order.core.model.SudoSwapEthPairDetail
import com.rarible.protocol.order.core.model.SudoSwapPairDetail
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.trace.TraceCallService
import org.springframework.stereotype.Component
import scalether.domain.response.Transaction
import java.math.BigInteger

@Component
class SudoSwapEventConverter(
    private val traceCallService: TraceCallService,
) {
    suspend fun getTransactionDetails(transient: Transaction): List<SudoSwapPairDetail> {
        val inputs = traceCallService.findAllRequiredCallInputs(
            txHash = transient.hash(),
            txInput = transient.input(),
            to = transient.to(),
            LSSVMPairFactoryV1.createPairETHSignature().id(),
            LSSVMPairFactoryV1.createPairERC20Signature().id()
        )
        return inputs.mapNotNull {
            when (it.methodSignatureId()) {
                LSSVMPairFactoryV1.createPairETHSignature().id() -> {
                    val decoded = LSSVMPairFactoryV1.createPairETHSignature().`in`().decode(it, 4)
                    SudoSwapEthPairDetail(
                        nft = decoded.value()._1(),
                        bondingCurve = decoded.value()._2(),
                        assetRecipient = decoded.value()._3(),
                        poolType = convert(decoded.value()._4()),
                        delta = decoded.value()._5(),
                        fee = decoded.value()._6(),
                        spotPrice = decoded.value()._7(),
                        inNft = decoded.value()._8().toList(),
                        ethBalance = transient.value(),
                    )
                }
                LSSVMPairFactoryV1.createPairERC20Signature().id() -> {
                    val decoded = LSSVMPairFactoryV1.createPairERC20Signature().`in`().decode(it, 4)
                    SudoSwapErc20PairDetail(
                        token = decoded.value()._1(),
                        nft = decoded.value()._2(),
                        bondingCurve = decoded.value()._3(),
                        assetRecipient = decoded.value()._4(),
                        poolType = convert(decoded.value()._5()),
                        delta = decoded.value()._6(),
                        fee = decoded.value()._7(),
                        spotPrice = decoded.value()._8(),
                        inNft = decoded.value()._9().toList(),
                        tokenBalance = decoded.value()._10(),
                    )
                }
                else -> null
            }
        }
    }

    private fun convert(value: BigInteger): SudoSwapPoolType {
        return when (value.intValueExact()) {
            0 -> SudoSwapPoolType.TOKEN
            1 -> SudoSwapPoolType.NFT
            2 -> SudoSwapPoolType.TRADE
            else -> throw IllegalArgumentException("Unrecognized pool type $value")
        }
    }
}


package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.LSSVMPairFactoryV1
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.HeadTransaction
import com.rarible.protocol.order.core.model.SimpleTraceResult
import com.rarible.protocol.order.core.model.SudoSwapAnyOutNftDetail
import com.rarible.protocol.order.core.model.SudoSwapErc20PairDetail
import com.rarible.protocol.order.core.model.SudoSwapEthPairDetail
import com.rarible.protocol.order.core.model.SudoSwapNftDepositDetail
import com.rarible.protocol.order.core.model.SudoSwapNftWithdrawDetail
import com.rarible.protocol.order.core.model.SudoSwapOutNftDetail
import com.rarible.protocol.order.core.model.SudoSwapPairDetail
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.model.SudoSwapTargetInNftDetail
import com.rarible.protocol.order.core.model.SudoSwapTargetOutNftDetail
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger

@Component
class SudoSwapEventConverter(
    private val traceCallService: TraceCallService,
) {
    fun getPoolHash(pollAddress: Address): Word {
        return Word.apply(EMPTY_PREFIX_BINARY.add(pollAddress))
    }

    suspend fun getCreatePairDetails(poolAddress: Address, transient: Transaction): List<SudoSwapPairDetail> {
        val inputs = findAllRequiredCalls(
            transient,
            poolAddress,
            LSSVMPairFactoryV1.createPairETHSignature().id(),
            LSSVMPairFactoryV1.createPairERC20Signature().id()
        )
        return inputs.mapNotNull {
            when (it.input.methodSignatureId()) {
                LSSVMPairFactoryV1.createPairETHSignature().id() -> {
                    val decoded = LSSVMPairFactoryV1.createPairETHSignature().`in`().decode(it.input, 4)
                    SudoSwapEthPairDetail(
                        nft = decoded.value()._1(),
                        bondingCurve = decoded.value()._2(),
                        assetRecipient = decoded.value()._3(),
                        poolType = convert(decoded.value()._4()),
                        delta = decoded.value()._5(),
                        fee = decoded.value()._6(),
                        spotPrice = decoded.value()._7(),
                        inNft = decoded.value()._8().toList(),
                        ethBalance = it.value ?: transient.value(),
                    )
                }
                LSSVMPairFactoryV1.createPairERC20Signature().id() -> {
                    val decoded = LSSVMPairFactoryV1.createPairERC20Signature().`in`().decode(it.input, 4)
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

    suspend fun getSwapOutNftDetails(poolAddress: Address, transient: Transaction): List<SudoSwapOutNftDetail> {
        val inputs = findAllRequiredCalls(
            transient,
            poolAddress,
            LSSVMPairV1.swapTokenForSpecificNFTsSignature().id(),
            LSSVMPairV1.swapTokenForAnyNFTsSignature().id()
        )
        return inputs.mapNotNull {
            val outputValue = it.output?.toBigInteger()?.let { output -> EthUInt256.of(output) } ?: EthUInt256.ZERO
            when (it.input.methodSignatureId()) {
                LSSVMPairV1.swapTokenForAnyNFTsSignature().id() -> {
                    val decoded = LSSVMPairV1.swapTokenForAnyNFTsSignature().`in`().decode(it.input, 4)
                    SudoSwapAnyOutNftDetail(
                        numberNft = decoded.value()._1(),
                        maxExpectedTokenInput = decoded.value()._2(),
                        nftRecipient = decoded.value()._3(),
                        outputValue = outputValue,
                    )
                }
                LSSVMPairV1.swapTokenForSpecificNFTsSignature().id() -> {
                    val decoded = LSSVMPairV1.swapTokenForSpecificNFTsSignature().`in`().decode(it.input, 4)
                    SudoSwapTargetOutNftDetail(
                        nft = decoded.value()._1().toList(),
                        maxExpectedTokenInput = decoded.value()._2(),
                        nftRecipient = decoded.value()._3(),
                        outputValue = outputValue,
                    )
                }
                else -> null
            }
        }
    }

    suspend fun getSwapInNftDetails(poolAddress: Address, transient: Transaction): List<SudoSwapTargetInNftDetail> {
        val inputs = findAllRequiredCalls(
            transient,
            poolAddress,
            LSSVMPairV1.swapNFTsForTokenSignature().id()
        )
        return inputs.map {
            val decoded = LSSVMPairV1.swapNFTsForTokenSignature().`in`().decode(it.input, 4)
            SudoSwapTargetInNftDetail(
                tokenIds = decoded.value()._1().toList(),
                minExpectedTokenOutput = decoded.value()._2(),
                tokenRecipient = decoded.value()._3(),
            )
        }
    }

    suspend fun getNftWithdrawDetails(poolAddress: Address, transient: Transaction): List<SudoSwapNftWithdrawDetail> {
        val inputs = findAllRequiredCalls(
            transient,
            poolAddress,
            LSSVMPairV1.withdrawERC721Signature().id()
        )
        return inputs.map {
            val decoded = LSSVMPairV1.withdrawERC721Signature().`in`().decode(it.input, 4)
            SudoSwapNftWithdrawDetail(
                collection = decoded.value()._1(),
                nft = decoded.value()._2().toList()
            )
        }
    }

    suspend fun getNftDepositDetails(pairFactory: Address, transient: Transaction): List<SudoSwapNftDepositDetail> {
        val inputs = findAllRequiredCalls(
            transient,
            pairFactory,
            LSSVMPairFactoryV1.depositNFTsSignature().id()
        )
        return inputs.map {
            val decoded = LSSVMPairFactoryV1.depositNFTsSignature().`in`().decode(it.input, 4)
            SudoSwapNftDepositDetail(
                collection = decoded.value()._1(),
                tokenIds = decoded.value()._2().toList(),
                pollAddress = decoded.value()._3()
            )
        }
    }

    private suspend fun findAllRequiredCalls(
        transient: Transaction,
        to: Address,
        vararg ids: Binary
    ): List<SimpleTraceResult> {
        return traceCallService.findAllRequiredCalls(
            headTransaction = HeadTransaction.from(transient),
            to = to,
            ids = ids
        )
    }

    private fun convert(value: BigInteger): SudoSwapPoolType {
        return when (value.intValueExact()) {
            0 -> SudoSwapPoolType.TOKEN
            1 -> SudoSwapPoolType.NFT
            2 -> SudoSwapPoolType.TRADE
            else -> throw IllegalArgumentException("Unrecognized pool type $value")
        }
    }

    private companion object {
        val EMPTY_PREFIX_BINARY: Binary = Binary.apply(ByteArray(12))
    }
}

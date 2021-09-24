package com.rarible.protocol.order.core.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.contracts.exchange.wyvern.WyvernExchange
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.converters.model.PartConverter
import com.rarible.protocol.order.core.misc.fixV
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.misc.toSignatureData
import com.rarible.protocol.order.core.misc.toTuple
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scala.*
import scalether.domain.Address
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

@Service
class PrepareTxService(
    private val transferProxyService: TransferProxyService,
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val eip712Domain: EIP712Domain,
    private val erc1271SignService: ERC1271SignService,
    private val orderInvertService: OrderInvertService,
    orderIndexerProperties: OrderIndexerProperties
) {
    private val privateKey = Numeric.toBigInt(orderIndexerProperties.operatorPrivateKey.bytes())
    private val protocolCommission = orderIndexerProperties.protocolCommission

    suspend fun prepareTransaction(order: Order, form: PrepareOrderTxFormDto): PrepareTxResponse {
        logger.info("prepareTransaction ${order.hash} $form bid: ${order.isBid()}")
        return when (order.type) {
            OrderType.RARIBLE_V1 -> {
                prepareTxForV1(form, order)
            }
            OrderType.RARIBLE_V2 -> {
                prepareTxForV2(order, form)
            }
            OrderType.OPEN_SEA_V1 -> {
                prepareTxForOpenSeaV1(order, form)
            }
            OrderType.CRYPTO_PUNKS -> {
                prepareTxForCryptoPunk(order, form)
            }
        }
    }

    fun prepareCancelTransaction(order: Order): PreparedTx {
        return when (order.type) {
            OrderType.RARIBLE_V1 -> {
                prepareCancelTxDataV1(order)
            }
            OrderType.RARIBLE_V2 -> {
                prepareCancelTxDataV2(order)
            }
            OrderType.OPEN_SEA_V1 -> {
                prepareCancelTxForOpenSeaV1(order)
            }
            OrderType.CRYPTO_PUNKS -> {
                prepareCancelTxForCryptoPunk(order)
            }
        }
    }

    fun prepareBuyerFeeSignature(order: Order, fee: Int): Sign.SignatureData {
        val buyerFeeMsg = MessageHelper.prepareBuyerFeeExchangeMessage(order, fee)
        return SignUtils.sign(privateKey, buyerFeeMsg)
    }

    private fun prepareTxForV1(
        form: PrepareOrderTxFormDto,
        order: Order
    ): PrepareTxResponse {
        val fee = form.originFees.map { it.value }.sum()
        val orderRight = order.invert(form.maker, form.amount)
        logger.info("inverted order: $orderRight")
        val buyerFeeSignature = prepareBuyerFeeSignature(order, fee)
        val data = ExchangeV1.exchangeSignature().encode(
            Tuple6(
                order.forV1Tx(),
                order.signature!!.toSignatureData().fixV().toTuple(),
                fee.toBigInteger(),
                buyerFeeSignature.toTuple(),
                orderRight.take.value.value,
                form.payouts.firstOrNull()?.account ?: Address.ZERO()
            )
        )
        val makeAsset = order.take.copy(value = EthUInt256(orderRight.make.value.value))
        val asset: Asset = if (makeAsset.type is EthAssetType || makeAsset.type is Erc20AssetType) {
            val value = makeAsset.value.value
            makeAsset.copy(value = EthUInt256(value + value.multiply(fee.toBigInteger()).divide(10000.toBigInteger())))
        } else {
            makeAsset
        }
        logger.info("asset: $asset")
        return PrepareTxResponse(
            transferProxyService.getTransferProxy(asset.type),
            asset,
            PreparedTx(
                to = exchangeContractAddresses.v1,
                data = data
            )
        )
    }

    private suspend fun prepareTxForV2(
        order: Order,
        form: PrepareOrderTxFormDto
    ): PrepareTxResponse {
        val orderRight = order.invert(form.maker, form.amount)
            .copy(data = OrderRaribleV2DataV1(form.payouts.toPartList(), form.originFees.toPartList()))
        return prepareTxFor2Orders(order, orderRight)
    }

    private fun prepareTxForOpenSeaV1(
        order: Order,
        form: PrepareOrderTxFormDto
    ): PrepareTxResponse {
        if (form.payouts.isNotEmpty() && form.payouts != listOf(PartDto(form.maker, 10000))) {
            throw IllegalArgumentException("payouts not supported by OpenSea orders")
        }
        val bytes = ByteArray(32)
        ThreadLocalRandom.current().nextBytes(bytes)
        val newSalt = Word.apply(bytes)

        val originFees = form.originFees.map { PartConverter.convert(it) }
        val invertedOrder = orderInvertService.invert(order, form.maker, form.amount, newSalt, originFees)

        val data = order.data as OrderOpenSeaV1DataV1
        val invertedData = invertedOrder.data as OrderOpenSeaV1DataV1

        val signature = order.signature?.toSignatureData() ?: EMPTY_SIGNATURE

        val (buyOrder, sellOrder) = when (data.side) {
            OpenSeaOrderSide.SELL -> invertedOrder to order
            OpenSeaOrderSide.BUY -> order to invertedOrder
        }
        val (buyData, sellData) = when (data.side) {
            OpenSeaOrderSide.SELL -> invertedData to data
            OpenSeaOrderSide.BUY -> data to invertedData
        }

        val inputData = WyvernExchange.atomicMatch_Signature().encode(
            Tuple11(
                arrayOf(
                    buyData.exchange,
                    buyOrder.maker,
                    buyOrder.taker ?: Address.ZERO(),
                    buyData.feeRecipient,
                    buyOrder.take.type.token,
                    buyData.staticTarget,
                    buyOrder.make.type.token,
                    sellData.exchange,
                    sellOrder.maker,
                    sellOrder.taker ?: Address.ZERO(),
                    sellData.feeRecipient,
                    sellOrder.make.type.token,
                    sellData.staticTarget,
                    sellOrder.take.type.token
                ),
                arrayOf(
                    buyData.makerRelayerFee,
                    buyData.takerRelayerFee,
                    buyData.makerProtocolFee,
                    buyData.takerProtocolFee,
                    buyOrder.make.value.value,
                    buyData.extra,
                    buyOrder.start?.toBigInteger() ?: BigInteger.valueOf(0),
                    buyOrder.end?.toBigInteger() ?: BigInteger.valueOf(0),
                    buyOrder.salt.value,
                    sellData.makerRelayerFee,
                    sellData.takerRelayerFee,
                    sellData.makerProtocolFee,
                    sellData.takerProtocolFee,
                    sellOrder.take.value.value,
                    sellData.extra,
                    sellOrder.start?.toBigInteger() ?: BigInteger.valueOf(0),
                    sellOrder.end?.toBigInteger() ?: BigInteger.valueOf(0),
                    sellOrder.salt.value
                ),
                arrayOf(
                    buyData.feeMethod.value,
                    buyData.side.value,
                    buyData.saleKind.value,
                    buyData.howToCall.value,
                    sellData.feeMethod.value,
                    sellData.side.value,
                    sellData.saleKind.value,
                    sellData.howToCall.value
                ),
                buyData.callData.bytes(),
                sellData.callData.bytes(),
                buyData.replacementPattern.bytes(),
                sellData.replacementPattern.bytes(),
                buyData.staticExtraData.bytes(),
                sellData.staticExtraData.bytes(),
                arrayOf(
                    BigInteger(byteArrayOf(signature.v)),
                    BigInteger(byteArrayOf(signature.v))
                ),
                arrayOf(
                    signature.r,
                    signature.s,
                    signature.r,
                    signature.s,
                    RARIBLE_PLATFORM_METADATA
                )
            )
        )
        return PrepareTxResponse(
            null,
            invertedOrder.make,
            PreparedTx(
                to = exchangeContractAddresses.openSeaV1,
                data = inputData
            )
        )
    }

    private fun prepareTxForCryptoPunk(
        order: Order,
        form: PrepareOrderTxFormDto
    ): PrepareTxResponse {
        check(form.amount == BigInteger.ONE)
        val encoded = if (order.make.type is CryptoPunksAssetType) {
            check(order.take.type is EthAssetType)
            // order = sell order, form = buy order.
            CryptoPunksMarket.buyPunkSignature().encode(order.make.type.tokenId.value)
        } else {
            // order = bid order, form = accept bid (sell) order.
            check(order.make.type is EthAssetType)
            check(order.take.type is CryptoPunksAssetType)
            CryptoPunksMarket.acceptBidForPunkSignature().encode(Tuple2(order.take.type.tokenId.value, order.make.value.value))
        }
        // Hack: add platform ID to the encoded transaction input data.
        val withPlatform = encoded.add(Platform.CRYPTO_PUNKS.id)
        return PrepareTxResponse(
            null,
            order.take,
            PreparedTx(exchangeContractAddresses.cryptoPunks, withPlatform)
        )
    }


    fun prepareCancelTxForOpenSeaV1(order: Order): PreparedTx {
        val data = order.data as OrderOpenSeaV1DataV1
        val signature = order.signature?.toSignatureData() ?: EMPTY_SIGNATURE

        val (nftAsset, paymentAsset) = when (data.side) {
            OpenSeaOrderSide.SELL -> order.make to order.take
            OpenSeaOrderSide.BUY -> order.take to order.make
        }
        val inputData = WyvernExchange.cancelOrder_Signature().encode(
            Tuple12(
                arrayOf(
                    data.exchange,
                    order.maker,
                    order.taker ?: Address.ZERO(),
                    data.feeRecipient,
                    nftAsset.type.token,
                    data.staticTarget,
                    paymentAsset.type.token
                ),
                arrayOf(
                    data.makerRelayerFee,
                    data.takerRelayerFee,
                    data.makerProtocolFee,
                    data.takerProtocolFee,
                    paymentAsset.value.value,
                    data.extra,
                    order.start?.toBigInteger() ?: BigInteger.valueOf(0),
                    order.end?.toBigInteger() ?: BigInteger.valueOf(0),
                    order.salt.value
                ),
                data.feeMethod.value,
                data.side.value,
                data.saleKind.value,
                data.howToCall.value,
                data.callData.bytes(),
                data.replacementPattern.bytes(),
                data.staticExtraData.bytes(),
                BigInteger(byteArrayOf(signature.v)),
                signature.r,
                signature.s
            )
        )
        return PreparedTx(
            exchangeContractAddresses.openSeaV1,
            inputData
        )
    }

    private fun prepareCancelTxForCryptoPunk(order: Order): PreparedTx {
        val encoded = if (order.make.type is CryptoPunksAssetType) {
            check(order.take.type is EthAssetType)
            // order = sell order
            CryptoPunksMarket.punkNoLongerForSaleSignature().encode(order.make.type.tokenId.value)
        } else {
            // order = bid order
            check(order.make.type is EthAssetType)
            check(order.take.type is CryptoPunksAssetType)
            CryptoPunksMarket.withdrawBidForPunkSignature().encode(order.take.type.tokenId.value)
        }
        return PreparedTx(exchangeContractAddresses.cryptoPunks, encoded)
    }

    suspend fun prepareTxFor2Orders(
        order: Order,
        orderRight: Order
    ): PrepareTxResponse {
        val fee = (orderRight.data as OrderRaribleV2DataV1).originFees.map { it.value.value.toInt() }.sum() + protocolCommission
        logger.info("inverted order: $orderRight")

        val data = ExchangeV2.matchOrdersSignature().encode(
            Tuple4(
                order.forTxFix(),
                order.signature.forTx(),
                orderRight.forTx(),
                orderRight.signature.forTx()
            )
        )
        val makeAsset = orderRight.make
        val asset: Asset = if (makeAsset.type is EthAssetType || makeAsset.type is Erc20AssetType) {
            val value = makeAsset.value.value
            makeAsset.copy(value = EthUInt256(value + value.multiply(fee.toBigInteger()).divide(10000.toBigInteger())))
        } else {
            makeAsset
        }
        logger.info("asset: $asset")
        return PrepareTxResponse(
            transferProxyService.getTransferProxy(asset.type),
            asset,
            PreparedTx(
                to = exchangeContractAddresses.v2,
                data = data
            )
        )
    }

    private fun Binary?.forTx(): ByteArray =
        this?.fixSignatureV()?.bytes() ?: ByteArray(0)

    private suspend fun Order.forTxFix() = run {
        val hash = eip712Domain.hashToSign(Order.hash(this))
        val wrong = erc1271SignService.isSigner(maker, hash, signature!!).not()
        logger.info("order is wrong: $wrong hash: $hash")
        forTx(wrong)
    }

    private fun prepareCancelTxDataV1(order: Order): PreparedTx {
        val orderKey = order.forV1Tx()._1()
        return PreparedTx(exchangeContractAddresses.v1, ExchangeV1.cancelSignature().encode(orderKey))
    }

    private fun prepareCancelTxDataV2(order: Order): PreparedTx {
        val orderKey = order.forTx()
        return PreparedTx(exchangeContractAddresses.v2, ExchangeV2.cancelSignature().encode(orderKey))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PrepareTxService::class.java)
        val EMPTY_SIGNATURE = Sign.SignatureData(0, ByteArray(32), ByteArray(32))
        val RARIBLE_PLATFORM_METADATA: ByteArray = Platform.RARIBLE.id.bytes()
    }
}

private fun Binary.fixSignatureV(): Binary = toSignatureData().fixV().toBinary()
private fun List<PartDto>.toPartList() = map { Part(it.account, EthUInt256.of(it.value.toLong())) }

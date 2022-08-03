package com.rarible.protocol.order.api.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.dto.LegacyOrderFormDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.RaribleV2OrderFormDto
import com.rarible.protocol.order.api.data.createOrder
import com.rarible.protocol.order.api.data.sign
import com.rarible.protocol.order.api.data.toForm
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.service.order.validation.OrderSignatureValidator
import com.rarible.protocol.order.core.converters.model.AssetConverter
import com.rarible.protocol.order.core.converters.model.OrderDataConverter
import com.rarible.protocol.order.core.converters.model.OrderTypeConverter
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.PrepareTxService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import javax.annotation.PostConstruct

abstract class AbstractOrderIt : AbstractIntegrationTest() {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    protected lateinit var orderSignatureValidator: OrderSignatureValidator
    @Autowired
    protected lateinit var prepareTxService: PrepareTxService

    protected lateinit var eip712Domain: EIP712Domain

    fun createOrder(maker: Address) = createOrder(
        maker,
        Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
    )

    fun createOrder(maker: Address, start: Long?, end: Long?) = createOrder(
        maker = maker,
        make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
        start = start,
        end = end
    )

    fun createOpenSeaOrder(maker: Address) = createOpenSeaOrder(
        maker,
        Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
    )

    fun orderCryptoPunksData(maker: Address) = orderCryptoPunksData(
        maker,
        Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
    )

    fun orderX2Y2Data(maker: Address) = orderX2Y2Data(
        maker,
        Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
    )

    fun orderLooksrareData(maker: Address) = orderLooksrareData(
        maker,
        Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
    )

    fun createOrder(maker: Address, make: Asset) = createOrder(
        maker = maker,
        taker = null,
        make = make
    )

    fun createOpenSeaOrder(maker: Address, make: Asset) = Order(
        maker = maker,
        taker = null,
        make = make,
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
        makeStock = make.value,
        type = OrderType.OPEN_SEA_V1,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderOpenSeaV1DataV1(
            exchange = AddressFactory.create(),
            makerRelayerFee = BigInteger.TEN,
            takerRelayerFee = BigInteger.ZERO,
            makerProtocolFee = BigInteger.ZERO,
            takerProtocolFee = BigInteger.ZERO,
            feeRecipient = Address.ZERO(),
            feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
            side = OpenSeaOrderSide.SELL,
            saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
            howToCall = OpenSeaOrderHowToCall.CALL,
            callData = Binary.apply(),
            replacementPattern = Binary.apply(),
            staticTarget = AddressFactory.create(),
            staticExtraData = Binary.apply(),
            extra = BigInteger.ZERO,
            target = null,
            nonce = null,
        ),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        platform = Platform.OPEN_SEA
    )

    fun orderCryptoPunksData(maker: Address, make: Asset) = Order(
        maker = maker,
        taker = null,
        make = make,
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
        makeStock = make.value,
        type = OrderType.CRYPTO_PUNKS,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderCryptoPunksData,
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        platform = Platform.CRYPTO_PUNKS
    )

    fun orderX2Y2Data(maker: Address, make: Asset) = Order(
        maker = maker,
        taker = null,
        make = make,
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
        makeStock = make.value,
        type = OrderType.X2Y2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderX2Y2DataV1(
            itemHash = Word.apply(randomWord()),
            isCollectionOffer = randomBoolean(),
            isBundle = randomBoolean(),
            side = randomInt(),
            orderId = randomBigInt()
        ),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        platform = Platform.X2Y2
    )

    fun orderLooksrareData(maker: Address, make: Asset) = Order(
        maker = maker,
        taker = null,
        make = make,
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
        makeStock = make.value,
        type = OrderType.LOOKSRARE,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderLooksrareDataV1(
            minPercentageToAsk = randomInt(),
            strategy = randomAddress(),
            nonce = randomLong(),
            params = randomBinary()
        ),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        platform = Platform.LOOKSRARE
    )

    @PostConstruct
    fun initialize() {
        setV2Address(AddressFactory.create())
    }

    protected fun setV2Address(address: Address) {
        eip712Domain = EIP712Domain(
            name = "Exchange",
            version = "2",
            chainId = BigInteger.valueOf(17),
            verifyingContract = address
        )
        orderSignatureValidator.eip712Domain = eip712Domain
        prepareTxService.eip712Domain = eip712Domain
    }

    fun Order.toForm(privateKey: BigInteger) = toForm(eip712Domain, privateKey)

    fun OrderFormDto.withSignature(signature: Binary): OrderFormDto {
        return when (this) {
            is LegacyOrderFormDto -> copy(signature = signature)
            is RaribleV2OrderFormDto -> copy(signature = signature)
        }
    }

    fun OrderFormDto.sign(privateKey: BigInteger): OrderFormDto {
        val type = OrderTypeConverter.convert(this)
        val data = when (this) {
            is LegacyOrderFormDto -> data
            is RaribleV2OrderFormDto -> data
        }
        val hash = Order.hash(
            maker,
            AssetConverter.convert(make),
            taker,
            AssetConverter.convert(take),
            salt,
            start,
            end,
            OrderDataConverter.convert(data),
            type
        )
        return when (this) {
            is LegacyOrderFormDto -> copy(signature = CommonSigner().hashToSign(hash).sign(privateKey))
            is RaribleV2OrderFormDto -> copy(signature = eip712Domain.hashToSign(hash).sign(privateKey))
        }
    }
}

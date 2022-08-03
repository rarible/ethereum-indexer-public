package com.rarible.protocol.order.api.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.dto.LegacyOrderFormDto
import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.OrderRaribleV2DataDto
import com.rarible.protocol.dto.RaribleV2OrderFormDto
import com.rarible.protocol.order.core.converters.dto.OrderDataDtoConverter
import com.rarible.protocol.order.core.converters.dto.OrderFormAssetDtoConverter
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Order.Companion.legacyMessage
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.service.CommonSigner
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createOrder(
    maker: Address = AddressFactory.create(),
    taker: Address? = AddressFactory.create(),
    make: Asset = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
    take: Asset = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
    start: Long? = null,
    end: Long? = null
): Order {
    return Order(
        maker = maker,
        taker = taker,
        make = make,
        take = take,
        makeStock = make.value,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = start,
        end = end,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis()
    )
}

fun Order.toForm(eip712Domain: EIP712Domain, privateKey: BigInteger): OrderFormDto {
    return when (type) {
        OrderType.RARIBLE_V2 -> RaribleV2OrderFormDto(
            maker = maker,
            make = OrderFormAssetDtoConverter.convert(make),
            taker = taker,
            take = OrderFormAssetDtoConverter.convert(take),
            salt = salt.value,
            data = OrderDataDtoConverter.convert(data) as OrderRaribleV2DataDto,
            start = start,
            end = end,
            signature = eip712Domain.hashToSign(Order.hash(this)).sign(privateKey)
        )
        OrderType.RARIBLE_V1 -> LegacyOrderFormDto(
            maker = maker,
            make = OrderFormAssetDtoConverter.convert(make),
            taker = taker,
            take = OrderFormAssetDtoConverter.convert(take),
            salt = salt.value,
            data = OrderDataDtoConverter.convert(data) as OrderDataLegacyDto,
            start = start,
            end = end,
            signature = CommonSigner().hashToSign(legacyMessage()).sign(privateKey)
        )
        OrderType.OPEN_SEA_V1,
        OrderType.SEAPORT_V1,
        OrderType.CRYPTO_PUNKS,
        OrderType.X2Y2,
        OrderType.LOOKSRARE -> throw IllegalArgumentException("$type order can't be created or updated")
    }
}

fun Word.sign(privateKey: BigInteger): Binary {
    val publicKey = Sign.publicKeyFromPrivate(privateKey)
    return Sign.signMessageHash(bytes(), publicKey, privateKey).toBinary()
}

fun generateNewKeys(): Triple<BigInteger, BigInteger, Address> {
    val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
    val publicKey = Sign.publicKeyFromPrivate(privateKey)
    val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
    return Triple(privateKey, publicKey, signer)
}

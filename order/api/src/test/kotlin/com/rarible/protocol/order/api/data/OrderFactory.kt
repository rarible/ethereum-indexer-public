package com.rarible.protocol.order.api.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.converters.dto.OrderDataDtoConverter
import com.rarible.protocol.order.core.converters.dto.OrderFormAssetDtoConverter
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.model.Order.Companion.legacyMessage
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
    start: Long? = null,
    end: Long? = null
): Order {
    return Order(
        maker = maker,
        taker = taker,
        make = make,
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
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
            data = OrderDataDtoConverter.convert(data) as OrderRaribleV2DataV1Dto,
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
        OrderType.OPEN_SEA_V1 -> throw IllegalArgumentException("OpenSea order can't be crerated or updated")
        OrderType.CRYPTO_PUNKS -> throw IllegalArgumentException("CryptoPunks orders are created on-chain")
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

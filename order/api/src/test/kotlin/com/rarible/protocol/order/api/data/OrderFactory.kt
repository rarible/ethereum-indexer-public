package com.rarible.protocol.order.api.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.dto.OrderDataDtoConverter
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createOrder(): Order {
    return Order(
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
        makeStock = EthUInt256.TEN,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
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
            make = AssetDtoConverter.convert(make),
            taker = null,
            take = AssetDtoConverter.convert(take),
            salt = salt.value,
            data = OrderDataDtoConverter.convert(data) as OrderRaribleV2DataV1Dto,
            start = null,
            end = null,
            signature = eip712Domain.hashToSign(Order.hash(this)).sign(privateKey)
        )
        OrderType.RARIBLE_V1 -> LegacyOrderFormDto(
            maker = maker,
            make = AssetDtoConverter.convert(make),
            taker = null,
            take = AssetDtoConverter.convert(take),
            salt = salt.value,
            data = OrderDataDtoConverter.convert(data) as OrderDataLegacyDto,
            start = null,
            end = null,
            signature = CommonSigner().hashToSign(Order.legacyMessage(maker, make, take, salt.value, data)).sign(privateKey)
        )
        OrderType.OPEN_SEA_V1 -> OpenSeaV1OrderFormDto(
            maker = maker,
            make = AssetDtoConverter.convert(make),
            taker = null,
            take = AssetDtoConverter.convert(take),
            salt = salt.value,
            data = OrderDataDtoConverter.convert(data) as OrderOpenSeaV1DataV1Dto,
            start = null,
            end = null,
            signature = null
        )
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

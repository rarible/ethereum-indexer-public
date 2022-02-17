package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.service.CommonSigner
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

internal class OpenSeaOrderTest {
    private val commonSigner = CommonSigner()

    @Test
    fun `should calculate hash`() {
        val openSeaOrder = OpenSeaTransactionOrder(
            exchange = Address.apply("0x5206e78b21ce315ce284fb24cf05e0585a93b1d9"),
            maker = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            taker = Address.apply("0x0000000000000000000000000000000000000000"),
            makerRelayerFee = BigInteger.valueOf(1250),
            takerRelayerFee = BigInteger.valueOf(0),
            makerProtocolFee = BigInteger.valueOf(0),
            takerProtocolFee = BigInteger.valueOf(0),
            feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
            feeMethod = OpenSeaOrderFeeMethod.fromBigInteger(BigInteger.valueOf(1)),
            side = OpenSeaOrderSide.fromBigInteger(BigInteger.valueOf(1)),
            saleKind = OpenSeaOrderSaleKind.fromBigInteger(BigInteger.valueOf(0)),
            target = Address.apply("0x4a6a5703a9796630e9fa04f5ecaf730065a7b827"),
            howToCall = OpenSeaOrderHowToCall.fromBigInteger(BigInteger.valueOf(0)),
            callData = Binary.apply("0x23b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a"),
            replacementPattern = Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000"),
            staticTarget = Address.apply("0x0000000000000000000000000000000000000000"),
            staticExtraData = Binary.apply("0x"),
            paymentToken = Address.apply("0x0000000000000000000000000000000000000000"),
            basePrice = BigInteger.valueOf(10000000000000000),
            extra = BigInteger.valueOf(0),
            listingTime = BigInteger.valueOf(1627563829),
            expirationTime = BigInteger.valueOf(0),
            salt = BigInteger("55652726740606487390401878629540348216828293896576543584531238490014347709943")
        )

        val hash = openSeaOrder.hash
        val hashToSign = keccak256(Binary.apply("\u0019Ethereum Signed Message:\n32".toByteArray()).add(hash))

        assertThat(hashToSign)
            .isEqualTo(Word.apply("0x666239c3d0d6cd5d12abbf3db2c7b2775cc74c4a37784df043db0cf45ffdf794"))
            .withFailMessage("Hash to Sign not matched")

        assertThat(hash)
            .isEqualTo(Word.apply("0x0a3e8d8ecd4ce553b998f62830fdf9d266208f6fec6c06d264fe5879f6aa20a7"))
            .withFailMessage("Hash not matched")
    }

    @Test
    fun `should calculate order hash`() {
        val buyOrder = Order(
            maker = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            taker = null,
            make = Asset(
                Erc20AssetType(
                    Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")
                ),
                EthUInt256.of(13000000000000000)),
            take = Asset(
                Erc721AssetType(
                    Address.apply("0x509fd4cdaa29be7b1fad251d8ea0fca2ca91eb60"),
                    EthUInt256.of(110711)
                ),
                EthUInt256.ONE
            ),
            makeStock = EthUInt256.TEN,
            type = OrderType.OPEN_SEA_V1,
            fill = EthUInt256.ZERO,
            cancelled = false,
            salt = EthUInt256.of(BigInteger("81538619411536663679971542969406122025226616498230290046022479480700489875715")),
            start = 1628140271,
            end = 1628745154,
            data = OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x5206e78b21ce315ce284fb24cf05e0585a93b1d9"),
                makerRelayerFee = BigInteger.ZERO,
                takerRelayerFee = BigInteger.valueOf(250),
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.BUY,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.CALL,
                callData = Binary.apply("0x23b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001b077"),
                replacementPattern = Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = null
            ),
            signature = Binary
                .apply("0x795def388ba0e82cf711448a6a36f64868d340b53a2f5277e9fc37651a156007")
                .add(Binary.apply("0x4c045da9e384ce70007ee08aa7602a1808a873d35ac2561f343ec1ab1d80ae4f"))
                .add(Binary.apply(byteArrayOf(28))),
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )
        val hash = Order.hash(buyOrder)
        val hashToSign = commonSigner.openSeaHashToSign(hash)

        assertThat(hash).isEqualTo(Word.apply("0x2fccca13e00c99fec6f6191054746ce11170c560acdf12dde088edd87a02eee5"))
        assertThat(hashToSign).isEqualTo(Word.apply("0x184cdfac6781cb507c4626f05378110da85e758299f3c12e4e0b24b8d684d58d"))
    }
}

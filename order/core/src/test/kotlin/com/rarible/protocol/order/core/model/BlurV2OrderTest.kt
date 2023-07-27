package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

class BlurV2OrderTest {
    @Test
    fun `calculate hash`() {
        // https://etherscan.io/tx/0xc199fd412393246039e22dcd70a8231bad64acee868b4e08349f9ec6509284d5#eventlog
        val order = BlurV2Order(
            trader = Address.apply("0x83fa03a1747aafe4e0f0ec3880740f49d74f1158"),
            collection = Address.apply("0x789e35a999c443fe6089544056f728239b8ffee7"),
            listingsRoot = Binary.apply("0xfe5511c35e70a0393b16a2240106f5e64b2b05580511231fda3ca216b9690cb1"),
            numberOfListings = BigInteger("1"),
            expirationTime = BigInteger("1690353958"),
            assetType = BlurV2AssetType.ERC721,
            makerFee = BlurV2FeeRate(
                Address.apply("0x0000000000000000000000000000000000000000"),
                BigInteger("0")
            ),
            salt = BigInteger("311795734093063913361629732513981094064")
        )

        val hash = Order.blurV2Hash(
            order = order,
            type = BlurV2OrderType.ASK,
            nonce = BigInteger("0")
        )
        assertThat(hash).isEqualTo(Word.apply("0x21b7d4ae06f010118c4c4e976182504160a6ad111f471ea8df38ad045ff7fa40"))
    }
}

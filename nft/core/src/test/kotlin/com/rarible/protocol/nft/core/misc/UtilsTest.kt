package com.rarible.protocol.nft.core.misc

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

class UtilsTest {

    @Test
    fun `to address list`() {
        val address1 = randomAddress()
        val address2 = randomAddress()
        val addresses = setOf(address1, address2)

        assertThat(toAddressSet("$address1,$address2")).isEqualTo(addresses)
        assertThat(toAddressSet(" $address1 , $address2 ")).isEqualTo(addresses)
        assertThat(toAddressSet("$address1")).isEqualTo(setOf(address1))
        assertThat(toAddressSet("")).hasSize(0)
        assertThat(toAddressSet(" ")).hasSize(0)
        assertThat(toAddressSet(" , , ")).hasSize(0)
        assertThat(toAddressSet(null)).hasSize(0)
    }

    @Test
    fun `split to ranges - tokens`() {
        val from = Address.ZERO()
        val to = Address.apply("0xffffffffffffffffffffffffffffffffffffffff")

        val range = splitToRanges(from, to, 4)

        assertThat(range[0]).isEqualTo(from)
        assertThat(range[1]).isEqualTo(Address.apply("0x3fffffffffffffffffffffffffffffffffffffff"))
        assertThat(range[2]).isEqualTo(Address.apply("0x7ffffffffffffffffffffffffffffffffffffffe"))
        assertThat(range[3]).isEqualTo(Address.apply("0xbffffffffffffffffffffffffffffffffffffffd"))
        assertThat(range[4]).isEqualTo(to)
    }

    @Test
    fun `split to ranges - tokens, single step`() {
        val from = Address.ZERO()
        val to = Address.THREE()

        val range = splitToRanges(from, to, 3)

        assertThat(range[0]).isEqualTo(from)
        assertThat(range[1]).isEqualTo(Address.ONE())
        assertThat(range[2]).isEqualTo(Address.TWO())
        assertThat(range[3]).isEqualTo(to)
    }

    @Test
    fun `should split to ranges, single token`() {
        val from = Address.THREE()
        val to = Address.THREE()

        val range = splitToRanges(from, to, 3)

        assertThat(range).containsExactly(from)
    }

    @Test
    fun `split to ranges - tokens, single`() {
        val from = Address.ZERO()
        val to = Address.apply("0xffffffffffffffffffffffffffffffffffffffff")

        val range = splitToRanges(from, to, 1)

        assertThat(range[0]).isEqualTo(from)
        assertThat(range[1]).isEqualTo(to)
    }

    @Test
    fun `split to ranges - tokens, incorrect`() {
        assertThatCode {
            splitToRanges(Address.ZERO(), Address.FOUR(), 0)
        }.isExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `split to ranges - item ids`() {
        val from = Address.ONE()
        val fromTokenId = randomBigInt()

        val to = Address.FOUR()
        val toTokenId = randomBigInt()

        val fromItemId = ItemId(from, EthUInt256(fromTokenId))
        val toItemId = ItemId(to, EthUInt256(toTokenId))
        val itemId1 = ItemId(Address.TWO(), EthUInt256(BigInteger.ZERO))
        val itemId2 = ItemId(Address.THREE(), EthUInt256(BigInteger.ZERO))

        val ranges = splitToRanges(fromItemId, toItemId, 3)

        assertThat(ranges[0]).isEqualTo(fromItemId to itemId1)
        assertThat(ranges[1]).isEqualTo(itemId1 to itemId2)
        assertThat(ranges[2]).isEqualTo(itemId2 to toItemId)
    }
}

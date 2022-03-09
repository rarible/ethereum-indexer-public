package com.rarible.protocol.nft.core.misc

import com.rarible.core.test.data.randomAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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

}
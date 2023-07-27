package com.rarible.protocol.erc20.core.admin

import com.rarible.core.test.data.randomAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Erc20ReduceTaskParamTest {

    private val token = randomAddress()
    private val owner = randomAddress()

    @Test
    fun `to string - ok`() {
        assertThat(Erc20ReduceTaskParam(token, owner).toString()).isEqualTo("${token.hex()}:${owner.hex()}")
        assertThat(Erc20ReduceTaskParam(token, null).toString()).isEqualTo(token.hex())
        assertThat(Erc20ReduceTaskParam(null, null).toString()).isEqualTo("")
    }

    @Test
    fun `to string - invalid data`() {
        assertThrows(IllegalArgumentException::class.java) {
            Erc20ReduceTaskParam(null, owner).toString()
        }
    }

    @Test
    fun `from string - ok`() {
        assertThat(Erc20ReduceTaskParam.fromString("${token.hex()}:${owner.hex()}"))
            .isEqualTo(Erc20ReduceTaskParam(token, owner))

        assertThat(Erc20ReduceTaskParam.fromString(token.hex()))
            .isEqualTo(Erc20ReduceTaskParam(token, null))

        assertThat(Erc20ReduceTaskParam.fromString(""))
            .isEqualTo(Erc20ReduceTaskParam(null, null))
    }

    @Test
    fun `is overlapped - ok`() {
        val token1andOwner1 = Erc20ReduceTaskParam(token, owner)
        val token2andOwner1 = Erc20ReduceTaskParam(randomAddress(), owner)
        val token1andOwner2 = Erc20ReduceTaskParam(token, randomAddress())
        val token1 = Erc20ReduceTaskParam(token, null)
        val token3 = Erc20ReduceTaskParam(randomAddress(), null)
        val full = Erc20ReduceTaskParam()

        // Same balance
        assertThat(token1andOwner1.isOverlapped(token1andOwner1)).isTrue()

        // Different tokens, same owner
        assertThat(token1andOwner1.isOverlapped(token2andOwner1)).isFalse
        assertThat(token2andOwner1.isOverlapped(token1andOwner1)).isFalse

        // Same token, different owners
        assertThat(token1andOwner2.isOverlapped(token1andOwner1)).isFalse
        assertThat(token1andOwner1.isOverlapped(token1andOwner2)).isFalse

        // Same tokens, owner specified only for one of params
        assertThat(token1.isOverlapped(token1andOwner1)).isTrue
        assertThat(token1andOwner1.isOverlapped(token1)).isTrue

        // Different tokens, different owners
        assertThat(token2andOwner1.isOverlapped(token1andOwner2)).isFalse
        assertThat(token1andOwner2.isOverlapped(token2andOwner1)).isFalse
        assertThat(token3.isOverlapped(token2andOwner1)).isFalse
        assertThat(token1andOwner2.isOverlapped(token3)).isFalse

        assertThat(full.isOverlapped(full)).isTrue
        assertThat(full.isOverlapped(token1)).isTrue
        assertThat(token1.isOverlapped(full)).isTrue
    }
}

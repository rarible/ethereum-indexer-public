package com.rarible.protocol.nft.core.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory

internal class ReindexTokenItemsTaskParamsTest {
    @Test
    fun testParamToAndFromStringConvertForSingleToken() {
        val standard = TokenStandard.ERC721
        val token = AddressFactory.create()
        val param = ReindexTokenItemsTaskParams(standard, listOf(token))

        val paramString = param.toParamString()
        val paramObject = ReindexTokenItemsTaskParams.fromParamString(paramString)

        assertThat(paramObject).isEqualTo(param)
    }

    @Test
    fun testParamToAndFromStringConvertForTokens() {
        val standard = TokenStandard.ERC721
        val tokens = (1..10).map { AddressFactory.create() }
        val param = ReindexTokenItemsTaskParams(standard, tokens)

        val paramString = param.toParamString()
        val paramObject = ReindexTokenItemsTaskParams.fromParamString(paramString)

        assertThat(paramObject).isEqualTo(param)
    }
}

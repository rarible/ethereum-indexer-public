package com.rarible.protocol.nft.core.service.token.filter

import com.rarible.core.test.data.randomBinary
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ByteCodeFragment
import com.rarible.protocol.nft.core.model.ByteCodeMarker
import com.rarible.protocol.nft.core.model.FeatureFlags
import io.daonomic.rpc.domain.Binary
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ScamByteCodeFilterTest {
    private val scamCode = Binary.apply("0x608060405236600a57005b600036606060008073a281c0d1dce3ff738bbb5aa6849c82f2a3e564936001600160a01b03168585604051603e92919060cc565b600060405180830381855af49150503d80600081146077576040519150601f19603f3d011682016040523d82523d6000602084013e607c565b606091505b50915091508160bf5760405162461bcd60e51b815260206004820152600b60248201526a4c6f636b6564204974656d60a81b604482015260640160405180910390fd5b8051945060200192505050f35b600082848337910190815291905056fea26469706673582212200cf4e8417b43b34bc628bf9368a6ef644f0f2ce18308d0f24fbfe198c168c11464736f6c63438862177216")

    private val featureFlags = mockk<FeatureFlags> {
        every { filterScamToken } returns true
    }
    private val scamByteCodeProperties = NftIndexerProperties.ScamByteCodeProperties(
        markers = listOf(
            ByteCodeMarker(
                payloads = listOf(
                    ByteCodeFragment(
                        offset = 0,
                        fragment = "0x608060405236600a57005b600036606060008073"
                    ),
                    ByteCodeFragment(
                        offset = 41,
                        fragment = "0x01600160a01b03168585604051603e92919060cc565b600060405180830381855af49150503d80600081146077576040519150601f19603f3d011682016040523d82523d6000602084013e607c565b606091505b50915091508160bf5760405162461bcd60e51b815260206004820152600b60248201526a4c6f636b6564204974656d60a81b604482015260640160405180910390fd5b8051945060200192505050f3"
                    ),
                )
            ),
            ByteCodeMarker(
                payloads = listOf(
                    ByteCodeFragment(
                        offset = 0,
                        fragment = "0x6111111111111111111111111111111111111111"
                    ),
                )
            )
        )
    )

    private val filter = ScamByteCodeFilter(featureFlags, scamByteCodeProperties)

    @Test
    fun `filter scam - ok, all scam fragment are matched`() {
        val resul = filter.isValid(scamCode)
        assertThat(resul).isFalse
    }

    @Test
    fun `filter scam - false`() {
        val code = randomBinary(1000)
        val resul = filter.isValid(code)
        assertThat(resul).isTrue
    }

    @Test
    fun `filter scam - ok, match first bytes`() {
        val code = Binary.apply("0x611111111111111111111111111111111111111123456421")
        val resul = filter.isValid(code)
        assertThat(resul).isFalse
    }

    @Test
    fun `filter scam - false, not all fragments are matched`() {
        val code = Binary.apply("0x608060405236600a57005b600036606060008073a281c0d1dce3ff738bbb5aa6849c82f2a3e564936001")
        val resul = filter.isValid(code)
        assertThat(resul).isTrue
    }
}

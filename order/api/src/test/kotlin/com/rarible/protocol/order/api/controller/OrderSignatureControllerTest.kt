package com.rarible.protocol.order.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.order.core.service.SeaportSignatureResolver
import com.rarible.x2y2.client.X2Y2ApiClient
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderSignatureControllerTest {

    private val erc1271SignService = mockk<ERC1271SignService>()

    private val client = mockk<X2Y2ApiClient>()

    private val resolver = mockk<SeaportSignatureResolver>()

    private val controller = OrderSignatureController(erc1271SignService, client, resolver)

    @BeforeEach
    fun beforeEach() {
        clearMocks(erc1271SignService)
    }

    @Test
    fun `validate signature - valid`() = runBlocking<Unit> {
        val form = EthereumSignatureValidationFormDto(
            signer = randomAddress(),
            message = randomString(),
            signature = randomBinary()
        )
        coEvery { erc1271SignService.isSigner(form.signer, form.message, form.signature) } returns true

        assertThat(controller.validate(form).body).isEqualTo(true)
    }

    @Test
    fun `validate signature - not valid`() = runBlocking<Unit> {
        val form = EthereumSignatureValidationFormDto(
            signer = randomAddress(),
            message = randomString(),
            signature = randomBinary()
        )
        coEvery { erc1271SignService.isSigner(form.signer, form.message, form.signature) } returns false

        assertThat(controller.validate(form).body).isEqualTo(false)
    }
}

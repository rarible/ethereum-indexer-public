package com.rarible.protocol.order.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test

@IntegrationTest
class OrderSignatureControllerFt : AbstractIntegrationTest() {

    @Test
    fun `invalid order signature`() {
        try {
            orderSignatureClient.validate(
                EthereumSignatureValidationFormDto(
                    signer = randomAddress(),
                    message = randomString(),
                    signature = Binary.apply("0xc143ea056dd2f62a128808cc0c47d9477f9080c080a037437ba52140dbac1d7dc65cdb58531e038930c82314817f91cb8d8ea36a2bd0a001e134479d567b8595d7")
                )
            ).block()
            fail("Should throw exception")
        } catch (e: OrderSignatureControllerApi.ErrorValidate) {
            assertThat(e.on400.message).isEqualTo("Value of 'v' is not recognised: -41")
        }
    }
}

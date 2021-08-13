package com.rarible.protocol.nftorder.api.controller.advice

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftIndexerApiErrorDto
import com.rarible.protocol.dto.NftOrderApiErrorDto
import com.rarible.protocol.dto.OrderIndexerApiErrorDto
import com.rarible.protocol.dto.UnlockableApiErrorDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ProtocolApiErrorDtoConverterTest {

    @Test
    fun `convert nft api error dto`() {
        val apiError = NftIndexerApiErrorDto(
            400,
            NftIndexerApiErrorDto.Code.VALIDATION,
            randomString()
        )

        val errorDto = ProtocolApiErrorDtoConverter.convert(apiError)

        assertThat(errorDto.status).isEqualTo(apiError.status)
        assertThat(errorDto.message).contains(apiError.message)
        assertThat(errorDto.code).isEqualTo(NftOrderApiErrorDto.Code.NFT_API_ERROR)
    }

    @Test
    fun `convert order api error dto`() {
        val apiError = OrderIndexerApiErrorDto(
            400,
            OrderIndexerApiErrorDto.Code.INCORRECT_ORDER_DATA,
            randomString()
        )

        val errorDto = ProtocolApiErrorDtoConverter.convert(apiError)

        assertThat(errorDto.status).isEqualTo(apiError.status)
        assertThat(errorDto.message).contains(apiError.message)
        assertThat(errorDto.code).isEqualTo(NftOrderApiErrorDto.Code.ORDER_API_ERROR)
    }

    @Test
    fun `convert unlockable api error dto`() {
        val apiError = UnlockableApiErrorDto(
            400,
            UnlockableApiErrorDto.Code.LOCK_EXISTS,
            randomString()
        )

        val errorDto = ProtocolApiErrorDtoConverter.convert(apiError)

        assertThat(errorDto.status).isEqualTo(apiError.status)
        assertThat(errorDto.message).contains(apiError.message)
        assertThat(errorDto.code).isEqualTo(NftOrderApiErrorDto.Code.UNLOCKABLE_API_ERROR)
    }

    @Test
    fun `convert unknown api error dto`() {
        val apiError = randomBigInt()

        val errorDto = ProtocolApiErrorDtoConverter.convert(apiError)

        assertThat(errorDto.status).isEqualTo(500)
        assertThat(errorDto.message).contains(apiError.toString())
        assertThat(errorDto.code).isEqualTo(NftOrderApiErrorDto.Code.UNEXPECTED_API_ERROR)
    }

}
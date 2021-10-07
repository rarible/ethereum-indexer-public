package com.rarible.protocol.order.core.converters.dto

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class OrderDtoConverterTest {

    private val priceNormalizer: PriceNormalizer = mockk()
    private val assetDtoConverter = AssetDtoConverter(priceNormalizer)
    private val orderExchangeHistoryDtoConverter = OrderExchangeHistoryDtoConverter(assetDtoConverter)

    private val orderDtoConverter = OrderDtoConverter(
        priceNormalizer, assetDtoConverter, orderExchangeHistoryDtoConverter
    )

    private val erc20AssetType = Erc20AssetType(randomAddress())
    private val erc1155AssetType = Erc1155AssetType(randomAddress(), EthUInt256(randomBigInt()))

    @BeforeEach
    fun beforeEach() {
        clearMocks(priceNormalizer)
        coEvery { priceNormalizer.normalize(any(), any()) }.answers {
            (it.invocation.args[1] as BigInteger).toBigDecimal()
        }
        coEvery { priceNormalizer.normalize(any()) }.answers {
            (it.invocation.args[0] as Asset).value.value.toBigDecimal()
        }
    }

    @Test
    fun `make price evaluated`() = runBlocking<Unit> {
        val order = createOrder().copy(
            make = Asset(erc20AssetType, EthUInt256.of(12)),
            take = Asset(erc1155AssetType, EthUInt256.of(3))
        )

        val dto = orderDtoConverter.convert(order)

        assertThat(dto.makePrice).isEqualTo(BigDecimal.valueOf(4))
        assertThat(dto.takePrice).isNull()
    }

    @Test
    fun `take price evaluated`() = runBlocking<Unit> {
        val order = createOrder().copy(
            make = Asset(erc1155AssetType, EthUInt256.of(3)),
            take = Asset(erc20AssetType, EthUInt256.of(12))
        )

        val dto = orderDtoConverter.convert(order)

        assertThat(dto.takePrice).isEqualTo(BigDecimal.valueOf(4))
        assertThat(dto.makePrice).isNull()
    }

    @Test
    fun `make and take price are not evaluated`() = runBlocking<Unit> {
        val bothNft = createOrder().copy(
            make = Asset(erc1155AssetType, EthUInt256.of(3)),
            take = Asset(erc1155AssetType, EthUInt256.of(12))
        )
        val nftDto = orderDtoConverter.convert(bothNft)
        assertThat(nftDto.takePrice).isNull()
        assertThat(nftDto.makePrice).isNull()

        val bothPayments = createOrder().copy(
            make = Asset(erc20AssetType, EthUInt256.of(3)),
            take = Asset(erc20AssetType, EthUInt256.of(12))
        )
        val paymentDto = orderDtoConverter.convert(bothPayments)
        assertThat(paymentDto.takePrice).isNull()
        assertThat(paymentDto.makePrice).isNull()
    }

}
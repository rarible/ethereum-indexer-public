package com.rarible.protocol.order.api.integration

import com.rarible.core.common.nowMillis
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.erc20.api.client.Erc20BalanceControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.data.createErc20BalanceDto
import com.rarible.protocol.order.api.data.createNftItemDto
import com.rarible.protocol.order.api.data.createNftOwnershipDto
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.lang3.RandomUtils
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionPoller
import java.math.BigDecimal

@TestConfiguration
class TestPropertiesConfiguration {

    @Bean
    @Primary
    fun testRestTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    @Primary
    fun mockOrderPublisher(): ProtocolOrderPublisher {
        return mockk {
            coEvery { publish(any<OrderEventDto>()) } returns Unit
            coEvery { publish(any<OrderActivityDto>()) } returns Unit
        }
    }

    @Bean
    fun poller(ethereum: MonoEthereum): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum)
    }

    @Bean
    @Primary
    fun mockedErc20BalanceApiClient(): Erc20BalanceControllerApi {
        return mockk {
            every { getErc20Balance(any(), any()) } returns Mono.just(createErc20BalanceDto())
        }
    }

    @Bean
    @Primary
    fun mockedNftItemApi(): NftItemControllerApi {
        return mockk {
            every { getNftItemById(any()) } returns Mono.just(createNftItemDto())
        }
    }

    @Bean
    @Primary
    fun mockedNftCollectionApi(): NftCollectionControllerApi {
        return mockk()
    }

    @Bean
    @Primary
    fun mockedNftOwnershipApi(): NftOwnershipControllerApi {
        return mockk {
            every { getNftOwnershipById(any()) } returns Mono.just(createNftOwnershipDto())
        }
    }

    @Bean
    @Primary
    fun mockedCurrencyApi(): CurrencyControllerApi {
        return mockk {
            coEvery {
                getCurrencyRate(any(), any(), any())
            } returns Mono.just(
                CurrencyRateDto(
                    "from",
                    "usd",
                    BigDecimal.valueOf(RandomUtils.nextDouble()),
                    nowMillis()
                )
            )
        }
    }
}

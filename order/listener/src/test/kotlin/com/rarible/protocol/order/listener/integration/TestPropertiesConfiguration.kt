package com.rarible.protocol.order.listener.integration

import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.erc20.api.client.Erc20BalanceControllerApi
import com.rarible.protocol.order.listener.data.createErc20BalanceDto
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionPoller

@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    fun testEthereum(@Value("\${parityUrls}") url: String): MonoEthereum {
        return MonoEthereum(WebClientTransport(url, MonoEthereum.mapper(), 10000, 10000))
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
    fun mockedCurrencyApi(): CurrencyControllerApi {
        return mockk {
            coEvery { getCurrencyRate(any(), any(), any()) } returns Mono.empty<CurrencyRateDto>()
        }
    }
}

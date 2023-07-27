package com.rarible.protocol.order.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class CurrencyService(
    private val priceUpdateService: PriceUpdateService
) {
    private val rateCache = ConcurrentHashMap<Address, Rate>()

    suspend fun getUsdRate(currency: Address): BigDecimal? {
        var cachedRate = rateCache[currency]
        if (cachedRate == null) {
            val rate = safeFetchRate(currency)
            if (rate == null) {
                logger.info("Currency $currency doesn't not support usd rate")
            }
            cachedRate = Rate(rate)
            rateCache[currency] = cachedRate
        }
        return cachedRate.value
    }

    @Scheduled(cron = "\${common.currency.refresh.cron:0 0/30 * * * *}")
    fun refreshCache() {
        runBlocking {
            val cachedCurrencyKeys = ArrayList<Address>()
            rateCache.forEach {
                cachedCurrencyKeys.add(it.key)
            }
            cachedCurrencyKeys.map {
                async { refreshRate(it) }
            }.awaitAll()
        }
    }

    private suspend fun refreshRate(currency: Address) {
        val updated = safeFetchRate(currency)
        val current = rateCache[currency]
        if (updated != null) {
            logger.info("Currency {}: updated: {} -> {}", currency, updated, current)
            rateCache[currency] = Rate(updated)
        } else {
            logger.warn(
                "Unable to refresh currency rate with address [{}], will use old value: {}", currency, current
            )
        }
    }

    private suspend fun safeFetchRate(currency: Address): BigDecimal? {
        return try {
            priceUpdateService.getTokenRate(currency, Instant.now())
        } catch (ex: Throwable) {
            logger.error("Can't fetch rate for $currency", ex)
            null
        }
    }

    private data class Rate(val value: BigDecimal? = null)

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(CurrencyService::class.java)
    }
}

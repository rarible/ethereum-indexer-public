package com.rarible.protocol.order.api.controller

import com.rarible.protocol.order.api.integration.IntegrationTest
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import scalether.domain.AddressFactory
import java.math.BigInteger

@IntegrationTest
internal class OrderBidControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Test
    @Disabled //TODO: Fix me
    fun `test query dates parsed`() {
        val response = queryBids("2021-06-29T10:00:00Z", "Tue Jun 29 11:00:00 GMT 2021")
        assertEquals(200, response.statusCodeValue)
    }

    @Test
    fun `test query dates parsed with 1 or 3 digits ms`() {
        val response = queryBids("2021-06-29T10:00:00.001Z", "2021-06-30T10:00:00.5Z")
        assertEquals(200, response.statusCodeValue)
    }

    @Test
    fun `test query dates not parsed`() {
        assertThrows<HttpClientErrorException> {
            queryBids("That's not a date", "And that's too")
        }
    }

    private fun queryBids(dateFrom: String, dateTo: String): ResponseEntity<String> {
        val tokenId = BigInteger.valueOf(RandomUtils.nextLong(1000, 10000))
        val contract = AddressFactory.create()
        val baseUrl = "http://localhost:${port}/v0.1/bids/byItem?"
        val queryUrl = "${baseUrl}tokenId=${tokenId}&contract=${contract}&status=ACTIVE" +
                "&startDate=${dateFrom}" +
                "&endDate=${dateTo}"

        return restTemplate.getForEntity(queryUrl, String::class.java)
    }
}

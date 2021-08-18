package com.rarible.protocol.gateway.controller

import com.rarible.protocol.gateway.End2EndTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.client.RestTemplate

@End2EndTest
internal class OpenapiControllerFt {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var testTemplate: RestTemplate

    @Test
    fun `get openapi yaml`() {
        val yaml = testTemplate.getForObject("${baseUrl()}/openapi.yaml", String::class.java)

        assertTrue(yaml.contains("openapi:"))
        assertTrue(yaml.contains("paths:"))
        assertTrue(yaml.contains("components:"))
    }

    @Test
    fun `get redocly html`() {
        val redocly = testTemplate.getForObject("${baseUrl()}/doc", String::class.java)

        assertTrue(redocly.contains("<html>"))
    }

    private fun baseUrl(): String {
        return "http://localhost:${port}/v0.1"
    }

}

package com.rarible.protocol.erc20.core

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.erc20.core.configuration.CoreConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@EnableAutoConfiguration
@ContextConfiguration(classes = [CoreConfiguration::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@MongoTest
@MongoCleanup
annotation class IntegrationTest
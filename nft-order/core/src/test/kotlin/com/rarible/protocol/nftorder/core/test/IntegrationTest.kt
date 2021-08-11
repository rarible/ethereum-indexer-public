package com.rarible.protocol.nftorder.core.test

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.nftorder.core.configuration.CoreConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@MongoTest
@MongoCleanup
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.cloud.bootstrap.enabled=false"]
)
@Import(value = [IntegrationTestConfiguration::class])
@ContextConfiguration(classes = [CoreConfiguration::class])
@ActiveProfiles("test")
annotation class IntegrationTest
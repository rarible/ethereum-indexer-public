package com.rarible.protocol.nftorder.listener.test

import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@MongoTest
@MongoCleanup
@KafkaTest
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.cloud.bootstrap.enabled=false"]
)
@Import(value = [IntegrationTestConfiguration::class])
@ActiveProfiles("test")
annotation class IntegrationTest
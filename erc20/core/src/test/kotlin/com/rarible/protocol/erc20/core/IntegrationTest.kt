package com.rarible.protocol.erc20.core

import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@Retention
@AutoConfigureJson
@ContextConfiguration(classes = [MockContext::class])
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@MongoTest
annotation class IntegrationTest
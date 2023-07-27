package com.rarible.protocol.order.core.service

import com.rarible.core.test.data.randomBytes
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderVersionCleanSignatureServiceTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderVersionCleanSignatureService: OrderVersionCleanSignatureService

    @Test
    fun `clean signature - ok`() = runBlocking<Unit> {
        val id = Word.apply(randomWord())
        val version1 = createOrderVersion().copy(hash = id, signature = Binary(randomBytes()))
        val version2 = createOrderVersion().copy(hash = id, signature = Binary(randomBytes()))
        listOf(version1, version2).forEach {
            orderVersionRepository.save(it).awaitSingle()
        }
        orderVersionCleanSignatureService.cleanSignature(id)

        val versions = orderVersionRepository.findAllByHash(id).toList()
        assertThat(versions).hasSize(2)

        versions.forEach {
            assertThat(it.signature).isNull()
        }
    }
}

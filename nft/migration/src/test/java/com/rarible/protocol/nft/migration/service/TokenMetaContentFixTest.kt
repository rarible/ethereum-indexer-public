package com.rarible.protocol.nft.migration.service

import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.TokenMetaContent
import com.rarible.protocol.nft.core.service.token.meta.TokenMetaService
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00023TokenMetaContentFix
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.insert
import scalether.domain.Address

@IntegrationTest
class TokenMetaContentFixTest: AbstractIntegrationTest() {

    @Autowired
    lateinit var tokeMetaService: TokenMetaService

    @Test
    fun `should change content field to correct type`() = runBlocking<Unit> {
        val wrongDoc = Document.parse("""
            {"_id": "0xd5fbd81cef9aba7464c5f17e529444918a8ecc57", "data": {"properties": {"name": "EtherTulip", "description": "", "image": "https://openseauserdata.com/files/504c6c05734861fe203314142cf5ca1f.jpg", "externalUri": "http://ethertulip.com", "sellerFeeBasisPoints": 250, "tags": [], "genres": [], "content": [] }, "fetchAt": {"${'$'}date": {"${'$'}numberLong": "1656017964664"} }, "_class": "com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService${'$'}CachedTokenProperties"}, "updateDate": {"${'$'}date": {"${'$'}numberLong": "1656017964664"} }, "version": 79, "_class": "com.rarible.core.cache.Cache"}
        """.trimIndent())


        mongo.insert(wrongDoc, TokenPropertiesService.TOKEN_METADATA_COLLECTION).awaitSingle()
        ChangeLog00023TokenMetaContentFix().fixData(mongo)
        val tokenMeta = tokeMetaService.get(Address.apply("0xd5fbd81cef9aba7464c5f17e529444918a8ecc57"))

        Assertions.assertThat(tokenMeta.properties.content).isEqualTo(TokenMetaContent())
    }

}
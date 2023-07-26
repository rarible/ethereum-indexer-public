package com.rarible.protocol.nft.migration.service

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00024UpdateVersionField
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.findAll

@IntegrationTest
@Disabled
class AddVersionMigrationTest : AbstractIntegrationTest() {

    @Test
    fun `should update version field`() = runBlocking<Unit> {

        mongo.insert(Entity("item1", null), Item.COLLECTION).awaitFirst()
        mongo.insert(Entity("item2", null), Item.COLLECTION).awaitFirst()
        mongo.insert(Entity("item3", 3), Item.COLLECTION).awaitFirst()

        mongo.insert(Entity("ownership1", null), Ownership.COLLECTION).awaitFirst()
        mongo.insert(Entity("ownership2", null), Ownership.COLLECTION).awaitFirst()
        mongo.insert(Entity("ownership3", 3), Ownership.COLLECTION).awaitFirst()

        ChangeLog00024UpdateVersionField().updateVersion(template = mongo)

        val items = mongo.findAll<Entity>(Item.COLLECTION).collectList().awaitFirst()
        assertThat(items.find { it._id == "item1" }?.version).isEqualTo(0)
        assertThat(items.find { it._id == "item2" }?.version).isEqualTo(0)
        assertThat(items.find { it._id == "item3" }?.version).isEqualTo(3)

        val ownership = mongo.findAll<Entity>(Ownership.COLLECTION).collectList().awaitFirst()
        assertThat(ownership.find { it._id == "ownership1" }?.version).isEqualTo(0)
        assertThat(ownership.find { it._id == "ownership2" }?.version).isEqualTo(0)
        assertThat(ownership.find { it._id == "ownership3" }?.version).isEqualTo(3)
    }

    data class Entity(val _id: String, val version: Long?)
}

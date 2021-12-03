package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.model.CryptoPunksMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksPropertiesResolver
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.net.URL

@ChangeLog(order = "00013")
class ChangeLog00013InsertAttributesForCryptoPunks {

    @ChangeSet(id = "ChangeLog00013InsertAttributesForCryptoPunks.create", order = "1", author = "protocol")
    fun insertCryptoPunksAttributes(@NonLockGuarded punksService: CryptoPunksPropertiesResolver) = runBlocking<Unit> {
        listOf(
            "0-999.csv",
            "1000-1999.csv",
            "2000-2999.csv",
            "3000-3999.csv",
            "4000-4999.csv",
            "5000-5999.csv",
            "6000-6999.csv",
            "7000-7999.csv",
            "8000-8999.csv",
            "9000-9999.csv"
        ).map { "https://raw.githubusercontent.com/cryptopunksnotdead/punks.attributes/master/original/$it" }
            .forEach { url ->
                logger.info("Reading $url")
                val punks = URL(url).readText().lines()
                punks.drop(1).filter { it.trim().isNotEmpty() }.forEach { punk ->
                    savePunk(punk, punksService)
                }
                logger.info("Finished with $url")
            }
    }

    suspend fun savePunk(punk: String, cryptoPunksPropertiesResolver: CryptoPunksPropertiesResolver) {
        val id = BigInteger(punk.split(",")[0])
        val extra = punk.split(",").drop(1).map { it.trim() }
        val props = mapOf(
            "type" to extra[0],
            "gender" to extra[1],
            "skin tone" to extra[2],
            "count" to extra[3],
            "accessory" to extra[4].split("/").map { it.trim() }
        )
        val attributes = props.flatMap {
            when (it.value) {
                is List<*> -> (it.value as List<*>).map { e -> ItemAttribute(it.key, e.toString()) }
                else -> listOf(ItemAttribute(it.key, it.value.toString()))
            }
        }
        cryptoPunksPropertiesResolver.save(CryptoPunksMeta(id, null, attributes))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00013InsertAttributesForCryptoPunks::class.java)
    }
}

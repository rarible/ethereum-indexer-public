package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.model.CryptoPunksMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.service.CryptoPunksMetaService
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger

@ChangeLog(order = "00013")
class ChangeLog00013InsertAttributesForCryptoPunks {

    @ChangeSet(id = "ChangeLog00013InsertAttributesForCryptoPunks.create", order = "1", author = "protocol")
    fun create(@NonLockGuarded punksService: CryptoPunksMetaService) = runBlocking<Unit> {

        // dataset from https://github.com/cryptopunksnotdead/punks.attributes/tree/master/original
        val names = resource2Lines(path)
        logger.info("Found: $names")

        names.map{ "$path/$it" }.forEach { file ->
            logger.info("Reading $file")
            val punks = resource2Lines(file)
            punks.drop(1).filter { it.trim().isNotEmpty() }.forEach { punk ->
                savePunk(punk, punksService)
            }
            logger.info("Finished with $file")
        }
        logger.info("Inserting attributes for CryptoPunks")
    }

    suspend fun savePunk(punk: String, punksService: CryptoPunksMetaService) {
        val id = BigInteger(punk.split(",")[0])
        val extra = punk.split(",").drop(1).map { it.trim() }
        val props = mapOf(
            "type" to extra[0],
            "gender" to extra[1],
            "skin tone" to extra[2],
            "count" to extra[3],
            "accessory" to extra[4].split("/").map { it.trim() }
        )
        val attributes = props.flatMap { when {
            it.value is List<*> -> (it.value as List<*>).map { e -> ItemAttribute(it.key, e.toString()) }
            else -> listOf(ItemAttribute(it.key, it.value.toString()))
        } }
        val punk = CryptoPunksMeta(id, null, attributes)
        punksService.save(punk)
    }

    fun resource2Lines(path: String): List<String> = javaClass.getResourceAsStream(path).use {
        return if( it == null) emptyList()
        else BufferedReader(InputStreamReader(it)).readLines()
    }

    companion object {
        val path = "/data/cryptopunks/attributes"
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00013InsertAttributesForCryptoPunks::class.java)
    }
}

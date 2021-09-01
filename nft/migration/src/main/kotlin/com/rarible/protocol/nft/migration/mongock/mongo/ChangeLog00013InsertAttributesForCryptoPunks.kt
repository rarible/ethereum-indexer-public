package com.rarible.protocol.nft.migration.mongock.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemPropertyRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scalether.domain.Address
import java.io.BufferedReader
import java.io.InputStreamReader

@ChangeLog(order = "00013")
class ChangeLog00013InsertAttributesForCryptoPunks {

    @ChangeSet(id = "ChangeLog00013InsertAttributesForCryptoPunks.create", order = "1", author = "protocol")
    fun create(
        repository: ItemPropertyRepository,
        mapper: ObjectMapper,
        @NonLockGuarded nftIndexerProperties: NftIndexerProperties
    ) = runBlocking<Unit> {

        // dataset from https://github.com/cryptopunksnotdead/punks.attributes/tree/master/original
        val names = resource2Lines(path)
        logger.info("Found: $names")

        names.map{ "$path/$it" }.forEach { file ->
            logger.info("Reading $file")
            val punks = resource2Lines(file)
            punks.drop(1).filter { it.trim().isNotEmpty() }.forEach { punk ->
                val id = EthUInt256.of(punk.split(",")[0].toLong())
                val extra = punk.split(",").drop(1).map { it.trim() }
                val props = mapOf(
                    "type" to extra[0],
                    "gender" to extra[1],
                    "skin tone" to extra[2],
                    "count" to extra[3],
                    "accessories" to extra[4].split("/").map { it.trim() }
                )
                val itemId = ItemId(Address.apply(nftIndexerProperties.cryptoPunksContractAddress), id)
                repository.save(itemId, mapper.writeValueAsString(mapOf("attributes" to props))).awaitSingle()
            }
            logger.info("Finished with $file")
        }
        logger.info("Inserting attributes for CryptoPunks")
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

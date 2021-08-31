package com.rarible.protocol.nft.migration

import org.apache.commons.lang3.StringUtils
import org.springframework.web.client.RestTemplate
import scalether.abi.Uint256Type
import scalether.util.Hex
import java.io.File
import java.math.BigInteger

val rest = RestTemplate()
val filename = "data.csv"

// Creates the dataset with punks attributes
fun main() {
    val startId = lastId() + 1
    val file = File(filename)
    for (punkId in startId..10000) {
        val attributes = getAttributes(punkId.toLong())
        file.appendText("\n${punkId},${attributes.joinToString(" / ")}")
        println("Added: $punkId")
    }
}

fun lastId(): Int {
    val file = File(filename)
    if (file.exists()) {
        val lines = File(filename).useLines { it.toList() }
        return if (lines.size > 1) lines.last().split(",")[0].toInt() else -1
    } else {
        file.writeText("id,attributes")
        return -1
    }
}

fun getAttributes(punkId: Long): List<String> {
    val r2 = mapOf(
        "jsonrpc" to "2.0", "id" to 5, "method" to "eth_call",
        "params" to listOf(
            mapOf(
                "to" to "0x16F5A35647D6F03D5D3da7b35409D65ba03aF3B2",
                "data" to "0x76dfe297${Uint256Type.encode(BigInteger.valueOf(punkId)).hex()}"
            ), "latest"
        )
    )
    val response =
        rest.postForEntity("https://mainnet.infura.io/v3/90880ea69ac546a091223cba5f884868", r2, Map::class.java)
    var result = Hex.toBytes(response.body.get("result") as String)
    return String(result.copyOfRange(64, result.size))
        .filter { StringUtils.isAsciiPrintable(it.toString()) }
        .split(",").map { it.trim() }.toList()
}

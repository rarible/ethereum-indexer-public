package com.rarible.protocol.nft.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NftIndexerApiApplication

fun main(args: Array<String>) {
    runApplication<NftIndexerApiApplication>(*args)
}

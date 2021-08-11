package com.rarible.protocol.erc20.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Erc20IndexerApiApplication

fun main(args: Array<String>) {
    runApplication<Erc20IndexerApiApplication>(*args)
}

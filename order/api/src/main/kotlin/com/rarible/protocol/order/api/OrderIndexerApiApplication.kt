package com.rarible.protocol.order.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrderIndexerApiApplication

fun main(args: Array<String>) {
    runApplication<OrderIndexerApiApplication>(*args)
}

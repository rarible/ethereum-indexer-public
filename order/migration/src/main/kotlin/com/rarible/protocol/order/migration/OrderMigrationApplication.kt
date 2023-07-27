package com.rarible.protocol.order.migration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrderMigrationApplication

fun main(args: Array<String>) {
    runApplication<OrderMigrationApplication>(*args)
}

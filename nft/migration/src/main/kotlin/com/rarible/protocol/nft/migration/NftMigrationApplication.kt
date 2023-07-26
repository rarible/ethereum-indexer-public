package com.rarible.protocol.nft.migration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NftMigrationApplication

fun main(args: Array<String>) {
    runApplication<NftMigrationApplication>(*args)
}

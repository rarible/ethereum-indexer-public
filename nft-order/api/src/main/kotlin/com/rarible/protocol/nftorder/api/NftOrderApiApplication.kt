package com.rarible.protocol.nftorder.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication(scanBasePackages = ["com.rarible.protocol"])
class NftOrderApiApplication

fun main(args: Array<String>) {
    runApplication<NftOrderApiApplication>(*args)
}

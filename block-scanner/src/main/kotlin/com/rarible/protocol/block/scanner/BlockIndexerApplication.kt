package com.rarible.protocol.block.scanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BlockIndexerApplication

fun main(args: Array<String>) {
    runApplication<BlockIndexerApplication>(*args)
}

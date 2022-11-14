package com.rarible.protocol.erc20.listener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Erc20ListenerApplication

fun main(args: Array<String>) {
    runApplication<Erc20ListenerApplication>(*args)
}
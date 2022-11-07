package com.rarible.protocol.order.api

import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class OrderIndexerApiApplication

fun main(args: Array<String>) {
    GracefulshutdownSpringApplication.run(OrderIndexerApiApplication::class.java, *args)
}

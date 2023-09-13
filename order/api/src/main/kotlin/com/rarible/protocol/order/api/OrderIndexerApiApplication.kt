package com.rarible.protocol.order.api

import ch.sbb.esta.openshift.gracefullshutdown.GracefulShutdownHook
import com.rarible.core.kafka.KafkaShutdownHook
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class OrderIndexerApiApplication

fun main(args: Array<String>) {
    val app = SpringApplication(OrderIndexerApiApplication::class.java)
    app.setRegisterShutdownHook(false)
    val context = app.run(*args)
    Runtime.getRuntime().addShutdownHook(Thread(KafkaShutdownHook(context, GracefulShutdownHook(context))))
}

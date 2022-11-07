package com.rarible.protocol.nft.api

import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class NftIndexerApiApplication

fun main(args: Array<String>) {
    GracefulshutdownSpringApplication.run(NftIndexerApiApplication::class.java, *args)
}

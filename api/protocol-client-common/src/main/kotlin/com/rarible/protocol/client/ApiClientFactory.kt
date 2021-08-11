package com.rarible.protocol.client

interface ApiClientFactory {

    fun <T> getClient(clientClass: Class<T>, path: String)

}

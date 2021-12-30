package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.protocol.contracts.creators.CreatorsEvent
import com.rarible.protocol.nft.listener.service.descriptors.creators.parseCreatorsEvent
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.java.Lists
import java.math.BigInteger

class ParseCreatorsEventTest {
    @Test
    fun indexedWorks() {
        val tokenId = Word.apply("0xfb571f9da71d1ac33e069571bf5c67fadcff18e400000000000000000000002b")
        val data = Binary.apply("0x00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000fb571f9da71d1ac33e069571bf5c67fadcff18e40000000000000000000000000000000000000000000000000000000000002710")
        val event = parseLog(listOf(topic, tokenId), data)
        assertEquals(event.tokenId(), BigInteger("113684458893483085791140453563089956290380401423266949679181692179405588135979"))
    }

    @Test
    fun nonIndexedWorks() {
        val data = Binary.apply("0xe285e6d7b9bac60008b95267a39dc10340bacb8db0000000000000000000000100000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000001000000000000000000000000e285e6d7b9bac60008b95267a39dc10340bacb8d0000000000000000000000000000000000000000000000000000000000002710")
        val event = parseLog(listOf(topic), data)
        assertEquals(event.tokenId(), BigInteger("102459287657041632249809693258220486371967076492625071896106277934946311995393"))
    }

    private val topic: Word = Word.apply("0x841ffb90d4cabdd1f16034f3fa831d79060febbb8167bdd54a49269365bdf78f")

    private fun parseLog(topics: List<Word>, data: Binary): CreatorsEvent {
        return parseCreatorsEvent(
            Log(
                BigInteger.ZERO, BigInteger.ZERO, topics[0], topics[0], BigInteger.ZERO, Address.ZERO(),
                data, false, Lists.toScala(topics), ""
            )
        )
    }
}

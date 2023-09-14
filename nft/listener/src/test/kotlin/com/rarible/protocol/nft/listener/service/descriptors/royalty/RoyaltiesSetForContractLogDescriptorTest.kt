package com.rarible.protocol.nft.listener.service.descriptors.royalty

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.log
import com.rarible.protocol.nft.core.model.Part
import io.daonomic.rpc.domain.Word
import io.mockk.mockk
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction

class RoyaltiesSetForContractLogDescriptorTest {
    private val properties = mockk<NftIndexerProperties>()
    private val descriptor = RoyaltiesSetForContractLogDescriptor(properties)

    @Test
    fun convert() = runBlocking<Unit> {
        // https://etherscan.io/tx/0x8f6221362d90b1be7ec0285b21b8dca5f753d0c32432b4007a0648cc2aa115f4#eventlog
        val log = log(
            topics = listOf(
                Word.apply("0xc026171b9a7c9009d6a748a19a0a3cb877978a585e1647a87a786d724bbde127"),
                Word.apply("0x000000000000000000000000a01d803e2734c542d13a13772deced63cd6453bf"),
            ),
            data = "0x0000000000000000000000000000000000000000000000000000000000000020" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000003e9eec552296d024e98cbf874f2ae6c12a3faeb0" +
                    "00000000000000000000000000000000000000000000000000000000000003e8"
        )
        val transaction = mockk<Transaction>()

        val history = descriptor.convert(log, transaction, 1, 0, 0).awaitSingle()
        assertThat(history.token).isEqualTo(Address.apply("0xA01d803E2734C542D13A13772DECeD63CD6453Bf"))
        assertThat(history.parts).isEqualTo(listOf(Part(Address.apply("0x3e9eec552296d024e98cbf874f2ae6c12a3faeb0"), 1000)))
    }
}

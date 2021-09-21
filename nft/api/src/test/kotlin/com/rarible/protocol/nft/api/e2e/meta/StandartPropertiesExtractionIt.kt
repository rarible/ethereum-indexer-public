package com.rarible.protocol.nft.api.e2e.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.misc.SignUtils
import com.rarible.protocol.nft.api.service.item.ItemService
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaCacheDescriptor
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.kotlin.isA
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.util.stream.Stream


@Tag("manual")
@End2EndTest
class StandartPropertiesExtractionIt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var itemService: ItemService

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @MockBean
    private lateinit var openSeaCacheDescriptor: OpenSeaCacheDescriptor


    companion object {
        @JvmStatic
        fun itemsMeta(): Stream<Arguments?>? {
            var mapper = ObjectMapper().registerKotlinModule()
            val data : Map<String, List<ParamItem>> = mapper.readValue(ClassPathResource("/data/itemsMeta.json").getFile())
            return data.get("ERC721")?.stream()?.map { arguments(it) }
        }
    }

    @BeforeEach
    fun setupOpenseaclient() {
        Mockito.`when`(openSeaCacheDescriptor.fetchAsset(isA(), isA()))
            .thenReturn(Mono.empty<ItemProperties>())
    }

    @ParameterizedTest
    @MethodSource("itemsMeta")
    fun `should have correct meta`(argv: ParamItem) = runBlocking<Unit> {

        val token = argv.token
        val tokenId = argv.tokenId

        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(7000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val contract = MintableToken.deployAndWait(userSender, poller, "TEST", "TST", userSender.from(), argv.contractURI, argv.contractURI).block()!!
        val nonce = SignUtils.sign(privateKey, tokenId, contract.address())
        tokenRepository.save(Token(contract.address(), name = "TEST", standard = TokenStandard.ERC721)).awaitFirst()

        contract.mint(BigInteger.valueOf(nonce.value), nonce.v.toEth(), nonce.r.bytes(), nonce.s.bytes(), emptyArray(), argv.tokenURI).execute().verifySuccess()

        val itemId = ItemId(
            contract.address(),
            EthUInt256.of(nonce.value)
        )
        val itemDto = itemService.getMeta(itemId)
        assertEquals(argv.data.name, itemDto.name)
        assertEquals(argv.data.description, itemDto.description)
        assertEquals(argv.data.attributes?.size, itemDto.attributes?.size)

        // no images with diff resolutions
        assertEquals(argv.data.image?.url?.get("ORIGINAL"), itemDto.image?.url?.get("ORIGINAL"))
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = ethereum.executeRaw(
                Request(1, "trace_replayTransaction", Lists.toScala(
                    receipt.transactionHash().toString(),
                    Lists.toScala("trace", "stateDiff")
                ), "2.0")
            ).block()!!
            "traces: ${result.result().get()}"
        }
        return receipt
    }

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }

    data class ParamItem(
        val token: Address,
        val tokenId: Long,
        val contractURI: String,
        val tokenURI: String,
        val data: NftItemMetaDto
    )
}

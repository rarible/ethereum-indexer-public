package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import org.junit.jupiter.api.Disabled

@Disabled
@IntegrationTest
class StandardTokenPropertiesResolverTest : AbstractIntegrationTest() {
//    @Autowired
//    private lateinit var ipfsService: IpfsService
//
//    @Autowired
//    private lateinit var innerGatewaysProvider: GatewayProvider
//
//    @Autowired
//    private lateinit var tokenUriResolver: BlockchainTokenUriResolver
//
//    private lateinit var userSender: MonoSigningTransactionSender
//    private lateinit var erc721: ERC721Rarible
//
//    val defaultWebClientBuilder = DefaultWebClientBuilder(followRedirect = false)
//    val proxyWebClientBuilder = ProxyWebClientBuilder(
//        readTimeout = 60000,
//        connectTimeout = 60000,
//        proxyUrl = "",
//        followRedirect = false
//    )
//
//    @BeforeEach
//    fun before() = runBlocking<Unit> {
//        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
//        userSender = newSender(privateKey).second
//        erc721 = ERC721Rarible.deployAndWait(userSender, poller).awaitFirst()
//        erc721.__ERC721Rarible_init(
//            "Feudalz",
//            "FEUDALZ",
//            "baseURI",
//            "ipfs://QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"
//        ).execute().verifySuccess()
//        val tokenToSave = createToken().copy(
//            id = erc721.address(),
//            standard = TokenStandard.ERC721
//        )
//        tokenRepository.save(tokenToSave).awaitSingle()
//    }
//
//    @Test
//    fun `should parse from ipfs`() = runBlocking<Unit> {
//        val resolver = mock(mockSuccessIpfsResponse())
//        val props = resolver.resolve(erc721.address())
//        assertThat(props).isEqualTo(TokenProperties(
//            name = "Feudalz",
//            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
//            externalLink = "https://feudalz.io",
//            image = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
//            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
//            sellerFeeBasisPoints = 250
//        ))
//    }
//
//    @Test
//    fun `should return null if not found`() = runBlocking<Unit> {
//        val resolver = mock(mockNotFoundIpfsResponse())
//        val props = resolver.resolve(erc721.address())
//        assertThat(props).isNull()
//    }
//
//    @Test
//    fun `should return null if broken json`() = runBlocking<Unit> {
//        val resolver = mock(mockBrokenJsonIpfsResponse())
//        val props = resolver.resolve(erc721.address())
//        assertThat(props).isNull()
//    }
//
//    private fun mock(webClient: WebClient): StandardTokenPropertiesResolver {
//        val externalHttpClient = object: ExternalHttpClient(
//            openseaUrl = "https://api.opensea.io/api/v1",
//            openseaApiKey = "",
//            proxyUrl = "",
//            proxyWebClientBuilder = proxyWebClientBuilder,
//            defaultWebClientBuilder = defaultWebClientBuilder
//        ) {
//            override val defaultClient get() = webClient
//        }
//
//        val propertiesHttpLoader = PropertiesHttpLoader(
//            externalHttpClient = externalHttpClient,
//            defaultRequestTimeout = BasePropertiesResolverTest.REQUEST_TIMEOUT,
//            openseaRequestTimeout = BasePropertiesResolverTest.REQUEST_TIMEOUT
//        )
//
//        return StandardTokenPropertiesResolver(
//            ipfsService = ipfsService,
//            propertiesHttpLoader = propertiesHttpLoader,
//            tokenUriResolver = tokenUriResolver
//        )
//    }
//
//    private fun mockSuccessIpfsResponse(): WebClient {
//        return WebClient.builder()
//            .exchangeFunction { request ->
//                assertThat(request.url())
//                    // URL taken from application-integration.properties
//                    .isEqualTo(URI("https://ipfs.io/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"))
//                Mono.just(
//                    ClientResponse.create(HttpStatus.OK)
//                        .header("content-type", "application/json")
//                        .body("ipfs.json".asResource())
//                        .build()
//                )
//            }.build()
//    }
//
//    private fun mockNotFoundIpfsResponse(): WebClient {
//        val httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)
//        return WebClient.builder()
//            .clientConnector(ReactorClientHttpConnector(httpClient))
//            .exchangeFunction { request ->
//                assertThat(request.url()).isEqualTo(
////                    URI("${publicGatewayProvider.getGateway()}/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T")
//                    URI("${innerGatewaysProvider.getGateway()}/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T")
//                )
//                Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
//            }.build()
//    }
//
//    private fun mockBrokenJsonIpfsResponse(): WebClient {
//        return WebClient.builder()
//            .exchangeFunction { request ->
//                Mono.just(
//                    ClientResponse.create(HttpStatus.OK)
//                        .header("content-type", "plain/text")
//                        .body("Hello world!")
//                        .build()
//                )
//            }.build()
//    }
//
//    fun String.asResource() = this.javaClass::class.java.getResource("/meta/response/$this").readText()

}

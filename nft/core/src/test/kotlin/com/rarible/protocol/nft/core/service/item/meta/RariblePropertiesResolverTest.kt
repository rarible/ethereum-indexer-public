package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@ItemMetaTest
@EnabledIfSystemProperty(named = "RARIBLE_TESTS_OPENSEA_PROXY_URL", matches = ".+")
class RariblePropertiesResolverTest : BasePropertiesResolverTest() {

    private val lazyNftItemHistoryRepository = mockk<LazyNftItemHistoryRepository>()

    @BeforeEach
    @Suppress("ReactiveStreamsUnusedPublisher")
    private fun before() {
        clearMocks(lazyNftItemHistoryRepository)
        every { lazyNftItemHistoryRepository.findLazyMintById(any()) } returns Flux.empty()
        every { lazyNftItemHistoryRepository.find(any(), any(), any()) } returns Flux.empty()
    }

    /**
     * Aavegotchi collection has 'image_content' attribute in raw metadata.
     */
    @Test
    fun aavegotchi() = runBlocking<Unit> {
        // TODO[meta]: compare with opensea.
        //  val imageUrl = "https://storage.opensea.io/files/9931fc66855dccf59e0524864b2c2231.svg"
        val address = Address.apply("0x1906fd9c4ac440561f7197da0a4bd2e88df5fa70")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(ItemId(address, EthUInt256.of(21089)))
        val description = properties!!.description!!
        val timestamp = description.substringAfter("Kinship, and 12.").substringBefore(" amWMATIC")
        val devoted = description.substringAfter("Devoted (").substringBefore(") Kinship")
        val experience = properties.attributes.find { it.key == "Experience" }?.value!!
        val name = properties.name
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = name,
                description = "Hi fren! I'm \$$name, a Haunt 2 Aavegotchi with a Rarity Score of 495, Devoted ($devoted) Kinship, and 12.$timestamp amWMATIC staked inside me.\n \n" +
                    " If you'd like to learn more about me and my gotchigang, head to https://wiki.aavegotchi.com for lots of good info!",
                attributes = listOf(
                    ItemAttribute("Haunt", "2"),
                    ItemAttribute("Rarity Score", "495"),
                    ItemAttribute("Name", "203 kinship"),
                    ItemAttribute("Staked Amount", "12.$timestamp amWMATIC"),
                    ItemAttribute("Kinship", "Devoted"),
                    ItemAttribute("Experience", experience),
                    ItemAttribute("⚡️ Energy", "Zen"),
                    ItemAttribute("👹 Aggression", "Neutral"),
                    ItemAttribute("👻  Spookiness", "Creepy"),
                    ItemAttribute("🧠 Brain Size", "Big"),
                    ItemAttribute("👀 Eye Shape", "Uncommon High"),
                    ItemAttribute("👁 Eye Color", "Uncommon High")
                ),
                rawJsonContent = """{"name":"$name","description":"Hi fren! I'm ${'$'}$name, a Haunt 2 Aavegotchi with a Rarity Score of 495, Devoted ($devoted) Kinship, and 12.$timestamp amWMATIC staked inside me.\n \n If you'd like to learn more about me and my gotchigang, head to https://wiki.aavegotchi.com for lots of good info!","image_data":"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\"><g class=\"gotchi-bg\"><defs fill=\"#fff\"><pattern id=\"a\" patternUnits=\"userSpaceOnUse\" width=\"4\" height=\"4\"><path d=\"M0 0h1v1H0zm2 2h1v1H2z\"/></pattern><pattern id=\"b\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"2\" height=\"2\"><path d=\"M0 0h1v1H0z\"/></pattern><pattern id=\"c\" patternUnits=\"userSpaceOnUse\" x=\"-2\" y=\"0\" width=\"8\" height=\"1\"><path d=\"M0 0h1v1H0zm2 0h1v1H2zm2 0h1v1H4z\"/></pattern><pattern id=\"d\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"4\" height=\"4\"><path d=\"M0 0h1v1H0zm0 2h1v1H0zm1 0V1h1v1zm1 0h1v1H2zm0-1h1V0H2zm1 2h1v1H3z\"/></pattern><pattern id=\"e\" patternUnits=\"userSpaceOnUse\" width=\"64\" height=\"32\"><path d=\"M4 4h1v1H4zm7 0h1v1h-1zm7 0h1v1h-1zm7 0h1v1h-1zm7 0h1v1h-1zm7 0h1v1h-1zm7 0h1v1h-1zm7 0h1v1h-1zm7 0h1v1h-1z\"/><path fill=\"url(#a)\" d=\"M0 8h64v7H0z\"/><path fill=\"url(#b)\" d=\"M0 16h64v1H0z\"/><path fill=\"url(#c)\" d=\"M0 18h64v1H0z\"/><path fill=\"url(#b)\" d=\"M22 18h15v1H22zM0 20h64v3H0z\"/><path fill=\"url(#d)\" d=\"M0 24h64v8H0z\"/></pattern><mask id=\"f\"><path fill=\"url(#e)\" d=\"M0 0h64v32H0z\"/></mask></defs><path fill=\"#fff\" d=\"M0 0h64v32H0z\"/><path fill=\"#dea8ff\" class=\"gotchi-secondary\" mask=\"url(#f)\" d=\"M0 0h64v32H0z\"/><path fill=\"#dea8ff\" class=\"gotchi-secondary\" d=\"M0 32h64v32H0z\"/><path mask=\"url(#f)\" fill=\"#fff\" transform=\"matrix(1 0 0 -1 0 64)\" d=\"M0 0h64v32H0z\"/></g><style>.gotchi-primary{fill:#824EE2;}.gotchi-secondary{fill:#E6DCF9;}.gotchi-cheek{fill:#F696C6;}.gotchi-eyeColor{fill:#36818E;}.gotchi-primary-mouth{fill:#824EE2;}.gotchi-sleeves-up{display:none;}.gotchi-handsUp{display:none;}.gotchi-handsDownOpen{display:none;}.gotchi-handsDownClosed{display:block}</style><g class=\"gotchi-body\"><g class=\"gotchi-primary\"><path d=\"M21 12h2v-2h-4v2h1z\"/><path d=\"M19 14v-2h-2v2h1zm6-4h2V8h-4v2h1z\"/><path d=\"M29 8h8V6H27v2h1zm16 6h2v-2h-2v1z\"/><path d=\"M48 14h-1v39h-2v2h4V14zm-11-4h4V8h-4v1z\"/><path d=\"M41 12h4v-2h-4v1zM17 53V14h-2v41h4v-2h-1z\"/><path d=\"M24 51h-5v2h5v-1z\"/><path d=\"M27 53h-3v2h5v-2h-1zm18-2h-5v2h5v-1z\"/><path d=\"M35 51h-6v2h6v-1z\"/><path d=\"M38 53h-3v2h5v-2h-1z\"/></g><g class=\"gotchi-secondary\"><path d=\"M18 43v6h2v-1h2v1h2v2h-5v2h-2V14h2v1h-1v26z\"/><path d=\"M27 51h-3v2h5v-2h-1zm11 0h-3v2h5v-2h-1z\"/><path d=\"M35 49h-2v-1h-2v1h-2v2h6v-1zM25 11h2v-1h-4v1h1zm-4 2h2v-1h-4v1h1zm24 31v5h-1v-1h-2v1h-2v2h5v2h2V14h-2v29z\"/><path d=\"M37 8H27v1h5v1h5V9zm8 4h-4v2h4v-1z\"/><path d=\"M41 10h-4v2h4v-1z\"/></g><path d=\"M44 14h-3v-2h-4v-2h-5V9h-5v2h-4v2h-4v2h-1v34h2v-1h2v1h2v2h5v-2h2v-1h2v1h2v2h5v-2h2v-1h2v1h1V14z\" fill=\"#fff\"/></g><path class=\"gotchi-cheek\" d=\"M21 32v2h2v-2h-1zm21 0h-1v2h2v-2z\"/><g class=\"gotchi-primary-mouth\"><path d=\"M29 32h-2v2h2v-1z\"/><path d=\"M33 34h-4v2h6v-2h-1z\"/><path d=\"M36 32h-1v2h2v-2z\"/></g><g class=\"gotchi-shadow\"><path opacity=\".25\" d=\"M25 58H19v1h1v1h24V59h1V58h-1z\" fill=\"#000\"/></g><g class=\"gotchi-collateral\"><path d=\"M37.25 17.04v-.75h-.75v-.75h-.75v-.75H33.5v.75h-.75v.75h-1.5v.75h-.75v.75h-2.25v-.75h-.75v-2.25h.75v-.75h2.25v.75h.75v.75H32v-1.5h-.75v-.75h-.75v-.75h-2.25v.75h-.75v.75h-.75v.75H26v2.25h.75v.75h.75v.75h.75v.75h2.25v-.75h.75v-.75h1.5v-.75h.75v-.75h2.25v.75h.75v2.25h-.75v.75H33.5v-.75h-.75v-.75H32v1.5h.75v.75h.75v.75h2.25v-.75h.75v-.75h.75v-.75H38v-2.25h-.75z\" fill=\"#824ee2\"/></g><g class=\"gotchi-eyeColor\"><path d=\"M27 23H26H25H24H23H22H21V24V25H22H23H24H25H26H27H28H29V24V23H28H27Z\" class=\"gotchi-primary\" /><path d=\"M42 23H41H40H39H38H37H36H35V24V25H36H37H38H39H40H41H42H43V24V23H42Z\" class=\"gotchi-primary\" /><rect x=\"24\" y=\"23\" width=\"2\" height=\"2\" /> <rect x=\"38\" y=\"23\" width=\"2\" height=\"2\" /></g><g class=\"gotchi-handsDownClosed\"><g class=\"gotchi-primary\"><path d=\"M19 42h1v1h-1zm1-6h1v1h-1z\"/><path d=\"M21 37h1v1h-1zm5 3v4h1v-4zm-5 3h-1v1h2v-1z\"/><path d=\"M24 44h-2v1h4v-1h-1zm1-5h-1v1h2v-1z\"/><path d=\"M23 38h-1v1h2v-1z\"/></g><g class=\"gotchi-secondary\"><path d=\"M19 43h1v1h-1zm5 2h-2v1h4v-1h-1z\"/><path d=\"M27 41v3h1v-3zm-6 3h-1v1h2v-1z\"/><path d=\"M26 44h1v1h-1zm-7-3h-1v2h1v-1z\"/></g><g class=\"gotchi-primary\"><path d=\"M44 42h1v1h-1zm-1-6h1v1h-1z\"/><path d=\"M42 37h1v1h-1z\"/><path d=\"M42 39v-1h-2v1h1zm0 4v1h2v-1h-1z\"/><path d=\"M40 44h-2v1h4v-1h-1z\"/><path d=\"M38 42v-2h-1v4h1v-1z\"/><path d=\"M40 40v-1h-2v1h1z\"/></g><g class=\"gotchi-secondary\"><path d=\"M42 44v1h2v-1h-1zm-5-2v-1h-1v3h1v-1z\"/><path d=\"M40 45h-2v1h4v-1h-1z\"/><path d=\"M37 44h1v1h-1zm7-1h1v1h-1z\"/></g></g><g class=\"gotchi-handsDownOpen\"><g class=\"gotchi-primary\"><path d=\"M14 40h1v1h-1v-1zm-1-6h1v1h-1v-1z\"/><path d=\"M14 33h1v1h-1v-1zm-2 2h1v1h-1v-1zm-5 3h1v4H7v-4zm5 3h2v1h-2v-1z\"/><path d=\"M8 42h4v1H8v-1zm0-5h2v1H8v-1z\"/><path d=\"M10,36h2v1h-2V36z\"/></g><g class=\"gotchi-secondary\"><path d=\"M14,39h1v1h-1V39z\"/><path d=\"M12,40h2v1h-2V40z\"/><path d=\"M8,41h4v1H8V41z\"/></g><path d=\"M8,38v3h4v-1h2v-1h1v-5h-1v1h-1v1h-1v1h-2v1H8z\" fill=\"#fff\" /><g class=\"gotchi-primary\"><path d=\"M49 40h1v1h-1v-1zm1-6h1v1h-1v-1z\"/><path d=\"M49 33h1v1h-1v-1zm2 2h1v1h-1v-1zm5 3h1v4h-1v-4zm-6 3h2v1h-2v-1z\"/><path d=\"M52 42h4v1h-4v-1zm2-5h2v1h-2v-1z\"/><path d=\"M52,36h2v1h-2V36z\"/></g><g class=\"gotchi-secondary\"><path d=\"M49,39h1v1h-1V39z\"/><path d=\"M50,40h2v1h-2V40z\"/><path d=\"M52,41h4v1h-4V41z\"/></g><path d=\"M54,38v-1h-2v-1h-1v-1h-1v-1h-1v5h1v1h2v1h4v-3H54z\" fill=\"#fff\" /></g><g class=\"gotchi-handsUp\"><g class=\"gotchi-secondary\"><path d=\"M50,38h1v1h-1V38z\"/><path d=\"M49 39h1v1h-1v-1zm2-2h1v1h-1v-1z\"/><path d=\"M52,36h2v1h-2V36z\"/><path d=\"M54,35h2v1h-2V35z\"/></g><path d=\"M52,32v1h-2v1h-1v5h1v-1h1v-1h1v-1h2v-1h2v-3H52z\" fill=\"#fff\"/><g class=\"gotchi-primary\"><path d=\"M49,33h1v1h-1V33z\"/><path d=\"M50 32h2v1h-2v-1zm0 7h1v1h-1v-1z\"/><path d=\"M49 40h1v1h-1v-1zm2-2h1v1h-1v-1z\"/><path d=\"M52 37h2v1h-2v-1zm0-6h4v1h-4v-1z\"/><path d=\"M56,32h1v4h-1V32z\"/><path d=\"M54,36h2v1h-2V36z\"/></g><g class=\"gotchi-secondary\"><path d=\"M13,38h1v1h-1V38z\"/><path d=\"M14 39h1v1h-1v-1zm-2-2h1v1h-1v-1z\"/><path d=\"M10,36h2v1h-2V36z\"/><path d=\"M8,35h2v1H8V35z\"/></g><path d=\"M8,32v3h2v1h2v1h1v1h1v1h1v-5h-1v-1h-2v-1H8z\" fill=\"#fff\"/><g class=\"gotchi-primary\"><path d=\"M14,33h1v1h-1V33z\"/><path d=\"M12 32h2v1h-2v-1zm1 7h1v1h-1v-1z\"/><path d=\"M14 40h1v1h-1v-1zm-2-2h1v1h-1v-1z\"/><path d=\"M10 37h2v1h-2v-1zm-2-6h4v1H8v-1z\"/><path d=\"M7,32h1v4H7V32z\"/><path d=\"M8,36h2v1H8V36z\"/></g></g></svg>","external_url":"https://aavegotchi.com/gotchi/21089","attributes":[{"trait_type":"Haunt","value":"2"},{"trait_type":"Rarity Score","value":"495"},{"trait_type":"Name","value":"203 kinship"},{"trait_type":"Staked Amount","value":"12.$timestamp amWMATIC"},{"trait_type":"Kinship","value":"Devoted"},{"trait_type":"Experience","value":"$experience"},{"trait_type":"⚡️ Energy","value":"Zen"},{"trait_type":"👹 Aggression","value":"Neutral"},{"trait_type":"👻  Spookiness","value":"Creepy"},{"trait_type":"🧠 Brain Size","value":"Big"},{"trait_type":"👀 Eye Shape","value":"Uncommon High"},{"trait_type":"👁 Eye Color","value":"Uncommon High"}]}""",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "${publicGatewayProvider.getGateway()}/ipfs/Qmf6we8fwu8KEou5s2iswb1q6bxscbNRmgw5vpmZj18evK"
                )
            )
        )
    }

    @Test
    fun uniswap() = runBlocking<Unit> {
        val token = Address.apply("0xc36442b4a4522e871399cd717abdd847ab11fe88")
        mockTokenStandard(token, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                token,
                EthUInt256.of(51561)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Uniswap - 0.3% - MATIC/WETH - 1555.6<>1603.0",
                description = """This NFT represents a liquidity position in a Uniswap V3 MATIC-WETH pool. The owner of this NFT can modify or redeem the position.

Pool Address: 0x290a6a7460b308ee3f19023d2d00de604bcf5b42
MATIC Address: 0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0
WETH Address: 0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2
Fee Tier: 0.3%
Token ID: 51561

⚠️ DISCLAIMER: Due diligence is imperative when assessing this NFT. Make sure token addresses match the expected tokens, as token symbols may be imitated.""",
                attributes = emptyList(),
                rawJsonContent = """{"name":"Uniswap - 0.3% - MATIC/WETH - 1555.6<>1603.0","description":"This NFT represents a liquidity position in a Uniswap V3 MATIC-WETH pool. The owner of this NFT can modify or redeem the position.\n\nPool Address: 0x290a6a7460b308ee3f19023d2d00de604bcf5b42\nMATIC Address: 0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0\nWETH Address: 0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2\nFee Tier: 0.3%\nToken ID: 51561\n\n⚠️ DISCLAIMER: Due diligence is imperative when assessing this NFT. Make sure token addresses match the expected tokens, as token symbols may be imitated.","image":"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjkwIiBoZWlnaHQ9IjUwMCIgdmlld0JveD0iMCAwIDI5MCA1MDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9J2h0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsnPjxkZWZzPjxmaWx0ZXIgaWQ9ImYxIj48ZmVJbWFnZSByZXN1bHQ9InAwIiB4bGluazpocmVmPSJkYXRhOmltYWdlL3N2Zyt4bWw7YmFzZTY0LFBITjJaeUIzYVdSMGFEMG5Namt3SnlCb1pXbG5hSFE5SnpVd01DY2dkbWxsZDBKdmVEMG5NQ0F3SURJNU1DQTFNREFuSUhodGJHNXpQU2RvZEhSd09pOHZkM2QzTG5jekxtOXlaeTh5TURBd0wzTjJaeWMrUEhKbFkzUWdkMmxrZEdnOUp6STVNSEI0SnlCb1pXbG5hSFE5SnpVd01IQjRKeUJtYVd4c1BTY2pOMlF4WVdaaEp5OCtQQzl6ZG1jKyIvPjxmZUltYWdlIHJlc3VsdD0icDEiIHhsaW5rOmhyZWY9ImRhdGE6aW1hZ2Uvc3ZnK3htbDtiYXNlNjQsUEhOMlp5QjNhV1IwYUQwbk1qa3dKeUJvWldsbmFIUTlKelV3TUNjZ2RtbGxkMEp2ZUQwbk1DQXdJREk1TUNBMU1EQW5JSGh0Ykc1elBTZG9kSFJ3T2k4dmQzZDNMbmN6TG05eVp5OHlNREF3TDNOMlp5YytQR05wY21Oc1pTQmplRDBuTVRFNUp5QmplVDBuTWpVekp5QnlQU2N4TWpCd2VDY2dabWxzYkQwbkkyTXdNbUZoWVNjdlBqd3ZjM1puUGc9PSIvPjxmZUltYWdlIHJlc3VsdD0icDIiIHhsaW5rOmhyZWY9ImRhdGE6aW1hZ2Uvc3ZnK3htbDtiYXNlNjQsUEhOMlp5QjNhV1IwYUQwbk1qa3dKeUJvWldsbmFIUTlKelV3TUNjZ2RtbGxkMEp2ZUQwbk1DQXdJREk1TUNBMU1EQW5JSGh0Ykc1elBTZG9kSFJ3T2k4dmQzZDNMbmN6TG05eVp5OHlNREF3TDNOMlp5YytQR05wY21Oc1pTQmplRDBuTVRjd0p5QmplVDBuTXpNd0p5QnlQU2N4TWpCd2VDY2dabWxzYkQwbkkyTm1aV0ppTUNjdlBqd3ZjM1puUGc9PSIgLz48ZmVJbWFnZSByZXN1bHQ9InAzIiB4bGluazpocmVmPSJkYXRhOmltYWdlL3N2Zyt4bWw7YmFzZTY0LFBITjJaeUIzYVdSMGFEMG5Namt3SnlCb1pXbG5hSFE5SnpVd01DY2dkbWxsZDBKdmVEMG5NQ0F3SURJNU1DQTFNREFuSUhodGJHNXpQU2RvZEhSd09pOHZkM2QzTG5jekxtOXlaeTh5TURBd0wzTjJaeWMrUEdOcGNtTnNaU0JqZUQwbk1URTVKeUJqZVQwbk5EQTNKeUJ5UFNjeE1EQndlQ2NnWm1sc2JEMG5JemMxTm1Oak1pY3ZQand2YzNablBnPT0iIC8+PGZlQmxlbmQgbW9kZT0ib3ZlcmxheSIgaW49InAwIiBpbjI9InAxIiAvPjxmZUJsZW5kIG1vZGU9ImV4Y2x1c2lvbiIgaW4yPSJwMiIgLz48ZmVCbGVuZCBtb2RlPSJvdmVybGF5IiBpbjI9InAzIiByZXN1bHQ9ImJsZW5kT3V0IiAvPjxmZUdhdXNzaWFuQmx1ciBpbj0iYmxlbmRPdXQiIHN0ZERldmlhdGlvbj0iNDIiIC8+PC9maWx0ZXI+IDxjbGlwUGF0aCBpZD0iY29ybmVycyI+PHJlY3Qgd2lkdGg9IjI5MCIgaGVpZ2h0PSI1MDAiIHJ4PSI0MiIgcnk9IjQyIiAvPjwvY2xpcFBhdGg+PHBhdGggaWQ9InRleHQtcGF0aC1hIiBkPSJNNDAgMTIgSDI1MCBBMjggMjggMCAwIDEgMjc4IDQwIFY0NjAgQTI4IDI4IDAgMCAxIDI1MCA0ODggSDQwIEEyOCAyOCAwIDAgMSAxMiA0NjAgVjQwIEEyOCAyOCAwIDAgMSA0MCAxMiB6IiAvPjxwYXRoIGlkPSJtaW5pbWFwIiBkPSJNMjM0IDQ0NEMyMzQgNDU3Ljk0OSAyNDIuMjEgNDYzIDI1MyA0NjMiIC8+PGZpbHRlciBpZD0idG9wLXJlZ2lvbi1ibHVyIj48ZmVHYXVzc2lhbkJsdXIgaW49IlNvdXJjZUdyYXBoaWMiIHN0ZERldmlhdGlvbj0iMjQiIC8+PC9maWx0ZXI+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVwIiB4MT0iMSIgeDI9IjAiIHkxPSIxIiB5Mj0iMCI+PHN0b3Agb2Zmc2V0PSIwLjAiIHN0b3AtY29sb3I9IndoaXRlIiBzdG9wLW9wYWNpdHk9IjEiIC8+PHN0b3Agb2Zmc2V0PSIuOSIgc3RvcC1jb2xvcj0id2hpdGUiIHN0b3Atb3BhY2l0eT0iMCIgLz48L2xpbmVhckdyYWRpZW50PjxsaW5lYXJHcmFkaWVudCBpZD0iZ3JhZC1kb3duIiB4MT0iMCIgeDI9IjEiIHkxPSIwIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwLjAiIHN0b3AtY29sb3I9IndoaXRlIiBzdG9wLW9wYWNpdHk9IjEiIC8+PHN0b3Agb2Zmc2V0PSIwLjkiIHN0b3AtY29sb3I9IndoaXRlIiBzdG9wLW9wYWNpdHk9IjAiIC8+PC9saW5lYXJHcmFkaWVudD48bWFzayBpZD0iZmFkZS11cCIgbWFza0NvbnRlbnRVbml0cz0ib2JqZWN0Qm91bmRpbmdCb3giPjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9InVybCgjZ3JhZC11cCkiIC8+PC9tYXNrPjxtYXNrIGlkPSJmYWRlLWRvd24iIG1hc2tDb250ZW50VW5pdHM9Im9iamVjdEJvdW5kaW5nQm94Ij48cmVjdCB3aWR0aD0iMSIgaGVpZ2h0PSIxIiBmaWxsPSJ1cmwoI2dyYWQtZG93bikiIC8+PC9tYXNrPjxtYXNrIGlkPSJub25lIiBtYXNrQ29udGVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCI+PHJlY3Qgd2lkdGg9IjEiIGhlaWdodD0iMSIgZmlsbD0id2hpdGUiIC8+PC9tYXNrPjxsaW5lYXJHcmFkaWVudCBpZD0iZ3JhZC1zeW1ib2wiPjxzdG9wIG9mZnNldD0iMC43IiBzdG9wLWNvbG9yPSJ3aGl0ZSIgc3RvcC1vcGFjaXR5PSIxIiAvPjxzdG9wIG9mZnNldD0iLjk1IiBzdG9wLWNvbG9yPSJ3aGl0ZSIgc3RvcC1vcGFjaXR5PSIwIiAvPjwvbGluZWFyR3JhZGllbnQ+PG1hc2sgaWQ9ImZhZGUtc3ltYm9sIiBtYXNrQ29udGVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHJlY3Qgd2lkdGg9IjI5MHB4IiBoZWlnaHQ9IjIwMHB4IiBmaWxsPSJ1cmwoI2dyYWQtc3ltYm9sKSIgLz48L21hc2s+PC9kZWZzPjxnIGNsaXAtcGF0aD0idXJsKCNjb3JuZXJzKSI+PHJlY3QgZmlsbD0iN2QxYWZhIiB4PSIwcHgiIHk9IjBweCIgd2lkdGg9IjI5MHB4IiBoZWlnaHQ9IjUwMHB4IiAvPjxyZWN0IHN0eWxlPSJmaWx0ZXI6IHVybCgjZjEpIiB4PSIwcHgiIHk9IjBweCIgd2lkdGg9IjI5MHB4IiBoZWlnaHQ9IjUwMHB4IiAvPiA8ZyBzdHlsZT0iZmlsdGVyOnVybCgjdG9wLXJlZ2lvbi1ibHVyKTsgdHJhbnNmb3JtOnNjYWxlKDEuNSk7IHRyYW5zZm9ybS1vcmlnaW46Y2VudGVyIHRvcDsiPjxyZWN0IGZpbGw9Im5vbmUiIHg9IjBweCIgeT0iMHB4IiB3aWR0aD0iMjkwcHgiIGhlaWdodD0iNTAwcHgiIC8+PGVsbGlwc2UgY3g9IjUwJSIgY3k9IjBweCIgcng9IjE4MHB4IiByeT0iMTIwcHgiIGZpbGw9IiMwMDAiIG9wYWNpdHk9IjAuODUiIC8+PC9nPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyOTAiIGhlaWdodD0iNTAwIiByeD0iNDIiIHJ5PSI0MiIgZmlsbD0icmdiYSgwLDAsMCwwKSIgc3Ryb2tlPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMikiIC8+PC9nPjx0ZXh0IHRleHQtcmVuZGVyaW5nPSJvcHRpbWl6ZVNwZWVkIj48dGV4dFBhdGggc3RhcnRPZmZzZXQ9Ii0xMDAlIiBmaWxsPSJ3aGl0ZSIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMHB4IiB4bGluazpocmVmPSIjdGV4dC1wYXRoLWEiPjB4YzAyYWFhMzliMjIzZmU4ZDBhMGU1YzRmMjdlYWQ5MDgzYzc1NmNjMiDigKIgV0VUSCA8YW5pbWF0ZSBhZGRpdGl2ZT0ic3VtIiBhdHRyaWJ1dGVOYW1lPSJzdGFydE9mZnNldCIgZnJvbT0iMCUiIHRvPSIxMDAlIiBiZWdpbj0iMHMiIGR1cj0iMzBzIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIgLz48L3RleHRQYXRoPiA8dGV4dFBhdGggc3RhcnRPZmZzZXQ9IjAlIiBmaWxsPSJ3aGl0ZSIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMHB4IiB4bGluazpocmVmPSIjdGV4dC1wYXRoLWEiPjB4YzAyYWFhMzliMjIzZmU4ZDBhMGU1YzRmMjdlYWQ5MDgzYzc1NmNjMiDigKIgV0VUSCA8YW5pbWF0ZSBhZGRpdGl2ZT0ic3VtIiBhdHRyaWJ1dGVOYW1lPSJzdGFydE9mZnNldCIgZnJvbT0iMCUiIHRvPSIxMDAlIiBiZWdpbj0iMHMiIGR1cj0iMzBzIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIgLz4gPC90ZXh0UGF0aD48dGV4dFBhdGggc3RhcnRPZmZzZXQ9IjUwJSIgZmlsbD0id2hpdGUiIGZvbnQtZmFtaWx5PSInQ291cmllciBOZXcnLCBtb25vc3BhY2UiIGZvbnQtc2l6ZT0iMTBweCIgeGxpbms6aHJlZj0iI3RleHQtcGF0aC1hIj4weDdkMWFmYTdiNzE4ZmI4OTNkYjMwYTNhYmMwY2ZjNjA4YWFjZmViYjAg4oCiIE1BVElDIDxhbmltYXRlIGFkZGl0aXZlPSJzdW0iIGF0dHJpYnV0ZU5hbWU9InN0YXJ0T2Zmc2V0IiBmcm9tPSIwJSIgdG89IjEwMCUiIGJlZ2luPSIwcyIgZHVyPSIzMHMiIHJlcGVhdENvdW50PSJpbmRlZmluaXRlIiAvPjwvdGV4dFBhdGg+PHRleHRQYXRoIHN0YXJ0T2Zmc2V0PSItNTAlIiBmaWxsPSJ3aGl0ZSIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMHB4IiB4bGluazpocmVmPSIjdGV4dC1wYXRoLWEiPjB4N2QxYWZhN2I3MThmYjg5M2RiMzBhM2FiYzBjZmM2MDhhYWNmZWJiMCDigKIgTUFUSUMgPGFuaW1hdGUgYWRkaXRpdmU9InN1bSIgYXR0cmlidXRlTmFtZT0ic3RhcnRPZmZzZXQiIGZyb209IjAlIiB0bz0iMTAwJSIgYmVnaW49IjBzIiBkdXI9IjMwcyIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiIC8+PC90ZXh0UGF0aD48L3RleHQ+PGcgbWFzaz0idXJsKCNmYWRlLXN5bWJvbCkiPjxyZWN0IGZpbGw9Im5vbmUiIHg9IjBweCIgeT0iMHB4IiB3aWR0aD0iMjkwcHgiIGhlaWdodD0iMjAwcHgiIC8+IDx0ZXh0IHk9IjcwcHgiIHg9IjMycHgiIGZpbGw9IndoaXRlIiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXdlaWdodD0iMjAwIiBmb250LXNpemU9IjM2cHgiPk1BVElDL1dFVEg8L3RleHQ+PHRleHQgeT0iMTE1cHgiIHg9IjMycHgiIGZpbGw9IndoaXRlIiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXdlaWdodD0iMjAwIiBmb250LXNpemU9IjM2cHgiPjAuMyU8L3RleHQ+PC9nPjxyZWN0IHg9IjE2IiB5PSIxNiIgd2lkdGg9IjI1OCIgaGVpZ2h0PSI0NjgiIHJ4PSIyNiIgcnk9IjI2IiBmaWxsPSJyZ2JhKDAsMCwwLDApIiBzdHJva2U9InJnYmEoMjU1LDI1NSwyNTUsMC4yKSIgLz48ZyBtYXNrPSJ1cmwoI2ZhZGUtZG93bikiIHN0eWxlPSJ0cmFuc2Zvcm06dHJhbnNsYXRlKDcycHgsMTg5cHgpIj48cmVjdCB4PSItMTZweCIgeT0iLTE2cHgiIHdpZHRoPSIxODBweCIgaGVpZ2h0PSIxODBweCIgZmlsbD0ibm9uZSIgLz48cGF0aCBkPSJNMSAxQzMzIDQ5IDk3IDExMyAxNDUgMTQ1IiBzdHJva2U9InJnYmEoMCwwLDAsMC4zKSIgc3Ryb2tlLXdpZHRoPSIzMnB4IiBmaWxsPSJub25lIiBzdHJva2UtbGluZWNhcD0icm91bmQiIC8+PC9nPjxnIG1hc2s9InVybCgjZmFkZS1kb3duKSIgc3R5bGU9InRyYW5zZm9ybTp0cmFuc2xhdGUoNzJweCwxODlweCkiPjxyZWN0IHg9Ii0xNnB4IiB5PSItMTZweCIgd2lkdGg9IjE4MHB4IiBoZWlnaHQ9IjE4MHB4IiBmaWxsPSJub25lIiAvPjxwYXRoIGQ9Ik0xIDFDMzMgNDkgOTcgMTEzIDE0NSAxNDUiIHN0cm9rZT0icmdiYSgyNTUsMjU1LDI1NSwxKSIgZmlsbD0ibm9uZSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiAvPjwvZz48Y2lyY2xlIGN4PSI3M3B4IiBjeT0iMTkwcHgiIHI9IjRweCIgZmlsbD0id2hpdGUiIC8+PGNpcmNsZSBjeD0iNzNweCIgY3k9IjE5MHB4IiByPSIyNHB4IiBmaWxsPSJub25lIiBzdHJva2U9IndoaXRlIiAvPiA8ZyBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZSgyOXB4LCAzODRweCkiPjxyZWN0IHdpZHRoPSI5MXB4IiBoZWlnaHQ9IjI2cHgiIHJ4PSI4cHgiIHJ5PSI4cHgiIGZpbGw9InJnYmEoMCwwLDAsMC42KSIgLz48dGV4dCB4PSIxMnB4IiB5PSIxN3B4IiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXNpemU9IjEycHgiIGZpbGw9IndoaXRlIj48dHNwYW4gZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjYpIj5JRDogPC90c3Bhbj41MTU2MTwvdGV4dD48L2c+IDxnIHN0eWxlPSJ0cmFuc2Zvcm06dHJhbnNsYXRlKDI5cHgsIDQxNHB4KSI+PHJlY3Qgd2lkdGg9IjE0MHB4IiBoZWlnaHQ9IjI2cHgiIHJ4PSI4cHgiIHJ5PSI4cHgiIGZpbGw9InJnYmEoMCwwLDAsMC42KSIgLz48dGV4dCB4PSIxMnB4IiB5PSIxN3B4IiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXNpemU9IjEycHgiIGZpbGw9IndoaXRlIj48dHNwYW4gZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjYpIj5NaW4gVGljazogPC90c3Bhbj4tNzM4MDA8L3RleHQ+PC9nPiA8ZyBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZSgyOXB4LCA0NDRweCkiPjxyZWN0IHdpZHRoPSIxNDBweCIgaGVpZ2h0PSIyNnB4IiByeD0iOHB4IiByeT0iOHB4IiBmaWxsPSJyZ2JhKDAsMCwwLDAuNikiIC8+PHRleHQgeD0iMTJweCIgeT0iMTdweCIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMnB4IiBmaWxsPSJ3aGl0ZSI+PHRzcGFuIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC42KSI+TWF4IFRpY2s6IDwvdHNwYW4+LTczNTAwPC90ZXh0PjwvZz48ZyBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZSgyMjZweCwgNDMzcHgpIj48cmVjdCB3aWR0aD0iMzZweCIgaGVpZ2h0PSIzNnB4IiByeD0iOHB4IiByeT0iOHB4IiBmaWxsPSJub25lIiBzdHJva2U9InJnYmEoMjU1LDI1NSwyNTUsMC4yKSIgLz48cGF0aCBzdHJva2UtbGluZWNhcD0icm91bmQiIGQ9Ik04IDlDOC4wMDAwNCAyMi45NDk0IDE2LjIwOTkgMjggMjcgMjgiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIC8+PGNpcmNsZSBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZTNkKDhweCwgMTAuNXB4LCAwcHgpIiBjeD0iMHB4IiBjeT0iMHB4IiByPSI0cHgiIGZpbGw9IndoaXRlIi8+PC9nPjwvc3ZnPg=="}""",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjkwIiBoZWlnaHQ9IjUwMCIgdmlld0JveD0iMCAwIDI5MCA1MDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9J2h0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsnPjxkZWZzPjxmaWx0ZXIgaWQ9ImYxIj48ZmVJbWFnZSByZXN1bHQ9InAwIiB4bGluazpocmVmPSJkYXRhOmltYWdlL3N2Zyt4bWw7YmFzZTY0LFBITjJaeUIzYVdSMGFEMG5Namt3SnlCb1pXbG5hSFE5SnpVd01DY2dkbWxsZDBKdmVEMG5NQ0F3SURJNU1DQTFNREFuSUhodGJHNXpQU2RvZEhSd09pOHZkM2QzTG5jekxtOXlaeTh5TURBd0wzTjJaeWMrUEhKbFkzUWdkMmxrZEdnOUp6STVNSEI0SnlCb1pXbG5hSFE5SnpVd01IQjRKeUJtYVd4c1BTY2pOMlF4WVdaaEp5OCtQQzl6ZG1jKyIvPjxmZUltYWdlIHJlc3VsdD0icDEiIHhsaW5rOmhyZWY9ImRhdGE6aW1hZ2Uvc3ZnK3htbDtiYXNlNjQsUEhOMlp5QjNhV1IwYUQwbk1qa3dKeUJvWldsbmFIUTlKelV3TUNjZ2RtbGxkMEp2ZUQwbk1DQXdJREk1TUNBMU1EQW5JSGh0Ykc1elBTZG9kSFJ3T2k4dmQzZDNMbmN6TG05eVp5OHlNREF3TDNOMlp5YytQR05wY21Oc1pTQmplRDBuTVRFNUp5QmplVDBuTWpVekp5QnlQU2N4TWpCd2VDY2dabWxzYkQwbkkyTXdNbUZoWVNjdlBqd3ZjM1puUGc9PSIvPjxmZUltYWdlIHJlc3VsdD0icDIiIHhsaW5rOmhyZWY9ImRhdGE6aW1hZ2Uvc3ZnK3htbDtiYXNlNjQsUEhOMlp5QjNhV1IwYUQwbk1qa3dKeUJvWldsbmFIUTlKelV3TUNjZ2RtbGxkMEp2ZUQwbk1DQXdJREk1TUNBMU1EQW5JSGh0Ykc1elBTZG9kSFJ3T2k4dmQzZDNMbmN6TG05eVp5OHlNREF3TDNOMlp5YytQR05wY21Oc1pTQmplRDBuTVRjd0p5QmplVDBuTXpNd0p5QnlQU2N4TWpCd2VDY2dabWxzYkQwbkkyTm1aV0ppTUNjdlBqd3ZjM1puUGc9PSIgLz48ZmVJbWFnZSByZXN1bHQ9InAzIiB4bGluazpocmVmPSJkYXRhOmltYWdlL3N2Zyt4bWw7YmFzZTY0LFBITjJaeUIzYVdSMGFEMG5Namt3SnlCb1pXbG5hSFE5SnpVd01DY2dkbWxsZDBKdmVEMG5NQ0F3SURJNU1DQTFNREFuSUhodGJHNXpQU2RvZEhSd09pOHZkM2QzTG5jekxtOXlaeTh5TURBd0wzTjJaeWMrUEdOcGNtTnNaU0JqZUQwbk1URTVKeUJqZVQwbk5EQTNKeUJ5UFNjeE1EQndlQ2NnWm1sc2JEMG5JemMxTm1Oak1pY3ZQand2YzNablBnPT0iIC8+PGZlQmxlbmQgbW9kZT0ib3ZlcmxheSIgaW49InAwIiBpbjI9InAxIiAvPjxmZUJsZW5kIG1vZGU9ImV4Y2x1c2lvbiIgaW4yPSJwMiIgLz48ZmVCbGVuZCBtb2RlPSJvdmVybGF5IiBpbjI9InAzIiByZXN1bHQ9ImJsZW5kT3V0IiAvPjxmZUdhdXNzaWFuQmx1ciBpbj0iYmxlbmRPdXQiIHN0ZERldmlhdGlvbj0iNDIiIC8+PC9maWx0ZXI+IDxjbGlwUGF0aCBpZD0iY29ybmVycyI+PHJlY3Qgd2lkdGg9IjI5MCIgaGVpZ2h0PSI1MDAiIHJ4PSI0MiIgcnk9IjQyIiAvPjwvY2xpcFBhdGg+PHBhdGggaWQ9InRleHQtcGF0aC1hIiBkPSJNNDAgMTIgSDI1MCBBMjggMjggMCAwIDEgMjc4IDQwIFY0NjAgQTI4IDI4IDAgMCAxIDI1MCA0ODggSDQwIEEyOCAyOCAwIDAgMSAxMiA0NjAgVjQwIEEyOCAyOCAwIDAgMSA0MCAxMiB6IiAvPjxwYXRoIGlkPSJtaW5pbWFwIiBkPSJNMjM0IDQ0NEMyMzQgNDU3Ljk0OSAyNDIuMjEgNDYzIDI1MyA0NjMiIC8+PGZpbHRlciBpZD0idG9wLXJlZ2lvbi1ibHVyIj48ZmVHYXVzc2lhbkJsdXIgaW49IlNvdXJjZUdyYXBoaWMiIHN0ZERldmlhdGlvbj0iMjQiIC8+PC9maWx0ZXI+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVwIiB4MT0iMSIgeDI9IjAiIHkxPSIxIiB5Mj0iMCI+PHN0b3Agb2Zmc2V0PSIwLjAiIHN0b3AtY29sb3I9IndoaXRlIiBzdG9wLW9wYWNpdHk9IjEiIC8+PHN0b3Agb2Zmc2V0PSIuOSIgc3RvcC1jb2xvcj0id2hpdGUiIHN0b3Atb3BhY2l0eT0iMCIgLz48L2xpbmVhckdyYWRpZW50PjxsaW5lYXJHcmFkaWVudCBpZD0iZ3JhZC1kb3duIiB4MT0iMCIgeDI9IjEiIHkxPSIwIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwLjAiIHN0b3AtY29sb3I9IndoaXRlIiBzdG9wLW9wYWNpdHk9IjEiIC8+PHN0b3Agb2Zmc2V0PSIwLjkiIHN0b3AtY29sb3I9IndoaXRlIiBzdG9wLW9wYWNpdHk9IjAiIC8+PC9saW5lYXJHcmFkaWVudD48bWFzayBpZD0iZmFkZS11cCIgbWFza0NvbnRlbnRVbml0cz0ib2JqZWN0Qm91bmRpbmdCb3giPjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9InVybCgjZ3JhZC11cCkiIC8+PC9tYXNrPjxtYXNrIGlkPSJmYWRlLWRvd24iIG1hc2tDb250ZW50VW5pdHM9Im9iamVjdEJvdW5kaW5nQm94Ij48cmVjdCB3aWR0aD0iMSIgaGVpZ2h0PSIxIiBmaWxsPSJ1cmwoI2dyYWQtZG93bikiIC8+PC9tYXNrPjxtYXNrIGlkPSJub25lIiBtYXNrQ29udGVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCI+PHJlY3Qgd2lkdGg9IjEiIGhlaWdodD0iMSIgZmlsbD0id2hpdGUiIC8+PC9tYXNrPjxsaW5lYXJHcmFkaWVudCBpZD0iZ3JhZC1zeW1ib2wiPjxzdG9wIG9mZnNldD0iMC43IiBzdG9wLWNvbG9yPSJ3aGl0ZSIgc3RvcC1vcGFjaXR5PSIxIiAvPjxzdG9wIG9mZnNldD0iLjk1IiBzdG9wLWNvbG9yPSJ3aGl0ZSIgc3RvcC1vcGFjaXR5PSIwIiAvPjwvbGluZWFyR3JhZGllbnQ+PG1hc2sgaWQ9ImZhZGUtc3ltYm9sIiBtYXNrQ29udGVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHJlY3Qgd2lkdGg9IjI5MHB4IiBoZWlnaHQ9IjIwMHB4IiBmaWxsPSJ1cmwoI2dyYWQtc3ltYm9sKSIgLz48L21hc2s+PC9kZWZzPjxnIGNsaXAtcGF0aD0idXJsKCNjb3JuZXJzKSI+PHJlY3QgZmlsbD0iN2QxYWZhIiB4PSIwcHgiIHk9IjBweCIgd2lkdGg9IjI5MHB4IiBoZWlnaHQ9IjUwMHB4IiAvPjxyZWN0IHN0eWxlPSJmaWx0ZXI6IHVybCgjZjEpIiB4PSIwcHgiIHk9IjBweCIgd2lkdGg9IjI5MHB4IiBoZWlnaHQ9IjUwMHB4IiAvPiA8ZyBzdHlsZT0iZmlsdGVyOnVybCgjdG9wLXJlZ2lvbi1ibHVyKTsgdHJhbnNmb3JtOnNjYWxlKDEuNSk7IHRyYW5zZm9ybS1vcmlnaW46Y2VudGVyIHRvcDsiPjxyZWN0IGZpbGw9Im5vbmUiIHg9IjBweCIgeT0iMHB4IiB3aWR0aD0iMjkwcHgiIGhlaWdodD0iNTAwcHgiIC8+PGVsbGlwc2UgY3g9IjUwJSIgY3k9IjBweCIgcng9IjE4MHB4IiByeT0iMTIwcHgiIGZpbGw9IiMwMDAiIG9wYWNpdHk9IjAuODUiIC8+PC9nPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyOTAiIGhlaWdodD0iNTAwIiByeD0iNDIiIHJ5PSI0MiIgZmlsbD0icmdiYSgwLDAsMCwwKSIgc3Ryb2tlPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMikiIC8+PC9nPjx0ZXh0IHRleHQtcmVuZGVyaW5nPSJvcHRpbWl6ZVNwZWVkIj48dGV4dFBhdGggc3RhcnRPZmZzZXQ9Ii0xMDAlIiBmaWxsPSJ3aGl0ZSIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMHB4IiB4bGluazpocmVmPSIjdGV4dC1wYXRoLWEiPjB4YzAyYWFhMzliMjIzZmU4ZDBhMGU1YzRmMjdlYWQ5MDgzYzc1NmNjMiDigKIgV0VUSCA8YW5pbWF0ZSBhZGRpdGl2ZT0ic3VtIiBhdHRyaWJ1dGVOYW1lPSJzdGFydE9mZnNldCIgZnJvbT0iMCUiIHRvPSIxMDAlIiBiZWdpbj0iMHMiIGR1cj0iMzBzIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIgLz48L3RleHRQYXRoPiA8dGV4dFBhdGggc3RhcnRPZmZzZXQ9IjAlIiBmaWxsPSJ3aGl0ZSIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMHB4IiB4bGluazpocmVmPSIjdGV4dC1wYXRoLWEiPjB4YzAyYWFhMzliMjIzZmU4ZDBhMGU1YzRmMjdlYWQ5MDgzYzc1NmNjMiDigKIgV0VUSCA8YW5pbWF0ZSBhZGRpdGl2ZT0ic3VtIiBhdHRyaWJ1dGVOYW1lPSJzdGFydE9mZnNldCIgZnJvbT0iMCUiIHRvPSIxMDAlIiBiZWdpbj0iMHMiIGR1cj0iMzBzIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIgLz4gPC90ZXh0UGF0aD48dGV4dFBhdGggc3RhcnRPZmZzZXQ9IjUwJSIgZmlsbD0id2hpdGUiIGZvbnQtZmFtaWx5PSInQ291cmllciBOZXcnLCBtb25vc3BhY2UiIGZvbnQtc2l6ZT0iMTBweCIgeGxpbms6aHJlZj0iI3RleHQtcGF0aC1hIj4weDdkMWFmYTdiNzE4ZmI4OTNkYjMwYTNhYmMwY2ZjNjA4YWFjZmViYjAg4oCiIE1BVElDIDxhbmltYXRlIGFkZGl0aXZlPSJzdW0iIGF0dHJpYnV0ZU5hbWU9InN0YXJ0T2Zmc2V0IiBmcm9tPSIwJSIgdG89IjEwMCUiIGJlZ2luPSIwcyIgZHVyPSIzMHMiIHJlcGVhdENvdW50PSJpbmRlZmluaXRlIiAvPjwvdGV4dFBhdGg+PHRleHRQYXRoIHN0YXJ0T2Zmc2V0PSItNTAlIiBmaWxsPSJ3aGl0ZSIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMHB4IiB4bGluazpocmVmPSIjdGV4dC1wYXRoLWEiPjB4N2QxYWZhN2I3MThmYjg5M2RiMzBhM2FiYzBjZmM2MDhhYWNmZWJiMCDigKIgTUFUSUMgPGFuaW1hdGUgYWRkaXRpdmU9InN1bSIgYXR0cmlidXRlTmFtZT0ic3RhcnRPZmZzZXQiIGZyb209IjAlIiB0bz0iMTAwJSIgYmVnaW49IjBzIiBkdXI9IjMwcyIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiIC8+PC90ZXh0UGF0aD48L3RleHQ+PGcgbWFzaz0idXJsKCNmYWRlLXN5bWJvbCkiPjxyZWN0IGZpbGw9Im5vbmUiIHg9IjBweCIgeT0iMHB4IiB3aWR0aD0iMjkwcHgiIGhlaWdodD0iMjAwcHgiIC8+IDx0ZXh0IHk9IjcwcHgiIHg9IjMycHgiIGZpbGw9IndoaXRlIiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXdlaWdodD0iMjAwIiBmb250LXNpemU9IjM2cHgiPk1BVElDL1dFVEg8L3RleHQ+PHRleHQgeT0iMTE1cHgiIHg9IjMycHgiIGZpbGw9IndoaXRlIiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXdlaWdodD0iMjAwIiBmb250LXNpemU9IjM2cHgiPjAuMyU8L3RleHQ+PC9nPjxyZWN0IHg9IjE2IiB5PSIxNiIgd2lkdGg9IjI1OCIgaGVpZ2h0PSI0NjgiIHJ4PSIyNiIgcnk9IjI2IiBmaWxsPSJyZ2JhKDAsMCwwLDApIiBzdHJva2U9InJnYmEoMjU1LDI1NSwyNTUsMC4yKSIgLz48ZyBtYXNrPSJ1cmwoI2ZhZGUtZG93bikiIHN0eWxlPSJ0cmFuc2Zvcm06dHJhbnNsYXRlKDcycHgsMTg5cHgpIj48cmVjdCB4PSItMTZweCIgeT0iLTE2cHgiIHdpZHRoPSIxODBweCIgaGVpZ2h0PSIxODBweCIgZmlsbD0ibm9uZSIgLz48cGF0aCBkPSJNMSAxQzMzIDQ5IDk3IDExMyAxNDUgMTQ1IiBzdHJva2U9InJnYmEoMCwwLDAsMC4zKSIgc3Ryb2tlLXdpZHRoPSIzMnB4IiBmaWxsPSJub25lIiBzdHJva2UtbGluZWNhcD0icm91bmQiIC8+PC9nPjxnIG1hc2s9InVybCgjZmFkZS1kb3duKSIgc3R5bGU9InRyYW5zZm9ybTp0cmFuc2xhdGUoNzJweCwxODlweCkiPjxyZWN0IHg9Ii0xNnB4IiB5PSItMTZweCIgd2lkdGg9IjE4MHB4IiBoZWlnaHQ9IjE4MHB4IiBmaWxsPSJub25lIiAvPjxwYXRoIGQ9Ik0xIDFDMzMgNDkgOTcgMTEzIDE0NSAxNDUiIHN0cm9rZT0icmdiYSgyNTUsMjU1LDI1NSwxKSIgZmlsbD0ibm9uZSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiAvPjwvZz48Y2lyY2xlIGN4PSI3M3B4IiBjeT0iMTkwcHgiIHI9IjRweCIgZmlsbD0id2hpdGUiIC8+PGNpcmNsZSBjeD0iNzNweCIgY3k9IjE5MHB4IiByPSIyNHB4IiBmaWxsPSJub25lIiBzdHJva2U9IndoaXRlIiAvPiA8ZyBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZSgyOXB4LCAzODRweCkiPjxyZWN0IHdpZHRoPSI5MXB4IiBoZWlnaHQ9IjI2cHgiIHJ4PSI4cHgiIHJ5PSI4cHgiIGZpbGw9InJnYmEoMCwwLDAsMC42KSIgLz48dGV4dCB4PSIxMnB4IiB5PSIxN3B4IiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXNpemU9IjEycHgiIGZpbGw9IndoaXRlIj48dHNwYW4gZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjYpIj5JRDogPC90c3Bhbj41MTU2MTwvdGV4dD48L2c+IDxnIHN0eWxlPSJ0cmFuc2Zvcm06dHJhbnNsYXRlKDI5cHgsIDQxNHB4KSI+PHJlY3Qgd2lkdGg9IjE0MHB4IiBoZWlnaHQ9IjI2cHgiIHJ4PSI4cHgiIHJ5PSI4cHgiIGZpbGw9InJnYmEoMCwwLDAsMC42KSIgLz48dGV4dCB4PSIxMnB4IiB5PSIxN3B4IiBmb250LWZhbWlseT0iJ0NvdXJpZXIgTmV3JywgbW9ub3NwYWNlIiBmb250LXNpemU9IjEycHgiIGZpbGw9IndoaXRlIj48dHNwYW4gZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjYpIj5NaW4gVGljazogPC90c3Bhbj4tNzM4MDA8L3RleHQ+PC9nPiA8ZyBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZSgyOXB4LCA0NDRweCkiPjxyZWN0IHdpZHRoPSIxNDBweCIgaGVpZ2h0PSIyNnB4IiByeD0iOHB4IiByeT0iOHB4IiBmaWxsPSJyZ2JhKDAsMCwwLDAuNikiIC8+PHRleHQgeD0iMTJweCIgeT0iMTdweCIgZm9udC1mYW1pbHk9IidDb3VyaWVyIE5ldycsIG1vbm9zcGFjZSIgZm9udC1zaXplPSIxMnB4IiBmaWxsPSJ3aGl0ZSI+PHRzcGFuIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC42KSI+TWF4IFRpY2s6IDwvdHNwYW4+LTczNTAwPC90ZXh0PjwvZz48ZyBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZSgyMjZweCwgNDMzcHgpIj48cmVjdCB3aWR0aD0iMzZweCIgaGVpZ2h0PSIzNnB4IiByeD0iOHB4IiByeT0iOHB4IiBmaWxsPSJub25lIiBzdHJva2U9InJnYmEoMjU1LDI1NSwyNTUsMC4yKSIgLz48cGF0aCBzdHJva2UtbGluZWNhcD0icm91bmQiIGQ9Ik04IDlDOC4wMDAwNCAyMi45NDk0IDE2LjIwOTkgMjggMjcgMjgiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIC8+PGNpcmNsZSBzdHlsZT0idHJhbnNmb3JtOnRyYW5zbGF0ZTNkKDhweCwgMTAuNXB4LCAwcHgpIiBjeD0iMHB4IiBjeT0iMHB4IiByPSI0cHgiIGZpbGw9IndoaXRlIi8+PC9nPjwvc3ZnPg=="
                )
            )
        )
    }

    @Test
    fun erc1155() = runBlocking<Unit> {
        val address = Address.apply("0xfaafdc07907ff5120a76b34b731b278c38d6043c")
        mockTokenStandard(address, TokenStandard.ERC1155)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                address,
                EthUInt256(
                    "10855508365998400056289941914472950957046112164229867356526540410650888241152".toBigInteger()
                )
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Argent",
                description = "Straight forward, no fuss, iron cast stock and woodgrain grips. \"The Argent Defender.\"",
                attributes = emptyList(),
                rawJsonContent = """{"name":"Argent","description":"Straight forward, no fuss, iron cast stock and woodgrain grips. \"The Argent Defender.\"","image":"https://alterverse.sfo2.digitaloceanspaces.com/WalletArt/Disruption/Intimidators/WalletArt_Argent.jpg"}""",
                tokenUri = "https://alterverse.sfo2.digitaloceanspaces.com/argent.json",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://alterverse.sfo2.digitaloceanspaces.com/WalletArt/Disruption/Intimidators/WalletArt_Argent.jpg"
                )
            )
        )
    }

    @Test
    fun `she ava`() = runBlocking<Unit> {
        val address = Address.apply("0xb74d8477f9724d974af87436ac0371d3899c6415")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                address,
                EthUInt256.of(44)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "SHE AVA | Madame #32",
                description = "",
                attributes = listOf(
                    ItemAttribute("Background", "Velvet"),
                    ItemAttribute("Hair", " None"),
                    ItemAttribute("Headdress", "Crown"),
                    ItemAttribute("Dress", "Black")
                ),
                rawJsonContent = """{"name":"SHE AVA | Madame #32","description":"","image":"ipfs://ipfs/QmNfi2Muxm56SU3Kxy6tfcXcgofT9N7fiMC9sQPDZU3hqi/image.png","external_url":"https://rarible.com/token/0xb74d8477f9724d974af87436ac0371d3899c6415:44","attributes":[{"key":"Background","trait_type":"Background","value":"Velvet"},{"key":"Hair","trait_type":"Hair","value":" None"},{"key":"Headdress","trait_type":"Headdress","value":"Crown"},{"key":"Dress","trait_type":"Dress","value":"Black"}]}""",
                tokenUri = "ipfs://ipfs/QmSvGJYvQkM87q4s2LrVjDhgMvSV1xx2D86BDZe7QC53HC",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "ipfs://ipfs/QmNfi2Muxm56SU3Kxy6tfcXcgofT9N7fiMC9sQPDZU3hqi/image.png"
                )
            )
        )
    }

    @Test
    fun `collection with built-in svg`() = runBlocking<Unit> {
        val address = Address.apply("0xff9c1b15b16263c61d017ee9f65c50e4ae0113d7")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                address,
                EthUInt256.of(44)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Bag #44",
                description = "Loot is randomized adventurer gear generated and stored on chain. Stats, images, and other functionality are intentionally omitted for others to interpret. Feel free to use Loot in any way you want.",
                attributes = emptyList(),
                rawJsonContent = "{\"name\":\"Bag #44\",\"description\":\"Loot is randomized adventurer gear generated and stored on chain. Stats, images, and other functionality are intentionally omitted for others to interpret. Feel free to use Loot in any way you want.\",\"image\":\"data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHByZXNlcnZlQXNwZWN0UmF0aW89InhNaW5ZTWluIG1lZXQiIHZpZXdCb3g9IjAgMCAzNTAgMzUwIj48c3R5bGU+LmJhc2UgeyBmaWxsOiB3aGl0ZTsgZm9udC1mYW1pbHk6IHNlcmlmOyBmb250LXNpemU6IDE0cHg7IH08L3N0eWxlPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9ImJsYWNrIiAvPjx0ZXh0IHg9IjEwIiB5PSIyMCIgY2xhc3M9ImJhc2UiPlNob3J0IFN3b3JkPC90ZXh0Pjx0ZXh0IHg9IjEwIiB5PSI0MCIgY2xhc3M9ImJhc2UiPkNoYWluIE1haWw8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjYwIiBjbGFzcz0iYmFzZSI+SG9vZDwvdGV4dD48dGV4dCB4PSIxMCIgeT0iODAiIGNsYXNzPSJiYXNlIj5Xb29sIFNhc2g8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjEwMCIgY2xhc3M9ImJhc2UiPlNpbGsgU2xpcHBlcnM8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjEyMCIgY2xhc3M9ImJhc2UiPkxlYXRoZXIgR2xvdmVzPC90ZXh0Pjx0ZXh0IHg9IjEwIiB5PSIxNDAiIGNsYXNzPSJiYXNlIj5BbXVsZXQ8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjE2MCIgY2xhc3M9ImJhc2UiPlNpbHZlciBSaW5nPC90ZXh0Pjwvc3ZnPg==\"}",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHByZXNlcnZlQXNwZWN0UmF0aW89InhNaW5ZTWluIG1lZXQiIHZpZXdCb3g9IjAgMCAzNTAgMzUwIj48c3R5bGU+LmJhc2UgeyBmaWxsOiB3aGl0ZTsgZm9udC1mYW1pbHk6IHNlcmlmOyBmb250LXNpemU6IDE0cHg7IH08L3N0eWxlPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9ImJsYWNrIiAvPjx0ZXh0IHg9IjEwIiB5PSIyMCIgY2xhc3M9ImJhc2UiPlNob3J0IFN3b3JkPC90ZXh0Pjx0ZXh0IHg9IjEwIiB5PSI0MCIgY2xhc3M9ImJhc2UiPkNoYWluIE1haWw8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjYwIiBjbGFzcz0iYmFzZSI+SG9vZDwvdGV4dD48dGV4dCB4PSIxMCIgeT0iODAiIGNsYXNzPSJiYXNlIj5Xb29sIFNhc2g8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjEwMCIgY2xhc3M9ImJhc2UiPlNpbGsgU2xpcHBlcnM8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjEyMCIgY2xhc3M9ImJhc2UiPkxlYXRoZXIgR2xvdmVzPC90ZXh0Pjx0ZXh0IHg9IjEwIiB5PSIxNDAiIGNsYXNzPSJiYXNlIj5BbXVsZXQ8L3RleHQ+PHRleHQgeD0iMTAiIHk9IjE2MCIgY2xhc3M9ImJhc2UiPlNpbHZlciBSaW5nPC90ZXh0Pjwvc3ZnPg=="
                )
            )
        )
    }

    @Test
    fun `tokenURI is redirected`() = runBlocking<Unit> {
        val address = Address.apply("0xbd13e53255ef917da7557db1b7d2d5c38a2efe24")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(ItemId(address, EthUInt256.of(967928015015L)))
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Rio",
                description = "Rio is DozerFriends’ adorable yellow bear, happy as sunshine and always thirsty for his favourite drink made of yellow flowers.",
                attributes = emptyList(),
                rawJsonContent = """{"name":"Rio","description":"Rio is DozerFriends’ adorable yellow bear, happy as sunshine and always thirsty for his favourite drink made of yellow flowers.","language":"en-US","image":"https://cryptodozer.io/static/images/dozer/meta/dolls/100.png","attributes":{"id":"100","type":"Bear00","grade":"bronze","star":"1"}}""",
                tokenUri = "http://me2.do/GSBSXU8g",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://cryptodozer.io/static/images/dozer/meta/dolls/100.png"
                )
            )
        )
    }

    @Test
    fun boredApeYachtClub() = runBlocking<Unit> {
        val address = Address.apply("0xbc4ca0eda7647a8ab7c2061c2e118a18a936f13d")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                address,
                EthUInt256.of(9163)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "BoredApeYachtClub #9163",
                description = null,
                attributes = listOf(
                    ItemAttribute("Eyes", "Bored"),
                    ItemAttribute("Clothes", "Biker Vest"),
                    ItemAttribute("Mouth", "Phoneme Vuh"),
                    ItemAttribute("Hat", "Army Hat"),
                    ItemAttribute("Background", "New Punk Blue"),
                    ItemAttribute("Fur", "Black")
                ),
                rawJsonContent = """{"image":"ipfs://QmQ5aovKD1HRG1GR2NYspJEQt758RCm8pMuv9CGFoK5yhy","attributes":[{"trait_type":"Eyes","value":"Bored"},{"trait_type":"Clothes","value":"Biker Vest"},{"trait_type":"Mouth","value":"Phoneme Vuh"},{"trait_type":"Hat","value":"Army Hat"},{"trait_type":"Background","value":"New Punk Blue"},{"trait_type":"Fur","value":"Black"}]}""",
                tokenUri = "ipfs://QmeSjSinHpPnmXmspMjwiXyN6zS4E9zccariGR3jxcaWtq/9163",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "ipfs://QmQ5aovKD1HRG1GR2NYspJEQt758RCm8pMuv9CGFoK5yhy",
                )
            )
        )
    }

    @Test
    fun ashChapter() = runBlocking<Unit> {
        val address = Address.apply("0x4d232cd85294acd53ec03f4a57f57888c9ea1946")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                address,
                EthUInt256.of(4423)
            )
        )
        val expectedProperties =
            ItemProperties(
                name = "Sunlight Over a City, 2022 #173",
                description = "\"Sunlight Over the City\" is an expression of a day passing through a made-up architectural maze of a nonsensical composition.  \n" +
                    "Artist : LIŔONA  \n" +
                    "b. 1989  \n" +
                    "Medium: Digital, \"Painted on Wood\" (Not)  \n" +
                    "Year : 2022",
                attributes = listOf(
                    ItemAttribute("Creator", "Lirona"),
                    ItemAttribute("Form", "1")
                ),
                rawJsonContent = """{"name":"Sunlight Over a City, 2022 #173","description":"\"Sunlight Over the City\" is an expression of a day passing through a made-up architectural maze of a nonsensical composition.  \nArtist : LIŔONA  \nb. 1989  \nMedium: Digital, \"Painted on Wood\" (Not)  \nYear : 2022","created_by":"Lirona","image":"https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg","animation_url":"https://arweave.net/MkwJUiGCyTRIFkyrrnpApVEdAcFggVIFYtHIE65h8D8","attributes":[{"trait_type":"Creator","value":"Lirona"},{"trait_type":"Form","value":"1"}]}""",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                    videoOriginal = "https://arweave.net/MkwJUiGCyTRIFkyrrnpApVEdAcFggVIFYtHIE65h8D8"
                )
            )
        assertThat(properties).isEqualTo(expectedProperties)
    }

    @Test
    fun etherCats() = runBlocking<Unit> {
        val token = Address.apply("0xff3559412c4618af7c6e6f166c74252ff6364456")
        mockTokenStandard(token, TokenStandard.ERC1155)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(token, EthUInt256.of(50101))
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Gatinho [10 Votes, Common]",
                description = "This Chainlink VRF common Gatinho purrs with a rating of 10, and a multiplier of 1x. It has a total score of 10, and confers the same amount of votes in the EtherCats DAO. The card game properties are 8 for North, Stringalong for East, Box for South, and 1 for West. Gatinho has the personality suit of punter. Each Founders Series cat is part of NFT history. These fine felines represent the first verifiably random packs minted with Chainlink VRF.",
                attributes = listOf(
                    ItemAttribute("Rarity", "Common"),
                    ItemAttribute("Rating", "10"),
                    ItemAttribute("Multiplier", "1"),
                    ItemAttribute("Total Score", "10"),
                    ItemAttribute("North", "8"),
                    ItemAttribute("East", "Stringalong"),
                    ItemAttribute("South", "Box"),
                    ItemAttribute("West", "1"),
                    ItemAttribute("Personality Suit", "Punter")
                ),
                rawJsonContent = """{"attributes":[{"trait_type":"Rarity","value":"Common"},{"trait_type":"Rating","value":10},{"trait_type":"Multiplier","value":1},{"trait_type":"Total Score","value":10},{"trait_type":"North","value":"8"},{"trait_type":"East","value":"Stringalong"},{"trait_type":"South","value":"Box"},{"trait_type":"West","value":"1"},{"trait_type":"Personality Suit","value":"Punter"}],"description":"This Chainlink VRF common Gatinho purrs with a rating of 10, and a multiplier of 1x. It has a total score of 10, and confers the same amount of votes in the EtherCats DAO. The card game properties are 8 for North, Stringalong for East, Box for South, and 1 for West. Gatinho has the personality suit of punter. Each Founders Series cat is part of NFT history. These fine felines represent the first verifiably random packs minted with Chainlink VRF.","external_url":"https://www.ethercats.io/nfts/gatinho/","animation_url":"https://www.ethercats.io/founders-series/50101-87412-3.html","image":"ipfs://QmQSKwVhvTcfpgz8g47XgfvrSHTWe6a29WARdDs2uUHcZE/50101.png","name":"Gatinho [10 Votes, Common]"}""",
                tokenUri = "https://www.ethercats.io/founders-series/metadata/000000000000000000000000000000000000000000000000000000000000c3b5.json",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "ipfs://QmQSKwVhvTcfpgz8g47XgfvrSHTWe6a29WARdDs2uUHcZE/50101.png",
                    videoOriginal = "https://www.ethercats.io/founders-series/50101-87412-3.html"
                )
            )
        )
    }

    @Test
    fun `russianMountain - animation URL from contract`() = runBlocking<Unit> {
        val address = Address.apply("0xfbeef911dc5821886e1dda71586d90ed28174b7d")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(address, EthUInt256.of(200026))
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "A Russian Mountain",
                description = "HD version. An abandoned roller coaster in the middle of nowhere.\n" +
                    "Still version: https://knownorigin.io/edition/194575\n" +
                    "GIF version: https://knownorigin.io/edition/185875",
                attributes = emptyList(),
                rawJsonContent = """{"name":"A Russian Mountain","description":"HD version. An abandoned roller coaster in the middle of nowhere.\nStill version: https://knownorigin.io/edition/194575\nGIF version: https://knownorigin.io/edition/185875","attributes":{"artist":"javierdeazkue","scarcity":"ultrarare","tags":["dark","sci-fi","ghostly","3d","loop","video","speed","monochrome","black and white"],"asset_type":"video/mp4","asset_size_in_bytes":39787313},"external_uri":"https://knownorigin.io/artists/0x8ea2e589d1c37c850912d9e36ae103206d440427","image":"https://ipfs.infura.io/ipfs/QmY5c8rW2W4M8qUCiG4RSymprHvMPxhDfhRLUc2u5YMDJN/asset.mp4","animation_url":"https://ipfs.infura.io/ipfs/QmY5c8rW2W4M8qUCiG4RSymprHvMPxhDfhRLUc2u5YMDJN/asset.mp4","artist":"0x8ea2e589d1c37c850912d9e36ae103206d440427"}""",
                tokenUri = "https://ipfs.infura.io/ipfs/QmeQmKDXhEsotwmV3QV2xHN39XJThYVX7U9sCY9nK3nKXB",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://ipfs.infura.io/ipfs/QmY5c8rW2W4M8qUCiG4RSymprHvMPxhDfhRLUc2u5YMDJN/asset.mp4",
                    videoOriginal = "https://ipfs.infura.io/ipfs/QmY5c8rW2W4M8qUCiG4RSymprHvMPxhDfhRLUc2u5YMDJN/asset.mp4"
                )
            )
        )
    }

    @Test
    fun keepCalm() = runBlocking<Unit> {
        // https://rarible.com/token/0x1866c6907e70eb176109363492b95e3617b4a195:27
        val token = Address.apply("0x1866c6907e70eb176109363492b95e3617b4a195")
        mockTokenStandard(token, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(ItemId(token, EthUInt256.of(27)))
        // Raw JSON content of this item has changing order of parameters.
        val expectedRawJsonContent =
            """{"forSale":true,"price":"0.00005","mintedOn":{"_seconds":1579357427,"_nanoseconds":148000000},"contractAddress":"0x1866c6907e70eb176109363492b95e3617b4a195","description":"First door at your left.","numAvailable":0,"type":"ERC721","attributes":[{"value":"https://opensea.io/assets/keep-calm-v2","trait_type":"website"}],"minted":"Minted on Mintbase.io","fiatPrice":"${'$'}0.01","name":"Freaky Hallway","tags":[],"minter":"","external_link":"https://mintbase.io/minted/0x1866c6907e70eb176109363492b95e3617b4a195/B4tb5lHTnjBNVKlJSG49","amountToMint":3,"external_url":"https://mintbase.io","image":"https://firebasestorage.googleapis.com/v0/b/thing-1d2be.appspot.com/o/token%2Fasset-1579357359533?alt=media&token=31a77b56-b030-4a29-ac52-8393f67584f3"}"""
        val mapper = jacksonObjectMapper()
        assertThat(mapper.readTree(properties!!.rawJsonContent)).isEqualTo(mapper.readTree(expectedRawJsonContent))
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Freaky Hallway",
                description = "First door at your left.",
                attributes = listOf(
                    ItemAttribute("website", "https://opensea.io/assets/keep-calm-v2")
                ),
                rawJsonContent = properties.rawJsonContent,
                tokenUri = "https://us-central1-thing-1d2be.cloudfunctions.net/getThing?thingId=B4tb5lHTnjBNVKlJSG49",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://firebasestorage.googleapis.com/v0/b/thing-1d2be.appspot.com/o/token%2Fasset-1579357359533?alt=media&token=31a77b56-b030-4a29-ac52-8393f67584f3"
                )
            )
        )
    }

    @Test
    fun `ens domain`() = runBlocking<Unit> {
        Assumptions.assumeFalse(true)
        // TODO: https://rarible.atlassian.net/browse/RPN-1432
        // https://rarible.com/token/0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85:13081560491824663516256299825698718846215943324818953005193338190475271748185?tab=details
        val token = Address.apply("0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85")
        mockTokenStandard(token, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                token,
                EthUInt256(
                    "13081560491824663516256299825698718846215943324818953005193338190475271748185".toBigInteger()
                )
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "name",
                description = "description",
                attributes = emptyList(),
                rawJsonContent = null,
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "imageOriginal",
                    imageBig = "imageBig",
                    imagePreview = "imagePreview",
                    videoOriginal = "videoOriginal"
                )
            )
        )
    }

    @Test
    fun `openSea shared store`() = runBlocking<Unit> {
        // https://rarible.com/token/0x495f947276749ce646f68ac8c248420045cb7b5e:7527318126427839760955556375940656963553345796624576370101029944213257584641?tab=details
        val token = Address.apply("0x495f947276749ce646f68ac8c248420045cb7b5e")
        mockTokenStandard(token, TokenStandard.ERC1155)
        val properties = rariblePropertiesResolver.resolve(
            ItemId(
                token,
                EthUInt256(
                    "7527318126427839760955556375940656963553345796624576370101029944213257584641".toBigInteger()
                )
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Abiding Axolotl #4907",
                description = null,
                attributes = emptyList(),
                rawJsonContent = """{"name":"Abiding Axolotl #4907","description":null,"external_link":null,"image":"https://lh3.googleusercontent.com/zBwPX8jENaTm2eeUb2a4MyN3iRIC39Kl_kz9hmOAx0gkC24nsVW_h3jxDEeo0QKJvRBppsq62O0sk8Uzvqt9dIhGdMScMCmR39F6Cw","animation_url":null}""",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://lh3.googleusercontent.com/zBwPX8jENaTm2eeUb2a4MyN3iRIC39Kl_kz9hmOAx0gkC24nsVW_h3jxDEeo0QKJvRBppsq62O0sk8Uzvqt9dIhGdMScMCmR39F6Cw"
                )
            )
        )
    }

    @Test
    fun `arweave net penguin`() = runBlocking<Unit> {
        val penguin = Address.apply("0x63d48ed3f50aba950c17e37ca03356ccd6b6a280")
        mockTokenStandard(penguin, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(ItemId(penguin, EthUInt256.of(9003)))

        assertThat(properties).isNotNull
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Cozy Penguin #9003",
                description = null,
                attributes = listOf(
                    ItemAttribute("Background", "Beige"),
                    ItemAttribute("Penguin", "Pastel Green"),
                    ItemAttribute("Eyes", "Shine"),
                    ItemAttribute("Prop", "Boxers - Striped")
                ),
                rawJsonContent = "{\"name\":\"Cozy Penguin #9003\",\"image\":\"ar://veLMprs2c--Rl6nXCeakR5FG9K8y4WXt62iLxayrflo/4574.png\",\"attributes\":[{\"trait_type\":\"Background\",\"value\":\"Beige\"},{\"trait_type\":\"Penguin\",\"value\":\"Pastel Green\"},{\"trait_type\":\"Eyes\",\"value\":\"Shine\"},{\"trait_type\":\"Prop\",\"value\":\"Boxers - Striped\"}]}",
                tokenUri = "ar://FaZDPjTzMSvTZ9V0s2D8mLUoSsJg2eyD4Pg0LzdT3Yg/9003.json",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "ar://veLMprs2c--Rl6nXCeakR5FG9K8y4WXt62iLxayrflo/4574.png"
                )
            )
        )
    }

    @Test
    fun `arweave net metacraft`() = runBlocking<Unit> {
        val metacraft = Address.apply("0x0b1d6565d88f9bf6473e21c2ab58d28a495d7bb5")
        mockTokenStandard(metacraft, TokenStandard.ERC721)
        val properties = rariblePropertiesResolver.resolve(ItemId(metacraft, EthUInt256.of(8200)))

        assertThat(properties).isNotNull
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Axolotl Gold #8200",
                description = "Minecraft biome medals hatched from on-chain transactions. Also available as Metacraft's 2022 Season Pass.",
                attributes = listOf(
                    ItemAttribute("score", "2530"),
                    ItemAttribute("creature", "Axolotl"),
                    ItemAttribute("material", "Gold"),
                ),
                rawJsonContent = "{\"name\":\"Axolotl Gold #8200\",\"description\":\"Minecraft biome medals hatched from on-chain transactions. Also available as Metacraft's 2022 Season Pass.\",\"image\":\"ar://ZVMqJEThT2vs_cYL7CMd4ijLvUqC9Koz2D6sdI1WvGc\",\"attributes\":[{\"trait_type\":\"score\",\"value\":\"2530\"},{\"trait_type\":\"creature\",\"value\":\"Axolotl\"},{\"trait_type\":\"material\",\"value\":\"Gold\"}]}",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "ar://ZVMqJEThT2vs_cYL7CMd4ijLvUqC9Koz2D6sdI1WvGc"
                )
            )
        )
    }
}

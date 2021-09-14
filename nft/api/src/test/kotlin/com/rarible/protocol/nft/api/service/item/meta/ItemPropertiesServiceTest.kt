package com.rarible.protocol.nft.api.service.item.meta

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TemporaryItemProperties
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.math.BigInteger
import java.time.temporal.ChronoUnit

@Tag("manual")
class ItemPropertiesServiceTest {
    private val tokenRepository = mockk<TokenRepository>()
    private val lazyNftItemHistoryRepository = mockk<LazyNftItemHistoryRepository>()
    private val temporaryItemPropertiesRepository = mockk<TemporaryItemPropertiesRepository>()
    private val sender = ReadOnlyMonoTransactionSender(MonoEthereum(WebClientTransport("https://dark-solitary-resonance.quiknode.pro/c0b7c629520de6c3d39261f6417084d71c3f8791/", MonoEthereum.mapper(), 10000, 10000)), Address.ZERO())
    private val ipfsService = IpfsService(IpfsService.IPFS_NEW_URL)
    private val propertiesCacheDescriptor = PropertiesCacheDescriptor(sender, tokenRepository, lazyNftItemHistoryRepository, ipfsService, 86400, 20000)
    private val kittiesCacheDescriptor = KittiesCacheDescriptor(86400)
    private val properties = NftIndexerProperties("", Blockchain.ETHEREUM, "0xb47e3cd837dDF8e4c57F05d70Ab865de6e193BBB", "", "")
    private val yInsureCacheDescriptor = YInsureCacheDescriptor(sender, "0x181aea6936b407514ebfc0754a37704eb8d98f91", 86400, "0x1776651F58a17a50098d31ba3C3cD259C1903f7A", "http://localhost:8080")
    private val hegicCacheDescriptor = HegicCacheDescriptor(sender, "0xcb9ebae59738d9dadc423adbde66c018777455a4", 86400, "http://localhost:8080")
    private val hashmasksCacheDescriptor = HashmasksCacheDescriptor(sender, "0xc2c747e0f7004f9e8817db2ca4997657a7746928", 86400)
    private val waifusionCacheDescriptor = WaifusionCacheDescriptor(sender, "0x2216d47494e516d8206b70fca8585820ed3c4946", 86400, "https://ipfs.io/ipfs/QmQuzMGqHxSXugCUvWQjDCDHWhiGB75usKbrZk6Ec6pFvw")
    private val testing = ItemPropertiesService(
        propertiesCacheDescriptor,
        kittiesCacheDescriptor,
        yInsureCacheDescriptor,
        hegicCacheDescriptor,
        hashmasksCacheDescriptor,
        waifusionCacheDescriptor,
        mockk(),
        OpenseaClient("https://api.opensea.io/api/v1", "", 10000, 3000, 86400, 20000, "", null),
        ipfsService,
        temporaryItemPropertiesRepository,
        properties,
        "0x181aea6936b407514ebfc0754a37704eb8d98f91",
        "0xcb9ebae59738d9dadc423adbde66c018777455a4",
        "0xc2c747e0f7004f9e8817db2ca4997657a7746928",
        "0x2216d47494e516d8206b70fca8585820ed3c4946",
        null
    )

    @BeforeEach
    private fun before() {
        every { tokenRepository.findById(any()) } returns Mono.empty()
        every { lazyNftItemHistoryRepository.findById(any()) } returns Mono.empty()
        every { temporaryItemPropertiesRepository.findById(any<String>()) } returns Mono.empty()
    }

    @Test
    @Disabled
    fun cryptoKitties() {
        val props = kittiesCacheDescriptor.get("${ItemPropertiesService.CRYPTO_KITTIES}:1001").block()!!
        assertEquals(props.name, "TheFirst")
        assertEquals(
            "Hey cutie! I'm TheFirst. In high school, I was voted most likely to work at NASA. " +
                    "When my owner isn't watching, I steal their oversized sweaters and use them for litter paper. " +
                    "I'm not sorry. I think you'll love me beclaws I have cattitude.",
            props.description
        )
        assertEquals("https://img.cn.cryptokitties.co/0x06012c8cf97bead5deae237070f9587f8e7a266d/1001.svg", props.image)
        assertEquals(props.attributes[0].key, "colorprimary")
        assertEquals(props.attributes[0].value, "shadowgrey")
    }

    @Test
    @Disabled
    fun getCustomProperties() {
        val props = testing.getProperties(Address.apply("0x9b1aa69fe9fca10aa41250bba054aabd92aba5b6"), BigInteger.valueOf(116L)).block()!!
        assertEquals(props.name, "↜Warrior↝ 6/20")
    }

    @Test
    fun erc1155() {
        val address = Address.apply("0xfaafdc07907ff5120a76b34b731b278c38d6043c")
        every { tokenRepository.findById(address) } returns Mono.just(Token(address, name = "", standard = TokenStandard.ERC1155))

        val props = testing.getProperties(address, "10855508365998400056289941914472950957046112164229867356526540410650888241152".toBigInteger()).block()!!
        assertEquals(props.name, "Argent")
        assertEquals(props.description, "Straight forward, no fuss, iron cast stock and woodgrain grips. \"The Argent Defender.\"")
        assertEquals("https://alterverse.sfo2.digitaloceanspaces.com/WalletArt/Disruption/Intimidators/WalletArt_Argent.jpg", props.image)
    }

    @Test
    @Disabled
    fun yInsure() {
        val props = testing.getProperties(Address.apply("0x181aea6936b407514ebfc0754a37704eb8d98f91"), "48".toBigInteger()).block()!!
        assertEquals("Balancer | 1 ETH \uD83D\uDD12 | 11/11/2020", props.name)
        assertEquals("http://localhost:8080/image/yinsure/48.svg", props.image)
        assertEquals(11, props.attributes.size)
    }

    @Test
    @Disabled
    fun hegic() {
        val props = testing.getProperties(Address.apply("0xcb9ebae59738d9dadc423adbde66c018777455a4"), "317".toBigInteger()).block()!!
        assertTrue(props.name.endsWith("call option for 1 ETH. Expires on 20.11.2020 07h44 UTC"))
        assertEquals("http://localhost:8080/image/hegic/317.svg", props.image)
        assertEquals(13, props.attributes.size)
    }

    @Test
    @Disabled
    fun hashmasks() {
        val props = testing.getProperties(Address.apply("0xc2c747e0f7004f9e8817db2ca4997657a7746928"), "9076".toBigInteger()).block()!!
        assertEquals("https://hashmasksstore.blob.core.windows.net/hashmasks/2833.jpg", props.image)
        assertEquals(2, props.attributes.size)
    }

    @Test
    @Disabled
    fun waifusion() {
        val props = testing.getProperties(Address.apply("0x2216d47494e516d8206b70fca8585820ed3c4946"), "1".toBigInteger()).block()!!
        assertEquals("Kurisu Makise", props.name)
        assertEquals("Waifusion is a digital Waifu collection. There are 16,384 guaranteed-unique Waifusion NFTs. They’re just like you; a beautiful work of art, but 2-D and therefore, superior, Anon-kun.", props.description)
        assertEquals("ipfs://ipfs/QmQuzMGqHxSXugCUvWQjDCDHWhiGB75usKbrZk6Ec6pFvw/1.png", props.image)
        assertEquals(2, props.attributes.size)
    }

    @Test
    fun standardWithRedirect() {
        val props = testing.getProperties(Address.apply("0xbd13e53255ef917da7557db1b7d2d5c38a2efe24"), 967928015015.toBigInteger()).block()!!
        assertEquals(props.name, "Rio")
        assertEquals(props.description, "Rio is DozerFriends’ adorable yellow bear, happy as sunshine and always thirsty for his favourite drink made of yellow flowers.")
        assertEquals(props.image, "https://cryptodozer.io/static/images/dozer/meta/dolls/100.png")
    }

    @Test
    @Disabled
    internal fun boredApeYachtClub() {
        val properties = testing.getProperties(Address.apply("0xbc4ca0eda7647a8ab7c2061c2e118a18a936f13d"), BigInteger.valueOf(9786)).block()!!
        assertEquals("#9786", properties.name)
    }

    @Test
    @Disabled
    fun goldenStellaWithAnimationUrl() {
        val props = testing.getProperties(Address.apply("0xdb7e971d39367b20bcf4df5ae2da0fa4261bf0e8"), 426.toBigInteger()).block()!!
        assertEquals(props.name, "Golden Stella [Batch 1]")
        assertEquals(props.animationUrl, "https://storage.opensea.io/files/33d17df2d5c0411521a58cd88491de35.glb")
    }

    @Test
    @Disabled
    fun russianMountainWithAnimationUrl() {
        val props = testing.getProperties(Address.apply("0xfbeef911dc5821886e1dda71586d90ed28174b7d"), 200026.toBigInteger()).block()!!
        assertEquals(props.name, "A Russian Mountain")
        assertEquals(props.animationUrl, "https://storage.opensea.io/files/8f32a2196bac4572c7b9eb8cba33f27a.mp4")
    }

    @Test
    @Disabled
    fun russianMountainWithAnimationUrlViaContract() {
        val itemProperties = propertiesCacheDescriptor.get("0xfbeef911dc5821886e1dda71586d90ed28174b7d:200026").block()!!
        assertEquals(
            itemProperties.animationUrl,
            "https://ipfs.infura.io/ipfs/QmY5c8rW2W4M8qUCiG4RSymprHvMPxhDfhRLUc2u5YMDJN/asset.mp4"
        )
    }

    @Test
    @Disabled
    fun keepCalm() {
        val props = testing.getProperties(Address.apply("0x1866c6907e70eb176109363492b95e3617b4a195"), 27.toBigInteger()).block()!!
        assertEquals(props.name, "Freaky Hallway")
    }

    @Test
    @Disabled
    fun cryptoPunks() {
        val props = testing.getProperties(Address.apply("0xb47e3cd837dDF8e4c57F05d70Ab865de6e193BBB"), 33.toBigInteger()).block()!!
        assertEquals("CryptoPunk #33", props.name)
        assertEquals("https://www.larvalabs.com/cryptopunks/cryptopunk33.png", props.image)
        assertEquals(listOf("accessory" to "Peak Spike", "type" to "Male"), props.attributes.sortedBy { it.key }.map { it.key to it.value })
    }

    @Test
    @Disabled
    fun standardImagePreview() {
        val props = testing.getProperties(Address.apply("0xe414a94e31f5f31e640a26d2822e8fef3328b667"), 536.toBigInteger()).block()!!
        assertNotNull(props.imagePreview)
    }

    @Test
    @Disabled
    fun immutable() {
        val props = testing.getProperties(Address.apply("0x0e3a2a1f2146d86a604adc220b4967a898d7fe07"), 187594956.toBigInteger()).block()!!
        assertEquals(props.image, "https://api.immutable.com/asset/0x0e3a2a1f2146d86a604adc220b4967a898d7fe07/187594956")
    }

    @Test
    @Disabled
    fun standard() {
        val props = testing.getProperties(Address.apply("0xdceaf1652a131F32a821468Dc03A92df0edd86Ea"), 10400663.toBigInteger()).block()!!
        assertEquals(props.name, "MCH Extension: #10400663 Lv.60")
        assertTrue(props.description!!.contains("Extensions"), props.description)
        assertEquals(props.image, "https://www.mycryptoheroes.net/images/extensions/2000/1040.png")
        val attr = props.attributes.find { it.key == "type_name" }
        assertNotNull(attr)
        assertEquals(attr!!.value, "Tangerine")
        assertEquals(props.attributes.size, 9)
    }

    @Test
    @Disabled
    fun standard2() {
        val props = testing.getProperties(Address.apply("0x6ebeaf8e8e946f0716e6533a6f2cefc83f60e8ab"), 142708.toBigInteger()).block()!!
        assertEquals(props.name, "Simple Satyr")
        assertEquals("At the end of your turn, heal your god for 2.", props.description!!)
        assertEquals(props.image, "https://images.godsunchained.com/cards/250/43.png")
        assertTrue(props.attributes.any { it.key == "god" && it.value == "nature" })
    }

    @Test
    @Disabled
    fun rarible() {
        val props = testing.getProperties(Address.apply("0xf79ab01289f85b970bf33f0543e41409ed2e1c1f"), 16.toBigInteger()).block()!!
        assertTrue(props.description!!.contains("Hi!"))
        assertEquals("ipfs://ipfs/QmVVBP63aBS9oyRpbw8LBhu8oPkTwkMhNwLYLS6yiH7apC", props.image)
        assertEquals(props.attributes[0].key, "First Name")
        assertEquals(props.attributes[0].value?.toLowerCase(), "alex")
    }

    @Test
    @Disabled
    fun ens() {
        val props = testing.getProperties(Address.apply("0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85"), "36071955485891595785443576745556172890537718974602336938223322572747980843639".toBigInteger()).block()!!
        assertTrue(props.name.contains(".eth"))
    }

    @Test
    fun fromTemporaryItemPropertiesRepository() {
        val token = Address.THREE()
        val tokenId = 2.toBigInteger()
        val id = "$token:$tokenId"
        val name = "name_testing"
        val temporaryItemProperties = TemporaryItemProperties(
            id = id,
            value = ItemProperties(
                name = name,
                description = "description",
                image = "image",
                imagePreview = "imagePreview",
                imageBig = "imageBig",
                animationUrl = "animationUrl",
                attributes = listOf()
            ),
            createDate = nowMillis().minus(1, ChronoUnit.DAYS)
        )
        every { temporaryItemPropertiesRepository.findById(id) } returns Mono.just(temporaryItemProperties)

        val props = testing.getProperties(token, tokenId).block()
        assertNotNull(props)
        assertEquals(props!!.name, name)
    }
}

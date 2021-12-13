package com.rarible.protocol.unlockable.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.client.FixedApiServiceUriProvider
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.dto.LockFormDto
import com.rarible.protocol.dto.SignatureFormDto
import com.rarible.protocol.dto.UnlockableApiErrorDto
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import com.rarible.protocol.unlockable.api.client.UnlockableApiClientFactory
import com.rarible.protocol.unlockable.api.event.KafkaLockEventListener
import com.rarible.protocol.unlockable.api.service.NftClientService
import com.rarible.protocol.unlockable.domain.Lock
import com.rarible.protocol.unlockable.repository.LockRepository
import com.rarible.protocol.unlockable.test.LockTestDataFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import javax.annotation.PostConstruct

@KafkaTest
@MongoTest
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "application.environment=test"
    ]
)
@ActiveProfiles("test")
class LockControllerFt {

    @MockkBean
    lateinit var nftClientService: NftClientService

    @MockkBean
    lateinit var kafkaLockEventListener: KafkaLockEventListener

    @Autowired
    lateinit var lockRepository: LockRepository

    @LocalServerPort
    private var port: Int = 0

    private var client: LockControllerApi = mockk()

    @PostConstruct
    private fun getClient() {
        coEvery { kafkaLockEventListener.onEvent(any()) } returns Unit
        val uri = URI.create("http://localhost:${port}")
        val clientFactory = UnlockableApiClientFactory(FixedApiServiceUriProvider(uri), NoopWebClientCustomizer())
        client = clientFactory.createUnlockableApiClient(Blockchain.ETHEREUM.name)
    }

    @Test
    fun `create lock`() = runBlocking {
        val itemId = RandomStringUtils.randomAlphanumeric(8)
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val nftItem = testData.nftItem
        val form = LockFormDto(testData.getItemLockSignature(), testData.lockContent)

        coEvery { nftClientService.getItem(any()) } returns nftItem

        val dto = client.createLock(itemId, form).block()

        assertEquals(itemId, dto.itemId)
        assertEquals(form.content, dto.content)
        assertEquals(form.signature, dto.signature)
        assertEquals(nftItem.owners!![0], dto.author)
    }

    @Test
    fun `lock creation failed - lock exist`() = runBlocking {
        val itemId = RandomStringUtils.randomAlphanumeric(8)
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val nftItem = testData.nftItem
        val form = LockFormDto(testData.getItemLockSignature(), testData.lockContent)

        lockRepository.save(Lock(itemId, form.content, nftItem.owners!![0], form.signature))

        coEvery { nftClientService.getItem(itemId) } returns nftItem

        val e = assertThrows(LockControllerApi.ErrorCreateLock::class.java, Executable {
            client.createLock(itemId, form).block()
        })

        val error = e.data as UnlockableApiErrorDto
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.status)
        assertEquals(UnlockableApiErrorDto.Code.LOCK_EXISTS, error.code)
    }

    @Test
    fun `lock creation failed - not an owner`() = runBlocking {
        val itemId = RandomStringUtils.randomAlphanumeric(8)
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val nftItem = testData.nftItem

        val otherOwnerData = LockTestDataFactory.randomLockTestData(itemId)
        val form = LockFormDto(otherOwnerData.getItemLockSignature(), testData.lockContent)

        coEvery { nftClientService.getItem(itemId) } returns nftItem

        val e = assertThrows(LockControllerApi.ErrorCreateLock::class.java, Executable {
            client.createLock(itemId, form).block()
        })

        val error = e.data as UnlockableApiErrorDto
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.status)
        assertEquals(UnlockableApiErrorDto.Code.OWNERHIP_ERROR, error.code)
    }

    @Test
    fun `is unlockable`() = runBlocking {
        val itemId = RandomStringUtils.randomAlphanumeric(8)
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val nftItem = testData.nftItem
        val form = LockFormDto(testData.getItemLockSignature(), testData.lockContent)

        coEvery { nftClientService.getItem(itemId) } returns nftItem

        client.createLock(itemId, form).block()
        val isUnlockable = client.isUnlockable(itemId).block()

        assertEquals(true, isUnlockable)
    }

    @Test
    fun `get content`() = runBlocking {
        val itemId = RandomStringUtils.randomAlphanumeric(8)
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val nftItem = testData.nftItem
        val lockForm = LockFormDto(testData.getItemLockSignature(), testData.lockContent)

        coEvery { nftClientService.getItem(itemId) } returns nftItem
        client.createLock(itemId, lockForm).block()

        val unlockForm = SignatureFormDto(testData.getItemUnlockSignature())
        coEvery { nftClientService.hasItem(nftItem.contract, nftItem.tokenId, nftItem.owners!![0]) } returns true

        val content = client.getLockContent(itemId, unlockForm).block()

        assertEquals(lockForm.content, content)
    }

    @Test
    fun `get content failed - lock not found`() = runBlocking {
        val itemId = RandomStringUtils.randomAlphanumeric(8)
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val nftItem = testData.nftItem
        val unlockForm = SignatureFormDto(testData.getItemUnlockSignature())

        coEvery { nftClientService.getItem(itemId) } returns nftItem
        coEvery { nftClientService.hasItem(nftItem.contract, nftItem.tokenId, nftItem.owners!![0]) } returns true

        val e = assertThrows(LockControllerApi.ErrorGetLockContent::class.java, Executable {
            client.getLockContent(itemId, unlockForm).block()
        })

        val error = e.data as UnlockableApiErrorDto
        // TODO there should be defined error code
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.status)
        assertEquals(UnlockableApiErrorDto.Code.OWNERHIP_ERROR, error.code)
    }

    @Test
    fun `get content failed - nft index doesn't have an item`() = runBlocking {
        val itemId = RandomStringUtils.randomAlphanumeric(8)
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val nftItem = testData.nftItem
        val lockForm = LockFormDto(testData.getItemLockSignature(), testData.lockContent)

        coEvery { nftClientService.getItem(itemId) } returns nftItem
        client.createLock(itemId, lockForm).block()

        // TODO not really sure this case is correct - we getting item at first step, but at second it doesn't exist?
        coEvery { nftClientService.hasItem(nftItem.contract, nftItem.tokenId, nftItem.owners!![0]) } returns false

        val unlockForm = SignatureFormDto(testData.getItemUnlockSignature())
        val e = assertThrows(LockControllerApi.ErrorGetLockContent::class.java, Executable {
            client.getLockContent(itemId, unlockForm).block()
        })

        val error = e.data as UnlockableApiErrorDto
        // TODO there should be defined error code
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.status)
        assertEquals(UnlockableApiErrorDto.Code.OWNERHIP_ERROR, error.code)
    }
}

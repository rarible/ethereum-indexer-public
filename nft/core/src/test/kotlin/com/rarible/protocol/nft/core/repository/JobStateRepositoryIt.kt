package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class JobStateRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var jobStateRepository: JobStateRepository

    data class SomeState(val a: Int, val b: String)
    data class OtherState(val c: Double, val d: Boolean)

    @Test
    fun `should save and get different job states`() = runBlocking<Unit> {
        // given
        val someState = SomeState(1, "2")
        val otherState = OtherState(3.0, true)

        // when
        jobStateRepository.save("some", someState)
        jobStateRepository.save("other", otherState)
        val actual1 = jobStateRepository.get("some", SomeState::class.java)
        val actual2 = jobStateRepository.get("other", OtherState::class.java)
        val actual3 = jobStateRepository.get("unknown", SomeState::class.java)

        // then
        assertThat(actual1).isEqualTo(someState)
        assertThat(actual2).isEqualTo(otherState)
        assertThat(actual3).isEqualTo(null)
    }
}

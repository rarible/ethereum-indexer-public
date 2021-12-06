package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.MediaMeta
import com.rarible.protocol.nft.core.service.IpfsService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class MediaMetaServiceTest {
    companion object {
        fun createMediaMetaService(): MediaMetaService = MediaMetaService(
            ipfsService = IpfsService(),
            cacheService = null,
            proxyUrl = "http://69.197.181.202:3128",
            timeout = 5000,
            maxLoadedContentSize = 1000000
        )
    }

    private val service = createMediaMetaService()

    @Test
    fun gif() = runBlocking<Unit> {
        val mediaMeta =
            service.getMediaMeta("https://lh3.googleusercontent.com/CIKzsJLHKmoC8YmHt3l6h7pzj-mJx5uHrS231VE006DCZ-IQLyONCtMBCYiOwbT9SzS5IdkSOF517Zq1CejmHVrMuQ=s250")
        assertThat(mediaMeta).isEqualTo(
            MediaMeta(
                type = "image/gif",
                width = 165,
                height = 250
            )
        )
    }

    @Test
    fun mp4() = runBlocking<Unit> {
        val mediaMeta = service.getMediaMeta("https://storage.opensea.io/files/3f89eab5930c7b61acb22a45412f1662.mp4")
        assertThat(mediaMeta).isEqualTo(
            MediaMeta(
                type = "video/mp4",
                width = null,
                height = null
            )
        )
    }

    @Test
    fun amazon() = runBlocking<Unit> {
        val mediaMeta =
            service.getMediaMeta("https://s3.us-west-2.amazonaws.com/sing.serve/e487c504da821859cbac142e63ef9d8cc36015f0dfaf1de2949e6f894f5aa538%2Feae9b612-df09-4023-9b53-ac73e6319b44")
        assertThat(mediaMeta).isEqualTo(
            MediaMeta(
                type = "video/mp4",
                width = null,
                height = null
            )
        )
    }

    @Test
    fun jpeg() = runBlocking<Unit> {
        val mediaMeta =
            service.getMediaMeta("https://lh3.googleusercontent.com/rnS-RmufKkrLlWb4gl0_3yHx_lsQI7V0kRbB1VAiSCBRcY-fiHa_2U42xexLz9ZtaUZnRuo2-o-CcYPuCkmVdko=s250")
        assertThat(mediaMeta).isEqualTo(
            MediaMeta(
                type = "image/jpeg",
                width = 167,
                height = 250
            )
        )
    }
}

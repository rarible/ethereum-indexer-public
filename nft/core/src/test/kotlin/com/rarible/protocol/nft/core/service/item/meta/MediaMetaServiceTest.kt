package com.rarible.protocol.nft.core.service.item.meta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("manual")
@Disabled
class MediaMetaServiceTest {
    private val service = MediaMetaService("http://69.197.181.202:3128", 5000, 1000000)

    @Test
    fun gif() {
        val b = service.getMediaMeta("https://lh3.googleusercontent.com/CIKzsJLHKmoC8YmHt3l6h7pzj-mJx5uHrS231VE006DCZ-IQLyONCtMBCYiOwbT9SzS5IdkSOF517Zq1CejmHVrMuQ=s250").block()!!
        assertEquals("image/gif", b.type)
        assertEquals(165, b.width)
        assertEquals(250, b.height)
    }

    @Test
    fun mp4() {
        val b = service.getMediaMeta("https://storage.opensea.io/files/3f89eab5930c7b61acb22a45412f1662.mp4").block()!!
        assertEquals("video/mp4", b.type)
        assertEquals(null, b.width)
        assertEquals(null, b.height)
    }

    @Test
    fun amazon() {
        val b = service.getMediaMeta("https://s3.us-west-2.amazonaws.com/sing.serve/e487c504da821859cbac142e63ef9d8cc36015f0dfaf1de2949e6f894f5aa538%2Feae9b612-df09-4023-9b53-ac73e6319b44").block()!!
        assertEquals("video/mp4", b.type)
        assertEquals(null, b.width)
        assertEquals(null, b.height)
    }

    @Test
    fun jpeg() {
        val b = service.getMediaMeta("https://lh3.googleusercontent.com/rnS-RmufKkrLlWb4gl0_3yHx_lsQI7V0kRbB1VAiSCBRcY-fiHa_2U42xexLz9ZtaUZnRuo2-o-CcYPuCkmVdko=s250").block()!!
        assertEquals("image/jpeg", b.type)
        assertEquals(167, b.width)
        assertEquals(250, b.height)
    }
}

package com.rarible.protocol.nft.core.misc;

import com.rarible.protocol.nft.core.misc.detector.SVGDetector;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

class SVGDetectorTest {

    @Test
    void testIsSvgImageFalse() {
        SVGDetector sVGDetector = new SVGDetector("url");
        boolean result = sVGDetector.canDecode();
        Assertions.assertEquals(false, result);
    }

   @Test
    void testIsSvgImageTrue() throws IOException {
        String svg = IOUtils.toString(SVGDetectorTest.class.getResource("/images/atom.svg").openStream(), UTF_8);
        SVGDetector sVGDetector = new SVGDetector(svg);
        boolean result = sVGDetector.canDecode();
        Assertions.assertEquals(false, result);
    }

}

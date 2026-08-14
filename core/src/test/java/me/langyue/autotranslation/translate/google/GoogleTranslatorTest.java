package me.langyue.autotranslation.translate.google;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.util.List;

class GoogleTranslatorTest {
    @Test void decodesAllGoogleSegments() {
        assertEquals("你好世界", GoogleTranslator.decodeGoogleResponse("[[[\"你好\",\"Hello\"],[\"世界\",\" world\"]],null,\"en\"]"));
        assertThrows(IllegalArgumentException.class, () -> GoogleTranslator.decodeGoogleResponse("{}"));
    }
    @Test void malformedLegacyIpDoesNotPreventInitialization() {
        GoogleTranslator translator = new GoogleTranslator("translate.google.com", List.of("not-an-ip", "999.1.1.1"));
        assertDoesNotThrow(translator::init);
        translator.close();
    }
}

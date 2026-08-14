package me.langyue.autotranslation.translate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Modifier;

class TranslatorManagerTest {
    @AfterEach void reset() { TranslatorManager.clearForTests(); }

    @Test void unknownSelectionFallsBackWithoutNullPointer() {
        List<String> warnings = new ArrayList<>();
        TranslatorManager.setWarningSink(warnings::add);
        TranslatorManager.registerTranslator("Google", StubTranslator::new);
        TranslatorManager.setTranslator("missing");
        assertEquals("Google", TranslatorManager.selectedName());
        assertNotNull(TranslatorManager.getTranslator());
        assertEquals(1, warnings.size());
    }

    @Test void legacyPublicSurfaceRemainsCallable() throws Exception {
        assertFalse(Modifier.isFinal(TranslatorManager.class.getModifiers()));
        assertTrue(Modifier.isPublic(TranslatorManager.class.getConstructor().getModifiers()));
        assertTrue(Modifier.isPublic(TranslatorManager.class.getMethod("init").getModifiers()));
        assertNotNull(TranslatorManager.class.getMethod("registerTranslator", String.class, java.util.function.Supplier.class));
    }

    @Test void lateRegistrationRestoresRequestedTranslator() {
        TranslatorManager.registerTranslator("Google", StubTranslator::new);
        TranslatorManager.setTranslator("LateAddon");
        assertEquals("Google", TranslatorManager.selectedName());
        TranslatorManager.registerTranslator("LateAddon", StubTranslator::new);
        assertEquals("LateAddon", TranslatorManager.selectedName());
        assertNotNull(TranslatorManager.getTranslator());
    }

    private static final class StubTranslator implements ITranslator {
        @Override public void init() { }
        @Override public int maxLength() { return 1000; }
        @Override public String translate(String text, String tl, String sl) { return text; }
    }
}

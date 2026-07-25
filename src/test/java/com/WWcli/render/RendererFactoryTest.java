package com.WWcli.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RendererFactoryTest {

    private String savedProp;

    @BeforeEach
    void saveProp() {
        savedProp = System.getProperty("WWcli.renderer");
    }

    @AfterEach
    void restoreProp() {
        if (savedProp == null) {
            System.clearProperty("WWcli.renderer");
        } else {
            System.setProperty("WWcli.renderer", savedProp);
        }
    }

    @Test
    void defaultsToInlineWhenUnset() {
        System.clearProperty("WWcli.renderer");
        // We can't easily clear env vars in tests; only verify property path
        assertEquals(RendererFactory.Mode.INLINE, RendererFactory.resolveMode());
    }

    @Test
    void propertyValueLanternaResolves() {
        System.setProperty("WWcli.renderer", "lanterna");
        assertEquals(RendererFactory.Mode.LANTERNA, RendererFactory.resolveMode());
    }

    @Test
    void propertyValuePlainResolves() {
        System.setProperty("WWcli.renderer", "plain");
        assertEquals(RendererFactory.Mode.PLAIN, RendererFactory.resolveMode());
    }

    @Test
    void propertyValueIsCaseInsensitive() {
        System.setProperty("WWcli.renderer", "LANTERNA");
        assertEquals(RendererFactory.Mode.LANTERNA, RendererFactory.resolveMode());
    }

    @Test
    void unknownValueFallsBackToInline() {
        System.setProperty("WWcli.renderer", "weird");
        assertEquals(RendererFactory.Mode.INLINE, RendererFactory.resolveMode());
    }

    @Test
    void tuiAliasResolvesToLanterna() {
        System.setProperty("WWcli.renderer", "tui");
        assertEquals(RendererFactory.Mode.LANTERNA, RendererFactory.resolveMode());
    }

    @Test
    void createPlainReturnsPlainRenderer() {
        Renderer renderer = RendererFactory.create(RendererFactory.Mode.PLAIN, null);
        assertInstanceOf(PlainRenderer.class, renderer);
    }

    @Test
    void createInlineReturnsRendererInstance() {
        // Day 1 stub still returns PlainRenderer; Day 2 will swap to InlineRenderer.
        Renderer renderer = RendererFactory.create(RendererFactory.Mode.INLINE, null);
        assertInstanceOf(PlainRenderer.class, renderer);
    }
}

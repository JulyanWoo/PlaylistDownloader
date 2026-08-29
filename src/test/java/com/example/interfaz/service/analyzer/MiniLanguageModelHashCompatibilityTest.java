package com.example.interfaz.service.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MiniLanguageModelHashCompatibilityTest {
    @Test
    void usesTheStableIndicesSharedWithPythonTrainer() {
        assertEquals(40029, MiniLanguageModel.featureIndex(" el"));
        assertEquals(48984, MiniLanguageModel.featureIndex("muj"));
        assertEquals(22021, MiniLanguageModel.featureIndex("mujer"));
        assertEquals(55303, MiniLanguageModel.featureIndex("the"));
        assertEquals(11402, MiniLanguageModel.featureIndex("você"));
        assertEquals(1641, MiniLanguageModel.featureIndex("œ"));
    }
}

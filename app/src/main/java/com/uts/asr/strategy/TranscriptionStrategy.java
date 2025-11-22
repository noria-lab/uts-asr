package com.uts.asr.strategy;

import java.nio.file.Path;

/**
 * Estrategia de transcripción que define cómo obtener y procesar audio.
 * Implementaciones: SingleFileStrategy (archivo), LiveMicStrategy (micrófono).
 */
public interface TranscriptionStrategy {
    
    /**
     * Ejecuta la transcripción según la estrategia.
     * 
     * @param audioFile ruta del archivo de audio (null para modo live)
     * @param listener receptor de eventos de transcripción
     * @throws Exception si ocurre un error durante la transcripción
     */
    void execute(Path audioFile, TranscriptionListener listener) throws Exception;
    
    /**
     * Indica si la estrategia puede ser cancelada.
     */
    default boolean isCancellable() {
        return false;
    }
    
    /**
     * Cancela la transcripción en curso.
     */
    default void cancel() {
        // Por defecto no hace nada
    }
}
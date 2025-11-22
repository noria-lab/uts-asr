package com.uts.asr.strategy;

/**
 * Listener para recibir eventos durante la transcripción.
 */
public interface TranscriptionListener {
    
    /**
     * Llamado cuando se recibe un resultado parcial.
     * 
     * @param json resultado parcial en formato JSON de Vosk
     */
    void onPartial(String json);
    
    /**
     * Llamado cuando se recibe un resultado final.
     * 
     * @param json resultado final en formato JSON de Vosk
     */
    void onFinal(String json);
    
    /**
     * Llamado cuando ocurre un error.
     * 
     * @param error el error ocurrido
     */
    void onError(Throwable error);
    
    /**
     * Llamado cuando la transcripción se completa exitosamente.
     */
    default void onComplete() {
        // Por defecto no hace nada
    }
}
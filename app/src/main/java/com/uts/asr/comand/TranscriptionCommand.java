package com.uts.asr.command;

import com.uts.asr.strategy.TranscriptionListener;
import com.uts.asr.strategy.TranscriptionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Comando que encapsula una estrategia de transcripción.
 * El panel UI no conoce si la transcripción viene de archivo o micrófono.
 */
public class TranscriptionCommand {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionCommand.class);
    
    private final TranscriptionStrategy strategy;

    public TranscriptionCommand(TranscriptionStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy no puede ser null");
        }
        this.strategy = strategy;
    }

    /**
     * Ejecuta el comando de transcripción.
     * 
     * @param audioFile archivo de audio (puede ser null para modo live)
     * @param listener receptor de eventos
     */
    public void run(Path audioFile, TranscriptionListener listener) {
        logger.info("Ejecutando comando de transcripción con estrategia: {}", 
                    strategy.getClass().getSimpleName());
        
        try {
            strategy.execute(audioFile, listener);
        } catch (Exception e) {
            logger.error("Error al ejecutar comando de transcripción", e);
            listener.onError(e);
        }
    }

    /**
     * Cancela la transcripción si la estrategia lo permite.
     */
    public void cancel() {
        if (strategy.isCancellable()) {
            logger.info("Cancelando transcripción...");
            strategy.cancel();
        } else {
            logger.warn("La estrategia no es cancelable");
        }
    }

    /**
     * Indica si la transcripción puede ser cancelada.
     */
    public boolean isCancellable() {
        return strategy.isCancellable();
    }
}

package com.uts.asr.strategy;

import com.uts.asr.config.AppConfig;
import com.uts.asr.core.AudioDeviceManager;
import com.uts.asr.core.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Recognizer;

import javax.sound.sampled.TargetDataLine;
import java.nio.file.Path;

/**
 * Estrategia para transcribir audio en tiempo real desde el micrófono.
 */
public class LiveMicStrategy implements TranscriptionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(LiveMicStrategy.class);
    
    private volatile boolean running = false;
    private TargetDataLine line;

    @Override
    public void execute(Path audioFile, TranscriptionListener listener) throws Exception {
        Recognizer recognizer = null;
        
        try {
            // Adquirir permiso para usar recognizer
            WorkerFactory.acquireRecognizerPermit();
            
            // Abrir micrófono
            line = AudioDeviceManager.openLine(AppConfig.SAMPLE_RATE);
            logger.info("Micrófono abierto, iniciando captura...");
            
            // Crear recognizer para este thread
            recognizer = WorkerFactory.createRecognizer();
            
            running = true;
            byte[] buffer = new byte[AppConfig.CHUNK_SIZE_LIVE];
            
            while (running && !Thread.currentThread().isInterrupted()) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        // Resultado final disponible
                        String result = recognizer.getResult();
                        listener.onFinal(result);
                        logger.debug("Final: {}", result);
                    } else {
                        // Resultado parcial
                        String partial = recognizer.getPartialResult();
                        listener.onPartial(partial);
                        logger.trace("Partial: {}", partial);
                    }
                }
            }
            
            // Al detener, generar último resultado final
            if (recognizer != null) {
                String finalResult = recognizer.getFinalResult();
                if (!finalResult.isEmpty()) {
                    listener.onFinal(finalResult);
                    logger.info("Resultado final al detener: {}", finalResult);
                }
            }
            
            listener.onComplete();
            logger.info("Captura de audio finalizada");
            
        } catch (Exception e) {
            logger.error("Error en captura de audio", e);
            listener.onError(e);
            throw e;
            
        } finally {
            // Liberar recursos
            if (recognizer != null) {
                try {
                    recognizer.close();
                } catch (Exception e) {
                    logger.error("Error al cerrar recognizer", e);
                }
            }
            
            AudioDeviceManager.closeLine(line);
            WorkerFactory.releaseRecognizerPermit();
            running = false;
        }
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    @Override
    public void cancel() {
        logger.info("Cancelando captura de audio...");
        running = false;
        
        // Cerrar línea para interrumpir read() bloqueante
        if (line != null && line.isOpen()) {
            line.stop();
        }
    }
}
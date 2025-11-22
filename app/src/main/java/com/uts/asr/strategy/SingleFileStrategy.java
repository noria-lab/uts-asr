package com.uts.asr.strategy;

import com.uts.asr.config.AppConfig;
import com.uts.asr.core.WorkerFactory;
import com.uts.asr.util.SoundConverter;
import com.uts.asr.util.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Recognizer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Estrategia para transcribir archivos de audio pregrabados.
 */
public class SingleFileStrategy implements TranscriptionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SingleFileStrategy.class);
    
    private final String sessionName;

    public SingleFileStrategy(String sessionName) {
        this.sessionName = sessionName != null ? sessionName : AppConfig.DEFAULT_SESSION_NAME;
    }

    @Override
    public void execute(Path audioFile, TranscriptionListener listener) throws Exception {
        if (audioFile == null || !Files.exists(audioFile)) {
            throw new IllegalArgumentException("Archivo de audio no existe: " + audioFile);
        }

        logger.info("Iniciando transcripción de archivo: {}", audioFile);
        
        Recognizer recognizer = null;
        Path convertedFile = null;
        
        try {
            // Adquirir permiso
            WorkerFactory.acquireRecognizerPermit();
            
            // Convertir a formato compatible si es necesario
            convertedFile = SoundConverter.convertToPCM(audioFile);
            logger.info("Audio convertido: {}", convertedFile);
            
            // Crear recognizer
            recognizer = WorkerFactory.createRecognizer();
            
            // Procesar archivo en chunks
            try (InputStream ais = new BufferedInputStream(new FileInputStream(convertedFile.toFile()))) {
                byte[] buffer = new byte[AppConfig.CHUNK_SIZE_FILE];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = ais.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        String result = recognizer.getResult();
                        listener.onFinal(result);
                        logger.debug("Final chunk: {}", result);
                    } else {
                        String partial = recognizer.getPartialResult();
                        listener.onPartial(partial);
                        logger.trace("Partial chunk: {}", partial);
                    }
                }
                
                logger.info("Procesados {} bytes de audio", totalBytes);
            }
            
            // Obtener resultado final
            String finalResult = recognizer.getFinalResult();
            listener.onFinal(finalResult);
            logger.info("Transcripción completada");
            
            // Guardar resultados
            Writer.saveTranscription(sessionName, finalResult);
            
            listener.onComplete();
            
        } catch (Exception e) {
            logger.error("Error durante transcripción de archivo", e);
            listener.onError(e);
            throw e;
            
        } finally {
            // Limpiar recursos
            if (recognizer != null) {
                try {
                    recognizer.close();
                } catch (Exception e) {
                    logger.error("Error al cerrar recognizer", e);
                }
            }
            
            // Eliminar archivo temporal convertido
            if (convertedFile != null && !convertedFile.equals(audioFile)) {
                try {
                    Files.deleteIfExists(convertedFile);
                    logger.debug("Archivo temporal eliminado: {}", convertedFile);
                } catch (Exception e) {
                    logger.warn("No se pudo eliminar archivo temporal", e);
                }
            }
            
            WorkerFactory.releaseRecognizerPermit();
        }
    }
}
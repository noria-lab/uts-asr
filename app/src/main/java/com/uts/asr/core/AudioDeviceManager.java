package com.uts.asr.core;

import com.uts.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;

/**
 * Factory para gestionar dispositivos de audio (micrófono).
 * Proporciona TargetDataLine configuradas correctamente para Vosk.
 */
public final class AudioDeviceManager {
    private static final Logger logger = LoggerFactory.getLogger(AudioDeviceManager.class);

    private AudioDeviceManager() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    /**
     * Abre una línea de captura de audio configurada para Vosk.
     * 
     * @param sampleRate tasa de muestreo (típicamente 16000 Hz)
     * @return línea de audio abierta y lista para usar
     * @throws LineUnavailableException si no hay micrófono disponible
     */
    public static TargetDataLine openLine(float sampleRate) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
            sampleRate,
            AppConfig.SAMPLE_SIZE_BITS,
            AppConfig.CHANNELS,
            true,  // signed
            false  // little endian
        );

        logger.info("Intentando abrir línea de audio: {}", format);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // Verificar si el formato está soportado
        if (!AudioSystem.isLineSupported(info)) {
            logger.error("Formato de audio no soportado: {}", format);
            throw new LineUnavailableException(
                "El formato de audio no está soportado por el hardware.\n" +
                "Requerido: PCM 16 bits, " + sampleRate + " Hz, mono"
            );
        }

        // Intentar obtener y abrir la línea
        TargetDataLine line = null;
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            logger.info("Línea de audio abierta exitosamente");
            return line;
            
        } catch (LineUnavailableException e) {
            logger.error("No se pudo abrir la línea de audio", e);
            if (line != null && line.isOpen()) {
                line.close();
            }
            throw e;
        }
    }

    /**
     * Cierra una línea de audio de forma segura.
     * 
     * @param line la línea a cerrar (puede ser null)
     */
    public static void closeLine(TargetDataLine line) {
        if (line != null) {
            try {
                if (line.isActive()) {
                    line.stop();
                }
                if (line.isOpen()) {
                    line.close();
                }
                logger.info("Línea de audio cerrada");
            } catch (Exception e) {
                logger.error("Error al cerrar línea de audio", e);
            }
        }
    }
}
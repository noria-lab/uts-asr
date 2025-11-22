package com.uts.asr.util;

import com.uts.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Convierte archivos de audio a formato PCM compatible con Vosk usando FFmpeg.
 */
public final class SoundConverter {
    private static final Logger logger = LoggerFactory.getLogger(SoundConverter.class);
    private static final int TIMEOUT_SECONDS = 300; // 5 minutos

    private SoundConverter() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    /**
     * Convierte un archivo de audio a PCM 16kHz, 16 bits, mono.
     * 
     * @param inputFile archivo de entrada (puede ser WAV, MP3, etc.)
     * @return archivo convertido en formato PCM
     * @throws IOException si la conversión falla
     */
    public static Path convertToPCM(Path inputFile) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IOException("Archivo no existe: " + inputFile);
        }

        // Crear directorio temporal si no existe
        Files.createDirectories(AppConfig.TEMP_DIR);

        // Archivo de salida temporal
        String outputName = sanitizeFileName(inputFile.getFileName().toString());
        Path outputFile = AppConfig.TEMP_DIR.resolve(outputName + "_converted.wav");

        logger.info("Convirtiendo {} a {}", inputFile, outputFile);

        // Comando FFmpeg
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg",
            "-i", inputFile.toAbsolutePath().toString(),
            "-ar", String.valueOf((int) AppConfig.SAMPLE_RATE),
            "-ac", String.valueOf(AppConfig.CHANNELS),
            "-sample_fmt", "s16",
            "-f", "wav",
            "-y", // sobrescribir
            outputFile.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        
        Process process = null;
        try {
            process = pb.start();
            
            // Capturar salida de FFmpeg
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.trace("FFmpeg: {}", line);
                }
            }

            // Esperar con timeout
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFmpeg timeout después de " + TIMEOUT_SECONDS + " segundos");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("FFmpeg falló con código {}: {}", exitCode, output);
                throw new IOException("FFmpeg falló con código " + exitCode);
            }

            if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
                throw new IOException("Archivo convertido está vacío o no existe");
            }

            logger.info("Conversión exitosa: {} ({} bytes)", 
                       outputFile, Files.size(outputFile));
            return outputFile;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversión interrumpida", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Sanitiza un nombre de archivo eliminando caracteres problemáticos.
     */
    private static String sanitizeFileName(String fileName) {
        // Remover extensión
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        
        // Reemplazar caracteres inválidos
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
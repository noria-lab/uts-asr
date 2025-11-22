package com.uts.asr.util;

import com.uts.asr.config.AppConfig;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para guardar transcripciones en formato JSON y texto.
 */
public final class Writer {
    private static final Logger logger = LoggerFactory.getLogger(Writer.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private Writer() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    /**
     * Guarda una transcripción en formato JSON (temp) y texto (transcriptions).
     * 
     * @param sessionName nombre de la sesión
     * @param voskJson resultado JSON de Vosk
     * @throws IOException si hay error al escribir
     */
    public static void saveTranscription(String sessionName, String voskJson) throws IOException {
        // Crear directorios si no existen
        Files.createDirectories(AppConfig.TEMP_DIR);
        Files.createDirectories(AppConfig.TRANSCRIPTIONS_DIR);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String sanitizedName = sanitizeSessionName(sessionName);

        // 1. Guardar JSON crudo en temp/
        Path jsonFile = AppConfig.TEMP_DIR.resolve(sanitizedName + "_" + timestamp + ".json");
        saveJsonFile(jsonFile, voskJson);
        logger.info("JSON guardado en: {}", jsonFile);

        // 2. Extraer texto y guardar en transcriptions/
        String text = extractTextFromJson(voskJson);
        Path textFile = AppConfig.TRANSCRIPTIONS_DIR.resolve(sanitizedName + "_" + timestamp + ".txt");
        saveTextFile(textFile, text);
        logger.info("Transcripción guardada en: {}", textFile);
    }

    /**
     * Guarda el JSON crudo de forma atómica.
     */
    private static void saveJsonFile(Path destination, String json) throws IOException {
        Path temp = Files.createTempFile(AppConfig.TEMP_DIR, "vosk_", ".json.tmp");
        try {
            Files.writeString(temp, json);
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, 
                      StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /**
     * Guarda el texto legible de forma atómica.
     */
    private static void saveTextFile(Path destination, String text) throws IOException {
        Path temp = Files.createTempFile(AppConfig.TRANSCRIPTIONS_DIR, "trans_", ".txt.tmp");
        try {
            Files.writeString(temp, text);
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING,
                      StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /**
     * Extrae el texto de un resultado JSON de Vosk.
     */
    private static String extractTextFromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("text")) {
                return obj.getString("text").trim();
            }
            return "";
        } catch (Exception e) {
            logger.warn("No se pudo parsear JSON de Vosk: {}", e.getMessage());
            return json; // Fallback: devolver JSON completo
        }
    }

    /**
     * Sanitiza el nombre de sesión para uso como nombre de archivo.
     */
    private static String sanitizeSessionName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "session";
        }
        
        // Reemplazar caracteres inválidos por guion bajo
        String sanitized = name.trim()
            .replaceAll("[<>:\"/\\\\|?*]", "_")
            .replaceAll("\\s+", "_");
        
        // Limitar longitud
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized;
    }
}
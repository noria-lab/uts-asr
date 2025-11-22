package com.uts.asr.config;

import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración centralizada de la aplicación.
 * Contiene constantes para colores, rutas, y parámetros de audio.
 */
public final class AppConfig {
    
    // Paleta de colores
    public static final Color COLOR_BG_1 = parseColor("212,224,155");
    public static final Color COLOR_BG_2 = parseColor("246,244,210");
    public static final Color COLOR_BG_3 = parseColor("203,223,189");
    public static final Color COLOR_ACCENT = parseColor("241,156,121");
    public static final Color COLOR_TEXT = parseColor("70,63,58");
    
    // Rutas del sistema
    public static final Path TEMP_DIR = Paths.get("temp");
    public static final Path TRANSCRIPTIONS_DIR = Paths.get("transcriptions");
    public static final String MODEL_PATH = "model"; // Carpeta del modelo Vosk
    
    // Parámetros de audio
    public static final float SAMPLE_RATE = 16000.0f;
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1; // Mono
    public static final int CHUNK_SIZE_LIVE = 4000; // ~250ms
    public static final int CHUNK_SIZE_FILE = 8000; // ~500ms
    
    // UI
    public static final String DEFAULT_SESSION_NAME = "Nueva Sesión";
    
    private AppConfig() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }
    
    /**
     * Parsea un string RGB como "R,G,B" a Color.
     */
    private static Color parseColor(String rgb) {
        String[] parts = rgb.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato RGB inválido: " + rgb);
        }
        try {
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return new Color(r, g, b);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Componente RGB no numérico: " + rgb, e);
        }
    }
}
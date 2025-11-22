package com.uts.asr.core;

import com.uts.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Model;

import java.io.IOException;

/**
 * Servicio singleton para gestionar el modelo Vosk.
 * Carga el modelo una sola vez al inicio y lo libera al cerrar la app.
 */
public final class VoskService {
    private static final Logger logger = LoggerFactory.getLogger(VoskService.class);
    private static volatile Model model;
    private static volatile boolean initialized = false;

    private VoskService() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    /**
     * Inicializa el modelo Vosk desde la ruta configurada.
     * Debe llamarse UNA VEZ al inicio de la aplicación.
     * 
     * @throws IOException si el modelo no se puede cargar
     */
    public static synchronized void init() throws IOException {
        if (initialized) {
            logger.warn("VoskService ya estaba inicializado");
            return;
        }

        logger.info("Cargando modelo Vosk desde: {}", AppConfig.MODEL_PATH);
        model = new Model(AppConfig.MODEL_PATH);
        initialized = true;

        // Registrar hook para liberar recursos al cerrar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Liberando recursos de Vosk...");
            shutdown();
        }, "vosk-shutdown-hook"));

        logger.info("Modelo Vosk cargado exitosamente");
    }

    /**
     * Obtiene la instancia compartida del modelo.
     * 
     * @return el modelo Vosk cargado
     * @throws IllegalStateException si no se ha inicializado
     */
    public static Model getModel() {
        if (!initialized || model == null) {
            throw new IllegalStateException(
                "VoskService no inicializado. Llamar a init() primero."
            );
        }
        return model;
    }

    /**
     * Libera los recursos del modelo.
     * Solo debe llamarse al cerrar la aplicación.
     */
    private static synchronized void shutdown() {
        if (model != null) {
            try {
                model.close();
                logger.info("Modelo Vosk cerrado correctamente");
            } catch (Exception e) {
                logger.error("Error al cerrar modelo Vosk", e);
            } finally {
                model = null;
                initialized = false;
            }
        }
    }
}
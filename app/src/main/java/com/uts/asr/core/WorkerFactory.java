package com.uts.asr.core;

import com.uts.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Recognizer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory para crear recognizers de Vosk y gestionar threads de trabajo.
 * Implementa un límite de recognizers concurrentes basado en CPUs disponibles.
 */
public final class WorkerFactory {
    private static final Logger logger = LoggerFactory.getLogger(WorkerFactory.class);
    
    // Limitar recognizers concurrentes según CPUs disponibles
    private static final Semaphore AVAILABLE = new Semaphore(
        Runtime.getRuntime().availableProcessors()
    );
    
    // Executor compartido para todas las tareas de transcripción
    private static final ExecutorService EXECUTOR;
    
    static {
        AtomicInteger counter = new AtomicInteger(0);
        
        EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("vosk-worker-" + counter.incrementAndGet());
                t.setDaemon(true);
                
                // Handler para excepciones no capturadas
                t.setUncaughtExceptionHandler((thread, throwable) -> {
                    logger.error("Excepción no capturada en {}", thread.getName(), throwable);
                });
                
                return t;
            }
        });
        
        // Registrar shutdown del executor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando executor de transcripciones...");
            EXECUTOR.shutdownNow();
        }, "worker-factory-shutdown"));
    }

    private WorkerFactory() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    /**
     * Obtiene el executor compartido para tareas de transcripción.
     */
    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    /**
     * Adquiere un permiso para crear un recognizer.
     * Debe llamarse antes de createRecognizer().
     * 
     * @throws InterruptedException si el thread es interrumpido
     */
    public static void acquireRecognizerPermit() throws InterruptedException {
        logger.debug("Adquiriendo permiso para recognizer...");
        AVAILABLE.acquire();
        logger.debug("Permiso adquirido. Disponibles: {}", AVAILABLE.availablePermits());
    }

    /**
     * Libera un permiso de recognizer.
     * Debe llamarse en finally después de cerrar el recognizer.
     */
    public static void releaseRecognizerPermit() {
        AVAILABLE.release();
        logger.debug("Permiso liberado. Disponibles: {}", AVAILABLE.availablePermits());
    }

    /**
     * Crea un nuevo recognizer para el thread actual.
     * IMPORTANTE: El recognizer debe cerrarse en finally.
     * 
     * @return un nuevo recognizer configurado
     * @throws IllegalStateException si VoskService no está inicializado
     * @throws IOException si ocurre un error al crear el recognizer
     */
    public static Recognizer createRecognizer() throws IOException {
        logger.debug("Creando recognizer en thread: {}", Thread.currentThread().getName());
        
        Recognizer recognizer = new Recognizer(
            VoskService.getModel(),
            AppConfig.SAMPLE_RATE
        );
        
        logger.debug("Recognizer creado exitosamente");
        return recognizer;
    }
}
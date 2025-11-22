package com.uts.asr;

import com.uts.asr.config.AppConfig;
import com.uts.asr.core.VoskService;
import com.uts.asr.gui.LiveSessionPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;


/**
 * Punto de entrada de la aplicación de transcripción de voz.
 * <p>
 * Punto de entrada de la aplicación de transcripción.
 * Inicializa Vosk, configura el frame principal y muestra la interfaz.
 * </p>
 */


public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // Configurar look and feel nativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("No se pudo establecer look and feel del sistema", e);
        }

        // Inicializar Vosk en el hilo principal
        try {
            logger.info("Inicializando servicio Vosk...");
            VoskService.init();
            logger.info("Vosk inicializado correctamente");
        } catch (Exception e) {
            logger.error("Error fatal al inicializar Vosk", e);
            JOptionPane.showMessageDialog(null,
                    "No se pudo cargar el modelo de Vosk.\n" +
                            "Verifica que la carpeta del modelo esté presente.\n" +
                            "Error: " + e.getMessage(),
                    "Error de Inicialización",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Crear y mostrar interfaz en EDT
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Transcriptor de Audio - Vosk ASR");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                LiveSessionPanel panel = new LiveSessionPanel();
                frame.add(panel);
                
                frame.setSize(800, 600);
                frame.setMinimumSize(new Dimension(600, 400));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                
                logger.info("Interfaz gráfica iniciada");
            } catch (Exception e) {
                logger.error("Error al crear la interfaz", e);
                JOptionPane.showMessageDialog(null,
                        "Error al iniciar la interfaz: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

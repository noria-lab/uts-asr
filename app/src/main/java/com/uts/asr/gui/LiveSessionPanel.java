package com.uts.asr.gui;

import com.uts.asr.command.TranscriptionCommand;
import com.uts.asr.config.AppConfig;
import com.uts.asr.core.WorkerFactory;
import com.uts.asr.strategy.LiveMicStrategy;
import com.uts.asr.strategy.TranscriptionListener;
import com.uts.asr.util.Writer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Future;

/**
 * Panel principal para sesión de transcripción en vivo.
 */
public class LiveSessionPanel extends JPanel implements TranscriptionListener {
    private static final Logger logger = LoggerFactory.getLogger(LiveSessionPanel.class);

    private enum State { STOPPED, RUNNING }

    // Componentes UI
    private JTextField sessionNameField;
    private JTextArea transcriptionArea;
    private JButton toggleButton;
    private JButton saveButton;
    private JButton clearButton;
    private JButton closeButton;
    private JLabel statusLabel;

    // Estado
    private State currentState = State.STOPPED;
    private TranscriptionCommand currentCommand;
    private Future<?> currentTask;
    private final StringBuilder fullTranscription = new StringBuilder();

    public LiveSessionPanel() {
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        setBackground(AppConfig.COLOR_BG_1);

        // Campo de nombre de sesión
        sessionNameField = new JTextField(AppConfig.DEFAULT_SESSION_NAME);
        sessionNameField.setFont(new Font("SansSerif", Font.BOLD, 16));
        sessionNameField.setForeground(AppConfig.COLOR_TEXT);
        sessionNameField.setBackground(AppConfig.COLOR_BG_2);
        sessionNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppConfig.COLOR_BG_3, 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Área de transcripción
        transcriptionArea = new JTextArea();
        transcriptionArea.setEditable(false);
        transcriptionArea.setLineWrap(true);
        transcriptionArea.setWrapStyleWord(true);
        transcriptionArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        transcriptionArea.setForeground(AppConfig.COLOR_TEXT);
        transcriptionArea.setBackground(AppConfig.COLOR_BG_2);
        transcriptionArea.setMargin(new Insets(10, 10, 10, 10));

        // Botones superiores
        saveButton = createButton("Guardar", AppConfig.COLOR_BG_3);
        clearButton = createButton("Limpiar", AppConfig.COLOR_BG_3);
        closeButton = createButton("Cerrar", AppConfig.COLOR_ACCENT);

        saveButton.addActionListener(e -> saveTranscription());
        clearButton.addActionListener(e -> clearTranscription());
        closeButton.addActionListener(e -> closeWindow());

        // Botón toggle START/STOP
        toggleButton = createButton("START", AppConfig.COLOR_BG_3);
        toggleButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        toggleButton.setPreferredSize(new Dimension(200, 60));
        toggleButton.addActionListener(e -> toggleRecording());

        // Label de estado
        statusLabel = new JLabel("Estado: Idle");
        statusLabel.setForeground(AppConfig.COLOR_TEXT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel superior: nombre de sesión
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(AppConfig.COLOR_BG_1);
        topPanel.add(new JLabel("Nombre de sesión:"), BorderLayout.WEST);
        topPanel.add(sessionNameField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Panel central: área de texto
        JScrollPane scrollPane = new JScrollPane(transcriptionArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppConfig.COLOR_BG_3, 2));
        add(scrollPane, BorderLayout.CENTER);

        // Panel inferior: botones y estado
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(AppConfig.COLOR_BG_1);

        // Botones de acción
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setBackground(AppConfig.COLOR_BG_1);
        actionPanel.add(saveButton);
        actionPanel.add(clearButton);
        actionPanel.add(closeButton);

        // Panel de control
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setBackground(AppConfig.COLOR_BG_1);
        controlPanel.add(toggleButton);

        bottomPanel.add(actionPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(AppConfig.COLOR_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppConfig.COLOR_TEXT, 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void toggleRecording() {
        if (currentState == State.STOPPED) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        logger.info("Iniciando grabación...");
        updateStatus("Escuchando...");
        
        try {
            LiveMicStrategy strategy = new LiveMicStrategy();
            currentCommand = new TranscriptionCommand(strategy);
            
            currentTask = WorkerFactory.getExecutor().submit(() -> {
                currentCommand.run(null, this);
            });
            
            currentState = State.RUNNING;
            toggleButton.setText("STOP");
            toggleButton.setBackground(AppConfig.COLOR_ACCENT);
            sessionNameField.setEnabled(false);
            
        } catch (Exception e) {
            logger.error("Error al iniciar grabación", e);
            onError(e);
        }
    }

    private void stopRecording() {
        logger.info("Deteniendo grabación...");
        updateStatus("Procesando...");
        
        if (currentCommand != null && currentCommand.isCancellable()) {
            currentCommand.cancel();
        }
        
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        
        currentState = State.STOPPED;
        toggleButton.setText("START");
        toggleButton.setBackground(AppConfig.COLOR_BG_3);
        sessionNameField.setEnabled(true);
        updateStatus("Idle");
    }

    private void saveTranscription() {
        String text = fullTranscription.toString().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No hay transcripción para guardar.",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String sessionName = sessionNameField.getText().trim();
            Writer.saveTranscription(sessionName, 
                new JSONObject().put("text", text).toString());
            
            JOptionPane.showMessageDialog(this,
                "Transcripción guardada exitosamente.",
                "Guardado",
                JOptionPane.INFORMATION_MESSAGE);
                
            logger.info("Transcripción guardada manualmente");
            
        } catch (Exception e) {
            logger.error("Error al guardar transcripción", e);
            JOptionPane.showMessageDialog(this,
                "Error al guardar: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearTranscription() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Desea limpiar toda la transcripción?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            transcriptionArea.setText("");
            fullTranscription.setLength(0);
            logger.info("Transcripción limpiada");
        }
    }

    private void closeWindow() {
        if (currentState == State.RUNNING) {
            stopRecording();
        }
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> 
            statusLabel.setText("Estado: " + status)
        );
    }

    // Implementación de TranscriptionListener

    @Override
    public void onPartial(String json) {
        SwingUtilities.invokeLater(() -> {
            try {
                JSONObject obj = new JSONObject(json);
                String text = obj.optString("partial", "").trim();
                if (!text.isEmpty()) {
                    transcriptionArea.append("⌛ " + text + "\n");
                    transcriptionArea.setCaretPosition(transcriptionArea.getDocument().getLength());
                }
            } catch (Exception e) {
                logger.warn("Error parseando partial JSON", e);
            }
        });
    }

    @Override
    public void onFinal(String json) {
        SwingUtilities.invokeLater(() -> {
            try {
                JSONObject obj = new JSONObject(json);
                String text = obj.optString("text", "").trim();
                if (!text.isEmpty()) {
                    transcriptionArea.append("✅ " + text + "\n\n");
                    transcriptionArea.setCaretPosition(transcriptionArea.getDocument().getLength());
                    fullTranscription.append(text).append(" ");
                }
            } catch (Exception e) {
                logger.warn("Error parseando final JSON", e);
            }
        });
    }

    @Override
    public void onError(Throwable error) {
        logger.error("Error en transcripción", error);
        SwingUtilities.invokeLater(() -> {
            updateStatus("Error");
            JOptionPane.showMessageDialog(this,
                "Error: " + error.getMessage(),
                "Error de Transcripción",
                JOptionPane.ERROR_MESSAGE);
            
            if (currentState == State.RUNNING) {
                stopRecording();
            }
        });
    }

    @Override
    public void onComplete() {
        logger.info("Transcripción completada");
        SwingUtilities.invokeLater(() -> {
            updateStatus("Completado");
        });
    }
}

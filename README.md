# 🎤 Sistema de Transcripción de Audio - Vosk ASR

> Prototipo educacional de transcripción en tiempo real usando Java 17, Swing y Vosk Speech Recognition

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Vosk](https://img.shields.io/badge/Vosk-0.3.45-blue.svg)](https://alphacephei.com/vosk/)
[![License](https://img.shields.io/badge/License-Educational-green.svg)]()

---

## 📋 Descripción

Aplicación desktop en Java que demuestra conceptos de Programación Orientada a Objetos aplicados a reconocimiento de voz en tiempo real. Utiliza la biblioteca Vosk para transcribir audio desde el micrófono con arquitectura profesional pero sin complejidad excesiva.

### ✨ Características Principales

- ✅ **Transcripción en tiempo real** desde micrófono
- ✅ **Transcripción de archivos** de audio pregrabados
- ✅ **Resultados parciales** cada ~250ms para feedback inmediato
- ✅ **Guardado automático** en JSON y TXT legible
- ✅ **Interfaz limpia** con Swing y paleta personalizada
- ✅ **Arquitectura extensible** con patrones de diseño
- ✅ **Gestión robusta** de recursos nativos (JNI)

---

## 🏗️ Arquitectura

### Patrones de Diseño Implementados

| Patrón | Clase | Propósito |
|--------|-------|-----------|
| **Singleton** | `VoskService` | Un solo modelo compartido |
| **Strategy** | `TranscriptionStrategy` | Algoritmos intercambiables |
| **Command** | `TranscriptionCommand` | Encapsular peticiones |
| **Observer** | `TranscriptionListener` | Notificaciones asíncronas |
| **Factory** | `WorkerFactory`, `AudioDeviceManager` | Creación centralizada |

### Stack Tecnológico

```
┌─────────────────────────────────────┐
│         Swing GUI (EDT)             │
│    LiveSessionPanel + Listeners     │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Command & Strategy Layer        │
│  TranscriptionCommand + Strategies  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Core Services              │
│  VoskService + WorkerFactory +      │
│       AudioDeviceManager            │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Native Libraries             │
│      Vosk (JNI) + JNA + FFmpeg      │
└─────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisitos

- **Java 17+** (OpenJDK recomendado)
- **Gradle 7.0+** (wrapper incluido)
- **FFmpeg** instalado en PATH
- **Micrófono** funcional

### Instalación

```bash
# 1. Clonar proyecto
git clone <tu-repo>
cd vosk-transcription

# 2. Descargar modelo Vosk (Español)
wget https://alphacephei.com/vosk/models/vosk-model-es-0.42.zip
unzip vosk-model-es-0.42.zip
mv vosk-model-es-0.42 model

# 3. Instalar FFmpeg
# Ubuntu/Debian:
sudo apt-get install ffmpeg

# MacOS:
brew install ffmpeg

# Windows: Descargar desde https://ffmpeg.org

# 4. Compilar y ejecutar
./gradlew run
```

### Uso Básico

1. **Inicia la aplicación** - El modelo Vosk se carga automáticamente
2. **Edita el nombre de sesión** - Ej: "Reunión Cliente"
3. **Click en START** - Comienza a hablar al micrófono
4. **Observa resultados** - Parciales (⌛) y finales (✅) en tiempo real
5. **Click en STOP** - Finaliza grabación
6. **Guardar** - Exporta transcripción a archivo .txt

---

## 📦 Estructura del Proyecto

```
src/main/java/com/uts/asr/
├── App.java                      # Punto de entrada
├── config/
│   └── AppConfig.java           # Constantes y configuración
├── core/
│   ├── VoskService.java         # Singleton del modelo
│   ├── AudioDeviceManager.java  # Factory de micrófono
│   └── WorkerFactory.java       # Gestión de threads
├── strategy/
│   ├── TranscriptionStrategy.java      # Interface
│   ├── TranscriptionListener.java      # Callbacks
│   ├── LiveMicStrategy.java            # Tiempo real
│   └── SingleFileStrategy.java         # Archivos
├── command/
│   └── TranscriptionCommand.java       # Wrapper
├── util/
│   ├── SoundConverter.java      # Conversión FFmpeg
│   └── Writer.java              # Guardado atómico
└── gui/
    └── LiveSessionPanel.java    # Panel principal Swing
```

---

## 🎨 Interfaz de Usuario

### Paleta de Colores

```java
COLOR_BG_1    = RGB(212, 224, 155)  // Fondo principal
COLOR_BG_2    = RGB(246, 244, 210)  // Fondo secundario
COLOR_BG_3    = RGB(203, 223, 189)  // Botones normales
COLOR_ACCENT  = RGB(241, 156, 121)  // Botón STOP
COLOR_TEXT    = RGB(70, 63, 58)     // Texto
```

### Componentes

- **Campo de sesión** (editable) - Define nombre del archivo
- **Área de transcripción** (scroll) - Muestra resultados en vivo
- **Botón START/STOP** (toggle) - Control principal
- **Botones de acción** - Guardar, Limpiar, Cerrar
- **Indicador de estado** - Idle / Escuchando / Procesando / Error

---

## 🔧 Configuración Avanzada

### Cambiar Tamaño de Chunks

En `AppConfig.java`:

```java
public static final int CHUNK_SIZE_LIVE = 4000;  // ~250ms (recomendado)
public static final int CHUNK_SIZE_FILE = 8000;  // ~500ms para archivos
```

**Guía:**
- Chunks más pequeños (2000) → Menor latencia, más overhead JNI
- Chunks más grandes (8000) → Mayor latencia, mejor throughput

### Cambiar Modelo de Idioma

```java
// En AppConfig.java
public static final String MODEL_PATH = "model-en";  // Inglés

// Descargar modelo correspondiente:
// https://alphacephei.com/vosk/models
```

### Ajustar Nivel de Logs

En `src/main/resources/simplelogger.properties`:

```properties
# Debugging detallado
org.slf4j.simpleLogger.log.com.uts.asr.strategy=trace

# Solo errores
org.slf4j.simpleLogger.log.com.uts.asr.gui=error
```

---

## 📚 Conceptos Pedagógicos

### Para Estudiantes de POO

Este proyecto demuestra:

1. **Encapsulación** - Cada clase tiene responsabilidad única
2. **Herencia** - `TranscriptionStrategy` con implementaciones
3. **Polimorfismo** - Estrategias intercambiables dinámicamente
4. **Abstracción** - Interfaces desacoplan UI de lógica
5. **Composición** - Command contiene Strategy
6. **Singleton** - Modelo Vosk compartido globalmente
7. **Factory Method** - Creación de recognizers y líneas de audio
8. **Observer** - Callbacks asíncronos desde workers

---

## 🧪 Testing

### Test Manual Rápido

```bash
# 1. Verificar modelo
ls -la model/  # Debe tener: am, conf, graph, ivector

# 2. Verificar FFmpeg
ffmpeg -version

# 3. Verificar micrófono (Linux)
arecord -l
```

## 🐛 Troubleshooting

### Error: "Model not found"
**Causa:** Carpeta `model/` vacía o mal ubicada  
**Solución:** Descargar modelo Vosk y descomprimir en raíz del proyecto

### Error: "LineUnavailableException"
**Causa:** Micrófono en uso o sin permisos  
**Solución:** 
- Cerrar otras apps que usen micrófono
- Verificar permisos del SO (MacOS: Preferencias → Seguridad)

### Error: "FFmpeg not found"
**Causa:** FFmpeg no instalado o no en PATH  
**Solución:** Instalar FFmpeg según tu OS (ver Quick Start)

### UI se congela
**Causa:** Operación bloqueante en EDT  
**Solución:** Ya implementado - todas las operaciones pesadas corren en `WorkerFactory.getExecutor()`

### Fugas de memoria
**Causa:** Recognizer no cerrado  
**Solución:** Ya implementado - `finally` blocks garantizan limpieza

---

### Optimizaciones Implementadas

- ✅ Modelo cargado una sola vez (Singleton)
- ✅ Semáforo limita recognizers a número de CPUs
- ✅ Chunks optimizados para balance latencia/throughput
- ✅ Guardado atómico con archivos temporales
- ✅ Worker threads reutilizables (CachedThreadPool)

---

## 📄 Licencia

Este proyecto es material educacional para curso de Java OOP.  
Libre uso para fines académicos.

---

## 🤝 Contribuciones

Sugerencias de mejora para estudiantes:

1. **Fork** el proyecto
2. Crear rama de feature (`git checkout -b feature/MejoraBuenisima`)
3. Commit cambios (`git commit -m 'Agrega característica X'`)
4. Push a rama (`git push origin feature/MejoraBuenisima`)
5. Abrir **Pull Request**

---

## 📞 Soporte

- **Documentación Vosk:** https://alphacephei.com/vosk/
- **Java Docs:** https://docs.oracle.com/en/java/javase/17/
- **Gradle:** https://docs.gradle.org/

---

## 🎓 Créditos

- **Vosk ASR:** Alpha Cephei Inc.
- **SLF4J:** QOS.ch
- **JSON:** org.json
- **FFmpeg:** FFmpeg team

---

**Desarrollado como material educacional - 2025**

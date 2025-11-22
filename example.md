# Rol: 
Senior Java Desktop Engineer + Vosk ASR Expert

# Objetivo: 
Entregar un prototipo funcional (Java 17 + Swing) que transcriba archivos WAV y audio en vivo con Vosk 0.3.45, siguiendo la arquitectura validada y sin librerías externas salvo Vosk + JNA.

# contexto:
aplicacion para un curso JAVA OOP, se quiere una arquitectura profecional, sin entrar en sobre ingeniería, complejidad abrumadora 

---

# **Arquitectura del Proyecto de Transcripción en Java (POO + Vosk + Swing)**

## **Stack Tecnológico**

* **Java 17**
* **Swing GUI**
* **Vosk (JNI)**
* **FFmpeg**
* **SLF4J** (logging)
* **Gradle Kotlin DSL (`build.gradle.kts`)**

---

# **Estructura General del Proyecto**

## **App (Main)**

* Crea y muestra el `LiveSessionPanel`.
* Invoca `VoskService.init()` al inicio para cargar el modelo.
* Configura el frame principal.

## **AppConfig**

* Paleta de colores.
* Rutas estándar (por ejemplo: carpeta `temp/` y `transcriptions/`).
* Constantes varias.

---

# **Core**

## **VoskService**

* Singleton.
* Carga **una sola vez** el modelo Vosk al arrancar la app.
* Expone el `Model` compartido.
* Registra un **shutdown hook** para liberar correctamente el modelo.

## **AudioDeviceManager**

* Factory para `TargetDataLine`.
* Método estático `openLine(float sampleRate)`:

  * Devuelve un micrófono configurado a **PCM 16 bits, 16 kHz, mono**.
  * Verifica si el hardware soporta el formato.
  * En caso de no soporte, aplica **fallback** o muestra un error.
* Gestiona apertura y cierre seguro de líneas.

## **WorkerFactory**

* Crea instancias de `Recognizer`.
* Un recognizer **por hilo**.
* Cada recognizer se cierra al finalizar su estrategia.
* Naming de threads y uncaught-exception handler recomendado.

---

# **Patrones: Strategy + Command**

## **Interface: `TranscriptionStrategy`**

Define el algoritmo de transcripción según el origen del audio:

```java
void execute(Path audioFile, TranscriptionListener listener) throws Exception;
```

* `audioFile` puede ser `null` para el modo **live**.
* La implementación decide cómo obtener bytes (fast I/O, chunked, real-time).

---

## **SingleFileStrategy**

* Lee un archivo completo de audio.
* Usa chunks de ~200 ms (≈ 6.4 kB) para buena latencia sin overhead JNI.
* Emite:

  * `onPartial()` para progreso.
  * `onFinal()` para resultados sólidos.
* Guarda:

  * **JSON crudo** en carpeta `temp/`.
  * **.txt humano** en `transcriptions/` mediante `Writer`.

---

## **LiveMicStrategy**

* Abre `TargetDataLine`.
* Loop de captura en tiempo real con chunks del mismo tamaño (~200 ms).
* Envía partials periódicos (cada ~200 ms).
* En Stop:

  * Genera un último `final`.
  * Cierra línea en `finally`.
  * Libera semáforo.

---

## **TranscriptionCommand**

* Wrapper ejecutado por el botón **Start/Stop** del panel.
* El panel no conoce si la transcripción viene de archivo o del micrófono.
* Llama solo:
  `cmd.run(null, this);`

---

# **Utilidades**

## **SoundConverter**

* Convierte audio pregrabado a PCM 16 kHz / 16 bits / mono usando FFmpeg.
* Manejo de:

  * timeouts
  * stdout/stderr
  * sanitización de rutas temporales

## **Writer**

* Guarda el JSON del recognizer en carpeta `temp/`.
* Genera versión `.txt` legible en `transcriptions/`.
* Sanitiza el nombre basado en el título de la sesión.
* Usa archivo temporal + rename atómico para evitar corrupción.

---

# **GUI – LiveSessionPanel**

Interfaz mínima.

## **Componentes**

* boton (togle) en la parte inferior **START/STOP** 
* `JTextArea` (scrollable)
* 3 botones superiores:

  * **Guardar**
  * **Limpiar**
  * **Cerrar**
* Indicador de estado (Idle / Listening / Processing / Error).
* `JLabel` en la parte superior, editable para título de sesión:

  * Se usa para nombrar archivo .txt.
  * Se sanitiza para evitar caracteres ilegales.
* Aplica paleta de colores desde `AppConfig`.

## **Implementa `TranscriptionListener`**

```java
onPartial(json) → textArea.append("⌛ " + texto/parcial)
onFinal(json)   → textArea.append("✅ " + texto/final)
```

Ambos en EDT usando `SwingUtilities.invokeLater`.

---

# **Flujo UI – Recomendado**

### **Estado interno**

```java
enum State { STOPPED, RUNNING }
```

### **Acciones**

#### Si el botón muestra **Start**:

1. Adquiere semáforo.
2. Ejecuta `LiveMicStrategy` en executor.
3. Cambia a estado **RUNNING**.
4. Botón se muestra como **Stop**.

#### Si el botón muestra **Stop**:

1. Cancela la tarea (via `Future.cancel(true)`).
2. Envía señal de finalización.
3. Cierra línea en `finally`.
4. Libera semáforo.
5. Vuelve a estado **STOPPED**.
6. Botón vuelve a mostrar **Start**.

---

# **Paleta de colores**

```java
COLOR_BG_1 = parseColor("212,224,155");
COLOR_BG_2 = parseColor("246,244,210");
COLOR_BG_3 = parseColor("203,223,189");
COLOR_ACCENT = parseColor("241,156,121");   // STOP
COLOR_TEXT = parseColor("70,63,58");
```

---

# **Concurrencia**
ejemplo:
``` java
ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("vosk-worker-" + counter.incrementAndGet());
        return t;
    }
});
```

## **Limiter de recognizers**

``` java
private static final Semaphore AVAILABLE = new Semaphore(Runtime.getRuntime().availableProcessors());
```
Adquirido antes de crear Recognizer, liberado en finally
## **ExecutorService**

* Un único executor centralizado.
* Cada transcripción corre en un `Future`.

## **Cancelación**

* `Future.cancel(true)`
* Flags internos `volatile boolean running`.

## **JNI Safety**

* `Recognizer.close()` en `finally`.
* `Model.close()` en shutdown hook.
* Cada recognizer es exclusivo del hilo que lo usa.

---

# **Carpetas de salida**

## **temp/**

* Guarda **JSON crudo** del recognizer.
* Estructura recomendada:

  ```
  temp/sessionName/timestamp.json
  ```

## **transcriptions/**

* Guarda la salida **.txt humana**.
* Formato:

  ```
  sessionName_YYYY-MM-DD_HH-mm.txt
  ```

---

# **Recomendaciones adicionales**

### 1. Evitar freezes en Swing

* Nunca llamar Vosk desde el EDT.
* Solo la actualización a la UI usa `invokeLater`.

### 2. Tamaño de buffer

* Usar ~6.4kB por chunk (= 200 ms audio)
* Permite buen balance entre latencia y overhead JNI.

### 3. FFmpeg

* Validar exit codes.
* Imponer timeout.
* Registrar logs SLF4J.

### 4. Sanitización

* El título de sesión editado por el usuario debe:

  * Remover caracteres inválidos.
  * Evitar nombres repetidos (agregar timestamp).


---

# **Checklist de aceptación**

* Modelo Vosk cargado solo una vez.
* UI no se congela.
* Start inicia mic y partials llegan cada ~200 ms.
* Stop genera final y libera recursos.
* JSON crudo en `temp/`.
* `.txt` limpio en `transcriptions/`.
* TargetDataLine se abre y cierra correctamente.
* No hay fugas JNI (tests repetidos).
* Logging SLF4J capturando lifecycle y errores.

---
## build.gradle.kts
```kotlin
plugins {
    id("java")
    id("application")
}

group = "org.UTS"
version = "0.3-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    // Ajusta si tu clase main está en otro paquete
    mainClass.set("com.uts.asr.App")
}

dependencies {
    // Vosk - Speech Recognition Library
    implementation("com.alphacephei:vosk:0.3.45")

    // JNA (Java Native Access) - Requerido por Vosk
    implementation("net.java.dev.jna:jna:5.13.0")

    // JSON - Para parsear resultados de Vosk
    implementation("org.json:json:20230227")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("org.slf4j:slf4j-simple:2.0.12")   
}
```
---

## Calidad y robustez
Todos los recursos nativos (Recognizer, TargetDataLine) en try-with-resources o finally.
Excepciones capturadas en TranscriptionListener.onError → muestra JOptionPane y log a consola.
Prohibido compartir Recognizer entre hilos; cada strategy crea el suyo.
Audio entrante siempre convertido a 16 kHz, 16 bits, mono antes de llegar a acceptWaveForm.
Chunk size: 4000 bytes (≈ 250 ms) para live; 8000 para archivo.

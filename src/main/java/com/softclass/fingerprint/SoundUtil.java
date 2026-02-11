package com.softclass.fingerprint;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundUtil {

    private static void play(String resourcePath) {
        try {
            // Intentar varias rutas comunes si falla la primera
            String[] paths = {
                    resourcePath,
                    resourcePath.startsWith("/") ? resourcePath.substring(1) : "/" + resourcePath,
                    resourcePath.replace("/com/softclass/fingerprint", ""),
                    "/sounds" + resourcePath.substring(resourcePath.lastIndexOf("/"))
            };

            InputStream audioSrc = null;
            String foundPath = null;
            for (String path : paths) {
                audioSrc = SoundUtil.class.getResourceAsStream(path);
                if (audioSrc == null) {
                    audioSrc = ClassLoader.getSystemResourceAsStream(path);
                }
                if (audioSrc != null) {
                    foundPath = path;
                    break;
                }
            }

            if (audioSrc == null) {
                System.out.println("ℹ Sonido NO encontrado en ninguna ruta: " + resourcePath);
                return;
            } else {
                System.out.println("✅ Sonido CARGADO desde: " + foundPath);
            }

            try (InputStream bufferedIn = new BufferedInputStream(audioSrc)) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                // Listener para cerrar recursos después de tocar
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("⚠ No se pudo reproducir sonido (" + resourcePath + "): " + e.getMessage());
        }
    }

    /** Sonido para registro exitoso (entrada/salida) */
    public static void playSuccess() {
        play("/com/softclass/fingerprint/sounds/success.wav");
    }

    /** Sonido para error (huella no reconocida o fallida) */
    public static void playError() {
        play("/com/softclass/fingerprint/sounds/error.wav");
    }

    /** Sonido simple (beep del sistema, multiplataforma) */
    public static void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }
}

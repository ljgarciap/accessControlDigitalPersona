package com.softclass.fingerprint;

import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.*;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPTemplateStatus;

import java.util.Base64;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio del lector DigitalPersona.
 * Reemplaza la implementación de SecuGen manteniendo la compatibilidad de
 * firma.
 */
public class FingerprintService {

    private final DPFPCapture capture = DPFPGlobal.getCaptureFactory().createCapture();
    private final DPFPVerification verifier = DPFPGlobal.getVerificationFactory().createVerification();
    private final DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory()
            .createFeatureExtraction();
    private final DPFPEnrollment enrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();

    private final LinkedBlockingQueue<DPFPSample> sampleQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean continuousMode = new AtomicBoolean(false);
    private FingerprintListener continuousListener;

    public FingerprintService() {
        capture.addDataListener(new DPFPDataAdapter() {
            @Override
            public void dataAcquired(DPFPDataEvent e) {
                if (e.getSample() != null) {
                    processAcquiredSample(e.getSample());
                }
            }
        });

        capture.addReaderStatusListener(new DPFPReaderStatusAdapter() {
            @Override
            public void readerConnected(DPFPReaderStatusEvent e) {
                System.out.println("✅ Lector DigitalPersona conectado");
            }

            @Override
            public void readerDisconnected(DPFPReaderStatusEvent e) {
                System.out.println("🛑 Lector DigitalPersona desconectado");
            }
        });

        try {
            // Intento seguro de listar lectores
            try {
                java.util.Collection<?> readers = (java.util.Collection<?>) DPFPGlobal.class
                        .getMethod("getReaderCollection").invoke(null);
                System.out.println("🔍 Lectores detectados: " + (readers != null ? readers.size() : 0));
            } catch (Exception e) {
                System.out.println("ℹ No se pudo listar lectores via DPFPGlobal: " + e.getMessage());
            }

            capture.startCapture();
            System.out.println("✅ Captura iniciada correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error iniciando captura.");

            Throwable cause = e;
            while (cause != null) {
                if (cause.getClass().getName().contains("JniException")) {
                    try {
                        java.lang.reflect.Method getErrorCode = cause.getClass().getMethod("getErrorCode");
                        Object code = getErrorCode.invoke(cause);
                        System.err.println("👉 Error JNI Detectado - Código: " + code);
                    } catch (Exception ignored) {
                    }
                }
                System.err.println("   Causa: " + cause.toString());
                cause = cause.getCause();
            }
            e.printStackTrace();
        }
    }

    private void processAcquiredSample(DPFPSample sample) {
        if (continuousMode.get() && continuousListener != null) {
            try {
                String base64 = extractTemplateBase64(sample);
                if (base64 != null) {
                    continuousListener.onFingerprintDetected(base64);
                }
            } catch (Exception e) {
                System.err.println("Error en procesamiento continuo: " + e.getMessage());
            }
        }
        // Siempre ofrecemos a la cola para enrolamiento manual
        sampleQueue.offer(sample);
    }

    public String enrollFingerprint(EnrollCallback callback) throws Exception {
        System.out.println("🏁 Iniciando proceso de enrolamiento (Se requieren 4 muestras)...");
        enrollment.clear();
        sampleQueue.clear();

        while (enrollment.getFeaturesNeeded() > 0) {
            int needed = enrollment.getFeaturesNeeded();
            System.out.println("☝ Muestras restantes: " + needed);
            if (callback != null) {
                callback.onProgress(needed);
            }

            DPFPSample sample = sampleQueue.poll(30, TimeUnit.SECONDS);

            if (sample == null) {
                throw new RuntimeException("Tiempo de espera agotado para capturar muestra");
            }

            try {
                DPFPFeatureSet featureSet = featureExtractor.createFeatureSet(sample,
                        DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
                enrollment.addFeatures(featureSet);
                System.out.println("✅ Muestra procesada correctamente");
            } catch (DPFPImageQualityException e) {
                System.err.println("⚠ Baja calidad de imagen, intenta de nuevo: " + e.getMessage());
            }
        }

        if (enrollment.getTemplateStatus() == DPFPTemplateStatus.TEMPLATE_STATUS_READY) {
            if (callback != null)
                callback.onProgress(0);
            return Base64.getEncoder().encodeToString(enrollment.getTemplate().serialize());
        } else {
            throw new RuntimeException("No se pudo generar el template, estado: " + enrollment.getTemplateStatus());
        }
    }

    public interface EnrollCallback {
        void onProgress(int samplesRemaining);
    }

    public String captureTemplateBase64() throws Exception {
        // Este método ahora se usa solo para verificación rápida (1 captura)
        System.out.println("⌛ Esperando huella para verificación...");
        sampleQueue.clear();
        DPFPSample sample = sampleQueue.poll(30, TimeUnit.SECONDS);

        if (sample == null) {
            throw new RuntimeException("Tiempo de espera agotado");
        }

        DPFPFeatureSet featureSet = featureExtractor.createFeatureSet(sample,
                DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        return Base64.getEncoder().encodeToString(featureSet.serialize());
    }

    private String extractTemplateBase64(DPFPSample sample) throws DPFPImageQualityException {
        // Para identificación/verificación usamos el propósito "Verification"
        DPFPFeatureSet featureSet = featureExtractor.createFeatureSet(sample,
                DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        return Base64.getEncoder().encodeToString(featureSet.serialize());
    }

    public boolean match(String storedTemplateBase64, String liveFeatureBase64) throws Exception {
        byte[] storedBytes = Base64.getDecoder().decode(storedTemplateBase64);
        byte[] liveBytes = Base64.getDecoder().decode(liveFeatureBase64);

        DPFPTemplate template = DPFPGlobal.getTemplateFactory().createTemplate(storedBytes);
        DPFPFeatureSet featureSet = DPFPGlobal.getFeatureSetFactory().createFeatureSet(liveBytes);

        DPFPVerificationResult result = verifier.verify(featureSet, template);
        return result.isVerified();
    }

    public void startContinuousMode(FingerprintListener listener) {
        this.continuousListener = listener;
        this.continuousMode.set(true);
        System.out.println("🎧 Modo continuo iniciado (DigitalPersona)");
    }

    public void stopContinuousMode() {
        this.continuousMode.set(false);
        this.continuousListener = null;
        System.out.println("🛑 Modo continuo detenido");
    }

    public interface FingerprintListener {
        void onFingerprintDetected(String templateBase64);
    }

    public void close() {
        stopContinuousMode();
        try {
            capture.stopCapture();
        } catch (Exception e) {
            System.err.println("⚠ Error al cerrar captura: " + e.getMessage());
        }
    }
}

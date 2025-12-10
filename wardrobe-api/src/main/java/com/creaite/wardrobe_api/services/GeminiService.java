package com.creaite.wardrobe_api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Service
public class GeminiService {

    /**
     * Simula processamento lento e sempre retorna camiseta.png do classpath.
     */
    public String processImageWithGemini(String imageBase64) {
        log.info("=== GeminiService: starting simulated slow processing ===");

        try {
            // Simula delay de 30 segundos para parecer processamento pesado
            for (int i = 0; i < 6; i++) {
                log.info("Processing... {}s elapsed", i * 5);
                Thread.sleep(2_000); // 5s de cada vez, total 30s
            }

            // Carrega camiseta.png do classpath
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("camiseta.png");

            if (is == null) {
                log.warn("camiseta.png não encontrada em root, tentando static/camiseta.png");
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream("static/camiseta.png");
            }

            if (is == null) {
                log.error("camiseta.png não encontrada no classpath!");
                throw new RuntimeException("camiseta.png não encontrada");
            }

            try (InputStream input = is) {
                byte[] imageBytes = StreamUtils.copyToByteArray(input);
                String encoded = Base64.getEncoder().encodeToString(imageBytes);
                log.info("✅ GeminiService: simulated slow processing complete, returning camiseta.png");
                return "data:image/png;base64," + encoded;
            }

        } catch (Exception e) {
            log.error("❌ Error in slow simulated Gemini processing: {}", e.getMessage(), e);
            // fallback: retorna imagem original
            return imageBase64 != null && imageBase64.startsWith("data:")
                    ? imageBase64
                    : "data:image/png;base64," + imageBase64;
        }
    }
}

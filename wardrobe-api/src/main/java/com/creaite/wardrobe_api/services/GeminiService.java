// wardrobe-api/src/main/java/com/creaite/wardrobe_api/services/GeminiService.java
package com.creaite.wardrobe_api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";

    private static final String PROCESSING_PROMPT = """
            Transform this garment photo into a high-end, e-commerce studio shot.
            Key Instructions:
            1. Background: Replace the existing background with a clean, seamless, pure white (RGB 255, 255, 255) backdrop.
            2. Lighting: Apply soft, even studio lighting (high-key setup) to ensure the image is bright and shadowless or has minimal, subtle shadowing only to define the shape.
            3. Retouching: Sharpen the fabric texture and enhance the overall resolution. Remove all wrinkles, creases, and lint from the garment, ensuring the fabric texture remains realistic.
            4. Presentation: Ensure the garment is perfectly draped, symmetrical, and flat-lay styled, ready for an online product catalog.
            
            Return ONLY the processed image data, no additional text or explanations.
            """;

    public String processImageWithGemini(String imageBase64) {
        try {
            log.info("=== Processing image with Gemini ===");

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Remove o prefixo "data:image/...;base64," se existir
            String cleanBase64 = imageBase64;
            if (imageBase64.contains(",")) {
                cleanBase64 = imageBase64.split(",")[1];
            }

            // Monta o request para o Gemini
            Map<String, Object> requestBody = new HashMap<>();

            Map<String, Object> part1 = new HashMap<>();
            part1.put("text", PROCESSING_PROMPT);

            Map<String, Object> part2 = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", cleanBase64);
            part2.put("inline_data", inlineData);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part1, part2));

            requestBody.put("contents", List.of(content));

            String url = GEMINI_API_URL + "?key=" + geminiApiKey;

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Sending request to Gemini API...");
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            log.info("Gemini API Response Status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Extrai a imagem processada da resposta
                Map<String, Object> responseBody = response.getBody();

                // NOTA: A estrutura exata da resposta pode variar
                // Gemini pode retornar a imagem em diferentes formatos
                // Você pode precisar ajustar isso baseado na resposta real

                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");

                    if (parts != null && !parts.isEmpty()) {
                        Map<String, Object> part = parts.get(0);

                        // Se a resposta contiver inline_data (imagem)
                        if (part.containsKey("inline_data")) {
                            Map<String, Object> inlineDataResponse = (Map<String, Object>) part.get("inline_data");
                            String processedImage = (String) inlineDataResponse.get("data");
                            log.info("✅ Image processed successfully by Gemini");
                            return "data:image/jpeg;base64," + processedImage;
                        }

                        // Se a resposta contiver texto (pode ser URL ou outra referência)
                        if (part.containsKey("text")) {
                            String text = (String) part.get("text");
                            log.info("✅ Gemini response received (text format)");
                            return text;
                        }
                    }
                }

                log.warn("⚠️ Unexpected response format from Gemini");
                throw new RuntimeException("Unexpected response format from Gemini API");
            }

            log.error("❌ Failed to process image with Gemini - Status: {}", response.getStatusCode());
            throw new RuntimeException("Failed to process image with Gemini");

        } catch (Exception e) {
            log.error("❌ Error processing image with Gemini: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing image with Gemini: " + e.getMessage());
        }
    }
}
// wardrobe-api/src/main/java/com/creaite/wardrobe_api/services/RemoveBGService.java
package com.creaite.wardrobe_api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Service
public class RemoveBGService {

    @Value("${REMOVEBG_API_KEY:your_removebg_key_here}")
    private String apiKey;

    private static final String REMOVEBG_API_URL = "https://api.remove.bg/v1.0/removebg";

    /**
     * Remove o fundo da imagem usando remove.bg API
     */
    public String removeBackground(String imageBase64) {
        log.info("=== RemoveBG: Starting background removal ===");

        try {
            // Remove o prefixo data:image se existir
            String cleanBase64 = imageBase64;
            if (imageBase64.contains(",")) {
                cleanBase64 = imageBase64.split(",")[1];
            }

            RestTemplate restTemplate = new RestTemplate();

            // Configura headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Api-Key", apiKey);

            // Configura body
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("image_file_b64", cleanBase64);
            body.add("size", "auto");
            body.add("format", "png");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            log.info("Sending image to remove.bg API...");

            // Faz a requisição
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    REMOVEBG_API_URL,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String resultBase64 = Base64.getEncoder().encodeToString(response.getBody());
                log.info("✅ RemoveBG processing complete");
                return "data:image/png;base64," + resultBase64;
            } else {
                throw new RuntimeException("RemoveBG API returned status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ RemoveBG failed: {}", e.getMessage(), e);
            // Em caso de erro, retorna a imagem original
            log.warn("Returning original image due to RemoveBG failure");
            return imageBase64;
        }
    }
}

// wardrobe-api/src/main/java/com/creaite/wardrobe_api/services/OpenAIService.java
package com.creaite.wardrobe_api.services;

import com.theokanning.openai.image.CreateImageEditRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Service
public class OpenAIService {

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    /**
     * Processa a imagem com OpenAI para deixá-la profissional
     */
    public String enhanceImageWithAI(String imageBase64) {
        log.info("=== OpenAI: Starting image enhancement ===");

        try {
            // Remove o prefixo data:image se existir
            String cleanBase64 = imageBase64;
            if (imageBase64.contains(",")) {
                cleanBase64 = imageBase64.split(",")[1];
            }

            // Decodifica a imagem
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // Cria o serviço OpenAI com timeout maior
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(120));

            // Cria um InputStream da imagem
            InputStream imageStream = new ByteArrayInputStream(imageBytes);

            // Cria a requisição para DALL-E
            CreateImageEditRequest request = CreateImageEditRequest.builder()
                    .prompt("Make this clothing item look like it was photographed in a professional studio for an e-commerce store. Clean white background, professional lighting, high quality.")
                    .n(1)
                    .size("1024x1024")
                    .build();

            log.info("Sending image to OpenAI DALL-E...");
            ImageResult result = service.createImageEdit(request, "image.png", imageStream);

            // Pega a URL da imagem gerada
            String imageUrl = result.getData().get(0).getUrl();

            log.info("✅ OpenAI processing complete - Image URL: {}", imageUrl);

            // Baixa a imagem e converte para base64
            String enhancedBase64 = downloadImageAsBase64(imageUrl);

            return "data:image/png;base64," + enhancedBase64;

        } catch (Exception e) {
            log.error("❌ OpenAI enhancement failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to enhance image with AI: " + e.getMessage());
        }
    }

    private String downloadImageAsBase64(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.io.InputStream in = url.openStream();
            byte[] imageBytes = in.readAllBytes();
            in.close();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("Failed to download image from URL: {}", e.getMessage());
            throw new RuntimeException("Failed to download enhanced image");
        }
    }
}
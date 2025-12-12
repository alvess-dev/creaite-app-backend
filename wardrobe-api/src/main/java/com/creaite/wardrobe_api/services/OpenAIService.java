// wardrobe-api/src/main/java/com/creaite/wardrobe_api/services/OpenAIService.java
package com.creaite.wardrobe_api.services;

import com.theokanning.openai.image.CreateImageEditRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

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

        File tempImageFile = null;

        try {
            // Remove o prefixo data:image se existir
            String cleanBase64 = imageBase64;
            if (imageBase64.contains(",")) {
                cleanBase64 = imageBase64.split(",")[1];
            }

            // Decodifica a imagem
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // ✅ CORRIGIDO: Cria arquivo temporário
            tempImageFile = createTempFile(imageBytes, "png");
            log.info("Temporary file created: {}", tempImageFile.getAbsolutePath());

            // Cria o serviço OpenAI com timeout maior
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(120));

            // Cria a requisição para DALL-E
            CreateImageEditRequest request = CreateImageEditRequest.builder()
                    .prompt("Make this clothing item look like it was photographed in a professional studio for an e-commerce store. Clean white background, professional lighting, high quality.")
                    .n(1)
                    .size("1024x1024")
                    .build();

            log.info("Sending image to OpenAI DALL-E...");

            // ✅ CORRIGIDO: Usa File em vez de InputStream
            ImageResult result = service.createImageEdit(request, tempImageFile.getAbsolutePath(), null);

            // Pega a URL da imagem gerada
            String imageUrl = result.getData().get(0).getUrl();

            log.info("✅ OpenAI processing complete - Image URL: {}", imageUrl);

            // Baixa a imagem e converte para base64
            String enhancedBase64 = downloadImageAsBase64(imageUrl);

            return "data:image/png;base64," + enhancedBase64;

        } catch (Exception e) {
            log.error("❌ OpenAI enhancement failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to enhance image with AI: " + e.getMessage());
        } finally {
            // ✅ Limpa arquivo temporário
            if (tempImageFile != null && tempImageFile.exists()) {
                try {
                    Files.deleteIfExists(tempImageFile.toPath());
                    log.debug("Temporary file deleted");
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Cria um arquivo temporário com os bytes da imagem
     */
    private File createTempFile(byte[] imageBytes, String extension) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "openai_" + UUID.randomUUID() + "." + extension;
        File tempFile = new File(tempDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(imageBytes);
        }

        return tempFile;
    }

    /**
     * Baixa a imagem da URL e converte para base64
     */
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
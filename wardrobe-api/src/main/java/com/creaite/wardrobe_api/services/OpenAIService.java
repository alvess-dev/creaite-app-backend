// wardrobe-api/src/main/java/com/creaite/wardrobe_api/services/OpenAIService.java
package com.creaite.wardrobe_api.services;

import com.theokanning.openai.image.CreateImageEditRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
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

    private static final int MAX_FILE_SIZE = 4 * 1024 * 1024; // 4 MB
    private static final int MAX_DIMENSION = 1024;

    /**
     * ✅ SOLUÇÃO COMPLETA: DALL-E Edit com máscara transparente
     * Funciona exatamente como no ChatGPT!
     */
    public String enhanceImageWithAI(String imageBase64) {
        log.info("=== OpenAI: Starting image enhancement with DALL-E Edit ===");

        File tempImageFile = null;
        File tempMaskFile = null;

        try {
            // Remove o prefixo data:image se existir
            String cleanBase64 = imageBase64;
            if (imageBase64.contains(",")) {
                cleanBase64 = imageBase64.split(",")[1];
            }

            // Decodifica a imagem
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
            log.info("Original image size: {} bytes ({} MB)", imageBytes.length, imageBytes.length / 1024.0 / 1024.0);

            // ✅ Processa a imagem (PNG válido, tamanho correto)
            byte[] processedImageBytes = processImageForOpenAI(imageBytes);
            log.info("Processed image size: {} bytes ({} MB)", processedImageBytes.length, processedImageBytes.length / 1024.0 / 1024.0);

            // Valida tamanho final
            if (processedImageBytes.length > MAX_FILE_SIZE) {
                throw new RuntimeException("Image too large: " + processedImageBytes.length + " bytes");
            }

            // Cria arquivo temporário da imagem
            tempImageFile = createTempFile(processedImageBytes, "png");
            log.info("Image file created: {}", tempImageFile.getAbsolutePath());

            // ✅ CRUCIAL: Cria máscara transparente do mesmo tamanho
            byte[] maskBytes = createTransparentMask(processedImageBytes);
            tempMaskFile = createTempFile(maskBytes, "png");
            log.info("Mask file created: {}", tempMaskFile.getAbsolutePath());

            // Cria o serviço OpenAI
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(120));

            // ✅ Cria a requisição de edição com prompt personalizado
            CreateImageEditRequest request = CreateImageEditRequest.builder()
                    .prompt("Make this image look like it was taken in a professional studio for an e-commerce store.")
                    .n(1)
                    .size("1024x1024")
                    .responseFormat("url")
                    .build();

            log.info("Sending image to DALL-E Edit...");

            // ✅ Envia para OpenAI com imagem + máscara
            ImageResult result = service.createImageEdit(
                    request,
                    tempImageFile.getAbsolutePath(),
                    tempMaskFile.getAbsolutePath()
            );

            // Pega a URL da imagem editada
            String imageUrl = result.getData().get(0).getUrl();
            log.info("✅ DALL-E Edit complete - Image URL: {}", imageUrl);

            // Baixa a imagem e converte para base64
            String enhancedBase64 = downloadImageAsBase64(imageUrl);

            return "data:image/png;base64," + enhancedBase64;

        } catch (Exception e) {
            log.error("❌ OpenAI enhancement failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to enhance image with AI: " + e.getMessage());
        } finally {
            // Limpa arquivos temporários
            cleanupFile(tempImageFile);
            cleanupFile(tempMaskFile);
        }
    }

    /**
     * ✅ Cria uma máscara transparente (PNG com alpha channel)
     * Permite editar a imagem inteira com o prompt
     */
    private byte[] createTransparentMask(byte[] imageBytes) throws IOException {
        // Lê a imagem para pegar as dimensões
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(bais);

        if (originalImage == null) {
            throw new IOException("Failed to read image for mask creation");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Cria imagem completamente transparente (máscara que permite edição total)
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = mask.createGraphics();

        // Preenche com transparente (alpha = 0)
        g2d.setComposite(java.awt.AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        // Converte para bytes
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(mask, "PNG", baos);

        log.info("Created transparent mask: {}x{} pixels", width, height);
        return baos.toByteArray();
    }

    /**
     * Processa a imagem: redimensiona, converte para PNG e comprime
     */
    private byte[] processImageForOpenAI(byte[] originalBytes) throws IOException {
        // Lê a imagem original
        ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
        BufferedImage originalImage = ImageIO.read(bais);

        if (originalImage == null) {
            throw new IOException("Failed to read image");
        }

        // Calcula dimensões mantendo aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // Redimensiona se necessário
        if (originalWidth > MAX_DIMENSION || originalHeight > MAX_DIMENSION) {
            double ratio = Math.min(
                    (double) MAX_DIMENSION / originalWidth,
                    (double) MAX_DIMENSION / originalHeight
            );
            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);

            log.info("Resizing image from {}x{} to {}x{}", originalWidth, originalHeight, newWidth, newHeight);
        }

        // Cria nova imagem redimensionada
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Melhora a qualidade do redimensionamento
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();

        // Salva como PNG
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(resizedImage, "PNG", baos);

        return baos.toByteArray();
    }

    /**
     * Cria um arquivo temporário com os bytes
     */
    private File createTempFile(byte[] bytes, String extension) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "openai_" + UUID.randomUUID() + "." + extension;
        File tempFile = new File(tempDir, fileName);
        Files.write(tempFile.toPath(), bytes);
        return tempFile;
    }

    /**
     * Limpa arquivo temporário
     */
    private void cleanupFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
                log.debug("Deleted temp file: {}", file.getName());
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", e.getMessage());
            }
        }
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
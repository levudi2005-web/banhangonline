package com.banhangonline;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.product.storage.LocalImageStorageProperties;
import com.banhangonline.product.storage.ProductImageStorageService;
import com.banhangonline.product.storage.S3StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductImageStorageServiceTest {
    @TempDir
    Path tempDirectory;

    private final S3Client client = mock(S3Client.class);
    private S3StorageProperties properties;
    private ProductImageStorageService service;

    @BeforeEach
    void setUp() {
        properties = new S3StorageProperties();
        properties.setEnabled(true);
        properties.setBucket("product-images");
        properties.setPublicBaseUrl("https://images.example.test");
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("s3Client", client);
        service = new ProductImageStorageService(properties, beans.getBeanProvider(S3Client.class));
    }

    @Test
    void uploadsValidatedImageToStoreScopedObjectKey() {
        var file = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1});

        String imageUrl = service.upload(12L, file);

        assertThat(imageUrl).startsWith("https://images.example.test/stores/12/products/")
                .endsWith(".png");
        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void rejectsUnsupportedImageContentBeforeCallingStorage() {
        var file = new MockMultipartFile("file", "photo.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> service.upload(12L, file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("PNG, JPG hoặc WEBP");
        verifyNoInteractions(client);
    }

    @Test
    void rejectsImagesLargerThanFiveMegabytes() {
        var file = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.upload(12L, file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("5 MB");
        verifyNoInteractions(client);
    }

    @Test
    void reportsUnavailableStorageWithoutPretendingTheUploadSucceeded() {
        properties.setEnabled(false);
        var file = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1});

        assertThatThrownBy(() -> service.upload(12L, file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("chưa được cấu hình");

        verifyNoInteractions(client);
    }

    @Test
    void onlyDeletesImageUrlsInTheRequestedStorePrefix() {
        boolean deleted = service.deleteIfManaged(12L,
                "https://images.example.test/stores/13/products/123e4567-e89b-12d3-a456-426614174000.png");

        assertThat(deleted).isFalse();
        verifyNoInteractions(client);
    }

    @Test
    void deletesManagedImagesUsingTheStoreScopedKey() {
        String imageUrl = "https://images.example.test/stores/12/products/"
                + "123e4567-e89b-12d3-a456-426614174000.png";

        assertThat(service.deleteIfManaged(12L, imageUrl)).isTrue();

        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void localStorageUploadsServesAndDeletesImagesWithinTheStoreDirectory() throws Exception {
        properties.setEnabled(false);
        LocalImageStorageProperties local = new LocalImageStorageProperties();
        local.setEnabled(true);
        local.setDirectory(tempDirectory.toString());
        service = new ProductImageStorageService(properties, local,
                new StaticListableBeanFactory().getBeanProvider(S3Client.class));
        var file = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1});

        String imageUrl = service.upload(12L, file);

        assertThat(imageUrl).matches("/uploads/stores/12/products/[0-9a-f-]{36}\\.png");
        Path stored = tempDirectory.resolve(imageUrl.substring("/uploads/".length()));
        assertThat(Files.readAllBytes(stored)).containsExactly(file.getBytes());
        assertThat(service.isManagedImageUrl(12L, imageUrl)).isTrue();
        assertThat(service.isManagedImageUrl(13L, imageUrl)).isFalse();
        assertThat(service.deleteIfManaged(13L, imageUrl)).isFalse();
        assertThat(service.deleteIfManaged(12L, imageUrl)).isTrue();
        assertThat(Files.exists(stored)).isFalse();
    }
}

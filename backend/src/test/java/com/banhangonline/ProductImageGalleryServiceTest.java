package com.banhangonline;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.inventory.entity.Inventory;
import com.banhangonline.inventory.repository.InventoryRepository;
import com.banhangonline.product.dto.ProductImageView;
import com.banhangonline.product.entity.Product;
import com.banhangonline.product.repository.ProductImageRepository;
import com.banhangonline.product.repository.ProductRepository;
import com.banhangonline.product.service.ProductImageGalleryService;
import com.banhangonline.product.storage.ProductImageStorageService;
import com.banhangonline.store.service.StorePermissionService;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ProductImageGalleryServiceTest {
    private final ProductImageRepository images = mock(ProductImageRepository.class);
    private final InventoryRepository inventory = mock(InventoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final StorePermissionService permissions = mock(StorePermissionService.class);
    private final ProductImageStorageService storage = mock(ProductImageStorageService.class);
    private final ProductImageGalleryService service =
            new ProductImageGalleryService(images, inventory, products, permissions, storage);

    @Test
    void replacePersistsOrderAndUsesFirstImageAsMain() {
        Product product = new Product();
        Inventory stock = new Inventory();
        stock.setProduct(product);
        when(inventory.findByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(inventory.existsByProductIdAndStoreIdNot(22L, 7L)).thenReturn(false);
        List<String> urls = List.of(
                "https://images.example.test/main.jpg",
                "https://images.example.test/side.jpg");
        when(storage.isManagedImageUrl(7L, urls.get(0))).thenReturn(true);
        when(storage.isManagedImageUrl(7L, urls.get(1))).thenReturn(true);

        List<ProductImageView> result =
                service.replace(7L, 22L, 12L, Set.of("OWNER"), urls);

        assertThat(product.getImageUrl()).isEqualTo(urls.get(0));
        assertThat(result).containsExactly(
                new ProductImageView(urls.get(0), 0, true),
                new ProductImageView(urls.get(1), 1, false));
        verify(images).replace(22L, urls);
        verify(permissions).require(7L, 12L, Set.of("OWNER"), "MANAGE_PRODUCTS");
    }

    @Test
    void replaceRejectsMoreThanEightImages() {
        List<String> urls = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> "https://images.example.test/" + index + ".jpg").toList();

        assertThatThrownBy(() -> service.replace(7L, 22L, 12L, Set.of("OWNER"), urls))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("tối đa 8 ảnh");

        verifyNoInteractions(images);
        verify(inventory, never()).findByStoreIdAndProductId(anyLong(), anyLong());
    }

    @Test
    void replaceRejectsDuplicateImageUrls() {
        String imageUrl = "https://images.example.test/main.jpg";

        assertThatThrownBy(() -> service.replace(7L, 22L, 12L, Set.of("OWNER"),
                List.of(imageUrl, imageUrl)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("bị trùng");

        verifyNoInteractions(images);
    }

    @Test
    void replaceRejectsNonHttpImageUrlsBeforeLoadingProduct() {
        assertThatThrownBy(() -> service.replace(7L, 22L, 12L, Set.of("OWNER"),
                List.of("file:///tmp/image.jpg")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("HTTP hoặc HTTPS");

        verifyNoInteractions(images);
        verify(inventory, never()).findByStoreIdAndProductId(anyLong(), anyLong());
    }

    @Test
    void replaceRejectsNewExternalImageUrls() {
        Product product = new Product();
        Inventory stock = new Inventory();
        stock.setProduct(product);
        when(inventory.findByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(inventory.existsByProductIdAndStoreIdNot(22L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.replace(7L, 22L, 12L, Set.of("OWNER"),
                List.of("https://untrusted.example.test/new.jpg")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("phải được tải lên");

        verify(images, never()).replace(anyLong(), anyList());
    }

    @Test
    void replaceRejectsSharedProductWithoutChangingItsPrimaryImage() {
        Product product = new Product();
        product.setImageUrl("https://images.example.test/current.jpg");
        Inventory stock = new Inventory();
        stock.setProduct(product);
        when(inventory.findByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(inventory.existsByProductIdAndStoreIdNot(22L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.replace(7L, 22L, 12L, Set.of("OWNER"),
                List.of("https://images.example.test/new.jpg")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cửa hàng khác");

        assertThat(product.getImageUrl()).isEqualTo("https://images.example.test/current.jpg");
        verifyNoInteractions(images);
    }

    @Test
    void replaceWithEmptyGalleryClearsLegacyPrimaryImage() {
        Product product = new Product();
        product.setImageUrl("https://images.example.test/current.jpg");
        Inventory stock = new Inventory();
        stock.setProduct(product);
        when(inventory.findByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(inventory.existsByProductIdAndStoreIdNot(22L, 7L)).thenReturn(false);

        assertThat(service.replace(7L, 22L, 12L, Set.of("OWNER"), List.of())).isEmpty();

        assertThat(product.getImageUrl()).isNull();
        verify(images).replace(22L, List.of());
    }

    @Test
    void replaceRequiresManagementPermissionBeforeWriting() {
        ApiException forbidden = new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Không có quyền");
        doThrow(forbidden).when(permissions).require(7L, 12L, Set.of("STAFF"), "MANAGE_PRODUCTS");

        assertThatThrownBy(() -> service.replace(7L, 22L, 12L, Set.of("STAFF"),
                List.of("https://images.example.test/new.jpg")))
                .isSameAs(forbidden);

        verifyNoInteractions(images, inventory);
    }

    @Test
    void customerGalleryFallsBackToLegacyPrimaryImage() {
        Product product = new Product();
        product.setImageUrl("https://images.example.test/legacy.jpg");
        Inventory stock = new Inventory();
        stock.setProduct(product);
        when(inventory.findAvailableByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(images.findByProductId(22L)).thenReturn(List.of());

        assertThat(service.forCustomer(7L, 22L))
                .containsExactly(new ProductImageView("https://images.example.test/legacy.jpg", 0, true));
    }
}

package com.banhangonline.product.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class LocalImageResourceConfiguration implements WebMvcConfigurer {
    private final LocalImageStorageProperties properties;

    public LocalImageResourceConfiguration(LocalImageStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!properties.isEnabled()) {
            return;
        }
        Path root = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}

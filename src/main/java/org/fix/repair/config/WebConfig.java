package org.fix.repair.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 注意：文件现在存储在MinIO中，不再需要本地静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 移除本地文件静态资源映射，文件现在存储在MinIO中
        // 如果需要访问MinIO文件，直接使用MinIO的URL即可
        
        // 保留其他静态资源映射（如果有的话）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
} 
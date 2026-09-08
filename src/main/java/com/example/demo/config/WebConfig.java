package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

@Configuration // 告訴 Spring Boot 這是一篇系統設定檔
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 將相對於專案根目錄的 uploads 資料夾映射為公開靜態資源。
        String uploadPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);

        System.out.println("🚪 [靜態資源映射] 本地上傳通道對接完畢！實體路徑指向：" + uploadPath);
    }
}

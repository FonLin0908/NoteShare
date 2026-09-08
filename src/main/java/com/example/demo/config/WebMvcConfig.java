package com.example.demo.config;

import com.example.demo.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") // 預設攔截「所有」網頁
                .excludePathPatterns(
                        "/","/error",            // 首頁
                        "/login","/doLogin",
                        "/register","/doRegister",
                        "error/**",
                        "/notes","/notes/view","/notes/share/**",
                        "/uploads/**", "/images/**",
                        "/api/**",
                        "/member/**",
                        "/css/**",      // 靜態資源（CSS、JS、圖片等）
                        "/js/**",
                        "/images/**",
                        "/ranking"
                );
    }
}
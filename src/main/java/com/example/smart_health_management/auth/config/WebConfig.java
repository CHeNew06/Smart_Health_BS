package com.example.smart_health_management.auth.config;

import com.example.smart_health_management.auth.security.AuthInterceptor;
import com.example.smart_health_management.common.RequestLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Value("${file.upload.path}")
    private String uploadPath;

    public WebConfig(AuthInterceptor authInterceptor, RequestLoggingInterceptor requestLoggingInterceptor) {
        this.authInterceptor = authInterceptor;
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh",
                        "/api/auth/send-code",
                        "/api/auth/reset-password",
                        "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:" + uploadPath);
    }
}

package com.cloudbrainmed.patient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadFileConfig implements WebMvcConfigurer {

    @Value("${upload.avatar.patient-dir:${user.dir}/upload/avatar}")
    private String avatarUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/avatar/patient/**")
                .addResourceLocations("file:" + avatarUploadDir + "/");
        // 兼容存量数据：历史头像 avatar 字段仍为 /avatar/{filename}，保留只读映射避免 404
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:" + avatarUploadDir + "/");
    }
}

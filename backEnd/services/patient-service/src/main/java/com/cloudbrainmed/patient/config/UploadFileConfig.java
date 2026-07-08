package com.cloudbrainmed.patient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadFileConfig implements WebMvcConfigurer {

    @Value("${upload.avatar.patient-dir:${user.dir}/upload/avatar}")
    private String avatarUploadDir;

    @Value("${upload.avatar.patient-legacy-dir:${user.dir}/uploads/avatar/patient}")
    private String avatarLegacyDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:" + avatarUploadDir + "/");
        registry.addResourceHandler("/files/avatar/patient/**")
                .addResourceLocations("file:" + avatarLegacyDir + "/");
    }
}

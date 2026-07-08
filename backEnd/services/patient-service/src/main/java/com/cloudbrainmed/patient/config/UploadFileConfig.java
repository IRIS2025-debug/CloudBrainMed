package com.cloudbrainmed.patient.config;

import com.cloudbrainmed.common.utils.FileResourceLocationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadFileConfig implements WebMvcConfigurer {

    @Value("${upload.avatar.patient-dir:${user.dir}/uploads/avatar/patient}")
    private String avatarUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/avatar/patient/**")
                .addResourceLocations(FileResourceLocationUtil.toDirectoryResourceLocation(avatarUploadDir));
    }
}

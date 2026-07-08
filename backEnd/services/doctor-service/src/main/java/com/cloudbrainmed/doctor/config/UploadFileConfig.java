package com.cloudbrainmed.doctor.config;

import com.cloudbrainmed.common.utils.FileResourceLocationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadFileConfig implements WebMvcConfigurer {

    @Value("${upload.avatar.doctor-dir:${user.dir}/uploads/avatar/doctor}")
    private String avatarUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/avatar/doctor/**")
                .addResourceLocations(FileResourceLocationUtil.toDirectoryResourceLocation(avatarUploadDir));
    }
}

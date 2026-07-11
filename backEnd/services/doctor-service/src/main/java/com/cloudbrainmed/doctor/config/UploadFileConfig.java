package com.cloudbrainmed.doctor.config;

import java.nio.file.Paths;

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
                .addResourceLocations(toDirectoryResourceLocation(avatarUploadDir));
    }

    static String toDirectoryResourceLocation(String directory) {
        String location = Paths.get(directory)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        return location;
    }
}

package com.cloudbrainmed.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件上传资源配置——将本地头像目录映射为静态资源URL
 */
@Configuration
public class UploadFileConfig implements WebMvcConfigurer {

    @Value("${upload.avatar.admin-dir:${user.dir}/uploads/avatar/admin}")
    private String avatarUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/avatar/admin/**")
                .addResourceLocations("file:" + avatarUploadDir + "/");
    }
}
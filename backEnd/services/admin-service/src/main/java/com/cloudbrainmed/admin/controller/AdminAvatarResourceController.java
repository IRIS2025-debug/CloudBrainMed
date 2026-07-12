package com.cloudbrainmed.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class AdminAvatarResourceController {

    private final Path avatarDirectory;

    public AdminAvatarResourceController(
            @Value("${upload.avatar.admin-dir:${user.dir}/uploads/avatar/admin}") String avatarUploadDir) {
        this.avatarDirectory = Path.of(avatarUploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/files/avatar/admin/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename)
            throws MalformedURLException {
        Path avatar = avatarDirectory.resolve(filename).normalize();
        if (!avatar.startsWith(avatarDirectory) || !Files.isRegularFile(avatar)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(avatar.toUri());
        MediaType contentType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                .body(resource);
    }
}

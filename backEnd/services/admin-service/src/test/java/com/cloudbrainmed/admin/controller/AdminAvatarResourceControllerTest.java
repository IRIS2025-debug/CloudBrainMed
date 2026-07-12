package com.cloudbrainmed.admin.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAvatarResourceControllerTest {

    @TempDir
    Path avatarDir;

    @Test
    void returnsExistingAvatarFromConfiguredDirectory() throws Exception {
        byte[] content = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff};
        Files.write(avatarDir.resolve("admin-1.jpg"), content);
        AdminAvatarResourceController controller =
                new AdminAvatarResourceController(avatarDir.toString());

        ResponseEntity<Resource> response = controller.getAvatar("admin-1.jpg");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(response.getBody()).isNotNull();
        try (InputStream input = response.getBody().getInputStream()) {
            assertThat(input.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void returnsNotFoundForMissingAvatar() throws Exception {
        AdminAvatarResourceController controller =
                new AdminAvatarResourceController(avatarDir.toString());

        ResponseEntity<Resource> response = controller.getAvatar("missing.png");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void rejectsPathTraversal() throws Exception {
        AdminAvatarResourceController controller =
                new AdminAvatarResourceController(avatarDir.toString());

        ResponseEntity<Resource> response = controller.getAvatar("../secret.jpg");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }
}

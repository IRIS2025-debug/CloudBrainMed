package com.cloudbrainmed.doctor.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UploadFileConfigTest {

    @Test
    void buildsLocalFileUriForAvatarDirectory() {
        Path avatarDirectory = Path.of("uploads", "avatar", "doctor");

        String location = UploadFileConfig.toDirectoryResourceLocation(avatarDirectory.toString());

        assertThat(location).startsWith("file:///");
        assertThat(location).endsWith("/uploads/avatar/doctor/");
        assertThat(location).doesNotStartWith("file://uploads");
    }
}

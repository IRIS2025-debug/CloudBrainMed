package com.cloudbrainmed.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileResourceLocationUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void toDirectoryResourceLocationBuildsFileUriWithTrailingSlash() {
        String location = FileResourceLocationUtil.toDirectoryResourceLocation(tempDir.toString());

        assertThat(location).isEqualTo(tempDir.toUri().toString());
        assertThat(location).startsWith("file:");
        assertThat(location).endsWith("/");
    }

    @Test
    void toDirectoryResourceLocationKeepsTrailingSlashForMissingDirectory() {
        Path missingDirectory = tempDir.resolve("missing-avatar-dir");

        String location = FileResourceLocationUtil.toDirectoryResourceLocation(
                missingDirectory.toString());

        assertThat(location).startsWith("file:");
        assertThat(location).endsWith("/missing-avatar-dir/");
    }
}

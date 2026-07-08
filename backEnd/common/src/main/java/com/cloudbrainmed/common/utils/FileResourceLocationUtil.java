package com.cloudbrainmed.common.utils;

import java.nio.file.Paths;

public final class FileResourceLocationUtil {

    private FileResourceLocationUtil() {
    }

    public static String toDirectoryResourceLocation(String directory) {
        String location = Paths.get(directory)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        return location.endsWith("/") ? location : location + "/";
    }
}

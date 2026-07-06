package com.cloudbrainmed.doctor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeCtUploadConfigTest {

    @Test
    void dockerDemoServicesAllowCtNiftiUploads() throws IOException {
        String compose = Files.readString(resolveComposeFile()).replace("\r\n", "\n");

        assertMultipartLimit(compose, "doctor-service");
        assertMultipartLimit(compose, "ai-service");
    }

    private Path resolveComposeFile() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path compose = current.resolve("docker").resolve("docker-compose.yml");
            if (Files.exists(compose)) {
                return compose;
            }
            current = current.getParent();
        }
        throw new AssertionError("docker/docker-compose.yml not found from test working directory");
    }

    private void assertMultipartLimit(String compose, String serviceName) {
        String serviceBlock = serviceBlock(compose, serviceName);

        assertThat(serviceBlock)
                .contains("SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: 100MB")
                .contains("SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE: 100MB");
    }

    private String serviceBlock(String compose, String serviceName) {
        String marker = "\n  " + serviceName + ":\n";
        int start = compose.indexOf(marker);
        assertThat(start).as("service block for %s", serviceName).isGreaterThanOrEqualTo(0);
        int end = compose.length();
        for (String name : new String[] {
                "nacos", "sentinel", "redis", "gateway-server", "admin-service",
                "ai-service", "auth-service", "doctor-service", "patient-service", "payment-service"
        }) {
            if (name.equals(serviceName)) {
                continue;
            }
            int candidate = compose.indexOf("\n  " + name + ":\n", start + marker.length());
            if (candidate >= 0 && candidate < end) {
                end = candidate;
            }
        }
        return compose.substring(start, end);
    }
}

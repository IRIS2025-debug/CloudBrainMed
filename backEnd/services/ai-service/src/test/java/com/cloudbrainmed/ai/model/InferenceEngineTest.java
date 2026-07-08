package com.cloudbrainmed.ai.model;

import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InferenceEngineTest {

    @Test
    void constructorUsesConfiguredPythonServiceUrlWithoutTrailingSlash() {
        InferenceEngine engine = new InferenceEngine(
                mock(AiInferenceLogMapper.class),
                "http://python-service:8000/");

        assertThat(engine.getPythonServiceUrl()).isEqualTo("http://python-service:8000");
    }

    @Test
    void constructorTrimsConfiguredPythonServiceUrl() {
        InferenceEngine engine = new InferenceEngine(
                mock(AiInferenceLogMapper.class),
                "  http://python-service:8000/  ");

        assertThat(engine.getPythonServiceUrl()).isEqualTo("http://python-service:8000");
    }

    @Test
    void constructorDefaultsPythonServiceUrlToPort8010WhenBlank() {
        InferenceEngine engine = new InferenceEngine(
                mock(AiInferenceLogMapper.class),
                "  ");

        assertThat(engine.getPythonServiceUrl()).isEqualTo("http://localhost:8010");
    }

    @Test
    void constructorConfiguresTimeoutsForMultipartInferenceRequests() {
        InferenceEngine engine = new InferenceEngine(
                mock(AiInferenceLogMapper.class),
                "http://python-service:8000");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(engine, "restTemplate");
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
        SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(30_000);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(300_000);
    }

    @Test
    void predictArtifactSendsMultipartFilePartToPythonService() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/predict-ct-artifact", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"status\":\"success\",\"positive_pixels\":1,\"total_pixels\":2,\"ratio\":50}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            InferenceEngine engine = new InferenceEngine(
                    mock(AiInferenceLogMapper.class),
                    "http://localhost:" + server.getAddress().getPort());
            MockMultipartFile file = new MockMultipartFile(
                    "file", "scan.nii.gz", "application/gzip", "ct-bytes".getBytes(StandardCharsets.UTF_8));

            engine.predictArtifact(file);

            assertThat(requestBody.get())
                    .contains("Content-Disposition: form-data; name=\"file\"; filename=\"scan.nii.gz\"")
                    .contains("Content-Type: application/gzip")
                    .contains("ct-bytes");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void predictArtifactPreservesStructuredReportInputFromPythonService() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/predict-ct-artifact", exchange -> {
            byte[] response = ("""
                    {
                      "status": "success",
                      "artifactDetected": true,
                      "positivePixels": 3,
                      "totalPixels": 8,
                      "artifactRatio": 37.5,
                      "reportInput": {
                        "task": "CT_ARTIFACT_REPORT",
                        "summary": "检测到CT金属伪影，伪影像素占比约37.5000%。"
                      }
                    }
                    """).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            InferenceEngine engine = new InferenceEngine(
                    mock(AiInferenceLogMapper.class),
                    "http://localhost:" + server.getAddress().getPort());
            MockMultipartFile file = new MockMultipartFile(
                    "file", "scan.nii.gz", "application/gzip", "ct-bytes".getBytes(StandardCharsets.UTF_8));

            Map<String, Object> result = engine.predictArtifact(file);

            assertThat(result)
                    .containsEntry("artifactDetected", true)
                    .containsEntry("artifactRatio", 37.5);
            assertThat(result.get("reportInput"))
                    .isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> reportInput = (Map<String, Object>) result.get("reportInput");
            assertThat(reportInput)
                    .containsEntry("task", "CT_ARTIFACT_REPORT")
                    .containsKey("summary");
            assertThat(result)
                    .containsKeys("logId", "latencyMs");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void predictLesionSendsMultipartFilePartToPythonService() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/predict-ct-lesion", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"status\":\"success\",\"lesionDetected\":true,\"lesionPixels\":4}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            InferenceEngine engine = new InferenceEngine(
                    mock(AiInferenceLogMapper.class),
                    "http://localhost:" + server.getAddress().getPort());
            MockMultipartFile file = new MockMultipartFile(
                    "file", "scan.nii.gz", "application/gzip", "ct-bytes".getBytes(StandardCharsets.UTF_8));

            Map<String, Object> result = engine.predictLesion(file);

            assertThat(requestBody.get())
                    .contains("Content-Disposition: form-data; name=\"file\"; filename=\"scan.nii.gz\"")
                    .contains("Content-Type: application/gzip")
                    .contains("ct-bytes");
            assertThat(result)
                    .containsEntry("lesionDetected", true)
                    .containsKeys("logId", "latencyMs");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void predictArtifactStreamsMultipartWithoutCallingGetBytes() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/predict-ct-artifact", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"status\":\"success\",\"positive_pixels\":1}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            InferenceEngine engine = new InferenceEngine(
                    mock(AiInferenceLogMapper.class),
                    "http://localhost:" + server.getAddress().getPort());
            MultipartFile file = new StreamOnlyMultipartFile(
                    "file", "scan.nii.gz", "application/gzip", "ct-bytes".getBytes(StandardCharsets.UTF_8));

            engine.predictArtifact(file);

            assertThat(requestBody.get())
                    .contains("filename=\"scan.nii.gz\"")
                    .contains("ct-bytes");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadPreviewFetchesPreviewPngFromPythonService() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/previews/scan_preview_z1.png", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            byte[] response = "png-bytes".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            InferenceEngine engine = new InferenceEngine(
                    mock(AiInferenceLogMapper.class),
                    "http://localhost:" + server.getAddress().getPort());

            byte[] result = engine.downloadPreview("scan_preview_z1.png");

            assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("png-bytes");
            assertThat(requestedPath.get()).isEqualTo("/previews/scan_preview_z1.png");
        } finally {
            server.stop(0);
        }
    }

    private static class StreamOnlyMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private StreamOnlyMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            throw new UnsupportedOperationException("getBytes should not be used for multipart forwarding");
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            throw new UnsupportedOperationException("transferTo is not needed in this test");
        }
    }
}

package com.cloudbrainmed.ai.model;

import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

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
                mock(ModelLoader.class),
                "http://python-service:8000/");

        assertThat(engine.getPythonServiceUrl()).isEqualTo("http://python-service:8000");
    }

    @Test
    void constructorTrimsConfiguredPythonServiceUrl() {
        InferenceEngine engine = new InferenceEngine(
                mock(AiInferenceLogMapper.class),
                mock(ModelLoader.class),
                "  http://python-service:8000/  ");

        assertThat(engine.getPythonServiceUrl()).isEqualTo("http://python-service:8000");
    }

    @Test
    void constructorDefaultsPythonServiceUrlToPort8010WhenBlank() {
        InferenceEngine engine = new InferenceEngine(
                mock(AiInferenceLogMapper.class),
                mock(ModelLoader.class),
                "  ");

        assertThat(engine.getPythonServiceUrl()).isEqualTo("http://localhost:8010");
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
                    mock(ModelLoader.class),
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
                    mock(ModelLoader.class),
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
}

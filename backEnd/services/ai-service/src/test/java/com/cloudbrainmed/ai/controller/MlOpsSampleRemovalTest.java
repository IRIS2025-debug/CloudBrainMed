package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MlOpsSampleRemovalTest {

    @Test
    void controllerNoLongerExposesSampleLabelEndpoints() {
        String mappedPaths = Arrays.stream(MlOpsController.class.getDeclaredMethods())
                .flatMap(this::mappingPaths)
                .reduce("", (left, right) -> left + "\n" + right);

        assertThat(mappedPaths)
                .doesNotContain("/sample/list")
                .doesNotContain("/sample/label")
                .doesNotContain("/samples/list")
                .doesNotContain("/samples/update");
    }

    @Test
    void mlOpsServiceContractNoLongerContainsSampleLabelOperations() {
        assertThat(Arrays.stream(MlOpsService.class.getDeclaredMethods())
                .map(Method::getName))
                .doesNotContain("getSampleList", "updateSample");
    }

    private Stream<String> mappingPaths(Method method) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return Arrays.stream(getMapping.value());
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return Arrays.stream(postMapping.value());
        }
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return Arrays.stream(putMapping.value());
        }
        return Stream.empty();
    }
}

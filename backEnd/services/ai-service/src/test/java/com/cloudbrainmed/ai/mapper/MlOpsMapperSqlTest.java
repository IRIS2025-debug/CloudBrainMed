package com.cloudbrainmed.ai.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MlOpsMapperSqlTest {

    @Test
    void inferenceLogMapperUsesCurrentRuntimeTimestampColumn() {
        String sql = mapperSql(AiInferenceLogMapper.class);

        assertThat(sql).contains("created_at")
                .doesNotContain("create_time");
    }

    @Test
    void modelVersionMapperUsesCurrentRuntimeRegistryTable() {
        String sql = mapperSql(ModelVersionMapper.class);

        assertThat(sql).contains("ai_model_registry")
                .doesNotContain("ai_model_version");
    }

    private String mapperSql(Class<?> mapperType) {
        return Arrays.stream(mapperType.getDeclaredMethods())
                .map(this::sqlAnnotationValue)
                .collect(Collectors.joining("\n"))
                .toLowerCase(Locale.ROOT);
    }

    private String sqlAnnotationValue(Method method) {
        Select select = method.getAnnotation(Select.class);
        if (select != null) {
            return String.join(" ", select.value());
        }
        Insert insert = method.getAnnotation(Insert.class);
        if (insert != null) {
            return String.join(" ", insert.value());
        }
        Update update = method.getAnnotation(Update.class);
        if (update != null) {
            return String.join(" ", update.value());
        }
        return "";
    }
}

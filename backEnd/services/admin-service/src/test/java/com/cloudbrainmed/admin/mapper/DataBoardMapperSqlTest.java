package com.cloudbrainmed.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataBoardMapperSqlTest {

    @Test
    void usesReadOnlyCountsForAdminOwnedTables() throws Exception {
        assertNotNull(DataBoardMapper.class.getAnnotation(Mapper.class));
        assertSelectContains("countDoctors", "FROM doctor", "is_deleted = 0");
        assertSelectContains("countDepartments", "FROM department", "status = 1");
        assertSelectContains("countTodaySchedules", "FROM doctor_schedule", "CURRENT_DATE",
                "status = 1", "schedule_status = 'PUBLISHED'");
        assertSelectContains("countMedicines", "FROM medicine");
    }

    private void assertSelectContains(String methodName, String... fragments) throws Exception {
        Method method = DataBoardMapper.class.getMethod(methodName);
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select);
        String sql = String.join(" ", select.value());
        for (String fragment : fragments) {
            assertTrue(sql.contains(fragment), () -> sql + " should contain " + fragment);
        }
    }
}

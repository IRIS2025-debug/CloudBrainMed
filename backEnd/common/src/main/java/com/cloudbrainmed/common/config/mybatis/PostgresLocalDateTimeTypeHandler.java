package com.cloudbrainmed.common.config.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * PostgreSQL TIMESTAMPTZ → LocalDateTime 类型处理器
 * PostgreSQL JDBC 驱动对 TIMESTAMPTZ 列返回 OffsetDateTime，
 * 无法直接转换为 LocalDateTime，需要此类手动处理。
 */
public class PostgresLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toLocalDateTime(rs.getObject(columnName));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toLocalDateTime(rs.getObject(columnIndex));
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toLocalDateTime(cs.getObject(columnIndex));
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toLocalDateTime();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        return null;
    }
}
package org.example.mybatis.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义类型转换器：数据库 VARCHAR <-> Java {@code List<String>}
 *
 * <p>数据库中 tags 存的是 {@code ["玄幻","热血"]} 这种 JSON 数组文本，
 * Java 侧希望直接拿到 {@code List<String>}，中间的转换由本处理器完成。
 *
 * <p>使用位置：NovelMapper.xml 的 resultMap 里 {@code typeHandler="org.example.mybatis.type.TagsTypeHandler"}。
 */
@MappedTypes(List.class)
public class TagsTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, toDb(parameter));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fromDb(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fromDb(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fromDb(cs.getString(columnIndex));
    }

    private String toDb(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return list.stream()
                .map(s -> "\"" + s.replace("\"", "") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private List<String> fromDb(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(text.replace("[", "").replace("]", "").split(","))
                .map(s -> s.trim().replace("\"", "").replace("'", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}

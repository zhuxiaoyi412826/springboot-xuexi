package org.example.mvc.convert;

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 自定义 PropertyEditor（JDK 原生机制，比 Converter 更古老）：
 * 在 {@code @InitBinder} 中 registerCustomEditor 注册，用于把 "2026-01-01" 变成 java.util.Date。
 */
public class DatePropertyEditor extends PropertyEditorSupport {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void setAsText(String text) {
        if (!StringUtils.hasText(text)) {
            setValue(null);
            return;
        }
        LocalDate date = LocalDate.parse(text.trim(), FMT);
        setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        if (value == null) {
            return "";
        }
        return FMT.format(((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
    }
}

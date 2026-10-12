package org.example.mvc.convert;

import org.springframework.format.Formatter;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * 自定义格式化器：BigDecimal <-> "1,234.56"。
 * Formatter 与 Converter 的区别：Formatter 能感知 Locale，适合「展示/解析」双向格式化。
 */
public class MoneyFormatter implements Formatter<BigDecimal> {

    @Override
    public BigDecimal parse(String text, Locale locale) throws ParseException {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Number number = NumberFormat.getInstance(Locale.US).parse(text.replace(",", ""));
        return new BigDecimal(number.toString());
    }

    @Override
    public String print(BigDecimal object, Locale locale) {
        return object == null ? "" : NumberFormat.getInstance(Locale.US).format(object);
    }
}

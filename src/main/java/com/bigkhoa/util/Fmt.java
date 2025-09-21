package com.bigkhoa.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Helper format tiền/tên số cho Thymeleaf email templates.
 * LƯU Ý: NumberFormat không thread-safe => tạo instance mỗi lần dùng.
 */
@Component
public class Fmt {

    private static final Locale VI = new Locale("vi", "VN");

    private NumberFormat currency() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(VI);
        // Ở VN thường hiển thị 0 số lẻ cho VND
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf;
    }

    private NumberFormat number(int minFrac, int maxFrac) {
        NumberFormat nf = NumberFormat.getNumberInstance(VI);
        nf.setMinimumFractionDigits(minFrac);
        nf.setMaximumFractionDigits(maxFrac);
        return nf;
    }

    /** Format tiền VND: fmt.v(amount) -> "12.345 ₫" */
    public String v(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return currency().format(amount);
    }

    /** Overload cho long/int nếu cần */
    public String v(long amount) {
        return currency().format(amount);
    }

    /** Format số thập phân tuỳ ý: fmt.d(value, minFrac, maxFrac) */
    public String d(Number value, int minFrac, int maxFrac) {
        if (value == null) value = 0;
        return number(minFrac, maxFrac).format(value);
    }
}

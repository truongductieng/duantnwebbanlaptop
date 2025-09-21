package com.bigkhoa.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DayDetailResponse(LocalDate date,
                                BigDecimal revenue,
                                List<OrderDetailDTO> orders) {}

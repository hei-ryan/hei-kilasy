package hei.school;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

@Component
public class GradeCalculator {

    private static final BigDecimal TWENTY = new BigDecimal("20");
    private static final BigDecimal DAILY_PENALTY = new BigDecimal("0.10");
    private static final BigDecimal MAX_PENALTY = new BigDecimal("0.50");

    public BigDecimal calculate(BigDecimal rawPoints, BigDecimal maxPoints, LocalDate deadline,
            LocalDateTime submittedAt) {
        BigDecimal rawGrade = rawPoints.multiply(TWENTY).divide(maxPoints, MathContext.DECIMAL128);
        BigDecimal penalty = DAILY_PENALTY.multiply(BigDecimal.valueOf(daysLate(deadline, submittedAt)))
                .min(MAX_PENALTY);
        return rawGrade.multiply(BigDecimal.ONE.subtract(penalty))
                .max(BigDecimal.ZERO)
                .setScale(1, RoundingMode.HALF_UP);
    }

    public long daysLate(LocalDate deadline, LocalDateTime submittedAt) {
        LocalDate submittedDate = submittedAt.toLocalDate();
        return submittedDate.isAfter(deadline) ? ChronoUnit.DAYS.between(deadline, submittedDate) : 0;
    }
}

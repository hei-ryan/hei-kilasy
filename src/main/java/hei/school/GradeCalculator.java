package hei.school;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

@Component
public class GradeCalculator {

    public static final ZoneId ZONE = ZoneId.of("Indian/Antananarivo");

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal TWENTY = new BigDecimal("20");
    private static final BigDecimal PENALTY_PER_DAY = new BigDecimal("0.10");
    private static final BigDecimal MAX_PENALTY = new BigDecimal("0.50");

    public LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public int lateDays(LocalDateTime submittedAt, LocalDate deadline) {
        long days = ChronoUnit.DAYS.between(deadline, submittedAt.toLocalDate());
        return days > 0 ? (int) days : 0;
    }

    public BigDecimal rawGrade(BigDecimal rawPoints, BigDecimal maxPoints) {
        return rawPoints.multiply(TWENTY).divide(maxPoints, PRECISION).setScale(1, RoundingMode.HALF_UP);
    }

    public BigDecimal penalizedGrade(BigDecimal rawPoints, BigDecimal maxPoints, LocalDateTime submittedAt,
            LocalDate deadline) {
        BigDecimal grade = rawPoints.multiply(TWENTY).divide(maxPoints, PRECISION);
        BigDecimal penalty = PENALTY_PER_DAY.multiply(BigDecimal.valueOf(lateDays(submittedAt, deadline)))
                .min(MAX_PENALTY);
        BigDecimal result = grade.multiply(BigDecimal.ONE.subtract(penalty));
        return result.signum() < 0 ? BigDecimal.ZERO.setScale(1) : result.setScale(1, RoundingMode.HALF_UP);
    }
}

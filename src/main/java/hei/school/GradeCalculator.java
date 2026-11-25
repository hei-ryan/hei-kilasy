package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class GradeCalculator {

    private static final BigDecimal OUT_OF = new BigDecimal("20");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal PENALTY_PER_STARTED_DAY = new BigDecimal("10");
    private static final BigDecimal MAX_PENALTY_PERCENT = new BigDecimal("50");

    public boolean isLate(LocalDate deadline, LocalDateTime submittedAt) {
        return submittedAt.toLocalDate().isAfter(deadline);
    }

    public long startedLateDays(LocalDate deadline, LocalDateTime submittedAt) {
        if (!isLate(deadline, submittedAt)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(deadline, submittedAt.toLocalDate());
    }

    public BigDecimal penaltyPercent(long startedLateDays) {
        if (startedLateDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal penalty = BigDecimal.valueOf(startedLateDays).multiply(PENALTY_PER_STARTED_DAY);
        return penalty.min(MAX_PENALTY_PERCENT);
    }

    public BigDecimal baseGrade(BigDecimal rawPoints, BigDecimal maxPoints) {
        return convert(rawPoints, maxPoints, BigDecimal.ONE);
    }

    public BigDecimal penalizedGrade(
            BigDecimal rawPoints, BigDecimal maxPoints, LocalDate deadline, LocalDateTime submittedAt) {
        BigDecimal percent = penaltyPercent(startedLateDays(deadline, submittedAt));
        BigDecimal factor = HUNDRED.subtract(percent).divide(HUNDRED);
        BigDecimal grade = convert(rawPoints, maxPoints, factor);
        return grade.signum() < 0 ? BigDecimal.ZERO.setScale(1) : grade;
    }

    private BigDecimal convert(BigDecimal rawPoints, BigDecimal maxPoints, BigDecimal factor) {
        if (maxPoints == null || maxPoints.signum() <= 0) {
            throw new IllegalArgumentException("Le barème doit être positif");
        }
        BigDecimal raw = rawPoints == null ? BigDecimal.ZERO : rawPoints;
        return raw.multiply(OUT_OF).multiply(factor).divide(maxPoints, 1, RoundingMode.HALF_UP);
    }
}

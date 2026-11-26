package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public final class GradeCalculator {

    private static final BigDecimal TWENTY = new BigDecimal("20");
    private static final BigDecimal PENALTY_PER_DAY = new BigDecimal("0.10");
    private static final int MAX_PENALTY_DAYS = 5;

    private GradeCalculator() {
    }

    public static long lateDays(LocalDate deadline, LocalDateTime submittedAt) {
        Objects.requireNonNull(deadline, "deadline");
        Objects.requireNonNull(submittedAt, "submittedAt");

        LocalDateTime cutoff = deadline.plusDays(1).atStartOfDay();
        if (submittedAt.isBefore(cutoff)) {
            return 0;
        }

        Duration delay = Duration.between(cutoff, submittedAt);
        long completeDays = delay.toDays();
        boolean hasPartialDay = !delay.minusDays(completeDays).isZero();
        return Math.max(1, completeDays + (hasPartialDay ? 1 : 0));
    }

    public static BigDecimal calculate(BigDecimal rawPoints, BigDecimal maxPoints, long lateDays) {
        Objects.requireNonNull(rawPoints, "rawPoints");
        Objects.requireNonNull(maxPoints, "maxPoints");
        if (maxPoints.signum() <= 0) {
            throw new IllegalArgumentException("maxPoints must be positive");
        }
        if (rawPoints.signum() < 0) {
            throw new IllegalArgumentException("rawPoints must not be negative");
        }
        if (lateDays < 0) {
            throw new IllegalArgumentException("lateDays must not be negative");
        }

        long penalizedDays = Math.min(lateDays, MAX_PENALTY_DAYS);
        BigDecimal factor = BigDecimal.ONE.subtract(PENALTY_PER_DAY.multiply(BigDecimal.valueOf(penalizedDays)));
        BigDecimal grade = rawPoints.multiply(TWENTY).multiply(factor)
                .divide(maxPoints, 1, RoundingMode.HALF_UP);
        return grade.max(BigDecimal.ZERO);
    }
}

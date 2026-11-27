package hei.school.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Business rules for grading: the only place where a grade is computed.
 * Pure Java: no Spring, no SQL, so it can be tested alone.
 */
public final class GradeRules {

    private static final BigDecimal GRADE_SCALE = new BigDecimal("20");
    private static final BigDecimal PENALTY_PER_DAY = new BigDecimal("0.10");
    private static final BigDecimal MAX_PENALTY = new BigDecimal("0.50");
    private static final long MINUTES_PER_DAY = 24 * 60;

    private GradeRules() {
    }

    /** The deadline day is valid until its end: a submission is late from the next day at 00:00. */
    public static boolean isLate(LocalDate deadline, LocalDateTime submittedAt) {
        return daysLate(deadline, submittedAt) > 0;
    }

    /** Number of started days of delay, 0 when on time. */
    public static long daysLate(LocalDate deadline, LocalDateTime submittedAt) {
        LocalDateTime limit = deadline.plusDays(1).atStartOfDay();
        long minutesLate = Duration.between(limit, submittedAt).toMinutes();
        if (minutesLate <= 0) {
            return 0;
        }
        return (minutesLate + MINUTES_PER_DAY - 1) / MINUTES_PER_DAY;
    }

    /** -10 % per started day of delay, capped at -50 %. */
    public static BigDecimal penaltyRate(long daysLate) {
        return PENALTY_PER_DAY.multiply(BigDecimal.valueOf(daysLate)).min(MAX_PENALTY);
    }

    /** Grade out of 20 after the late penalty, rounded once (half up) to one decimal. */
    public static BigDecimal penalizedGrade(BigDecimal rawPoints, BigDecimal maxPoints,
                                            LocalDate deadline, LocalDateTime submittedAt) {
        BigDecimal kept = BigDecimal.ONE.subtract(penaltyRate(daysLate(deadline, submittedAt)));
        return rawPoints.multiply(GRADE_SCALE).multiply(kept).divide(maxPoints, 1, RoundingMode.HALF_UP);
    }
}

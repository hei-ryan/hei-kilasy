package hei.school.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class GradeRulesTest {

    private static final LocalDate DEADLINE = LocalDate.of(2026, 11, 10);
    private static final BigDecimal SIXTEEN = new BigDecimal("16");
    private static final BigDecimal TWENTY = new BigDecimal("20");

    private static BigDecimal grade(BigDecimal raw, BigDecimal max, LocalDateTime submittedAt) {
        return GradeRules.penalizedGrade(raw, max, DEADLINE, submittedAt);
    }

    @Test
    void onTimeIsNotPenalized() {
        assertEquals(new BigDecimal("16.0"), grade(SIXTEEN, TWENTY, LocalDateTime.of(2026, 11, 10, 23, 59)));
        assertFalse(GradeRules.isLate(DEADLINE, LocalDateTime.of(2026, 11, 10, 23, 59)));
    }

    @Test
    void twoHoursLateIsOneStartedDay() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 11, 11, 2, 0);
        assertTrue(GradeRules.isLate(DEADLINE, submittedAt));
        assertEquals(new BigDecimal("14.4"), grade(SIXTEEN, TWENTY, submittedAt));
    }

    @Test
    void threeDaysLate() {
        assertEquals(new BigDecimal("11.2"), grade(SIXTEEN, TWENTY, LocalDateTime.of(2026, 11, 13, 10, 0)));
    }

    @Test
    void penaltyIsCappedAtFiftyPercent() {
        assertEquals(new BigDecimal("8.0"), grade(SIXTEEN, TWENTY, LocalDateTime.of(2026, 11, 18, 15, 0)));
        assertEquals(new BigDecimal("8.0"), grade(SIXTEEN, TWENTY, LocalDateTime.of(2027, 3, 1, 0, 0)));
    }

    @Test
    void gradeIsConvertedToTwentyThenRoundedOnce() {
        assertEquals(new BigDecimal("13.8"), grade(new BigDecimal("27.5"), new BigDecimal("40"),
                LocalDateTime.of(2026, 11, 10, 8, 0)));
        assertEquals(new BigDecimal("13.9"), grade(new BigDecimal("7.7"), new BigDecimal("10"),
                LocalDateTime.of(2026, 11, 11, 8, 40)));
    }

    @Test
    void gradeIsNeverNegative() {
        assertEquals(new BigDecimal("0.0"), grade(BigDecimal.ZERO, TWENTY, LocalDateTime.of(2027, 3, 1, 0, 0)));
    }
}

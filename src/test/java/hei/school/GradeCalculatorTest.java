package hei.school;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class GradeCalculatorTest {

    private final GradeCalculator calculator = new GradeCalculator();
    private final LocalDate deadline = LocalDate.of(2026, 11, 10);

    @Test
    void doesNotPenalizeSubmissionsOnOrBeforeDeadline() {
        assertGrade("16.0", "16", deadline.atTime(23, 59));
        assertGrade("16.0", "16", deadline.minusDays(1).atTime(12, 0));
    }

    @Test
    void chargesOneFullDayForSubmissionShortlyAfterDeadlineDate() {
        assertGrade("14.4", "16", deadline.plusDays(1).atTime(2, 0));
    }

    @Test
    void appliesTenPercentPerStartedCalendarDay() {
        assertGrade("12.8", "16", deadline.plusDays(2).atTime(0, 1));
        assertGrade("11.2", "16", deadline.plusDays(3).atTime(18, 30));
    }

    @Test
    void capsPenaltyAtFiftyPercent() {
        assertGrade("8.0", "16", deadline.plusDays(5).atTime(0, 1));
        assertGrade("8.0", "16", deadline.plusDays(8).atTime(23, 59));
    }

    @Test
    void neverReturnsANegativeGrade() {
        assertGrade("0.0", "0", deadline.plusDays(8).atTime(0, 1));
    }

    private void assertGrade(String expected, String rawPoints, LocalDateTime submittedAt) {
        BigDecimal grade = calculator.calculate(
                new BigDecimal(rawPoints),
                new BigDecimal("20"),
                deadline,
                submittedAt);
        assertEquals(new BigDecimal(expected), grade);
    }
}

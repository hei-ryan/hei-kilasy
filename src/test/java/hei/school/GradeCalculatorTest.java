package hei.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GradeCalculatorTest {

    private final GradeCalculator calculator = new GradeCalculator();

    private static final LocalDate DEADLINE = LocalDate.of(2026, 1, 10);

    private BigDecimal grade(String rawPoints, String maxPoints, LocalDateTime submittedAt) {
        return calculator.penalizedGrade(
                new BigDecimal(rawPoints), new BigDecimal(maxPoints), DEADLINE, submittedAt);
    }

    @ParameterizedTest(name = "rendu le {0} => {1}/20")
    @CsvSource({
            "2026-01-10T23:59:00, 16.0",
            "2026-01-11T02:00:00, 14.4",
            "2026-01-13T12:00:00, 11.2",
            "2026-01-18T09:00:00, 8.0",
            "2026-03-01T09:00:00, 8.0"
    })
    void examplesFromTheClient(String submittedAt, String expected) {
        assertEquals(new BigDecimal(expected), grade("16", "20", LocalDateTime.parse(submittedAt)));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-01-10T23:59:00, 0",
            "2026-01-11T02:00:00, 1",
            "2026-01-13T12:00:00, 3",
            "2026-01-18T09:00:00, 8"
    })
    void lateDaysAreCountedInStartedCalendarDays(String submittedAt, long expected) {
        LocalDateTime time = LocalDateTime.parse(submittedAt);
        assertEquals(expected, calculator.startedLateDays(DEADLINE, time));
        assertEquals(expected > 0, calculator.isLate(DEADLINE, time));
    }

    @Test
    void penaltyIsCappedAtFiftyPercent() {
        assertEquals(new BigDecimal("0"), calculator.penaltyPercent(0));
        assertEquals(new BigDecimal("10"), calculator.penaltyPercent(1));
        assertEquals(new BigDecimal("50"), calculator.penaltyPercent(5));
        assertEquals(new BigDecimal("50"), calculator.penaltyPercent(8));
        assertEquals(new BigDecimal("50"), calculator.penaltyPercent(200));
    }

    @Test
    void otherScalesAreSupported() {
        assertEquals(new BigDecimal("12.8"), grade("32", "40", LocalDateTime.of(2026, 1, 12, 8, 0)));
        assertEquals(new BigDecimal("14.9"), grade("33", "40", LocalDateTime.of(2026, 1, 11, 8, 0)));
    }

    @Test
    void gradeIsNeverNegative() {
        assertEquals(new BigDecimal("0.0"), grade("0", "20", LocalDateTime.of(2026, 2, 1, 8, 0)));
        assertEquals(new BigDecimal("0.0"), grade("0.01", "20", LocalDateTime.of(2026, 3, 1, 8, 0)));
    }

    @Test
    void onTimeSubmissionUsesTheSameGradeAsTheOriginalConversion() {
        LocalDateTime onTime = LocalDateTime.of(2026, 1, 10, 18, 30);
        assertFalse(calculator.isLate(DEADLINE, onTime));
        assertEquals(new BigDecimal("16.0"), calculator.baseGrade(new BigDecimal("16"), new BigDecimal("20")));
        assertEquals(new BigDecimal("16.0"), grade("16", "20", onTime));
        assertTrue(calculator.isLate(DEADLINE, LocalDateTime.of(2026, 1, 11, 0, 1)));
    }
}

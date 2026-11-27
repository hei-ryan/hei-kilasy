package hei.school.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class SubmissionWindowTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 27, 9, 0);

    @Test
    void timedExerciseWithoutDeadlineIsNeverLate() {
        SubmissionWindow window = new SubmissionWindow(null, 30);
        assertFalse(window.isLate(LocalDateTime.of(2030, 1, 1, 0, 0)));
        assertEquals(new BigDecimal("16.0"), GradeRules.penalizedGrade(new BigDecimal("16"), new BigDecimal("20"),
                window, LocalDateTime.of(2030, 1, 1, 0, 0)));
    }

    @Test
    void timeIsOverAfterTheDuration() {
        SubmissionWindow window = new SubmissionWindow(null, 30);
        assertFalse(window.isTimeOver(START, START.plusMinutes(30)));
        assertTrue(window.isTimeOver(START, START.plusMinutes(31)));
        assertEquals(START.plusMinutes(30), window.endsAt(START));
    }

    @Test
    void exerciseWithoutDurationIsNeverTimeOver() {
        assertFalse(new SubmissionWindow(LocalDate.of(2026, 11, 28), null).isTimeOver(START, START.plusYears(1)));
    }

    @Test
    void latePenaltyStillAppliesToTheDeadlineOfATimedExercise() {
        SubmissionWindow window = new SubmissionWindow(LocalDate.of(2026, 11, 10), 60);
        assertEquals(new BigDecimal("11.2"), GradeRules.penalizedGrade(new BigDecimal("16"), new BigDecimal("20"),
                window, LocalDateTime.of(2026, 11, 13, 10, 0)));
    }

    @Test
    void windowNeedsADeadlineOrADuration() {
        assertThrows(IllegalArgumentException.class, () -> new SubmissionWindow(null, null));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionWindow(null, 0));
    }
}

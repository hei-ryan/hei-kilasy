package hei.school.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * When an exercise can be submitted: an optional deadline (a day, valid until its end),
 * an optional time limit in minutes counted from the moment the student starts, or both.
 * All the rules about time live here, instead of being spread over every class reading a date.
 */
public record SubmissionWindow(LocalDate deadline, Integer durationMinutes) {

    private static final long MINUTES_PER_DAY = 24 * 60;

    public SubmissionWindow {
        if (deadline == null && durationMinutes == null) {
            throw new IllegalArgumentException("A deadline, a duration, or both are required");
        }
        if (durationMinutes != null && durationMinutes <= 0) {
            throw new IllegalArgumentException("The duration must be positive");
        }
    }

    public boolean hasDeadline() {
        return deadline != null;
    }

    public boolean isTimed() {
        return durationMinutes != null;
    }

    /** Number of started days after the end of the deadline day, 0 when on time or without deadline. */
    public long daysLate(LocalDateTime submittedAt) {
        if (deadline == null) {
            return 0;
        }
        long minutesLate = Duration.between(deadline.plusDays(1).atStartOfDay(), submittedAt).toMinutes();
        if (minutesLate <= 0) {
            return 0;
        }
        return (minutesLate + MINUTES_PER_DAY - 1) / MINUTES_PER_DAY;
    }

    public boolean isLate(LocalDateTime submittedAt) {
        return daysLate(submittedAt) > 0;
    }

    /** True when the time limit is over (always false for an exercise without duration). */
    public boolean isTimeOver(LocalDateTime startedAt, LocalDateTime now) {
        return durationMinutes != null && now.isAfter(startedAt.plusMinutes(durationMinutes));
    }

    public LocalDateTime endsAt(LocalDateTime startedAt) {
        return startedAt.plusMinutes(durationMinutes);
    }
}

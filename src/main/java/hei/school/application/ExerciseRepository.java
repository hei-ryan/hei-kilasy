package hei.school.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import hei.school.Exercise;

/** Port: how the application reads and writes exercises and their start times. */
public interface ExerciseRepository {

    List<Exercise> findAll();

    Optional<Exercise> findById(long id);

    long create(long teacherId, String title, String description, double maxPoints,
                LocalDate deadline, Integer durationMinutes);

    Optional<LocalDateTime> findStart(long exerciseId, long studentId);

    void start(long exerciseId, long studentId, LocalDateTime startedAt);
}

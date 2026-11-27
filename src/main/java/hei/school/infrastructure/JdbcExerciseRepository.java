package hei.school.infrastructure;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import hei.school.Exercise;
import hei.school.application.ExerciseRepository;
import hei.school.domain.SubmissionWindow;

/** Adapter: PostgreSQL through JDBC. The only place that maps an exercise row. */
@Repository
public class JdbcExerciseRepository implements ExerciseRepository {

    private static final RowMapper<Exercise> MAPPER = (rs, i) -> {
        Exercise e = new Exercise();
        e.setId(rs.getLong("id"));
        e.setTeacherId(rs.getLong("teacher_id"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setMaxPoints(rs.getDouble("max_points"));
        Date deadline = rs.getDate("deadline");
        e.setDeadline(deadline == null ? null : deadline.toLocalDate());
        int duration = rs.getInt("duration_minutes");
        e.setDurationMinutes(rs.wasNull() ? null : duration);
        return e;
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcExerciseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Builds the submission window from a row of a query that selects deadline and duration_minutes. */
    public static SubmissionWindow windowOf(Map<String, Object> row) {
        Date deadline = (Date) row.get("deadline");
        return new SubmissionWindow(deadline == null ? null : deadline.toLocalDate(), (Integer) row.get("duration_minutes"));
    }

    @Override
    public List<Exercise> findAll() {
        return jdbcTemplate.query("SELECT * FROM exercise ORDER BY deadline", MAPPER);
    }

    @Override
    public Optional<Exercise> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM exercise WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public long create(long teacherId, String title, String description, double maxPoints,
                       LocalDate deadline, Integer durationMinutes) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO exercise (teacher_id, title, description, max_points, deadline, duration_minutes) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class, teacherId, title, description, maxPoints,
                deadline == null ? null : Date.valueOf(deadline), durationMinutes);
    }

    @Override
    public Optional<LocalDateTime> findStart(long exerciseId, long studentId) {
        return jdbcTemplate.query("SELECT started_at FROM exercise_start WHERE exercise_id = ? AND student_id = ?",
                (rs, i) -> rs.getTimestamp("started_at").toLocalDateTime(), exerciseId, studentId).stream().findFirst();
    }

    @Override
    public void start(long exerciseId, long studentId, LocalDateTime startedAt) {
        jdbcTemplate.update("INSERT INTO exercise_start (exercise_id, student_id, started_at) VALUES (?, ?, ?)",
                exerciseId, studentId, Timestamp.valueOf(startedAt));
    }
}

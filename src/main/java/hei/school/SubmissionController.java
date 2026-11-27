package hei.school;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import hei.school.application.ExerciseRepository;
import hei.school.application.GradeNotifications;
import hei.school.application.GradePublished;
import hei.school.domain.GradeRules;
import hei.school.domain.SubmissionWindow;
import hei.school.infrastructure.JdbcExerciseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private GradeNotifications gradeNotifications;

    @PostMapping("/exercises/{id}/submissions")
    public ResponseEntity<?> submit(@PathVariable long id, @RequestBody Map<String, Object> body) {
        if (body.get("studentId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "studentId est obligatoire"));
        }
        long studentId = ((Number) body.get("studentId")).longValue();
        String content = (String) body.get("content");
        String repoUrl = (String) body.get("repoUrl");
        if ((content == null || content.isBlank()) && (repoUrl == null || repoUrl.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il faut du code ou un lien vers un dépôt GitHub"));
        }
        if (repoUrl != null && !repoUrl.isBlank() && !repoUrl.startsWith("https://github.com/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le lien doit pointer vers GitHub"));
        }

        Optional<Exercise> exercise = exerciseRepository.findById(id);
        if (exercise.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Exercice introuvable"));
        }
        List<Map<String, Object>> students = jdbcTemplate.queryForList("SELECT * FROM student WHERE id = ?", studentId);
        if (students.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Étudiant introuvable"));
        }
        Integer already = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM submission WHERE exercise_id = ? AND student_id = ?", Integer.class, id, studentId);
        if (already != null && already > 0) {
            return ResponseEntity.status(409).body(Map.of("error", "Cet étudiant a déjà soumis cet exercice"));
        }

        SubmissionWindow window = exercise.get().window();
        LocalDateTime now = LocalDateTime.now();
        if (window.isTimed()) {
            Optional<LocalDateTime> startedAt = exerciseRepository.findStart(id, studentId);
            if (startedAt.isEmpty()) {
                return ResponseEntity.status(422).body(Map.of("error",
                        "Cet exercice est minuté : démarrez-le avec POST /exercises/" + id + "/start avant de soumettre"));
            }
            if (window.isTimeOver(startedAt.get(), now)) {
                return ResponseEntity.status(422).body(Map.of("error",
                        "Temps écoulé : l'exercice durait " + window.durationMinutes() + " minutes et a été démarré à "
                                + startedAt.get()));
            }
        }
        boolean late = window.isLate(now);

        jdbcTemplate.update(
                "INSERT INTO submission (exercise_id, student_id, content, repo_url, submitted_at) VALUES (?, ?, ?, ?, ?)",
                id, studentId, content, repoUrl, Timestamp.valueOf(now));
        Long submissionId = jdbcTemplate.queryForObject("SELECT max(id) FROM submission", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("id", submissionId);
        result.put("submittedAt", now);
        result.put("late", late);
        result.put("message", late ? "Soumission enregistrée, mais en retard" : "Soumission enregistrée");
        return ResponseEntity.status(201).body(result);
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.*, e.title, e.deadline, e.duration_minutes, e.max_points FROM submission s JOIN exercise e ON e.id = s.exercise_id WHERE s.id = ?",
                id);
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Soumission introuvable"));
        }
        Map<String, Object> row = rows.get(0);
        SubmissionWindow window = JdbcExerciseRepository.windowOf(row);
        LocalDateTime submittedAt = ((Timestamp) row.get("submitted_at")).toLocalDateTime();

        Map<String, Object> result = new HashMap<>();
        result.put("id", row.get("id"));
        result.put("exercise", row.get("title"));
        result.put("studentId", row.get("student_id"));
        result.put("content", row.get("content"));
        result.put("repoUrl", row.get("repo_url"));
        result.put("submittedAt", submittedAt);
        result.put("late", window.isLate(submittedAt));
        if (row.get("raw_points") != null) {
            BigDecimal raw = (BigDecimal) row.get("raw_points");
            BigDecimal max = (BigDecimal) row.get("max_points");
            result.put("rawPoints", raw.doubleValue());
            result.put("grade", GradeRules.penalizedGrade(raw, max, window, submittedAt).doubleValue());
            result.put("feedback", row.get("feedback"));
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/submissions/{id}/grade")
    public ResponseEntity<?> grade(@PathVariable long id, @RequestBody Map<String, Object> body) {
        if (body.get("points") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "points est obligatoire"));
        }
        double points = ((Number) body.get("points")).doubleValue();
        String feedback = (String) body.get("feedback");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.*, e.title, e.deadline, e.duration_minutes, e.max_points, st.full_name, st.email, st.phone "
                        + "FROM submission s JOIN exercise e ON e.id = s.exercise_id JOIN student st ON st.id = s.student_id "
                        + "WHERE s.id = ?",
                id);
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Soumission introuvable"));
        }
        Map<String, Object> row = rows.get(0);
        double max = ((BigDecimal) row.get("max_points")).doubleValue();
        if (points < 0 || points > max) {
            return ResponseEntity.badRequest().body(Map.of("error", "Les points doivent être entre 0 et " + max));
        }

        jdbcTemplate.update("UPDATE submission SET raw_points = ?, feedback = ?, graded_at = now() WHERE id = ?",
                points, feedback, id);

        SubmissionWindow window = JdbcExerciseRepository.windowOf(row);
        LocalDateTime submittedAt = ((Timestamp) row.get("submitted_at")).toLocalDateTime();
        boolean late = window.isLate(submittedAt);
        double grade = GradeRules.penalizedGrade(BigDecimal.valueOf(points), (BigDecimal) row.get("max_points"),
                window, submittedAt).doubleValue();

        gradeNotifications.publish(new GradePublished((String) row.get("full_name"), (String) row.get("email"),
                (String) row.get("phone"), (String) row.get("title"), grade, feedback));

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("rawPoints", points);
        result.put("maxPoints", max);
        result.put("grade", grade);
        result.put("late", late);
        result.put("feedback", feedback);
        return ResponseEntity.ok(result);
    }
}

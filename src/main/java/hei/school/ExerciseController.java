package hei.school;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import hei.school.application.ExerciseRepository;

@RestController
public class ExerciseController {

    @Autowired
    private ExerciseRepository exercises;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/exercises")
    public List<Exercise> list() {
        return exercises.findAll();
    }

    @GetMapping("/exercises/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<Exercise> found = exercises.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Exercice introuvable"));
        }
        return ResponseEntity.ok(found.get());
    }

    @PostMapping("/exercises")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le titre est obligatoire"));
        }
        if (body.get("deadline") == null && body.get("durationMinutes") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il faut une échéance (format AAAA-MM-JJ), une durée en minutes, ou les deux"));
        }
        LocalDate deadline = null;
        if (body.get("deadline") != null) {
            try {
                deadline = LocalDate.parse((String) body.get("deadline"));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'échéance doit être au format AAAA-MM-JJ"));
            }
            if (deadline.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'échéance doit être dans le futur"));
            }
        }
        Integer durationMinutes = null;
        if (body.get("durationMinutes") != null) {
            durationMinutes = ((Number) body.get("durationMinutes")).intValue();
            if (durationMinutes <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "La durée doit être positive"));
            }
        }
        double maxPoints = 20;
        if (body.get("maxPoints") != null) {
            maxPoints = ((Number) body.get("maxPoints")).doubleValue();
        }
        if (maxPoints <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le barème doit être positif"));
        }
        // TODO: get the logged-in teacher once authentication exists
        long teacherId = 1;
        if (body.get("teacherId") != null) {
            teacherId = ((Number) body.get("teacherId")).longValue();
        }

        long id = exercises.create(teacherId, title, (String) body.get("description"), maxPoints, deadline, durationMinutes);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("maxPoints", maxPoints);
        result.put("deadline", deadline);
        result.put("durationMinutes", durationMinutes);
        System.out.println("Nouvel exercice créé : " + title + " (id " + id + ")");
        return ResponseEntity.status(201).body(result);
    }

    @PostMapping("/exercises/{id}/start")
    public ResponseEntity<?> start(@PathVariable long id, @RequestBody Map<String, Object> body) {
        if (body.get("studentId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "studentId est obligatoire"));
        }
        long studentId = ((Number) body.get("studentId")).longValue();
        Optional<Exercise> found = exercises.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Exercice introuvable"));
        }
        if (!found.get().window().isTimed()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cet exercice n'est pas minuté"));
        }
        if (jdbcTemplate.queryForList("SELECT * FROM student WHERE id = ?", studentId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Étudiant introuvable"));
        }
        Optional<LocalDateTime> alreadyStarted = exercises.findStart(id, studentId);
        LocalDateTime startedAt = alreadyStarted.orElseGet(LocalDateTime::now);
        if (alreadyStarted.isEmpty()) {
            exercises.start(id, studentId, startedAt);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("exerciseId", id);
        result.put("studentId", studentId);
        result.put("startedAt", startedAt);
        result.put("durationMinutes", found.get().getDurationMinutes());
        result.put("endsAt", found.get().window().endsAt(startedAt));
        return ResponseEntity.status(alreadyStarted.isEmpty() ? 201 : 200).body(result);
    }
}

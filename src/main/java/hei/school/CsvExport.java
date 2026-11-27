package hei.school;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import hei.school.domain.GradeRules;
import hei.school.domain.SubmissionWindow;
import hei.school.infrastructure.JdbcExerciseRepository;

/**
 * Grades export for the registrar's office (opened in Excel, semicolon separator).
 */
@RestController
public class CsvExport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/exports/grades.csv")
    public ResponseEntity<String> export() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT st.reference, st.full_name, st.email, st.cohort, e.title, e.deadline, e.duration_minutes, s.submitted_at, s.raw_points, e.max_points "
                        + "FROM submission s JOIN student st ON st.id = s.student_id JOIN exercise e ON e.id = s.exercise_id "
                        + "ORDER BY st.full_name, e.deadline");

        StringBuilder csv = new StringBuilder();
        csv.append("reference;etudiant;email;promotion;exercice;echeance;soumis_le;en_retard;note_sur_20\n");
        for (Map<String, Object> row : rows) {
            SubmissionWindow window = JdbcExerciseRepository.windowOf(row);
            LocalDateTime submittedAt = ((Timestamp) row.get("submitted_at")).toLocalDateTime();
            String note = "";
            if (row.get("raw_points") != null) {
                note = GradeRules.penalizedGrade((BigDecimal) row.get("raw_points"), (BigDecimal) row.get("max_points"),
                        window, submittedAt).toPlainString().replace('.', ',');
            }
            csv.append(row.get("reference")).append(';')
                    .append(row.get("full_name")).append(';')
                    .append(row.get("email")).append(';')
                    .append(row.get("cohort")).append(';')
                    .append(row.get("title")).append(';')
                    .append(window.hasDeadline() ? window.deadline() : "").append(';')
                    .append(row.get("submitted_at")).append(';')
                    .append(window.isLate(submittedAt) ? "oui" : "non").append(';')
                    .append(note).append('\n');
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"notes-kilasy.csv\"")
                .body(csv.toString());
    }
}

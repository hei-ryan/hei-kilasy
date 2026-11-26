package hei.school;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsvExport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GradeCalculator gradeCalculator;

    @GetMapping("/exports/grades.csv")
    public ResponseEntity<String> export() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT st.reference, st.full_name, st.email, st.cohort, e.title, e.deadline, s.submitted_at, s.raw_points, e.max_points "
                        + "FROM submission s JOIN student st ON st.id = s.student_id JOIN exercise e ON e.id = s.exercise_id "
                        + "ORDER BY st.full_name, e.deadline");

        StringBuilder csv = new StringBuilder();
        csv.append("reference;etudiant;email;promotion;exercice;echeance;soumis_le;en_retard;note_sur_20;note_brute_sur_20;jours_de_retard\n");
        for (Map<String, Object> row : rows) {
            LocalDate deadline = ((Date) row.get("deadline")).toLocalDate();
            LocalDateTime submittedAt = ((Timestamp) row.get("submitted_at")).toLocalDateTime();
            int daysLate = gradeCalculator.lateDays(submittedAt, deadline);
            String note = "";
            String rawNote = "";
            if (row.get("raw_points") != null) {
                BigDecimal raw = (BigDecimal) row.get("raw_points");
                BigDecimal max = (BigDecimal) row.get("max_points");
                note = format(gradeCalculator.penalizedGrade(raw, max, submittedAt, deadline));
                rawNote = format(gradeCalculator.rawGrade(raw, max));
            }
            csv.append(row.get("reference")).append(';')
                    .append(row.get("full_name")).append(';')
                    .append(row.get("email")).append(';')
                    .append(row.get("cohort")).append(';')
                    .append(row.get("title")).append(';')
                    .append(row.get("deadline")).append(';')
                    .append(row.get("submitted_at")).append(';')
                    .append(daysLate > 0 ? "oui" : "non").append(';')
                    .append(note).append(';')
                    .append(rawNote).append(';')
                    .append(daysLate).append('\n');
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"notes-kilasy.csv\"")
                .body(csv.toString());
    }

    private String format(BigDecimal value) {
        return value.toPlainString().replace('.', ',');
    }
}

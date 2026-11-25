package hei.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class GradeConsistencyTest {

    private static final LocalDate DEADLINE = LocalDate.of(2026, 1, 10);
    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 1, 11, 2, 0); // 1 jour entamé
    private static final BigDecimal RAW = new BigDecimal("16.00");
    private static final BigDecimal MAX = new BigDecimal("20.00");
    private static final BigDecimal EXPECTED = new BigDecimal("14.4");

    private final List<SimpleMailMessage> sentMails = new ArrayList<>();

    private SubmissionController submissionController;
    private GradeReport gradeReport;
    private CsvExport csvExport;

    @BeforeEach
    void setUp() {
        GradeCalculator calculator = new GradeCalculator();
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate();

        submissionController = new SubmissionController();
        ReflectionTestUtils.setField(submissionController, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(submissionController, "mailSender", capturingMailSender());
        ReflectionTestUtils.setField(submissionController, "gradeCalculator", calculator);

        gradeReport = new GradeReport();
        ReflectionTestUtils.setField(gradeReport, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(gradeReport, "gradeCalculator", calculator);

        csvExport = new CsvExport();
        ReflectionTestUtils.setField(csvExport, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(csvExport, "gradeCalculator", calculator);
    }

    @SuppressWarnings("unchecked")
    @Test
    void samePenalizedGradeInCorrectionTranscriptCsvAndEmail() {
        ResponseEntity<?> correctionResponse = submissionController.get(7L);
        Map<String, Object> correction = (Map<String, Object>) correctionResponse.getBody();

        Map<String, Object> transcript = gradeReport.build(3L);
        List<Map<String, Object>> lines = (List<Map<String, Object>>) transcript.get("exercises");

        ResponseEntity<String> csvResponse = csvExport.export();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("points", 16);
        body.put("feedback", "Bravo");
        Map<String, Object> grading = (Map<String, Object>) submissionController.grade(7L, body).getBody();

        assertEquals(EXPECTED, correction.get("grade"), "correction");
        assertEquals(EXPECTED, lines.get(0).get("grade"), "relevé de notes");
        assertTrue(csvResponse.getBody().contains(";14,4\n"), "export CSV : " + csvResponse.getBody());
        assertEquals(EXPECTED, grading.get("grade"), "réponse de notation");
        assertEquals(1L, grading.get("daysLate"));
        assertEquals(new BigDecimal("10"), grading.get("penaltyPercent"));

        assertEquals(1, sentMails.size());
        String mail = sentMails.get(0).getText();
        assertTrue(mail.contains("14.4/20"), "email : " + mail);
        assertTrue(mail.contains("10 %"), "email : " + mail);
    }

    private JavaMailSender capturingMailSender() {
        return (JavaMailSender) Proxy.newProxyInstance(
                JavaMailSender.class.getClassLoader(),
                new Class<?>[] {JavaMailSender.class},
                (proxy, method, args) -> {
                    if ("send".equals(method.getName()) && args != null) {
                        for (Object arg : args) {
                            if (arg instanceof SimpleMailMessage) {
                                sentMails.add((SimpleMailMessage) arg);
                            } else if (arg instanceof SimpleMailMessage[]) {
                                sentMails.addAll(Arrays.asList((SimpleMailMessage[]) arg));
                            }
                        }
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    return null;
                });
    }

    private static Map<String, Object> baseRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 7L);
        row.put("title", "Tri à bulles");
        row.put("student_id", 3L);
        row.put("content", "public class Tri {}");
        row.put("repo_url", null);
        row.put("submitted_at", Timestamp.valueOf(SUBMITTED));
        row.put("raw_points", RAW);
        row.put("feedback", "Bravo");
        row.put("deadline", Date.valueOf(DEADLINE));
        row.put("max_points", MAX);
        return row;
    }

    private static Map<String, Object> studentRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("reference", "ETU-003");
        row.put("full_name", "Aina Rakoto");
        row.put("email", "aina@example.test");
        row.put("cohort", "L3-INFO");
        return row;
    }

    private static List<Map<String, Object>> rowsFor(String sql) {
        if (sql.contains("SELECT * FROM student WHERE id")) {
            return List.of(studentRow());
        }
        if (sql.contains("LEFT JOIN submission s ON s.exercise_id")) {
            Map<String, Object> row = baseRow();
            row.put("submission_id", 7L);
            return List.of(row);
        }
        if (sql.contains("ORDER BY st.full_name, e.deadline")) {
            Map<String, Object> row = baseRow();
            row.putAll(studentRow());
            return List.of(row);
        }
        if (sql.contains("st.full_name")) {
            Map<String, Object> row = baseRow();
            row.putAll(studentRow());
            return List.of(row);
        }
        if (sql.contains("JOIN exercise e ON e.id = s.exercise_id")) {
            return List.of(baseRow());
        }
        assertFalse(true, "Requête SQL non prévue dans le faux JdbcTemplate : " + sql);
        return List.of();
    }

    private static class FakeJdbcTemplate extends JdbcTemplate {

        @Override
        public List<Map<String, Object>> queryForList(String sql) {
            return rowsFor(sql);
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            return rowsFor(sql);
        }

        @Override
        public int update(String sql, Object... args) {
            return 1;
        }
    }
}

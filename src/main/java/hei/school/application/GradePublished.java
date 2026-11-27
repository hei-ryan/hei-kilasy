package hei.school.application;

/** What a student must be told when a grade is published. */
public record GradePublished(String studentName, String email, String phone,
                             String exerciseTitle, double grade, String feedback) {
}

package hei.school.infrastructure;

import org.springframework.core.annotation.Order;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import hei.school.application.GradeNotifier;
import hei.school.application.GradePublished;

/** Adapter: sends the grade by email (unchanged behavior, including the failure of the request). */
@Component
@Order(1)
public class EmailGradeNotifier implements GradeNotifier {

    private final JavaMailSender mailSender;

    public EmailGradeNotifier(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(GradePublished event) {
        String firstName = event.studentName().split(" ")[0];
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("notes@kilasy.test");
        message.setTo(event.email());
        message.setSubject("Kilasy : nouvelle note pour " + event.exerciseTitle());
        String text = "Bonjour " + firstName + ",\n\n"
                + "Ta note pour « " + event.exerciseTitle() + " » est disponible : " + event.grade() + "/20.\n";
        if (event.feedback() != null && !event.feedback().isBlank()) {
            text += "\nCommentaire de l'enseignant : " + event.feedback() + "\n";
        }
        text += "\nL'équipe Kilasy";
        message.setText(text);
        mailSender.send(message);
        System.out.println("Email de note envoyé à " + event.email());
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}

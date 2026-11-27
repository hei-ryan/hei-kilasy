package hei.school.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import hei.school.application.GradeNotifier;
import hei.school.application.GradePublished;

/** Adapter: simulated SMS, written in the logs until the provider is chosen. */
@Component
@Order(2)
public class SmsLogGradeNotifier implements GradeNotifier {

    private static final Logger log = LoggerFactory.getLogger(SmsLogGradeNotifier.class);

    @Override
    public void send(GradePublished event) {
        log.info("[SMS] {} : Votre note pour « {} » est disponible : {}/20.",
                event.phone(), event.exerciseTitle(), String.valueOf(event.grade()).replace('.', ','));
    }
}

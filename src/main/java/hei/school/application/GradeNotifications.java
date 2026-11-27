package hei.school.application;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends a published grade through every available notifier.
 * The grade is already saved at this point: a non-critical notifier must never undo or hide it.
 */
@Service
public class GradeNotifications {

    private static final Logger log = LoggerFactory.getLogger(GradeNotifications.class);

    private final List<GradeNotifier> notifiers;

    public GradeNotifications(List<GradeNotifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void publish(GradePublished event) {
        for (GradeNotifier notifier : notifiers) {
            try {
                notifier.send(event);
            } catch (RuntimeException e) {
                if (notifier.isCritical()) {
                    throw e;
                }
                log.warn("Notification {} failed for {}: {}",
                        notifier.getClass().getSimpleName(), event.email(), e.getMessage());
            }
        }
    }
}

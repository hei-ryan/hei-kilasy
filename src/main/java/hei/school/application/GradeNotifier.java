package hei.school.application;

/**
 * Port: a way to tell a student that a grade is published (email, SMS, WhatsApp...).
 * The grading code depends on this interface, never on a provider.
 */
public interface GradeNotifier {

    void send(GradePublished event);

    /** When true, a failure of this notifier is propagated. Otherwise it is logged and ignored. */
    default boolean isCritical() {
        return false;
    }
}

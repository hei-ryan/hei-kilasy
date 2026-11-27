package hei.school.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GradeNotificationsTest {

    private static final GradePublished EVENT =
            new GradePublished("Alice Rakoto", "alice@etu.kilasy.test", "+261 34 00 000 01", "Tri à bulles", 14.4, null);

    private static GradeNotifier failing() {
        return event -> {
            throw new IllegalStateException("provider outage");
        };
    }

    @Test
    void failingNonCriticalNotifierDoesNotBreakGrading() {
        List<GradePublished> sent = new ArrayList<>();
        GradeNotifications notifications = new GradeNotifications(List.of(failing(), sent::add));

        notifications.publish(EVENT);

        assertEquals(List.of(EVENT), sent);
    }

    @Test
    void failingCriticalNotifierIsPropagated() {
        GradeNotifier critical = new GradeNotifier() {
            @Override
            public void send(GradePublished event) {
                throw new IllegalStateException("mail server unavailable");
            }

            @Override
            public boolean isCritical() {
                return true;
            }
        };

        assertThrows(IllegalStateException.class, () -> new GradeNotifications(List.of(critical)).publish(EVENT));
    }
}

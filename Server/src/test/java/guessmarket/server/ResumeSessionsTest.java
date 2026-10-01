package guessmarket.server;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeSessionsTest {
    @Test
    void onlyTheSavedTokenCanResumeAfterTheOriginalSessionEnds() {
        ResumeSessions sessions = new ResumeSessions();
        HttpSession original = session("one");
        HttpSession replacement = session("two");
        String token = sessions.register("Asaf", original);

        assertEquals(43, token.length());
        assertTrue(sessions.isActive(token));
        assertNull(sessions.resume(token, replacement));
        assertNull(sessions.resume("incorrect", replacement));

        sessions.sessionEnded(original);
        assertFalse(sessions.isActive(token));
        assertEquals("Asaf", sessions.resume(token, replacement));
        assertTrue(sessions.isActive(token));

        sessions.sessionEnded(original);
        assertTrue(sessions.isActive(token));
        sessions.sessionEnded(replacement);
        assertFalse(sessions.isActive(token));
    }

    @Test
    void tokensAreUniqueForDifferentUsers() {
        ResumeSessions sessions = new ResumeSessions();
        String first = sessions.register("Asaf", session("one"));
        String second = sessions.register("Trader", session("two"));
        assertFalse(first.equals(second));
        assertTrue(sessions.contains(first));
        assertTrue(sessions.contains(second));
    }

    private static HttpSession session(String id) {
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, arguments) -> {
                    if ("getId".equals(method.getName())) {
                        return id;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}

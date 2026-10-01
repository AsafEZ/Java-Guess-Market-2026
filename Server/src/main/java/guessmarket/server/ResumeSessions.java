package guessmarket.server;

import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

final class ResumeSessions {
    static final String CONTEXT_ATTRIBUTE = ResumeSessions.class.getName();

    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> usersByToken = new HashMap<>();
    private final Map<String, String> activeSessionByUser = new HashMap<>();

    synchronized String register(String userName, HttpSession session) {
        byte[] bytes = new byte[32];
        String token;
        do {
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (usersByToken.containsKey(token));
        usersByToken.put(token, userName);
        activeSessionByUser.put(userName, session.getId());
        return token;
    }

    synchronized String resume(String token, HttpSession session) {
        String userName = usersByToken.get(token);
        if (userName == null || activeSessionByUser.containsKey(userName)) {
            return null;
        }
        activeSessionByUser.put(userName, session.getId());
        return userName;
    }

    synchronized boolean isActive(String token) {
        String userName = usersByToken.get(token);
        return userName != null && activeSessionByUser.containsKey(userName);
    }

    synchronized boolean contains(String token) {
        return usersByToken.containsKey(token);
    }

    synchronized void sessionEnded(HttpSession session) {
        activeSessionByUser.entrySet().removeIf(entry ->
                session.getId().equals(entry.getValue()));
    }
}

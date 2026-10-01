package guessmarket.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.BufferedReader;

public final class ChatServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (HttpApi.sessionUserName(request) == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in to read the chat.");
            return;
        }
        long after;
        try {
            after = Long.parseLong(request.getParameter("after") == null
                    ? "0" : request.getParameter("after"));
            if (after < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_REQUEST", "The after cursor must be a nonnegative integer.");
            return;
        }
        ChatRoom room = HttpApi.chatRoom(getServletContext());
        if (room == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "CHAT_UNAVAILABLE", "The chat is unavailable.");
            return;
        }
        HttpApi.writeJson(response, HttpServletResponse.SC_OK, room.after(after));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String userName = HttpApi.sessionUserName(request);
        if (userName == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in to post a chat message.");
            return;
        }
        char[] buffer = new char[4097];
        int length = 0;
        BufferedReader reader = request.getReader();
        while (length < buffer.length) {
            int read = reader.read(buffer, length, buffer.length - length);
            if (read == -1) {
                break;
            }
            length += read;
        }
        String body = new String(buffer, 0, length);
        if (body.length() > 4096) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_MESSAGE", "A chat message is too long.");
            return;
        }
        String text;
        try {
            JsonElement json = JsonParser.parseString(body);
            if (!json.isJsonObject() || !json.getAsJsonObject().has("text")
                    || !json.getAsJsonObject().get("text").isJsonPrimitive()
                    || !json.getAsJsonObject().get("text").getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException();
            }
            text = json.getAsJsonObject().get("text").getAsString().trim();
        } catch (JsonParseException | IllegalArgumentException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_MESSAGE", "Supply a text message.");
            return;
        }
        if (text.isEmpty() || text.length() > 500) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_MESSAGE", "Enter a message of 1 to 500 characters.");
            return;
        }
        ChatRoom room = HttpApi.chatRoom(getServletContext());
        if (room == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "CHAT_UNAVAILABLE", "The chat is unavailable.");
            return;
        }
        HttpApi.writeJson(response, HttpServletResponse.SC_CREATED,
                room.post(userName, text));
    }
}

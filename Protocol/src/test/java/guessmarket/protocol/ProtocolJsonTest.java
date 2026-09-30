package guessmarket.protocol;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolJsonTest {
    private final Gson gson = new Gson();

    @Test
    void eventListRoundTripsWithoutEngineTypes() {
        List<EventView> events = List.of(new EventView(
                1, "Event", "Description", "NOT_STARTED", "LMSR", 5,
                "ON_PURCHASE", 0.0, "Maker",
                List.of(new OptionView(1, "Yes"), new OptionView(2, "No"))));

        List<EventView> parsed = gson.fromJson(gson.toJson(events),
                new TypeToken<List<EventView>>() {}.getType());

        assertEquals(events, parsed);
    }

    @Test
    void userUploadAndErrorRoundTrip() {
        UserView user = new UserView("Maker", 0.0, "ACTIVE", true);
        UploadView upload = new UploadView(1, 2, List.of("Event"));
        ErrorView error = new ErrorView("DUPLICATE_EVENT_NAME", "Name already exists.");

        assertEquals(user, gson.fromJson(gson.toJson(user), UserView.class));
        assertEquals(upload, gson.fromJson(gson.toJson(upload), UploadView.class));
        assertEquals(error, gson.fromJson(gson.toJson(error), ErrorView.class));
    }

    @Test
    void accountActivityRoundTripsWithoutEngineTypes() {
        AccountActivityView activity = new AccountActivityView(
                1, "2026-09-30T16:00:00Z", "PURCHASE", 2, "Event",
                -10.5, -0.5, 89.5);

        assertEquals(activity,
                gson.fromJson(gson.toJson(activity), AccountActivityView.class));
    }
}

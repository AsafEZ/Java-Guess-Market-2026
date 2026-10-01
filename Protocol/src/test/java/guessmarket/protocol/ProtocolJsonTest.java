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

    @Test
    void eventAndUserDetailsRoundTripWithoutPolymorphicEngineTypes() {
        EventView summary = new EventView(1, "Event", "Description",
                "ACTIVE", "ORDER_BOOK", 5, "ON_PURCHASE", 100.0,
                "Maker", List.of(new OptionView(1, "Yes")));
        OrderView order = new OrderView(3, "Buyer", 1, "BUY", 2, 1,
                0.50, "PARTIALLY_FILLED");
        PositionView position = new PositionView("Buyer", 1, "Event",
                "ACTIVE", "ORDER_BOOK", false,
                List.of(new OptionPositionView(1, "Yes", 1, 0.50, 0.025, false)),
                1, 0.50, 0.025, List.of(), null, null);
        EventDetailsView event = new EventDetailsView(summary, 0.025,
                null, null, null, true, 100, 1,
                List.of(new OptionMarketView(1, "Yes", 1, null, 0.50,
                        0.50, null, null, null, List.of(order), List.of())),
                List.of(), List.of(new ExecutionView(1, 1, "Buyer", null,
                        1, 0.50, 0.50, true)), List.of(position));
        UserDetailsView user = new UserDetailsView("Buyer", 99.475,
                "ACTIVE", List.of(), List.of(position));

        assertEquals(event, gson.fromJson(gson.toJson(event), EventDetailsView.class));
        assertEquals(user, gson.fromJson(gson.toJson(user), UserDetailsView.class));
    }

    @Test
    void actionResultRoundTrips() {
        ActionResultView result = new ActionResultView("open", null, null,
                new AccountActivityView(1, "2026-09-30T16:00:00Z",
                        "OPEN_EVENT", 1, "Event", -100, 0, 900), null);

        assertEquals(result, gson.fromJson(gson.toJson(result), ActionResultView.class));
    }

    @Test
    void chatMessageRoundTripsWithoutUiOrEngineTypes() {
        ChatMessageView message = new ChatMessageView(3, "Alice", "Hello", 1_800_000_000_000L);
        assertEquals(message, gson.fromJson(gson.toJson(message), ChatMessageView.class));
    }
}

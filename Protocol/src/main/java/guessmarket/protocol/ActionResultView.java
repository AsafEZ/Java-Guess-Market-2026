package guessmarket.protocol;

public record ActionResultView(String action, EventDetailsView event,
                               UserDetailsView account, AccountActivityView activity,
                               OrderView submittedOrder) {
}

export function sameData(left, right) {
  return JSON.stringify(left) === JSON.stringify(right);
}

export function filterEvents(events, { search, method, status, commission }) {
  const query = search.trim().toLocaleLowerCase();
  return events.filter((event) =>
    (!query || event.name.toLocaleLowerCase().includes(query))
    && (!method || event.tradingMethod === method)
    && (!status || event.status === status)
    && (!commission || event.commissionType === commission));
}

export function availableActions(event, userName) {
  if (!event) return { open: false, purchase: false, order: false, close: false };
  const maker = event.marketMakerName === userName;
  const active = event.status === 'ACTIVE';
  return {
    open: maker && event.status === 'NOT_STARTED',
    purchase: active && event.tradingMethod === 'LMSR',
    order: active && event.tradingMethod === 'ORDER_BOOK',
    close: maker && active,
  };
}

export function positiveWhole(value) {
  const number = Number(value);
  return Number.isSafeInteger(number) && number > 0 ? number : null;
}

export function positivePrice(value) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : null;
}

export function depositAmount(value) {
  const amount = Number(value);
  return value.trim() && Number.isFinite(amount) && amount > 0 ? amount : null;
}

export function selectedPosition(positions, eventId) {
  return positions.find((position) => position.eventId === eventId) ?? null;
}

export function formatTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('en-GB', {
    dateStyle: 'short', timeStyle: 'short',
  });
}

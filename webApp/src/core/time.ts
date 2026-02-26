export function toRelativeMinutes(timestamp: number): string {
  const diffMs = Math.max(0, Date.now() - timestamp);
  const mins = Math.max(1, Math.floor(diffMs / 60000));
  if (mins < 60) return `${mins} min`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs} h`;
  const days = Math.floor(hrs / 24);
  return `${days} d`;
}

export function withinDays(timestamp: number, days: number): boolean {
  const ms = days * 24 * 60 * 60 * 1000;
  return Date.now() - timestamp <= ms;
}

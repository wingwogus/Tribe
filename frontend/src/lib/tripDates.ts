const formatDateParts = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
};

export const getTripDateStringForDay = (startDate: string, day: number): string => {
  const normalizedStartDate = startDate.slice(0, 10);
  const normalizedDay = Number.isFinite(day) ? Math.max(1, Math.floor(day)) : 1;
  const start = new Date(`${normalizedStartDate}T00:00:00`);
  start.setDate(start.getDate() + normalizedDay - 1);

  return formatDateParts(start);
};

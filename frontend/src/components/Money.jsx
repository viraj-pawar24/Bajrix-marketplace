const formatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
});

export function formatMoney(value) {
  if (value === null || value === undefined) return null;
  return formatter.format(value);
}

const FA_DIGITS = new Intl.NumberFormat('fa-IR');
const EN_DIGITS = new Intl.NumberFormat('en-US');

let usePersianDigits = true;

export function setPersianDigits(value) {
  usePersianDigits = Boolean(value);
}

export function num(value) {
  const safe = Number.isFinite(Number(value)) ? Number(value) : 0;
  return (usePersianDigits ? FA_DIGITS : EN_DIGITS).format(safe);
}

export function money(value, withUnit = true) {
  return `${num(Math.round(Number(value) || 0))}${withUnit ? ' تومان' : ''}`;
}

export function signedMoney(value) {
  const amount = Number(value) || 0;
  return `${amount > 0 ? '+' : amount < 0 ? '−' : ''}${money(Math.abs(amount))}`;
}

export function quantity(value) {
  const amount = Number(value) || 0;
  const rounded = Math.round(amount * 1000) / 1000;
  return num(rounded);
}

const longDate = new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
  day: 'numeric',
  month: 'long',
  year: 'numeric'
});

const shortDate = new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
  day: 'numeric',
  month: 'short'
});

const monthDate = new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
  month: 'long',
  year: 'numeric'
});

const weekdayDate = new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
  weekday: 'long',
  day: 'numeric',
  month: 'long'
});

const timeFormat = new Intl.DateTimeFormat('fa-IR', {
  hour: '2-digit',
  minute: '2-digit'
});

export function dateFa(iso) {
  return longDate.format(new Date(iso));
}

export function dateShort(iso) {
  return shortDate.format(new Date(iso));
}

export function monthFa(iso) {
  return monthDate.format(new Date(iso));
}

export function todayFa() {
  return weekdayDate.format(new Date());
}

export function timeFa(iso) {
  return timeFormat.format(new Date(iso));
}

export function dateTimeFa(iso) {
  return `${dateFa(iso)}، ساعت ${timeFa(iso)}`;
}

export function todayIso() {
  const now = new Date();
  now.setHours(12, 0, 0, 0);
  return now.toISOString();
}

export function daysAgo(days, hour = 11) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  date.setHours(hour, 0, 0, 0);
  return date.toISOString();
}

export function isToday(iso) {
  const date = new Date(iso);
  const now = new Date();
  return date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();
}

export function isSameMonth(iso) {
  const date = new Date(iso);
  const now = new Date();
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
}

export function daysUntil(iso) {
  const target = new Date(iso);
  target.setHours(0, 0, 0, 0);
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return Math.round((target - now) / 86_400_000);
}

export function jalaliParts(iso) {
  const parts = new Intl.DateTimeFormat('en-US-u-ca-persian', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date(iso));
  const get = (type) => parts.find((part) => part.type === type)?.value ?? '';
  return { year: get('year'), month: get('month'), day: get('day') };
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

export function parseAmount(input) {
  const normalized = String(input ?? '')
    .replace(/[۰-۹]/g, (char) => String('۰۱۲۳۴۵۶۷۸۹'.indexOf(char)))
    .replace(/[٠-٩]/g, (char) => String('٠١٢٣٤٥٦٧٨٩'.indexOf(char)))
    .replace(/[٬,،\s]/g, '');
  const value = Number(normalized);
  return Number.isFinite(value) ? Math.max(0, Math.round(value)) : 0;
}

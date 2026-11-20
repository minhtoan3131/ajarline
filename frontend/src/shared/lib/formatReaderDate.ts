const READER_DATE = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'long',
  // Ajarline is a Vietnamese site; dates must not change with the reader's
  // device time zone.
  timeZone: 'Asia/Ho_Chi_Minh',
})

/** Formats an ISO-8601 instant as a long Vietnamese date, in Vietnam time. */
export function formatReaderDate(iso: string): string {
  return READER_DATE.format(new Date(iso))
}

import { defaultSchema, type Options as SanitizeSchema } from 'rehype-sanitize'

/**
 * Allowlist applied after every Markdown transform.
 *
 * The GitHub-style default schema already supports the GFM profile (tables,
 * task lists, footnotes, strikethrough), keeps task list checkboxes disabled
 * and only keeps `language-*` classes on code. URL protocols are narrowed to
 * match the content URL policy, so both layers agree.
 */
export const contentSanitizeSchema: SanitizeSchema = {
  ...defaultSchema,
  // Footnote ids already get the "user-content-" prefix when Markdown becomes
  // HTML. Raw HTML is skipped, so no author-written id reaches this point.
  // Prefixing again would break the footnote links.
  clobber: [],
  protocols: {
    ...defaultSchema.protocols,
    href: ['http', 'https', 'mailto'],
    src: ['https'],
  },
}

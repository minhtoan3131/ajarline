/** Language used when the author declares nothing or an unsupported name. */
export const PLAIN_TEXT = 'text'

// The allowlist of highlighted languages and their visible labels.
const LANGUAGE_LABELS: Readonly<Record<string, string>> = {
  java: 'Java',
  kotlin: 'Kotlin',
  javascript: 'JavaScript',
  typescript: 'TypeScript',
  jsx: 'JSX',
  tsx: 'TSX',
  html: 'HTML',
  css: 'CSS',
  json: 'JSON',
  xml: 'XML',
  yaml: 'YAML',
  sql: 'SQL',
  bash: 'Bash',
  dockerfile: 'Dockerfile',
  properties: 'Properties',
  markdown: 'Markdown',
}

const ALIASES: Readonly<Record<string, string>> = {
  plaintext: PLAIN_TEXT,
  txt: PLAIN_TEXT,
  js: 'javascript',
  ts: 'typescript',
  sh: 'bash',
  shell: 'bash',
  yml: 'yaml',
  md: 'markdown',
  htm: 'html',
}

/**
 * Maps the language written after a code fence to an allowlisted name.
 *
 * The language is never guessed from the code. Missing, unknown or invalid
 * names become plain text.
 */
export function normalizeCodeLanguage(declared: string | null): string {
  if (declared === null) {
    return PLAIN_TEXT
  }
  const key = declared.trim().toLowerCase()
  const name = ALIASES[key] ?? key
  return name === PLAIN_TEXT || name in LANGUAGE_LABELS ? name : PLAIN_TEXT
}

/** Visible label of a highlighted language, or null for plain text. */
export function codeLanguageLabel(language: string): string | null {
  return LANGUAGE_LABELS[language] ?? null
}

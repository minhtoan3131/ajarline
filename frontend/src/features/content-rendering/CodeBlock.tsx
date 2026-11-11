import { Component, useState, type ReactNode } from 'react'

import { codeLanguageLabel, normalizeCodeLanguage, PLAIN_TEXT } from './codeLanguage'
import { PrismHighlighter } from './prismHighlighter'

export interface CodeBlockProps {
  /** Exact code written by the author, without the closing line break. */
  code: string
  /** Language written after the opening fence, or null when it is missing. */
  declaredLanguage: string | null
}

/**
 * Fenced code block with language label, optional highlighting and Copy.
 *
 * Highlighting is progressive enhancement. When the language is unknown or
 * the highlighter fails, the same code is shown as plain text. Highlighting
 * uses CSS classes only, so the page needs no inline style permission.
 */
export function CodeBlock({ code, declaredLanguage }: CodeBlockProps) {
  const language = normalizeCodeLanguage(declaredLanguage)
  const label = codeLanguageLabel(language)
  const plainCode = <PlainCode code={code} />

  return (
    <div className="md-code-block">
      <div className="md-code-toolbar">
        {label !== null && <span className="md-code-language">{label}</span>}
        <CopyCodeButton code={code} />
      </div>
      {language === PLAIN_TEXT ? (
        plainCode
      ) : (
        <HighlightBoundary fallback={plainCode}>
          <PrismHighlighter
            language={language}
            useInlineStyles={false}
            PreTag={ScrollablePre}
            CodeTag={ClassOnlyCode}
          >
            {code}
          </PrismHighlighter>
        </HighlightBoundary>
      )}
    </div>
  )
}

function ScrollablePre({ className, children }: { className?: string; children?: ReactNode }) {
  // Long lines scroll inside the block. tabIndex lets keyboard users scroll.
  // The highlighter also passes a style prop; it is ignored on purpose.
  return (
    <pre className={className} tabIndex={0}>
      {children}
    </pre>
  )
}

function ClassOnlyCode({ className, children }: { className?: string; children?: ReactNode }) {
  // The highlighter adds style="white-space: pre" to <code> even when inline
  // styles are off. Drop it: the stylesheet already sets white-space on <pre>.
  return <code className={className}>{children}</code>
}

function PlainCode({ code }: { code: string }) {
  return (
    <ScrollablePre>
      <code>{code}</code>
    </ScrollablePre>
  )
}

interface HighlightBoundaryProps {
  fallback: ReactNode
  children: ReactNode
}

interface HighlightBoundaryState {
  failed: boolean
}

/** Shows plain code when the highlighter throws, so the page never crashes. */
class HighlightBoundary extends Component<HighlightBoundaryProps, HighlightBoundaryState> {
  state: HighlightBoundaryState = { failed: false }

  static getDerivedStateFromError(): HighlightBoundaryState {
    return { failed: true }
  }

  render() {
    return this.state.failed ? this.props.fallback : this.props.children
  }
}

type CopyState = 'idle' | 'copied' | 'failed'

const COPY_STATUS: Record<CopyState, string> = {
  idle: '',
  copied: 'Đã sao chép mã.',
  failed: 'Không sao chép được mã. Hãy chọn và sao chép thủ công.',
}

function CopyCodeButton({ code }: { code: string }) {
  const [state, setState] = useState<CopyState>('idle')

  async function copy() {
    try {
      // Copy exactly what the author wrote, never the highlighted markup.
      await navigator.clipboard.writeText(code)
      setState('copied')
    } catch {
      // The Clipboard API is missing or the browser denied permission.
      setState('failed')
    }
  }

  return (
    <>
      <button type="button" className="md-code-copy" onClick={() => void copy()}>
        {state === 'copied' ? 'Đã sao chép' : 'Sao chép'}
      </button>
      <span className="md-visually-hidden" role="status">
        {COPY_STATUS[state]}
      </span>
    </>
  )
}

import PrismLight from 'react-syntax-highlighter/dist/esm/prism-light'
import bash from 'react-syntax-highlighter/dist/esm/languages/prism/bash'
import css from 'react-syntax-highlighter/dist/esm/languages/prism/css'
import docker from 'react-syntax-highlighter/dist/esm/languages/prism/docker'
import java from 'react-syntax-highlighter/dist/esm/languages/prism/java'
import javascript from 'react-syntax-highlighter/dist/esm/languages/prism/javascript'
import json from 'react-syntax-highlighter/dist/esm/languages/prism/json'
import jsx from 'react-syntax-highlighter/dist/esm/languages/prism/jsx'
import kotlin from 'react-syntax-highlighter/dist/esm/languages/prism/kotlin'
import markdown from 'react-syntax-highlighter/dist/esm/languages/prism/markdown'
import markup from 'react-syntax-highlighter/dist/esm/languages/prism/markup'
import properties from 'react-syntax-highlighter/dist/esm/languages/prism/properties'
import sql from 'react-syntax-highlighter/dist/esm/languages/prism/sql'
import tsx from 'react-syntax-highlighter/dist/esm/languages/prism/tsx'
import typescript from 'react-syntax-highlighter/dist/esm/languages/prism/typescript'
import yaml from 'react-syntax-highlighter/dist/esm/languages/prism/yaml'

// PrismLight starts with no grammar. Registering only the allowlisted ones
// keeps the full language catalog out of the bundle. The names below must
// match normalizeCodeLanguage.
PrismLight.registerLanguage('bash', bash)
PrismLight.registerLanguage('css', css)
PrismLight.registerLanguage('dockerfile', docker)
PrismLight.registerLanguage('html', markup)
PrismLight.registerLanguage('java', java)
PrismLight.registerLanguage('javascript', javascript)
PrismLight.registerLanguage('json', json)
PrismLight.registerLanguage('jsx', jsx)
PrismLight.registerLanguage('kotlin', kotlin)
PrismLight.registerLanguage('markdown', markdown)
PrismLight.registerLanguage('properties', properties)
PrismLight.registerLanguage('sql', sql)
PrismLight.registerLanguage('tsx', tsx)
PrismLight.registerLanguage('typescript', typescript)
PrismLight.registerLanguage('xml', markup)
PrismLight.registerLanguage('yaml', yaml)

export { PrismLight as PrismHighlighter }

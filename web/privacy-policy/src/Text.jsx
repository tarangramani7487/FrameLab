// Highlights any "[placeholder]" left unfilled and turns emails into links.
export function Text({ children }) {
  const parts = String(children).split(/(\[[^\]]+\]|[\w.+-]+@[\w-]+\.[\w.]+)/g)
  return parts.map((p, i) => {
    if (/^\[[^\]]+\]$/.test(p)) return <mark key={i} className="ph">{p}</mark>
    if (/^[\w.+-]+@[\w-]+\.[\w.]+$/.test(p)) return <a key={i} href={`mailto:${p}`}>{p}</a>
    return p
  })
}

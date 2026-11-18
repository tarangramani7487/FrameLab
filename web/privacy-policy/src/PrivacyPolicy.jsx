import { useEffect, useState } from 'react'
import { developer } from './developer.js'
import { buildPolicy } from './buildPolicy.js'
import { Text } from './Text.jsx'
import { Header, Footer, Icon } from './Chrome.jsx'

export { Text }

function Section({ s, index }) {
  return (
    <section id={s.id} className="policy-section">
      <h2>
        <span className="num">{String(index + 1).padStart(2, '0')}</span>
        {s.title}
      </h2>
      {s.body?.map((t, i) => <p key={i}><Text>{t}</Text></p>)}
      {s.groups && (
        <dl className="groups">
          {s.groups.map((g) => (
            <div key={g.label} className="group">
              <dt>{g.label}</dt>
              <dd><Text>{g.text}</Text></dd>
            </div>
          ))}
        </dl>
      )}
      {s.list && <ul>{s.list.map((t, i) => <li key={i}><Text>{t}</Text></li>)}</ul>}
      {s.after && <p><Text>{s.after}</Text></p>}
      {s.extra?.length > 0 && <ul>{s.extra.map((t, i) => <li key={i}><Text>{t}</Text></li>)}</ul>}
    </section>
  )
}

// Highlights the section currently on screen in the side contents.
function useActive(ids) {
  const [active, setActive] = useState(ids[0])
  useEffect(() => {
    if (!('IntersectionObserver' in window)) return
    const io = new IntersectionObserver(
      (entries) => {
        const vis = entries.filter((e) => e.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
        if (vis[0]) setActive(vis[0].target.id)
      },
      { rootMargin: '-80px 0px -65% 0px' }
    )
    ids.forEach((id) => { const el = document.getElementById(id); if (el) io.observe(el) })
    return () => io.disconnect()
  }, [ids.join()])
  return active
}

export default function PrivacyPolicy({ app }) {
  const sections = buildPolicy(app)
  const active = useActive(sections.map((s) => s.id))
  const f = app.features
  const glance = [
    f.photos && { icon: 'image', text: 'Your photos are processed on your device and never uploaded.' },
    { icon: 'lock', text: 'We never sell or rent your personal information.' },
    !f.ads && { icon: 'check', text: 'No third-party ads. Your data is not used for advertising.' },
    { icon: 'email', text: `Ask us to see or delete your data any time at ${developer.email}.` },
  ].filter(Boolean)

  return (
    <div className="site" id="top">
      <Header app={app} current="privacy" />

      <header className="policy-hero">
        <div className="container">
          <p className="kicker">LEGAL</p>
          <h1 className="display display-sm">Privacy Policy</h1>
          <p className="policy-meta">
            {app.name} for {app.platform} · Effective {app.effectiveDate} · <Text>{developer.name}</Text>
          </p>
          <ul className="glance">
            {glance.map((g) => (
              <li key={g.icon}><span className="well well-sm"><Icon name={g.icon} size={18} /></span><span><Text>{g.text}</Text></span></li>
            ))}
          </ul>
        </div>
      </header>

      <main className="container policy">
        <aside className="policy-toc" aria-label="Contents">
          <p className="kicker">CONTENTS</p>
          <ol>
            {sections.map((s, i) => (
              <li key={s.id}>
                <a href={`#${s.id}`} className={active === s.id ? 'active' : ''}>
                  <span className="num">{String(i + 1).padStart(2, '0')}</span>{s.title}
                </a>
              </li>
            ))}
          </ol>
        </aside>

        <article className="policy-body">
          {sections.map((s, i) => <Section key={s.id} s={s} index={i} />)}
          <a className="back-top" href="#top">Back to top <Icon name="chevron_up" size={16} /></a>
        </article>
      </main>

      <Footer app={app} />
    </div>
  )
}

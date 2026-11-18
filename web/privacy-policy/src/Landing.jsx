import { useEffect } from 'react'
import { Text } from './Text.jsx'
import { Header, Footer, Icon, PlayButton } from './Chrome.jsx'

// Fades sections in as they scroll into view (skipped when reduced motion is on, see CSS).
function useReveal() {
  useEffect(() => {
    const els = document.querySelectorAll('.reveal')
    if (!('IntersectionObserver' in window)) {
      els.forEach((e) => e.classList.add('in'))
      return
    }
    const io = new IntersectionObserver(
      (entries) => entries.forEach((e) => e.isIntersecting && (e.target.classList.add('in'), io.unobserve(e.target))),
      { rootMargin: '0px 0px -8% 0px', threshold: 0.08 }
    )
    els.forEach((e) => io.observe(e))
    return () => io.disconnect()
  }, [])
}

function Phone({ shot, alt, className = '' }) {
  return (
    <figure className={`phone ${className}`}>
      <img src={`/shots/${shot}.jpg`} alt={alt} loading="lazy" width="693" height="1500" />
    </figure>
  )
}

// Three slides that join into one picture, then pull apart to show the cuts.
function SeamlessArt() {
  return (
    <div className="seam" aria-hidden="true">
      <div className="seam-track">
        {[0, 1, 2].map((i) => (
          <div key={i} className="seam-slide" style={{ '--i': i }}>
            <div className="seam-pano">
              <span className="sky" />
              <span className="sun" />
              <span className="sea" />
              <span className="ph ph-a"><span /></span>
              <span className="ph ph-b"><span /></span>
              <span className="tape" />
              <span className="title">summer<br />diary</span>
            </div>
            <span className="seam-num">0{i + 1}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function Landing({ app, content }) {
  useReveal()
  const f = app.features
  const privacyPoints = [
    f.photos && { icon: 'image', title: 'Photos stay on your phone', text: 'Picked with the Android photo picker and processed on device. Never uploaded.' },
    !f.ads && { icon: 'check', title: 'No ads', text: 'No third-party advertising, and your data is never used for ads.' },
    { icon: 'lock', title: 'Never sold', text: 'We do not sell or rent your personal information.' },
    { icon: 'designs', title: 'Drafts on device', text: 'Your drafts and settings live on your device until you delete them.' },
  ].filter(Boolean)

  return (
    <div className="site" id="top">
      <Header app={app} current="home" />

      <main>
        {/* ───── Hero ───── */}
        <section className="hero">
          <div className="hero-glow" aria-hidden="true" />
          <div className="container hero-in">
            <div className="hero-copy">
              <p className="pill"><span className="pill-dot" />{content.hero.kicker}</p>
              <h1 className="display">
                {content.hero.title.map((l, i) => (
                  <span key={l} className={`line ${i === 2 ? 'accent-line' : ''}`} style={{ '--d': `${i * 90}ms` }}>{l}</span>
                ))}
              </h1>
              <p className="hero-text">{content.hero.text}</p>
              <div className="hero-ctas" id="get">
                <PlayButton app={app} />
                <a className="btn btn-ghost" href="#features">Explore features <Icon name="arrow_right" size={18} /></a>
              </div>
            </div>
            <div className="hero-art">
              <Phone shot="main" alt="FrameLab home screen with template rows" className="p-left" />
              <Phone shot="e4-filled" alt="FrameLab editor with a summer diary carousel" className="p-mid" />
              <Phone shot="n7-preview" alt="Template preview showing all slides joined" className="p-right" />
              <span className="chip chip-a"><Icon name="templates" size={16} /> 120+ templates</span>
              <span className="chip chip-b"><Icon name="check" size={16} /> Saved in order</span>
            </div>
          </div>

          <div className="container">
            <dl className="stats">
              {content.stats.map((s) => (
                <div key={s.label} className="stat">
                  <dt>{s.value}</dt>
                  <dd>{s.label}</dd>
                </div>
              ))}
            </dl>
          </div>
        </section>

        {/* ───── Seamless ───── */}
        <section className="band band-dark">
          <div className="container split reveal">
            <div>
              <p className="kicker kicker-dark">THE SEAMLESS SWIPE</p>
              <h2 className="h2">Photos that cross from one slide into the next.</h2>
              <p className="sub sub-dark">
                A photo can span two slides. FrameLab keeps it lined up across the cut, so when people swipe,
                your post reads as one long picture.
              </p>
            </div>
            <SeamlessArt />
          </div>
        </section>

        {/* ───── How it works ───── */}
        <section className="section-pad" id="how">
          <div className="container">
            <div className="section-head reveal">
              <p className="kicker">HOW IT WORKS</p>
              <h2 className="h2">From camera roll to carousel in four steps.</h2>
            </div>
            <ol className="steps">
              {content.steps.map((s, i) => (
                <li key={s.title} className="step reveal" style={{ '--d': `${i * 70}ms` }}>
                  <div className="step-shot"><img src={`/shots/${s.shot}.jpg`} alt="" loading="lazy" width="693" height="1500" /></div>
                  <span className="step-num">{String(i + 1).padStart(2, '0')}</span>
                  <h3>{s.title}</h3>
                  <p>{s.text}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* ───── Features ───── */}
        <section className="section-pad section-sunk" id="features">
          <div className="container">
            <div className="section-head reveal">
              <p className="kicker">{content.kicker}</p>
              <h2 className="h2">Everything you need for a post that flows.</h2>
              <p className="sub">{content.intro}</p>
            </div>
            <div className="bento">
              {content.sections.map((s) => (
                <article key={s.id} id={s.id} className={`card reveal ${s.id === 'editor' ? 'card-wide card-dark' : ''} ${s.id === 'reliability' ? 'card-wide card-row' : ''}`}>
                  <div className="card-head">
                    <span className="well"><Icon name={s.icon} size={22} /></span>
                    <h3>{s.title}</h3>
                  </div>
                  <dl className="items">
                    {s.items.map((it) => (
                      <div key={it.label} className="item">
                        <dt>{it.label}</dt>
                        <dd><Text>{it.text}</Text></dd>
                      </div>
                    ))}
                  </dl>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* ───── Post in order ───── */}
        <section className="section-pad">
          <div className="container split split-rev reveal">
            <div className="duo">
              <Phone shot="n18-precheck" alt="Empty-spot check before saving" className="duo-a" />
              <Phone shot="n9-postinorder" alt="Post in this order guide" className="duo-b" />
            </div>
            <div>
              <p className="kicker">SAVE AND SHARE</p>
              <h2 className="h2">Full-size slides, numbered and in order.</h2>
              <ul className="ticks">
                <li><Icon name="check" size={18} />Each slide saved as its own image, in High or Max quality, as JPG or PNG.</li>
                <li><Icon name="check" size={18} />Numbered files in a “FrameLab” album in your gallery.</li>
                <li><Icon name="check" size={18} />A check for empty photo spots before you save.</li>
                <li><Icon name="check" size={18} />A step-by-step guide hands the images to Instagram in the right order.</li>
              </ul>
            </div>
          </div>
        </section>

        {/* ───── Privacy ───── */}
        <section className="section-pad section-tight">
          <div className="container">
            <div className="privacy-band reveal">
              <div className="privacy-band-head">
                <span className="well well-accent"><Icon name="lock" size={22} /></span>
                <div>
                  <h2 className="h3">Private by design.</h2>
                  <p>We collect only what we need to run the app. <a href="/privacy-policy">Read the privacy policy</a>.</p>
                </div>
              </div>
              <ul className="privacy-points">
                {privacyPoints.map((p) => (
                  <li key={p.title}>
                    <Icon name={p.icon} size={20} />
                    <strong>{p.title}</strong>
                    <span>{p.text}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </section>

        {/* ───── Pro + coming soon ───── */}
        <section className="section-pad section-tight" id="pro">
          <div className="container pro-grid">
            <div className="pro reveal">
              <span className="pro-badge"><Icon name="pro" size={16} /> PRO</span>
              <h2 className="h2">{content.pro.title}</h2>
              <p><Text>{content.pro.text}</Text></p>
            </div>
            {content.coming?.length > 0 && (
              <div className="coming reveal">
                <p className="kicker">COMING SOON</p>
                <ul>
                  {content.coming.map((t) => <li key={t}><Icon name="chevron_right" size={18} /><Text>{t}</Text></li>)}
                </ul>
              </div>
            )}
          </div>
        </section>

        {/* ───── Final CTA ───── */}
        <section className="section-pad">
          <div className="container">
            <div className="cta reveal">
              <h2 className="display display-sm">Make your next carousel<br />with {app.name}.</h2>
              <p>Pick a template, add your photos, post the swipe.</p>
              <div className="hero-ctas center">
                <PlayButton app={app} variant="accent" />
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer app={app} />
    </div>
  )
}

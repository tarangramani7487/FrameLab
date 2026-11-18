import { useEffect, useState } from 'react'
import { developer } from './developer.js'
import { Text } from './Text.jsx'

// Tinted icon from public/icons (FrameLab icon set), drawn with a CSS mask.
export function Icon({ name, size = 20, className = '' }) {
  return (
    <span
      className={`icon ${className}`}
      aria-hidden="true"
      style={{ width: size, height: size, WebkitMaskImage: `url(/icons/ic_${name}.svg)`, maskImage: `url(/icons/ic_${name}.svg)` }}
    />
  )
}

export function PlayButton({ app, variant = 'dark' }) {
  if (app.playStoreUrl)
    return (
      <a className={`btn btn-${variant}`} href={app.playStoreUrl} target="_blank" rel="noopener">
        Get it on Google Play <Icon name="external" size={18} />
      </a>
    )
  return (
    <span className={`btn btn-${variant} btn-static`} role="note">
      <span className="pulse" aria-hidden="true" /> Coming soon to Google Play
    </span>
  )
}

export function Header({ app, current }) {
  const [scrolled, setScrolled] = useState(false)
  useEffect(() => {
    const on = () => setScrolled(window.scrollY > 8)
    on()
    window.addEventListener('scroll', on, { passive: true })
    return () => window.removeEventListener('scroll', on)
  }, [])
  const home = current === 'home'
  return (
    <header className={`nav ${scrolled ? 'nav-scrolled' : ''}`}>
      <div className="nav-in">
        <a className="wordmark" href="/" aria-label={`${app.name} home`}>
          <img src="/icon-512.png" alt="" width="32" height="32" />
          {app.name}<span className="dot">.</span>
        </a>
        <nav className="nav-links" aria-label="Main">
          <a className="hide-sm" href={home ? '#features' : '/#features'}>Features</a>
          <a className="hide-sm" href={home ? '#how' : '/#how'}>How it works</a>
          <a href="/privacy-policy" aria-current={current === 'privacy' ? 'page' : undefined}>Privacy</a>
          {home && <a className="nav-cta hide-xs" href="#get">Get the app</a>}
        </nav>
      </div>
    </header>
  )
}

export function Footer({ app }) {
  return (
    <footer className="footer">
      <div className="footer-in">
        <div className="footer-brand">
          <span className="wordmark wordmark-lg">{app.name}<span className="dot">.</span></span>
          <p>{app.tagline} for {app.platform}.</p>
        </div>
        <nav className="footer-links" aria-label="Footer">
          <a href="/#features">Features</a>
          <a href="/#how">How it works</a>
          <a href="/privacy-policy">Privacy policy</a>
          <a href="/delete-data">Delete your data</a>
          <Text>{developer.email}</Text>
        </nav>
      </div>
      <p className="footer-legal">© {new Date().getFullYear()} <Text>{developer.name}</Text>. All rights reserved.</p>
    </footer>
  )
}

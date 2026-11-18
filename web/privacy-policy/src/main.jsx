import React from 'react'
import { createRoot } from 'react-dom/client'
import PrivacyPolicy from './PrivacyPolicy.jsx'
import Landing from './Landing.jsx'
import DeleteData from './DeleteData.jsx'
import { Text } from './Text.jsx'
import { apps, defaultApp } from './apps/index.js'
import { developer } from './developer.js'
import './styles.css'

// Routes:
//   /                          -> landing page (features & functionality) of the default app (FrameLab)
//   /features, /feature        -> same landing page
//   /privacy-policy, /privacy  -> default app's privacy policy
//   /<slug>, /<slug>/features  -> that app's landing page
//   /<slug>/privacy-policy     -> that app's policy
//   /apps                      -> list of all apps
function route(path) {
  const clean = path.replace(/\/+$/, '') || '/'
  if (clean === '/' || clean === '/features' || clean === '/feature') return { page: 'home', app: defaultApp }
  if (clean === '/privacy-policy' || clean === '/privacy') return { page: 'privacy', app: defaultApp }
  if (clean === '/delete-data' || clean === '/delete-account') return { page: 'delete', app: defaultApp }
  if (clean === '/apps') return { page: 'index' }
  const m = clean.match(/^\/([\w-]+)(?:\/(privacy-policy|privacy|features|feature))?$/)
  const app = m && apps.find((a) => a.slug === m[1])
  if (!app) return null
  return m[2]?.startsWith('priv') ? { page: 'privacy', app } : { page: 'home', app }
}

function AppList() {
  return (
    <div className="site">
      <main className="container section-pad">
        <p className="kicker"><Text>{developer.name}</Text></p>
        <h1 className="display display-sm">Our apps</h1>
        <ul className="applist">
          {apps.map((a) => (
            <li key={a.slug} className="card">
              <h3>{a.name}</h3>
              <p>{a.tagline}</p>
              <p className="applinks">
                <a href={`/${a.slug}`}>Features</a>
                <a href={`/${a.slug}/privacy-policy`}>Privacy policy</a>
              </p>
            </li>
          ))}
        </ul>
      </main>
    </div>
  )
}

function NotFound() {
  return (
    <div className="site">
      <main className="container section-pad notfound">
        <p className="kicker">404</p>
        <h1 className="display display-sm">Page not found</h1>
        <p><a className="btn btn-dark" href="/">Go to the home page</a></p>
      </main>
    </div>
  )
}

const r = route(window.location.pathname)
const setMeta = (name, v) => document.querySelector(`meta[name="${name}"]`)?.setAttribute('content', v)
if (r?.page === 'privacy') {
  document.title = `Privacy Policy · ${r.app.name}`
  setMeta('description', `How ${r.app.name} handles your information.`)
}
if (r?.page === 'home') {
  document.title = `${r.app.name} · ${r.app.featurePage.title}`
  setMeta('description', r.app.featurePage.intro)
}
if (r?.page === 'delete') document.title = `Delete your data · ${r.app.name}`
if (!r) document.title = 'Page not found'

function Page() {
  if (!r) return <NotFound />
  if (r.page === 'index') return <AppList />
  if (r.page === 'delete') return <DeleteData app={r.app} />
  if (r.page === 'home') return <Landing app={r.app} content={r.app.featurePage} />
  return <PrivacyPolicy app={r.app} />
}

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Page />
  </React.StrictMode>
)

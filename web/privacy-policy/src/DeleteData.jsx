import { developer } from './developer.js'
import { Text } from './Text.jsx'
import { Header, Footer, Icon } from './Chrome.jsx'

// Linked from Google Play's Data safety section ("Delete data URL").
export default function DeleteData({ app }) {
  const subject = `Delete my data - ${app.name}`
  const mail = `mailto:${developer.email}?subject=${encodeURIComponent(subject)}`
  return (
    <div className="site" id="top">
      <Header app={app} current="privacy" />

      <header className="policy-hero">
        <div className="container">
          <p className="kicker">YOUR DATA</p>
          <h1 className="display display-sm">Delete your {app.name} data</h1>
          <p className="policy-meta">
            {app.name} for {app.platform} by <Text>{developer.name}</Text>. {app.name} has no user accounts, so there is no
            account to delete. This page explains how to remove the data that is kept on your phone and on our servers.
          </p>
          <a className="btn btn-dark" href={mail}><Icon name="email" size={18} /> Email a deletion request</a>
        </div>
      </header>

      <main className="container policy-body" style={{ paddingTop: 48, paddingBottom: 96 }}>
        <section className="policy-section">
          <h2><span className="num">01</span>Delete data on your phone</h2>
          <p>Your drafts, saved posts, favourites and settings are stored only on your phone. To delete them:</p>
          <ul>
            <li>Open the app and go to <strong>{app.settingsPaths.clearData}</strong>, or</li>
            <li>Uninstall {app.name}, or go to your phone's <strong>Settings → Apps → {app.name} → Storage → Clear storage</strong>.</li>
          </ul>
          <p>Your original photos stay in your gallery. {app.name} never uploads them, so there is nothing to delete on our servers.</p>
        </section>

        <section className="policy-section">
          <h2><span className="num">02</span>Ask us to delete data on our servers</h2>
          <ol className="policy-steps">
            <li>Email <Text>{developer.email}</Text> with the subject <strong>“{subject}”</strong>.</li>
            <li>Tell us your phone model and, if you sent an issue report, roughly when you sent it and its title. This helps us find your data, because {app.name} has no accounts.</li>
            <li>We confirm your request and delete the data within 30 days.</li>
          </ol>
        </section>

        <section className="policy-section">
          <h2><span className="num">03</span>What we delete</h2>
          <dl className="groups">
            <div className="group"><dt>Issue reports</dt><dd>The title and description you wrote, and the device model, Android version, app version and language sent with them.</dd></div>
            <div className="group"><dt>Push notification token</dt><dd>The random ID that lets us send notifications to your phone. Uninstalling the app also stops it working.</dd></div>
            <div className="group"><dt>Support emails and feedback</dt><dd>Messages and screenshots you sent us.</dd></div>
            <div className="group"><dt>Analytics and crash data</dt><dd>Firebase Analytics and Crashlytics data linked to your app install, where we can identify it.</dd></div>
          </dl>
        </section>

        <section className="policy-section">
          <h2><span className="num">04</span>What we keep, and for how long</h2>
          <ul>
            <li>Analytics data we cannot link to you is deleted automatically after up to 14 months.</li>
            <li>Crash reports are deleted automatically after up to 90 days.</li>
            <li>We keep a record of your deletion request, and any data the law requires us to keep, only as long as required.</li>
            <li>Purchases and subscriptions are handled by Google Play. Manage or cancel them in the Play Store. Deleting your data does not cancel a subscription.</li>
          </ul>
          <p>Read more in the <a href="/privacy-policy">{app.name} Privacy Policy</a>.</p>
        </section>
      </main>

      <Footer app={app} />
    </div>
  )
}

import { developer } from './developer.js'

// Builds the policy sections for one app from its feature flags, so every
// app shares the same structure and wording and only differs where it must.
export function buildPolicy(app) {
  const f = app.features
  const dev = developer.name
  const s = []

  s.push({
    id: 'overview',
    title: 'Overview',
    body: [
      `This Privacy Policy explains how ${dev} ("we", "us") collects, uses and shares information when you use ${app.name} (the "App") on ${app.platform}.`,
      'We collect only what we need to run the App, keep it working well and support you. We do not sell your personal information.',
      'By using the App you agree to this policy. If you do not agree, please do not use the App.',
    ],
  })

  // ---- What we collect
  const groups = []
  if (f.photos)
    groups.push({
      label: 'Photos you choose',
      text: `With your permission, the App reads only the photos you pick so you can place them in templates. Photos are processed on your device and are not uploaded to our servers.`,
    })
  if (f.accountSignIn?.length)
    groups.push({
      label: 'Account information',
      text: `If you sign in (with ${listJoin(f.accountSignIn)}), we receive your email address and, where the provider shares it, your name and profile picture.${f.guestMode ? ' Signing in is optional. You can use the App as a guest.' : ''}`,
    })
  if (f.localDrafts)
    groups.push({
      label: 'Designs and drafts',
      text: 'Your drafts and settings are stored on your device.',
    })
  if (f.purchases)
    groups.push({
      label: 'Purchases',
      text: `Purchases (${f.purchases}) are processed by Google Play. We receive only the purchase and subscription status needed to unlock paid features. We never see or store your payment card details.`,
    })
  if (f.feedback)
    groups.push({
      label: 'Feedback and support',
      text: 'If you contact us or send feedback, we receive your message, any screenshots you attach, your email address if you give it, and, if you leave "Include device and app info" switched on, your device model, operating system version and app version.',
    })
  if (f.firebaseAnalytics || f.crashlytics)
    groups.push({
      label: 'Usage and diagnostics',
      text: [
        f.firebaseAnalytics &&
          'We use Google Firebase Analytics to understand how the App is used, for example which screens are opened and which features are used. It collects app-instance identifiers, device type, operating system, app version, approximate location derived from your IP address (country or city level) and usage events.',
        f.crashlytics &&
          'We use Firebase Crashlytics to receive crash reports, which include device state, operating system and app version, and a crash identifier. They help us fix bugs.',
        'This data is not used to identify you personally.',
      ]
        .filter(Boolean)
        .join(' '),
    })
  s.push({ id: 'collect', title: 'Information we collect', groups })

  const notCollected = [
    !f.location && 'precise location',
    !f.contacts && 'contacts',
    !f.healthData && 'health data',
    'messages',
  ].filter(Boolean)
  s.push({
    id: 'not-collected',
    title: 'What we do not collect',
    body: [`The App does not access your ${listJoin(notCollected)}.${f.ads ? '' : ' The App does not show third-party advertising and does not use your data for advertising.'}`],
  })

  // ---- Use
  s.push({
    id: 'use',
    title: 'How we use information',
    list: [
      'To provide and operate the App and its features.',
      f.accountSignIn?.length && 'To create and manage your account and keep you signed in.',
      f.purchases && 'To process and restore purchases and subscriptions.',
      f.feedback && 'To answer your questions and act on your feedback.',
      (f.firebaseAnalytics || f.crashlytics) && 'To understand usage, fix crashes and improve the App.',
      'To keep the App secure and prevent misuse.',
      'To comply with legal obligations.',
    ].filter(Boolean),
    after:
      'Where the law requires a legal basis (for example under the GDPR), we rely on: performance of a contract (providing the App you asked for), our legitimate interests (improving and securing the App), your consent (for example photo access, which you can withdraw at any time) and legal obligations.',
  })

  // ---- Sharing
  const providers = [
    f.backend && `${f.backend}: hosts the App's content${f.accountSignIn?.length ? ', user accounts and sign-in, including email sign-in links' : ''}.`,
    (f.firebaseAnalytics || f.crashlytics) &&
      `Google Firebase: ${[f.firebaseAnalytics && 'analytics', f.crashlytics && 'crash reporting'].filter(Boolean).join(' and ')}.`,
    f.purchases && 'Google Play: payments and subscriptions.',
    f.accountSignIn?.includes('Google') && 'Google: Sign in with Google.',
    f.accountSignIn?.includes('Apple') && 'Apple: Sign in with Apple.',
  ].filter(Boolean)
  s.push({
    id: 'sharing',
    title: 'How we share information',
    body: ['We do not sell or rent your personal information. We share it only with service providers that help us run the App, under contracts that require them to protect it:'],
    list: providers,
    after:
      'We may also disclose information if required by law, to protect our rights or users\' safety, or as part of a merger or sale of the business, in which case this policy will continue to apply.',
  })

  s.push({
    id: 'transfers',
    title: 'International transfers',
    body: [
      'Our service providers may process information in countries other than yours, including the United States. Where required, transfers are protected by appropriate safeguards such as the European Commission\'s Standard Contractual Clauses.',
    ],
  })

  // ---- Retention
  s.push({
    id: 'retention',
    title: 'How long we keep information',
    list: [
      f.localDrafts && `Data stored on your device stays until you delete it${app.settingsPaths?.clearData ? ` (${app.settingsPaths.clearData})` : ''} or uninstall the App.`,
      f.accountSignIn?.length && 'Account data is kept while your account is active and deleted within 30 days after you delete your account, except where we must keep it by law.',
      f.firebaseAnalytics && 'Analytics data is kept for up to 14 months.',
      f.crashlytics && 'Crash reports are kept for up to 90 days.',
      f.feedback && 'Support emails and feedback are kept for up to 24 months.',
    ].filter(Boolean),
  })

  // ---- Rights (all regions)
  s.push({
    id: 'rights',
    title: 'Your rights and choices',
    body: ['Wherever you live, you can ask us to:'],
    list: [
      'access the personal information we hold about you and get a copy of it;',
      'correct inaccurate information;',
      'delete your information;',
      'restrict or object to certain processing, including analytics;',
      'withdraw consent at any time, without affecting earlier processing.',
    ],
    after: `To make a request, email ${developer.email}. We will reply within 30 days and may need to verify your identity.`,
    extra: [
      f.photos && app.settingsPaths?.photoAccess && `Photo access: change it any time in your device settings (${app.settingsPaths.photoAccess}).`,
      f.accountSignIn?.length && app.settingsPaths?.deleteAccount && `Delete your account in the App: ${app.settingsPaths.deleteAccount}. You can also email us to ask for deletion.`,
      f.purchases && 'Subscriptions: manage or cancel them in the Google Play Store. Deleting the App or your account does not cancel a subscription.',
    ].filter(Boolean),
  })

  s.push({
    id: 'regional',
    title: 'Regional information',
    groups: [
      {
        label: 'European Economic Area, United Kingdom and Switzerland',
        text: `You have the rights listed above under the GDPR and UK GDPR, including data portability. ${dev} is the data controller. You may also complain to your local data protection authority.`,
      },
      {
        label: 'California and other US states',
        text: 'Under the CCPA/CPRA and similar state laws, you have the right to know, delete and correct your personal information, and the right not to be discriminated against for using these rights. We do not sell or "share" personal information for cross-context behavioural advertising.',
      },
      {
        label: 'India',
        text: 'Under the Digital Personal Data Protection Act, 2023, you may access, correct and erase your data, nominate another person to exercise your rights, and raise a grievance with us using the contact details below.',
      },
      {
        label: 'Brazil, Canada and elsewhere',
        text: 'You have the rights given by your local law (for example the LGPD in Brazil and PIPEDA in Canada). Contact us and we will honour them.',
      },
    ],
  })

  s.push({
    id: 'children',
    title: 'Children',
    body: [
      'The App is not directed at children under 13, or under the minimum age set by the law where you live (for example 16 in some EU countries). We do not knowingly collect information from children. If you believe a child has given us information, email us and we will delete it.',
    ],
  })

  s.push({
    id: 'security',
    title: 'Security',
    body: [
      'Information sent between the App and our servers is encrypted in transit (HTTPS), and access to stored data is limited to what our providers and we need. No method of transmission or storage is completely secure, but we work to protect your information.',
    ],
  })

  s.push({
    id: 'changes',
    title: 'Changes to this policy',
    body: [
      'We may update this policy from time to time. We will change the effective date at the top of this page and, for significant changes, tell you in the App.',
    ],
  })

  s.push({
    id: 'contact',
    title: 'Contact us',
    body: [`If you have questions or requests about this policy or your data, contact ${dev} at ${developer.email}.`],
  })

  return s
}

function listJoin(items) {
  if (items.length <= 1) return items.join('')
  return items.slice(0, -1).join(', ') + ' or ' + items[items.length - 1]
}

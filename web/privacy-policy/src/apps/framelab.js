import featurePage from '../features/framelab.js'

// FrameLab: what the Android app actually does (checked against the code).
export default {
  slug: 'framelab',
  name: 'FrameLab',
  tagline: 'Carousel and photo-collage templates',
  effectiveDate: '18 November 2026',
  platform: 'Android',
  // Google Play listing. Leave null until the app is live; the site then shows "Coming soon to Google Play".
  playStoreUrl: null,
  // Content of the landing page at / (and /framelab).
  featurePage,

  // Each feature switches on its section(s) in the shared policy.
  features: {
    photos: true, // reads photos the user picks; processed on device
    localDrafts: true,
    accountSignIn: ['Google', 'Apple', 'an email link'],
    guestMode: true,
    backend: 'Supabase',
    purchases: 'FrameLab Pro subscription via Google Play Billing',
    feedback: true, // message, screenshots, optional device info
    firebaseAnalytics: true,
    crashlytics: true,
    ads: false,
    location: false,
    contacts: false,
    healthData: false,
  },

  settingsPaths: {
    photoAccess: 'Settings → Permissions → Open system settings',
    deleteAccount: 'Settings → Account → Delete account',
    clearData: 'Settings → Storage',
  },
}

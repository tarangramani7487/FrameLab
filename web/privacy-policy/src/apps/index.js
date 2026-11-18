// Register every app here. Each gets /<slug>/privacy-policy, and /<slug>/features when it has a featurePage.
import framelab from './framelab.js'

export const apps = [framelab]

// The app served at the bare /privacy-policy URL (keeps old links working).
export const defaultApp = framelab

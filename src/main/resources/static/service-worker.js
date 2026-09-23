// The application has no offline asset cache. This no-op worker prevents browsers
// that still have the old SPA registration from requesting a missing resource.
self.addEventListener('install', event => event.waitUntil(self.skipWaiting()));
self.addEventListener('activate', event => event.waitUntil(self.clients.claim()));

# Required validation

Before publishing any change to index.html, run `node tests/startup.cjs` and resolve every failure. This checks authenticated startup, all tabs, and isolation of rendering errors with mocked Firebase and DOM; it does not verify live Firebase permissions or connectivity. For changes to cloud queries or permissions, also verify the affected operation against the real account when available and report that verification limit when unavailable.

Keep this regression test current when removing or changing features. Do not remove assertions just to make a failure pass. Preserve existing user data; failed reads must never cause default data to be written over it.

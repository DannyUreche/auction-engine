import { expect, test } from '@playwright/test'

// Live show video (WebRTC): the organizer turns on the camera, a visitor without an account sees it.
// Chrome's fake camera replaces a real one.
test.use({
  launchOptions: { args: ['--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream'] },
  permissions: ['camera', 'microphone'],
})

test('video-ul organizatorului ajunge la privitori', async ({ browser, request }) => {
  test.setTimeout(60_000)
  const username = `e2e_v${Date.now().toString(36)}`
  await request.post('/api/auth/register', { data: { username, email: `${username}@test.ro`, password: 'parola123' } })
  const { token } = await (await request.post('/api/auth/login', { data: { username, password: 'parola123' } })).json()
  const auth = { Authorization: `Bearer ${token}` }
  const ids = []
  for (const name of ['Lot video 1', 'Lot video 2']) {
    const item = await (await request.post('/api/items', { headers: auth, data: { name: `${name} ${username}`, startPrice: 100, durationInMinutes: 120 } })).json()
    ids.push(item.id)
  }
  const { show } = await (await request.post('/api/shows', { headers: auth, data: { title: `Video ${username}`, slotMinutes: 10, itemIds: ids } })).json()

  const hostContext = await browser.newContext()
  await hostContext.addInitScript((value) => localStorage.setItem('auction_token', value), token)
  const host = await hostContext.newPage()
  const viewer = await (await browser.newContext()).newPage()
  await host.goto(`/live/${show.id}`)
  await viewer.goto(`/live/${show.id}`)

  await host.getByRole('button', { name: 'Porneste camera' }).click()
  await expect(host.getByText('1 privitor conectat')).toBeVisible({ timeout: 20_000 })
  await expect
    .poll(() => viewer.evaluate(() => document.querySelector('video')?.videoWidth ?? 0), { timeout: 20_000 })
    .toBeGreaterThan(0)
  await expect(viewer.getByText('VIDEO LIVE')).toBeVisible()

  // Stopping the camera brings the photo back for the viewer
  await host.getByRole('button', { name: 'Opreste camera' }).click()
  await expect(viewer.locator('video')).toHaveCount(0, { timeout: 10_000 })
})

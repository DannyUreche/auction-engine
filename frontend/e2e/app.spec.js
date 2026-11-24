import { expect, test } from '@playwright/test'

// Un utilizator nou pentru fiecare rulare (numele trebuie sa fie unic)
const username = `e2e_${Date.now().toString(36)}`

test('pagina principala se incarca cu vitrina 3D si cifrele live', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /Liciteaza in timp real/ })).toBeVisible()
  await expect(page.locator('[data-card]').first()).toBeAttached()
  await expect(page.getByText('Licitatii active', { exact: true }).first()).toBeVisible()
})

test('inregistrare, punere la vanzare si cautare', async ({ page }) => {
  await page.goto('/register')
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Email').fill(`${username}@test.ro`)
  await page.getByLabel('Parola').fill('parola123')
  await page.getByRole('button', { name: /Creeaza cont/ }).click()

  // Dupa inregistrare utilizatorul e logat (numele apare in bara de sus)
  await expect(page.getByRole('link', { name: new RegExp(username) })).toBeVisible()

  await page.goto('/sell')
  await page.getByPlaceholder('ex: Ceas de buzunar vechi').fill(`Obiect E2E ${username}`)
  await page.getByRole('radio', { name: /Electronice/ }).click()
  await page.getByPlaceholder('100').fill('150')
  await page.getByRole('button', { name: 'Porneste licitatia' }).click()

  await expect(page).toHaveURL(/\/items\/\d+/)
  await expect(page.getByRole('heading', { name: `Obiect E2E ${username}` })).toBeVisible()
  await expect(page.getByText('Obiectul tau', { exact: true })).toBeVisible()

  // Obiectul apare pe pagina principala si poate fi gasit cu cautarea
  await page.goto('/')
  await page.getByPlaceholder('Cauta un obiect...').fill(username)
  await expect(page.getByRole('heading', { name: `Obiect E2E ${username}` })).toBeVisible()
})

test('modul intunecat se pastreaza dupa reincarcare', async ({ page }) => {
  await page.goto('/')
  await page.evaluate(() => localStorage.setItem('theme', 'light'))
  await page.reload()
  await page.getByRole('button', { name: 'Treci pe modul intunecat' }).click()
  await expect(page.locator('html')).toHaveClass(/dark/)
  await page.reload()
  await expect(page.locator('html')).toHaveClass(/dark/)
})

test('am uitat parola raspunde la fel pentru orice email', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('link', { name: 'Ai uitat parola?' }).click()
  await page.getByLabel('Emailul contului').fill('nimeni@exemplu.ro')
  await page.getByRole('button', { name: 'Trimite link-ul' }).click()
  await expect(page.getByText(/Daca exista un cont cu acest email/)).toBeVisible()
})

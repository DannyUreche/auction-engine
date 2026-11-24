// Genereaza src/dark.css (modul intunecat). Rulare: npm run gen:dark
// Adauga aici o culoare noua (HUES) sau o opacitate noua pentru bg-white (WHITE_OPACITIES) si ruleaza din nou.
import { writeFileSync } from 'node:fs'

const HUES = ['amber', 'blue', 'emerald', 'fuchsia', 'indigo', 'orange', 'pink', 'red', 'rose', 'sky', 'teal', 'violet']
const WHITE_OPACITIES = [50, 60, 80, 85, 90, 95]
// Nuantele deschise devin cele inchise si invers; 400-600 raman la fel
const SWAP_HUE = { 50: 950, 100: 900, 200: 800, 300: 700, 700: 300, 800: 200, 900: 100, 950: 50 }
// La gri, si 600 (text secundar) trebuie sa devina deschis
const SWAP_SLATE = { 50: 950, 100: 900, 200: 800, 300: 700, 600: 300, 700: 200, 800: 100, 900: 50, 950: 50 }

const maps = Object.fromEntries([...HUES.map((h) => [h, SWAP_HUE]), ['slate', SWAP_SLATE]])
const names = new Set()
for (const [hue, map] of Object.entries(maps)) {
  for (const [from, to] of Object.entries(map)) names.add(`${hue}-${from}`).add(`${hue}-${to}`)
}

const lines = [
  '/* GENERAT de scripts/generate-dark-css.mjs (npm run gen:dark) - nu modifica de mana.',
  ' * Cand <html> are clasa .dark, scalele de culori se inverseaza pe <body>: nuantele deschise (50-300)',
  ' * devin cele inchise (700-950) si invers, deci bg-slate-50 devine fundal inchis, text-slate-900 text deschis.',
  ' * Zonele care sunt deja intunecate (clasa .stay-dark) primesc inapoi culorile originale.',
  ' * --orig-*: culorile originale, citite pe :root inainte de inversare. */',
  ':root {',
  ...[...names].sort().map((n) => `  --orig-${n}: var(--color-${n});`),
  '}',
  '',
  'html.dark {',
  '  color-scheme: dark;',
  '}',
  '',
  'html.dark body {',
  ...Object.entries(maps).flatMap(([hue, map]) => Object.entries(map).map(([from, to]) => `  --color-${hue}-${from}: var(--orig-${hue}-${to});`)),
  '}',
  '',
  'html.dark .stay-dark {',
  ...Object.entries(maps).flatMap(([hue, map]) => Object.keys(map).map((from) => `  --color-${hue}-${from}: var(--orig-${hue}-${from});`)),
  '}',
  '',
  '/* Suprafetele albe (carduri, campuri, bara de sus) devin gri-albastru inchis, pastrand transparenta */',
]
const surface = (percent) => `color-mix(in oklab, var(--orig-slate-900) ${percent}%, transparent)`
const rules = [
  ['bg-white', 'background-color', 'var(--orig-slate-900)'],
  // String.raw: backslash-ul ramane in CSS (in selectori, ":" si "/" din numele claselor trebuie escapate)
  [String.raw`hover\:bg-white:hover`, 'background-color', 'var(--orig-slate-800)'],
  [String.raw`hover\:bg-white\/70:hover`, 'background-color', 'color-mix(in oklab, var(--orig-slate-800) 70%, transparent)'],
  [String.raw`border-white\/60`, 'border-color', 'color-mix(in oklab, var(--orig-slate-700) 50%, transparent)'],
  ...WHITE_OPACITIES.map((p) => [String.raw`bg-white\/` + p, 'background-color', surface(p)]),
]
for (const [cls, prop, value] of rules) {
  lines.push(`html.dark .${cls}:not(.stay-dark, .stay-dark *) { ${prop}: ${value}; }`)
}

writeFileSync(new URL('../src/dark.css', import.meta.url), lines.join('\n') + '\n')
console.log('src/dark.css generat:', lines.length, 'linii')

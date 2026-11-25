// Custom icon set. Every icon is drawn once on a 24x24 grid as a list of parts:
//   base   - the main body
//   accent - a secondary shape on top (screen, door, lens...); `hue` gives it its own color
//   line   - stroked details (hands of a watch, check marks...); `hue` changes the stroke color
// Rendered as glowing neon outlines (the glow is .icon-neon in index.css), or as a plain outline in
// currentColor ("mono") on colored buttons and badges.

const PALETTE = {
  indigo: { 100: '#e0e7ff', 300: '#a5b4fc', 400: '#818cf8', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca', 800: '#3730a3' },
  violet: { 100: '#ede9fe', 300: '#c4b5fd', 400: '#a78bfa', 500: '#8b5cf6', 600: '#7c3aed', 700: '#6d28d9', 800: '#5b21b6' },
  fuchsia: { 100: '#fae8ff', 300: '#f0abfc', 400: '#e879f9', 500: '#d946ef', 600: '#c026d3', 700: '#a21caf', 800: '#86198f' },
  pink: { 100: '#fce7f3', 300: '#f9a8d4', 400: '#f472b6', 500: '#ec4899', 600: '#db2777', 700: '#be185d', 800: '#9d174d' },
  rose: { 100: '#ffe4e6', 300: '#fda4af', 400: '#fb7185', 500: '#f43f5e', 600: '#e11d48', 700: '#be123c', 800: '#9f1239' },
  red: { 100: '#fee2e2', 300: '#fca5a5', 400: '#f87171', 500: '#ef4444', 600: '#dc2626', 700: '#b91c1c', 800: '#991b1b' },
  orange: { 100: '#ffedd5', 300: '#fdba74', 400: '#fb923c', 500: '#f97316', 600: '#ea580c', 700: '#c2410c', 800: '#9a3412' },
  amber: { 100: '#fef3c7', 300: '#fcd34d', 400: '#fbbf24', 500: '#f59e0b', 600: '#d97706', 700: '#b45309', 800: '#92400e' },
  yellow: { 100: '#fef9c3', 300: '#fde047', 400: '#facc15', 500: '#eab308', 600: '#ca8a04', 700: '#a16207', 800: '#854d0e' },
  emerald: { 100: '#d1fae5', 300: '#6ee7b7', 400: '#34d399', 500: '#10b981', 600: '#059669', 700: '#047857', 800: '#065f46' },
  teal: { 100: '#ccfbf1', 300: '#5eead4', 400: '#2dd4bf', 500: '#14b8a6', 600: '#0d9488', 700: '#0f766e', 800: '#115e59' },
  sky: { 100: '#e0f2fe', 300: '#7dd3fc', 400: '#38bdf8', 500: '#0ea5e9', 600: '#0284c7', 700: '#0369a1', 800: '#075985' },
  slate: { 100: '#f1f5f9', 300: '#cbd5e1', 400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155', 800: '#1e293b' },
}

const star4 = (x, y, r) =>
  `M${x} ${y - r}l${r * 0.36} ${r * 0.64} ${r * 0.64} ${r * 0.36}-${r * 0.64} ${r * 0.36}-${r * 0.36} ${r * 0.64}-${r * 0.36}-${r * 0.64}-${r * 0.64}-${r * 0.36} ${r * 0.64}-${r * 0.36}z`

export const ICONS = {
  gavel: {
    hue: 'violet',
    parts: [
      { t: 'base', d: 'M12.4 10 14 11.6 6.2 19.4a1.1 1.1 0 0 1-1.6-1.6z' },
      { t: 'base', d: 'M9.2 6.8 13.8 2.2 21.8 10.2 17.2 14.8z' },
      { t: 'accent', hue: 'amber', d: 'M10.3 7.9 14.9 3.3 15.9 4.3 11.3 8.9zM15.1 12.7 19.7 8.1 20.7 9.1 16.1 13.7z' },
      { t: 'base', hue: 'indigo', rect: [13.4, 18.2, 9.2, 3.4, 1.4] },
    ],
  },
  phone: {
    hue: 'sky',
    parts: [
      { t: 'base', rect: [6, 2, 12, 20, 3] },
      { t: 'accent', rect: [8, 4.6, 8, 12.4, 1.3] },
      { t: 'line', d: 'M10.5 19.4h3' },
    ],
  },
  watch: {
    hue: 'amber',
    parts: [
      { t: 'base', hue: 'slate', d: 'M8.8 1.8h6.4l.8 4.4H8zM8 17.8h8l-.8 4.4H8.8z' },
      { t: 'base', rect: [18.1, 10.6, 2, 2.8, 0.8] },
      { t: 'base', circle: [12, 12, 6.6] },
      { t: 'accent', circle: [12, 12, 4.9] },
      { t: 'line', d: 'M12 9.1V12l2 1.4' },
    ],
  },
  palette: {
    hue: 'pink',
    parts: [
      {
        t: 'base',
        d: 'M12 2.8a9.2 9.2 0 1 0 0 18.4c1.4 0 2.3-.9 2.3-2.1 0-.6-.2-1-.6-1.4-.4-.4-.6-.9-.6-1.5 0-1.2 1-2.1 2.2-2.1h2.5a4.4 4.4 0 0 0 4.4-4.4C22.2 6.4 17.6 2.8 12 2.8z',
      },
      { t: 'accent', hue: 'sky', circle: [7.2, 11.6, 1.7] },
      { t: 'accent', hue: 'yellow', circle: [8.9, 7, 1.7] },
      { t: 'accent', hue: 'emerald', circle: [14.1, 6.2, 1.7] },
      { t: 'accent', hue: 'violet', circle: [18, 9.7, 1.5] },
    ],
  },
  shirt: {
    hue: 'violet',
    parts: [
      {
        t: 'base',
        d: 'M8.6 3 3.8 5.4 2.3 10.2l3.6 1.5V20a1 1 0 0 0 1 1h10.2a1 1 0 0 0 1-1v-8.3l3.6-1.5-1.5-4.8L15.4 3c-.5 1.6-1.8 2.6-3.4 2.6S9.1 4.6 8.6 3z',
      },
      { t: 'accent', d: 'M13.4 10.6h3.4v2.6a1 1 0 0 1-1 1h-1.4a1 1 0 0 1-1-1z' },
      { t: 'line', d: 'M5.9 11.7V9.2M18.1 11.7V9.2' },
    ],
  },
  house: {
    hue: 'emerald',
    parts: [
      { t: 'base', d: 'M3.6 10.6 12 3.6l8.4 7V20a1.5 1.5 0 0 1-1.5 1.5H5.1A1.5 1.5 0 0 1 3.6 20z' },
      { t: 'accent', hue: 'amber', rect: [9.7, 13.6, 4.6, 7.9, 1.2] },
      { t: 'accent', rect: [15.4, 11.4, 2.8, 2.8, 0.7] },
      { t: 'line', hue: 'rose', d: 'M1.9 11.4 12 2.9l10.1 8.5' },
    ],
  },
  ball: {
    hue: 'orange',
    parts: [
      { t: 'base', circle: [12, 12, 9.2] },
      { t: 'line', d: 'M2.9 12h18.2M12 2.8v18.4M5.6 5.4c3.6 3.6 3.6 9.6 0 13.2M18.4 5.4c-3.6 3.6-3.6 9.6 0 13.2' },
    ],
  },
  book: {
    hue: 'indigo',
    parts: [
      { t: 'base', d: 'M2.5 5.2c3.2-1.4 6.4-1.2 9.5.9v14.2c-3.1-2.1-6.3-2.3-9.5-.9z' },
      { t: 'base', hue: 'violet', d: 'M21.5 5.2c-3.2-1.4-6.4-1.2-9.5.9v14.2c3.1-2.1 6.3-2.3 9.5-.9z' },
      { t: 'line', hue: 'white', d: 'M5 9.3c1.6-.4 3.1-.2 4.5.4M5 12.5c1.6-.4 3.1-.2 4.5.4M14.5 9.7c1.4-.6 2.9-.8 4.5-.4M14.5 12.9c1.4-.6 2.9-.8 4.5-.4' },
    ],
  },
  car: {
    hue: 'red',
    parts: [
      {
        t: 'base',
        d: 'M3 16.4v-3.2c0-.8.4-1.5 1.1-1.9l1.7-.9 1.8-3.7a2 2 0 0 1 1.8-1.1h5.2a2 2 0 0 1 1.8 1.1l1.8 3.7 1.7.9c.7.4 1.1 1.1 1.1 1.9v3.2a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z',
      },
      { t: 'accent', d: 'M8.5 10.5 9.7 8h4.6l1.2 2.5z' },
      { t: 'accent', hue: 'yellow', rect: [4.4, 12.6, 2.4, 1.6, 0.8] },
      { t: 'accent', hue: 'yellow', rect: [17.2, 12.6, 2.4, 1.6, 0.8] },
      { t: 'base', hue: 'slate', circle: [7.3, 17.6, 2.4] },
      { t: 'base', hue: 'slate', circle: [16.7, 17.6, 2.4] },
      { t: 'accent', circle: [7.3, 17.6, 0.9] },
      { t: 'accent', circle: [16.7, 17.6, 0.9] },
    ],
  },
  box: {
    hue: 'amber',
    parts: [
      { t: 'base', d: 'M12 2.6 20.6 7.1v9.8L12 21.4 3.4 16.9V7.1z' },
      { t: 'accent', hue: 'yellow', d: 'M12 2.6 20.6 7.1 12 11.6 3.4 7.1z' },
      { t: 'line', d: 'M12 11.6v9.8M7.7 4.9l8.6 4.5' },
    ],
  },
  bell: {
    hue: 'amber',
    parts: [
      { t: 'base', circle: [12, 19.4, 2.3] },
      {
        t: 'base',
        d: 'M12 2.8a6.2 6.2 0 0 0-6.2 6.2v3.6l-1.6 3a1 1 0 0 0 .9 1.5h13.8a1 1 0 0 0 .9-1.5l-1.6-3V9A6.2 6.2 0 0 0 12 2.8z',
      },
      { t: 'accent', rect: [5.3, 15.2, 13.4, 1.6, 0.8] },
    ],
  },
  heart: {
    hue: 'rose',
    parts: [{ t: 'base', d: 'M12 20.6S3.6 15.8 3.6 9.8A4.7 4.7 0 0 1 12 6.9a4.7 4.7 0 0 1 8.4 2.9c0 6-8.4 10.8-8.4 10.8z' }],
  },
  moon: {
    hue: 'violet',
    parts: [
      { t: 'base', d: 'M20.2 14.6A8.4 8.4 0 1 1 9.4 3.8a6.6 6.6 0 0 0 10.8 10.8z' },
      { t: 'accent', hue: 'yellow', d: star4(17.6, 4.6, 2.2) },
      { t: 'accent', hue: 'yellow', d: star4(20.6, 9.2, 1.4) },
    ],
  },
  sun: {
    hue: 'amber',
    parts: [
      { t: 'base', circle: [12, 12, 4.8] },
      { t: 'line', hue: 'amber', d: 'M12 2.2v2.4M12 19.4v2.4M2.2 12h2.4M19.4 12h2.4M5.1 5.1l1.7 1.7M17.2 17.2l1.7 1.7M5.1 18.9l1.7-1.7M17.2 6.8l1.7-1.7' },
    ],
  },
  robot: {
    hue: 'indigo',
    parts: [
      { t: 'base', hue: 'fuchsia', circle: [12, 3.5, 1.6] },
      { t: 'base', rect: [2.3, 10.6, 2.4, 4.6, 1.1] },
      { t: 'base', rect: [19.3, 10.6, 2.4, 4.6, 1.1] },
      { t: 'base', rect: [4.4, 6.8, 15.2, 12.2, 3.8] },
      { t: 'accent', rect: [6.9, 9.3, 10.2, 7, 2.4] },
      { t: 'base', hue: 'sky', circle: [9.8, 12.8, 1.4] },
      { t: 'base', hue: 'sky', circle: [14.2, 12.8, 1.4] },
      { t: 'line', d: 'M12 6.8V5.1' },
    ],
  },
  bolt: {
    hue: 'amber',
    parts: [{ t: 'base', d: 'M13.6 2 4.8 13.4h6.1L9.8 22l9.4-12.2h-6.3z' }],
  },
  trophy: {
    hue: 'yellow',
    parts: [
      { t: 'base', rect: [10.9, 13.2, 2.2, 4.2, 0.6] },
      { t: 'base', rect: [7.4, 17, 9.2, 3.9, 1.3] },
      { t: 'base', d: 'M6.8 3h10.4v5.6a5.2 5.2 0 0 1-10.4 0z' },
      { t: 'accent', d: 'M12 4.9l.85 1.75 1.9.25-1.4 1.3.35 1.9L12 9.2l-1.7.9.35-1.9-1.4-1.3 1.9-.25z' },
      { t: 'line', hue: 'yellow', d: 'M6.8 5.2H4.4v1.4a3.6 3.6 0 0 0 3.2 3.6M17.2 5.2h2.4v1.4a3.6 3.6 0 0 1-3.2 3.6' },
    ],
  },
  trendDown: {
    hue: 'rose',
    parts: [
      { t: 'base', rect: [2.6, 3.4, 18.8, 17.2, 4] },
      { t: 'line', hue: 'white', d: 'M6.4 8.6l3.8 3.8 3-3 4.4 4.4M17.6 10.2v3.6H14' },
    ],
  },
  coins: {
    hue: 'yellow',
    parts: [
      { t: 'base', circle: [15.2, 8.8, 6] },
      { t: 'accent', hue: 'yellow', circle: [15.2, 8.8, 4] },
      { t: 'base', circle: [8.8, 15.2, 6.4] },
      { t: 'accent', hue: 'yellow', circle: [8.8, 15.2, 4.3] },
      { t: 'line', d: 'M8.8 13v4.4M6.6 15.2H11' },
    ],
  },
  tagCheck: {
    hue: 'emerald',
    parts: [
      { t: 'base', d: 'M2.8 12.1V4.4a1.6 1.6 0 0 1 1.6-1.6h7.7l9.1 9.1a1.6 1.6 0 0 1 0 2.3l-7.4 7.4a1.6 1.6 0 0 1-2.3 0z' },
      { t: 'accent', circle: [7.4, 7.4, 1.7] },
      { t: 'line', hue: 'white', d: 'M9.8 13.3l2.4 2.4 4.4-4.6' },
    ],
  },
  hourglass: {
    hue: 'sky',
    parts: [
      { t: 'base', d: 'M7.4 4.6h9.2c0 3.1-2.3 4.9-3.7 7.4 1.4 2.5 3.7 4.3 3.7 7.4H7.4c0-3.1 2.3-4.9 3.7-7.4C9.7 9.5 7.4 7.7 7.4 4.6z' },
      { t: 'accent', hue: 'amber', d: 'M9.2 18.6h5.6c-.6-1.8-1.7-2.7-2.8-3.6-1.1.9-2.2 1.8-2.8 3.6zM9.6 7.2h4.8c-.6 1.2-1.4 1.9-2.4 2.6-1-.7-1.8-1.4-2.4-2.6z' },
      { t: 'base', hue: 'violet', rect: [5.6, 2.2, 12.8, 2.6, 1.2] },
      { t: 'base', hue: 'violet', rect: [5.6, 19.2, 12.8, 2.6, 1.2] },
    ],
  },
  chat: {
    hue: 'sky',
    parts: [
      {
        t: 'base',
        d: 'M3.6 5.8A2.8 2.8 0 0 1 6.4 3h11.2a2.8 2.8 0 0 1 2.8 2.8v7.8a2.8 2.8 0 0 1-2.8 2.8h-6.4L6.4 20.2v-3.8a2.8 2.8 0 0 1-2.8-2.8z',
      },
      { t: 'accent', circle: [8.4, 9.7, 1.3] },
      { t: 'accent', circle: [12, 9.7, 1.3] },
      { t: 'accent', circle: [15.6, 9.7, 1.3] },
    ],
  },
  camera: {
    hue: 'slate',
    parts: [
      { t: 'base', d: 'M8.4 6.6l1.4-2.2A1.6 1.6 0 0 1 11.2 3.6h1.6a1.6 1.6 0 0 1 1.4.8l1.4 2.2z' },
      { t: 'base', rect: [2.6, 6.2, 18.8, 14, 3] },
      { t: 'accent', circle: [12, 13.2, 4.6] },
      { t: 'base', hue: 'sky', circle: [12, 13.2, 2.8] },
      { t: 'accent', hue: 'amber', circle: [18.1, 9.3, 1] },
    ],
  },
  timer: {
    hue: 'indigo',
    parts: [
      { t: 'base', rect: [10.2, 1.8, 3.6, 2.6, 0.9] },
      { t: 'base', circle: [12, 13.4, 8.2] },
      { t: 'accent', circle: [12, 13.4, 6.2] },
      { t: 'line', d: 'M12 9.6v3.8l2.6 1.6' },
      { t: 'line', hue: 'indigo', d: 'M18.4 6.1l1.5-1.5' },
    ],
  },
  chart: {
    hue: 'violet',
    parts: [
      { t: 'base', hue: 'indigo', rect: [3.4, 12.6, 4.4, 8, 1.3] },
      { t: 'base', hue: 'violet', rect: [9.8, 8.4, 4.4, 12.2, 1.3] },
      { t: 'base', hue: 'fuchsia', rect: [16.2, 4.2, 4.4, 16.4, 1.3] },
      { t: 'line', hue: 'slate', d: 'M2.2 21.6h19.6' },
    ],
  },
  shield: {
    hue: 'indigo',
    parts: [
      { t: 'base', d: 'M12 2.4 4.4 5.4v6.1c0 4.8 3.2 8.6 7.6 10.1 4.4-1.5 7.6-5.3 7.6-10.1V5.4z' },
      { t: 'line', hue: 'white', d: 'M8.8 12.1l2.3 2.3 4.3-4.6' },
    ],
  },
  star: {
    hue: 'yellow',
    parts: [{ t: 'base', d: 'M12 2.6l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 16.9l-5.6 2.9 1.1-6.2L3 9.2l6.2-.9z' }],
  },
  pencil: {
    hue: 'amber',
    parts: [
      { t: 'base', d: 'M15.6 3.6a2 2 0 0 1 2.8 0l2 2a2 2 0 0 1 0 2.8L9.2 19.6 4 20l.4-5.2z' },
      { t: 'base', hue: 'rose', d: 'M15.6 3.6a2 2 0 0 1 2.8 0l2 2a2 2 0 0 1 0 2.8l-1.6 1.6-4.8-4.8z' },
      { t: 'accent', d: 'M4.4 14.8 9.2 19.6 4 20z' },
      { t: 'line', d: 'M12.8 6.6l4.6 4.6' },
    ],
  },
  image: {
    hue: 'sky',
    parts: [
      { t: 'base', rect: [2.6, 4, 18.8, 16, 3] },
      { t: 'accent', rect: [4.6, 6, 14.8, 12, 1.6] },
      { t: 'base', hue: 'emerald', d: 'M4.6 16.2l4.4-4.6 3.6 3.6 2.6-2.6 4.2 4.2v.6a1.6 1.6 0 0 1-1.6 1.6H6.2a1.6 1.6 0 0 1-1.6-1.6z' },
      { t: 'base', hue: 'amber', circle: [15.6, 9.4, 1.7] },
    ],
  },
  sparkles: {
    hue: 'fuchsia',
    parts: [
      { t: 'base', d: star4(10.6, 10.4, 7.6) },
      { t: 'base', hue: 'amber', d: star4(18.6, 17.4, 3.4) },
      { t: 'base', hue: 'sky', d: star4(18.4, 4.6, 2.4) },
    ],
  },
  clapper: {
    hue: 'slate',
    parts: [
      { t: 'base', rect: [3, 9.4, 18, 11.6, 2.2] },
      { t: 'base', hue: 'fuchsia', d: 'M3.3 8.4 4 5.6a1.6 1.6 0 0 1 1.9-1.2l14 3.4-.5 2.1z' },
      { t: 'line', hue: 'white', d: 'M8.4 5.4l-1.1 3.4M12.9 6.5l-1.1 3.4M17.4 7.6l-1.1 3.4' },
      { t: 'accent', rect: [6, 13, 12, 1.8, 0.9] },
      { t: 'accent', rect: [6, 16.4, 8, 1.8, 0.9] },
    ],
  },
  cart: {
    hue: 'emerald',
    parts: [
      { t: 'line', hue: 'slate', d: 'M2.6 3.6h2.6l2 9.8' },
      { t: 'base', d: 'M6.2 6.4h14.3a.8.8 0 0 1 .8 1l-1.6 6.6a1.6 1.6 0 0 1-1.6 1.2H8.7a1.6 1.6 0 0 1-1.6-1.3z' },
      { t: 'base', hue: 'slate', circle: [9.4, 19.4, 1.9] },
      { t: 'base', hue: 'slate', circle: [17.4, 19.4, 1.9] },
      { t: 'line', hue: 'white', d: 'M11 9.4v3M14.2 9.4v3M17.4 9.4v3' },
    ],
  },
  monitor: {
    hue: 'indigo',
    parts: [
      { t: 'base', hue: 'slate', d: 'M9.4 16.6h5.2l.8 3.4H8.6z' },
      { t: 'base', hue: 'slate', rect: [6.8, 19.6, 10.4, 2, 1] },
      { t: 'base', rect: [2.4, 3.4, 19.2, 13.4, 2.4] },
      { t: 'accent', rect: [4.4, 5.4, 15.2, 9.4, 1.2] },
      { t: 'line', hue: 'indigo', d: 'M7 12.4l2.4-2.6 2.2 1.8 3-3.4 2.4 2.2' },
    ],
  },
  server: {
    hue: 'violet',
    parts: [
      { t: 'base', rect: [3.4, 3.2, 17.2, 7.6, 2.2] },
      { t: 'base', rect: [3.4, 13.2, 17.2, 7.6, 2.2] },
      { t: 'accent', hue: 'emerald', circle: [7.4, 7, 1.3] },
      { t: 'accent', hue: 'emerald', circle: [7.4, 17, 1.3] },
      { t: 'line', hue: 'white', d: 'M11.4 7h5.6M11.4 17h5.6' },
    ],
  },
  database: {
    hue: 'sky',
    parts: [
      { t: 'base', d: 'M4.6 5.8c0-1.8 3.3-3.2 7.4-3.2s7.4 1.4 7.4 3.2v12.4c0 1.8-3.3 3.2-7.4 3.2s-7.4-1.4-7.4-3.2z' },
      { t: 'accent', d: 'M4.6 5.8c0 1.8 3.3 3.2 7.4 3.2s7.4-1.4 7.4-3.2-3.3-3.2-7.4-3.2-7.4 1.4-7.4 3.2z' },
      { t: 'line', d: 'M4.6 12c0 1.8 3.3 3.2 7.4 3.2s7.4-1.4 7.4-3.2' },
    ],
  },
  mail: {
    hue: 'rose',
    parts: [
      { t: 'base', rect: [2.6, 5, 18.8, 14, 2.6] },
      { t: 'accent', d: 'M3.3 6.3 12 12.8l8.7-6.5A2.6 2.6 0 0 0 18.8 5H5.2a2.6 2.6 0 0 0-1.9 1.3z' },
    ],
  },
}

// Extra icons (sharing, QR code, reports, language, install, wallet)
Object.assign(ICONS, {
  share: {
    hue: 'sky',
    parts: [
      { t: 'line', d: 'M8.6 10.6l6.8-4M8.6 13.4l6.8 4' },
      { t: 'base', circle: [18, 5.2, 3] },
      { t: 'base', circle: [6, 12, 3] },
      { t: 'base', circle: [18, 18.8, 3] },
    ],
  },
  qr: {
    hue: 'indigo',
    parts: [
      { t: 'base', rect: [3, 3, 7.5, 7.5, 1.6] },
      { t: 'base', rect: [13.5, 3, 7.5, 7.5, 1.6] },
      { t: 'base', rect: [3, 13.5, 7.5, 7.5, 1.6] },
      { t: 'accent', rect: [5.3, 5.3, 2.9, 2.9, 0.6] },
      { t: 'accent', rect: [15.8, 5.3, 2.9, 2.9, 0.6] },
      { t: 'accent', rect: [5.3, 15.8, 2.9, 2.9, 0.6] },
      { t: 'base', rect: [13.5, 13.5, 3, 3, 0.6] },
      { t: 'base', rect: [18, 18, 3, 3, 0.6] },
      { t: 'base', rect: [18, 13.5, 3, 3, 0.6] },
      { t: 'base', rect: [13.5, 18, 3, 3, 0.6] },
    ],
  },
  flag: {
    hue: 'rose',
    parts: [
      { t: 'line', hue: 'slate', d: 'M5 3v18' },
      { t: 'base', d: 'M5.5 4h11.5l-2.4 4.2L17 12.4H5.5z' },
    ],
  },
  globe: {
    hue: 'sky',
    parts: [
      { t: 'base', circle: [12, 12, 9] },
      { t: 'line', d: 'M3 12h18M12 3c2.5 2.5 3.8 5.6 3.8 9s-1.3 6.5-3.8 9M12 3c-2.5 2.5-3.8 5.6-3.8 9s1.3 6.5 3.8 9' },
    ],
  },
  pin: {
    hue: 'rose',
    parts: [
      { t: 'base', d: 'M12 21.5s-6.8-6.4-6.8-11.6a6.8 6.8 0 0 1 13.6 0c0 5.2-6.8 11.6-6.8 11.6z' },
      { t: 'accent', hue: 'amber', circle: [12, 9.8, 2.6] },
    ],
  },
  eye: {
    hue: 'teal',
    parts: [
      { t: 'base', d: 'M2.5 12C4.6 7.6 8 5.2 12 5.2s7.4 2.4 9.5 6.8c-2.1 4.4-5.5 6.8-9.5 6.8S4.6 16.4 2.5 12z' },
      { t: 'accent', hue: 'sky', circle: [12, 12, 3.6] },
    ],
  },
  search: {
    hue: 'indigo',
    parts: [
      { t: 'base', circle: [10.5, 10.5, 6.8] },
      { t: 'accent', circle: [10.5, 10.5, 4.4] },
      { t: 'line', d: 'M15.6 15.6 21 21' },
    ],
  },
  filter: {
    hue: 'violet',
    parts: [
      { t: 'base', d: 'M3.5 4.5h17l-6.6 7.6v6.1l-3.8 2v-8.1z' },
      { t: 'accent', hue: 'fuchsia', d: 'M8.6 8.2h6.8l-2.4 2.8h-2z' },
    ],
  },
  download: {
    hue: 'emerald',
    parts: [
      { t: 'base', d: 'M4 15.5v3A2.5 2.5 0 0 0 6.5 21h11a2.5 2.5 0 0 0 2.5-2.5v-3' },
      { t: 'line', d: 'M12 3v11.5M7.5 10l4.5 4.5 4.5-4.5' },
    ],
  },
  wallet: {
    hue: 'violet',
    parts: [
      { t: 'base', hue: 'amber', d: 'M5 6.5 15.5 3.2a1 1 0 0 1 1.3.7l.7 2.6z' },
      { t: 'base', rect: [2.5, 6.3, 19, 14, 3] },
      { t: 'accent', rect: [14.2, 10.6, 7.3, 5.4, 2] },
      { t: 'base', hue: 'amber', circle: [17, 13.3, 1.1] },
    ],
  },
})

// Category code (from the backend) -> icon name
export const CATEGORY_ICONS = {
  ELECTRONICE: 'phone',
  CEASURI_BIJUTERII: 'watch',
  ARTA_COLECTII: 'palette',
  MODA: 'shirt',
  CASA_GRADINA: 'house',
  SPORT: 'ball',
  CARTI_MUZICA: 'book',
  VEHICULE: 'car',
  ALTELE: 'box',
}

function Shape({ part, ...props }) {
  if (part.circle) {
    const [cx, cy, r] = part.circle
    return <circle cx={cx} cy={cy} r={r} {...props} />
  }
  if (part.rect) {
    const [x, y, width, height, rx = 0] = part.rect
    return <rect x={x} y={y} width={width} height={height} rx={rx} {...props} />
  }
  return <path d={part.d} {...props} />
}

// Glowing outlines (the glow is a CSS drop-shadow, see .icon-neon in index.css)
function NeonIcon({ def, ...svg }) {
  return (
    <svg {...svg} style={{ color: PALETTE[def.hue][500] }}>
      {def.parts.map((p, i) => {
        const stroke = p.hue && p.hue !== 'white' ? PALETTE[p.hue][500] : 'currentColor'
        if (p.t === 'base') {
          return <Shape key={i} part={p} fill={stroke} fillOpacity={0.12} stroke={stroke} strokeWidth={1.4} strokeLinejoin="round" />
        }
        if (p.t === 'accent') {
          return <Shape key={i} part={p} fill={stroke} fillOpacity={0.35} stroke={stroke} strokeWidth={1.1} strokeLinejoin="round" />
        }
        return <Shape key={i} part={p} fill="none" stroke={stroke} strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round" />
      })}
    </svg>
  )
}

// Outline in the current text color (for icons on colored buttons and badges)
function MonoIcon({ def, ...svg }) {
  return (
    <svg {...svg}>
      {def.parts.map((p, i) =>
        p.t === 'base' ? (
          <Shape key={i} part={p} fill="currentColor" fillOpacity={0.18} stroke="currentColor" strokeWidth={1.6} strokeLinejoin="round" />
        ) : p.t === 'accent' ? (
          <Shape key={i} part={p} fill="currentColor" fillOpacity={0.85} />
        ) : (
          <Shape key={i} part={p} fill="none" stroke="currentColor" strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        ),
      )}
    </svg>
  )
}

// <Icon name="trophy" size={20} /> - glowing neon icon; mono: outline in currentColor (for colored buttons).
// label: accessible name (otherwise the icon is decorative and hidden from screen readers)
export function Icon({ name, size = 20, className = '', mono = false, label }) {
  const def = ICONS[name]
  if (!def) return null
  const a11y = label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true }
  const svg = { viewBox: '0 0 24 24', width: size, height: size, ...a11y }
  if (mono) return <MonoIcon def={def} {...svg} className={`inline-block shrink-0 ${className}`} />
  return <NeonIcon def={def} {...svg} className={`icon-neon inline-block shrink-0 ${className}`} />
}

// Icon of a category ({ code } from /api/categories)
export function CategoryIcon({ category, size = 18, className = '' }) {
  return <Icon name={CATEGORY_ICONS[category?.code] ?? 'box'} size={size} className={className} />
}

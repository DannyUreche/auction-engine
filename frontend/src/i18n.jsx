import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { Icon } from './components/icons.jsx'

// RO/EN interface language. The Romanian text is the key: t('Contul meu') returns the English
// translation when English is on and the dictionary has it, otherwise the Romanian text unchanged.
// Placeholders: t('{n} rezultate', { n: 3 }). Messages that come from the server stay in Romanian.

const EN = {
  // Header, footer
  'Licitatii active': 'Live auctions',
  'Vinde un obiect': 'Sell an item',
  'Contul meu': 'My account',
  Despre: 'About',
  Active: 'Live',
  Vinde: 'Sell',
  Iesire: 'Log out',
  'Intra in cont': 'Log in',
  'Creeaza cont': 'Sign up',
  Offline: 'Offline',
  'Conectat: preturile se actualizeaza instant': 'Connected: prices update instantly',
  'Se reconecteaza...': 'Reconnecting...',
  'Profilul tau public': 'Your public profile',
  'Portofelul tau (disponibil pentru oferte)': 'Your wallet (available for bids)',
  'Licitatii in timp real · oferte instant · castigator stabilit automat ·': 'Real-time auctions · instant bids · winner picked automatically ·',
  'Despre proiect': 'About the project',
  Confidentialitate: 'Privacy',
  Termeni: 'Terms',
  'Instaleaza aplicatia': 'Install the app',
  'Mod luminos': 'Light mode',
  'Mod intunecat': 'Dark mode',
  'Notificari pe acest dispozitiv': 'Notifications on this device',
  'si cand site-ul e inchis': 'even when the site is closed',
  Notificari: 'Notifications',
  'ultimele 30': 'last 30',
  'Nicio notificare inca.': 'No notifications yet.',

  // Home
  'Se termina curand': 'Ending soon',
  'Pret crescator': 'Price: low to high',
  'Pret descrescator': 'Price: high to low',
  'Cele mai noi': 'Newest',
  '{n} licitatii active chiar acum': '{n} live auctions right now',
  'Se conecteaza la licitatii...': 'Connecting to the auctions...',
  Liciteaza: 'Bid',
  'in timp real': 'in real time',
  'Castiga ce iti place.': 'Win what you love.',
  'Ofertele apar instant pe ecranul tuturor. Cronometru live, prelungire automata in ultimele minute si optiunea „Cumpara acum” pentru cei grabiti.':
    'Bids show up instantly on everyone’s screen. Live countdown, automatic extension in the last minutes and a “Buy now” option for those in a hurry.',
  'Exploreaza licitatiile': 'Explore the auctions',
  'Oferte plasate': 'Bids placed',
  'Obiecte vandute': 'Items sold',
  'Valoare vanduta': 'Value sold',
  Toate: 'All',
  'Cauta un obiect...': 'Search for an item...',
  'Pret minim': 'Min price',
  'Pret maxim': 'Max price',
  rezultat: 'result',
  rezultate: 'results',
  'Sterge filtrele': 'Clear filters',
  'Niciun obiect nu se potriveste filtrelor': 'No item matches the filters',
  'Nu exista licitatii active': 'There are no live auctions',
  'Pune primul obiect la licitatie': 'Put the first item up for auction',
  'Te-ar putea interesa': 'You might like',
  'Gaseste ce cauti': 'Find what you want',
  Categorii: 'Categories',
  'Tip de licitatie': 'Auction type',
  'Toate tipurile': 'All types',
  'Cu Cumpara acum': 'With Buy now',
  Olandeze: 'Dutch',
  'Se termina azi': 'Ending today',
  'Incep curand': 'Starting soon',
  Sortare: 'Sort',
  Judet: 'County',
  Clasament: 'Leaderboard',
  'Clasamentul lunii': 'Leaderboard of the month',
  Licitatori: 'Bidders',
  Vanzatori: 'Sellers',
  obiecte: 'items',
  vanzare: 'sale',
  vanzari: 'sales',
  'Nicio oferta luna aceasta': 'No bids this month',
  'Nicio vanzare luna aceasta': 'No sales this month',
  'Fii primul in clasament!': 'Be the first on the leaderboard!',
  'Cei mai activi licitatori si cei mai buni vanzatori. Completeaza provocarile lunii si primesti insigne pe profil.': 'The most active bidders and the best sellers. Complete the monthly challenges to earn profile badges.',
  'Clasamentul se reia in': 'The leaderboard resets in',
  z: 'd',
  'Provocarile lunii': 'Monthly challenges',
  'Provocarile tale': 'Your challenges',
  'Intra in cont ca sa vezi provocarile tale si cat mai ai pana la insigne.': 'Log in to see your challenges and how close you are to the badges.',
  din: 'of',
  completate: 'completed',
  locul: 'rank',
  'la licitatori': 'among bidders',
  'Ai castigat insigna „Campionul provocarilor”!': 'You earned the “Challenge champion” badge!',
  'Licitator activ': 'Active bidder',
  'Plaseaza 10 oferte luna aceasta': 'Place 10 bids this month',
  Explorator: 'Explorer',
  'Liciteaza in 3 categorii diferite': 'Bid in 3 different categories',
  Castigator: 'Winner',
  'Castiga o licitatie': 'Win an auction',
  'Vanator de chilipiruri': 'Bargain hunter',
  'Urmareste 5 obiecte noi': 'Watch 5 new items',
  Curios: 'Curious',
  'Pune o intrebare unui vanzator': 'Ask a seller a question',
  'Licitatia saptamanii': 'Auction of the week',
  'VANDUT!': 'SOLD!',
  'Aleasa de echipa': 'Picked by our team',
  'Cea mai urmarita': 'Most watched',
  'Vandut cu': 'Sold for',
  oferta: 'bid',
  oferte: 'bids',
  urmaritori: 'watchers',
  'Castigata de {u}!': 'Won by {u}!',
  'Licitatia s-a incheiat.': 'The auction has ended.',
  'Liciteaza acum': 'Bid now',
  'Fii primul care liciteaza': 'Be the first to bid',
  'Timp ramas': 'Time left',
  'Licitatii in toata tara': 'Auctions across the country',
  'Culoarea arata cate licitatii sunt in fiecare judet. Punctele verzi sunt oferte live.': 'The color shows how many auctions each county has. Green dots are live bids.',
  'Toate judetele': 'All counties',
  'jud.': 'county',
  'Incarca mai multe': 'Load more',
  'Salveaza ca alerta': 'Save as alert',
  'Alerta salvata! O gasesti in Contul meu.': 'Alert saved! You can find it in My account.',
  '{shown} din {total}': '{shown} of {total}',
  'de la': 'from',
  'pana la': 'up to',
  'Alese dupa categoriile in care ai licitat sau pe care le urmaresti.': 'Picked from the categories you bid on or watch.',
  'In prim-plan:': 'Featured:',

  // Categories (labels come from the server)
  Electronice: 'Electronics',
  'Ceasuri si bijuterii': 'Watches & jewelry',
  'Arta si colectii': 'Art & collectibles',
  Moda: 'Fashion',
  'Casa si gradina': 'Home & garden',
  Sport: 'Sports',
  'Carti si muzica': 'Books & music',
  Vehicule: 'Vehicles',
  Altele: 'Other',

  // Item card, countdown
  'Fara descriere': 'No description',
  'Oferta curenta': 'Current bid',
  'Pret de pornire': 'Starting price',
  'Pret final': 'Final price',
  de: 'by',
  'Olandeza · pretul scade': 'Dutch · price drops',
  'Cumpara acum': 'Buy now',
  Incheiata: 'Ended',
  'Incepe in': 'Starts in',
  'Se inchide...': 'Closing...',
  zile: 'days',
  ore: 'hours',
  min: 'min',
  sec: 'sec',
  Urmareste: 'Watch',
  Urmaresti: 'Watching',

  // Auth
  'Bine ai revenit': 'Welcome back',
  'Verificare in doi pasi': 'Two-step verification',
  'Deschide aplicatia de autentificare si scrie codul de 6 cifre.': 'Open your authenticator app and enter the 6-digit code.',
  'Cod de rezerva': 'Backup code',
  'Foloseste codul din aplicatie': 'Use the code from the app',
  'Foloseste un cod de rezerva': 'Use a backup code',
  Cifra: 'Digit',
  Inapoi: 'Back',
  'Intra in cont ca sa licitezi si sa vinzi obiecte.': 'Log in to bid and sell items.',
  Parola: 'Password',
  'Ai uitat parola?': 'Forgot your password?',
  'Nu ai cont?': 'No account?',
  'Creeaza unul': 'Create one',
  'Creeaza un cont': 'Create an account',
  'Dureaza mai putin de un minut.': 'It takes less than a minute.',
  'Intre 3 si 50 de caractere': 'Between 3 and 50 characters',
  'Minim 6 caractere': 'At least 6 characters',
  'Creeaza contul': 'Create the account',
  'Ai deja cont?': 'Already have an account?',
  'Vinde si cumpara': 'Sell and buy',
  'prin licitatii': 'through auctions',
  'Emailul contului': 'Account email',
  'Retrimite link-ul': 'Resend the link',
  sau: 'or',
  'Deschide link-ul din email, apoi intra in cont.': 'Open the link in the email, then log in.',

  // Item page
  'Obiectul tau': 'Your item',
  Vanzator: 'Seller',
  Categorie: 'Category',
  'Evolutia pretului': 'Price history',
  'Istoricul ofertelor': 'Bid history',
  'Nicio oferta inca': 'No bids yet',
  'Fii primul care liciteaza!': 'Be the first to bid!',
  'Licitatia incepe in': 'The auction starts in',
  'Licitatia s-a incheiat fara nicio oferta.': 'The auction ended without any bids.',
  'Ai nevoie de un cont ca sa licitezi.': 'You need an account to bid.',
  'Intra in cont ca sa licitezi': 'Log in to bid',
  'Acesta este obiectul tau. Urmareste ofertele in timp real.': 'This is your item. Follow the bids in real time.',
  'Timpul a expirat. Licitatia se inchide in cateva secunde.': 'Time is up. The auction closes in a few seconds.',
  'Ai cea mai mare oferta in acest moment.': 'You have the highest bid right now.',
  'Castigi imediat, fara sa astepti finalul.': 'You win right away, no waiting for the end.',
  'Intrebari si raspunsuri': 'Questions & answers',
  Distribuie: 'Share',
  'a oferit': 'bid',
  Raporteaza: 'Report',
}

const DICTIONARIES = { en: EN }

// The translation for a language, with {placeholders} filled in; unknown text stays as it is (Romanian)
export function translate(lang, text, vars) {
  let result = DICTIONARIES[lang]?.[text] ?? text
  if (vars) for (const [key, value] of Object.entries(vars)) result = result.replaceAll(`{${key}}`, value)
  return result
}
const LanguageContext = createContext(null)

function detectLanguage() {
  const saved = localStorage.getItem('lang')
  // The site is Romanian by default; English only after the visitor picks it
  return saved === 'en' ? 'en' : 'ro'
}

export function LanguageProvider({ children }) {
  const [lang, setLang] = useState(detectLanguage)

  useEffect(() => {
    document.documentElement.lang = lang
    localStorage.setItem('lang', lang)
  }, [lang])

  const t = useCallback((text, vars) => translate(lang, text, vars), [lang])
  const value = useMemo(() => ({ lang, setLang, t }), [lang, t])
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export const useLanguage = () => useContext(LanguageContext)
export const useT = () => useContext(LanguageContext).t

// Header button: switches between Romanian and English
export function LanguageToggle() {
  const { lang, setLang } = useLanguage()
  const next = lang === 'ro' ? 'en' : 'ro'
  return (
    <button
      type="button"
      onClick={() => setLang(next)}
      title={next === 'en' ? 'Switch to English' : 'Schimba in romana'}
      aria-label={next === 'en' ? 'Switch to English' : 'Schimba in romana'}
      className="flex h-9 shrink-0 items-center gap-1 rounded-full bg-white/80 px-2.5 text-xs font-bold text-slate-600 ring-1 ring-slate-200 transition hover:text-indigo-600 hover:ring-indigo-300"
    >
      <span className="hidden sm:contents">
        <Icon name="globe" size={17} />
      </span>
      {lang.toUpperCase()}
    </button>
  )
}

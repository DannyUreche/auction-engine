import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api.js'
import { Icon } from '../components/icons.jsx'
import { Card } from '../components/ui.jsx'

// Pagina "Despre proiect": arhitectura (diagrama animata), functiile si tehnologiile folosite
export default function About() {
  // The AI features need ANTHROPIC_API_KEY on the server; without it their card says they are off
  const [aiEnabled, setAiEnabled] = useState(null)
  useEffect(() => {
    api('/ai/config', { auth: false })
      .then((config) => setAiEnabled(config.enabled))
      .catch(() => setAiEnabled(false))
  }, [])

  return (
    <div className="space-y-12">
      <section className="stay-dark relative overflow-hidden rounded-3xl bg-slate-950 px-6 py-12 text-white ring-1 ring-white/10 sm:px-10">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0">
          <div className="absolute -top-32 -left-20 h-96 w-96 animate-float rounded-full bg-indigo-600/40 blur-3xl" />
          <div className="absolute -right-24 -bottom-40 h-[28rem] w-[28rem] animate-float-slow rounded-full bg-fuchsia-600/30 blur-3xl" />
        </div>
        <div className="relative max-w-3xl animate-fade-up">
          <p className="text-sm font-semibold tracking-wide text-indigo-300 uppercase">Despre proiect</p>
          <h1 className="mt-3 text-4xl font-bold tracking-tight sm:text-5xl">
            O platforma de licitatii <span className="text-gradient">in timp real</span>
          </h1>
          <p className="mt-4 text-indigo-100/80">
            Aplicatie web completa: un server REST in Spring Boot, o interfata React si o baza de date PostgreSQL. Ofertele,
            notificarile si mesajele ajung instant in browser prin Server-Sent Events, iar regulile de licitare (pas minim,
            prelungire anti-sniping, licitare automata) sunt verificate pe server si acoperite de teste automate.
          </p>
        </div>
      </section>

      <section>
        <SectionTitle title="Arhitectura" subtitle="Cum circula datele intre browser, server si baza de date" />
        <Card className="mt-5 overflow-hidden">
          <Architecture />
        </Card>
      </section>

      <section>
        <SectionTitle title="Ce poate face aplicatia" />
        <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f, i) => (
            <div
              key={f.title}
              style={{ animationDelay: `${i * 50}ms` }}
              className="animate-fade-up rounded-2xl bg-white/85 p-5 ring-1 ring-slate-200/80 backdrop-blur transition hover:-translate-y-0.5 hover:shadow-lg hover:shadow-indigo-500/10"
            >
              <div className="flex items-start justify-between gap-2">
                <Icon name={f.icon} size={34} />
                {f.ai && aiEnabled === false && (
                  <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold text-slate-600 ring-1 ring-slate-200">
                    Oprit momentan · necesita cheie API
                  </span>
                )}
              </div>
              <h3 className="mt-2 font-semibold text-slate-900">{f.title}</h3>
              <p className="mt-1 text-sm text-slate-600">{f.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section>
        <SectionTitle title="Tehnologii" />
        <div className="mt-5 grid gap-4 md:grid-cols-3">
          {STACK.map((group) => (
            <Card key={group.title}>
              <h3 className="font-semibold text-slate-900">{group.title}</h3>
              <ul className="mt-3 space-y-2 text-sm text-slate-600">
                {group.items.map(([name, detail]) => (
                  <li key={name}>
                    <span className="font-medium text-slate-900">{name}</span> - {detail}
                  </li>
                ))}
              </ul>
            </Card>
          ))}
        </div>
      </section>

      <section className="text-center">
        <Link
          to="/"
          className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-6 py-3 font-semibold text-white shadow-lg shadow-indigo-500/25 transition hover:-translate-y-0.5"
        >
          Vezi licitatiile active →
        </Link>
      </section>
    </div>
  )
}

function SectionTitle({ title, subtitle }) {
  return (
    <div>
      <h2 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h2>
      {subtitle && <p className="mt-1 text-slate-500">{subtitle}</p>}
    </div>
  )
}

// Diagrama: trei coloane (browser -> server -> date), legate prin "fluxuri" animate.
// Pe telefon coloanele se aseaza una sub alta.
function Architecture() {
  return (
    <div className="grid items-stretch gap-3 lg:grid-cols-[1fr_auto_1.3fr_auto_1fr]">
      <Tier
        icon="monitor"
        title="Browser"
        subtitle="React 19 + Vite + Tailwind"
        rows={['Pagini: licitatii, obiect, cont, admin', 'Grafice SVG si animatii canvas', 'JWT pastrat in localStorage', 'O singura conexiune live (EventSource)']}
      />
      <Flow labels={['REST / JSON + JWT', 'Server-Sent Events']} />
      <Tier
        icon="server"
        title="Server Spring Boot 4"
        subtitle="Java 21"
        highlight
        rows={[
          'Spring Security + JWT (roluri USER / ADMIN)',
          'Controllere REST + validare (Bean Validation)',
          'Servicii: licitare, licitare automata, portofel, recenzii, chat',
          'Scheduler: inchide licitatiile, porneste cele programate, scade pretul olandez',
          'Evenimente dupa commit -> SSE, notificari, email, push',
        ]}
      />
      <Flow labels={['JPA / Hibernate', 'SMTP', 'Web Push']} />
      <div className="grid gap-3">
        <Tier icon="database" title="PostgreSQL" subtitle="Neon, schema prin Flyway" rows={['Obiecte, oferte, poze (bytea)', 'Portofele, notificari, recenzii, mesaje, intrebari']} />
        <Tier icon="mail" title="Email" subtitle="SMTP (ex: Brevo)" rows={['Oferta depasita, castigat, resetare parola']} />
      </div>
    </div>
  )
}

function Tier({ icon, title, subtitle, rows, highlight }) {
  return (
    <div
      className={`rounded-2xl p-4 ring-1 ${
        highlight ? 'bg-gradient-to-br from-indigo-50 to-fuchsia-50 ring-indigo-200' : 'bg-slate-50 ring-slate-200'
      }`}
    >
      <p className="flex items-center gap-2 font-semibold text-slate-900">
        <Icon name={icon} size={24} />
        {title}
      </p>
      <p className="text-xs text-slate-500">{subtitle}</p>
      <ul className="mt-3 space-y-1.5 text-sm text-slate-700">
        {rows.map((row) => (
          <li key={row} className="flex gap-2">
            <span className="mt-2 h-1 w-1 shrink-0 rounded-full bg-indigo-500" />
            {row}
          </li>
        ))}
      </ul>
    </div>
  )
}

// Legatura dintre doua coloane: puncte care "curg" (orizontal pe ecran mare, vertical pe telefon)
function Flow({ labels }) {
  return (
    <div className="flex flex-row items-center justify-center gap-4 py-1 lg:flex-col lg:px-2">
      {labels.map((label, i) => (
        <div key={label} className="flex flex-col items-center gap-1.5">
          <span className="text-[11px] font-medium whitespace-nowrap text-slate-500">{label}</span>
          <span
            className={`block h-10 w-1.5 rounded-full bg-[length:100%_16px] lg:h-1.5 lg:w-24 lg:bg-[length:16px_100%] ${
              i === 0
                ? 'animate-flow-y bg-[linear-gradient(to_bottom,var(--color-indigo-500)_40%,transparent_40%)] lg:animate-flow-x lg:bg-[linear-gradient(to_right,var(--color-indigo-500)_40%,transparent_40%)]'
                : 'animate-flow-y-slow bg-[linear-gradient(to_bottom,var(--color-fuchsia-500)_40%,transparent_40%)] lg:animate-flow-x-slow lg:bg-[linear-gradient(to_right,var(--color-fuchsia-500)_40%,transparent_40%)]'
            }`}
          />
        </div>
      ))}
    </div>
  )
}

const FEATURES = [
  { icon: 'bolt', title: 'Licitatii in timp real', text: 'Ofertele apar instant pe ecranul tuturor, fara reincarcarea paginii (Server-Sent Events).' },
  { icon: 'robot', title: 'Licitare automata', text: 'Spui suma maxima, iar serverul liciteaza pentru tine cu pasul minim, ca pe eBay.' },
  { icon: 'timer', title: 'Reguli corecte', text: 'Pas minim in functie de pret si prelungire anti-sniping cand cineva liciteaza in ultimele minute.' },
  { icon: 'cart', title: 'Cumpara acum', text: 'Pret fix optional: cine il plateste castiga imediat, iar licitatia se inchide.' },
  { icon: 'bell', title: 'Notificari si email', text: 'Clopotel cu istoric si emailuri cand esti depasit, cand castigi sau cand vinzi.' },
  { icon: 'chat', title: 'Chat si recenzii', text: 'Dupa vanzare, cumparatorul si vanzatorul vorbesc direct si isi dau note de la 1 la 5.' },
  { icon: 'heart', title: 'Favorite', text: 'Urmaresti obiecte si primesti notificari live cand cineva liciteaza la ele.' },
  { icon: 'chart', title: 'Grafice', text: 'Evolutia pretului pe fiecare obiect si statistici pentru administratori.' },
  { icon: 'shield', title: 'Securitate', text: 'JWT, parole criptate (BCrypt), limita de incercari la login, resetare parola cu link unic.' },
  { icon: 'trendDown', title: 'Licitatii olandeze si programate', text: 'Pretul scade in fiecare minut pana cumpara cineva; licitatiile pot incepe la o ora aleasa.' },
  { icon: 'wallet', title: 'Portofel virtual', text: 'Licitezi doar cu banii disponibili; la final suma trece automat de la cumparator la vanzator.' },
  { icon: 'chat', title: 'Intrebari publice', text: 'Oricine intreaba vanzatorul despre obiect, iar raspunsul apare pentru toti.' },
  { icon: 'download', title: 'Aplicatie instalabila', text: 'Se instaleaza pe telefon (PWA) si trimite notificari push chiar si cu site-ul inchis.' },
  { icon: 'flag', title: 'Moderare', text: 'Obiectele pot fi raportate; administratorii le verifica si pot bloca conturi.' },
  { icon: 'globe', title: 'Cont rapid, in doua limbi', text: 'Intrare cu Google, confirmarea emailului si interfata in romana sau engleza.' },
  {
    icon: 'sparkles',
    title: 'Asistent AI (Claude)',
    text: 'Claude propune titlul, descrierea, categoria si pretul din poza obiectului, transforma o cautare scrisa liber in filtre si verifica anunturile noi pentru administratori.',
    ai: true,
  },
  { icon: 'eye', title: 'Camera live', text: 'Vezi cine urmareste licitatia acum, scrii in chatul public si ofertele noi apar animat.' },
  { icon: 'bell', title: 'Alerte si statistici', text: 'Salvezi o cautare si esti anuntat cand apare ceva potrivit; vanzatorii vad vizualizari si oferte.' },
  { icon: 'trophy', title: 'Licitatia saptamanii si clasament', text: 'Licitatia evidentiata pe prima pagina, podiumul lunii si provocari cu insigne.' },
  { icon: 'coins', title: 'Oferte private', text: 'Propui vanzatorului un pret; el accepta, refuza sau face o contraoferta.' },
  { icon: 'shield', title: 'Licitatii verificabile', text: 'Ofertele sunt inlantuite cu SHA-256; browserul tau verifica istoricul si iti pastreaza chitantele, deci nimic nu poate fi schimbat pe ascuns.' },
  { icon: 'bookmark', title: 'Colectii', text: 'Liste personale de licitatii (ex: cadouri), publice cu link sau private.' },
  { icon: 'chart', title: 'Piata in cifre', text: 'Statistici publice: pret median pe categorii, evolutia vanzarilor, cele mai disputate licitatii si orele de varf.' },
  { icon: 'gavel', title: 'Licitatii live', text: 'Vanzatorul urca pe scena mai multe obiecte, unul dupa altul, cu cate un minut sau mai mult pentru fiecare lot.' },
  { icon: 'columns', title: 'Harta, comparatie, estimari', text: 'Harta Romaniei cu licitatii pe judete, comparatie intre 3 obiecte si cat s-au vandut obiectele asemanatoare.' },
  { icon: 'shield', title: 'Doi pasi si jurnal de administrare', text: 'Autentificare cu cod din aplicatie, coduri de rezerva si jurnalul tuturor actiunilor de administrare.' },
  { icon: 'pin', title: 'Oras, livrare, insigne', text: 'Filtrezi dupa oras, vezi cum se livreaza si cat de sigur e vanzatorul (insigne de reputatie).' },
]

const STACK = [
  {
    title: 'Backend',
    items: [
      ['Spring Boot 4 / Java 21', 'API REST, programare la intervale, evenimente'],
      ['Spring Security + JWT', 'autentificare fara sesiuni, roluri'],
      ['JPA / Hibernate + Flyway', 'acces la date si migrari versionate'],
      ['JUnit + MockMvc + JaCoCo', 'teste automate pe baza de date reala, cu acoperire'],
      ['Claude API (SDK Java)', 'functiile AI (optionale, active doar cu cheie API)'],
    ],
  },
  {
    title: 'Frontend',
    items: [
      ['React 19 + React Router', 'aplicatie cu o singura pagina'],
      ['Tailwind CSS v4', 'design, animatii, mod intunecat'],
      ['SVG + Canvas + CSS 3D', 'grafice, grila magnetica, vitrina 3D, iconite proprii (fara biblioteci)'],
      ['EventSource', 'actualizari live de la server'],
      ['Service Worker + Web Push', 'aplicatie instalabila si notificari push'],
      ['Harta SVG a Romaniei', 'contururi @svg-maps/romania (Popa Ioan Alexandru, CC BY 4.0)'],
      ['Vitest + Playwright', 'teste unitare si teste in browser'],
    ],
  },
  {
    title: 'Infrastructura',
    items: [
      ['Docker', 'o singura imagine: interfata + server'],
      ['Render', 'gazduire, deploy automat la fiecare push'],
      ['Neon PostgreSQL', 'baza de date in cloud, gratuita'],
      ['GitHub Actions', 'testele ruleaza automat la fiecare push'],
    ],
  },
]

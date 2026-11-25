import { Link } from 'react-router'

// Privacy policy and terms of service (public pages, also linked on the Google sign-in consent screen).
// Keep them in sync with what the app really stores and does.

const UPDATED = '24 noiembrie 2026'
const CONTACT_URL = 'https://github.com/DannyUreche/auction-engine/issues'

function LegalPage({ title, children }) {
  return (
    <article className="mx-auto max-w-3xl rounded-3xl bg-white/90 p-6 shadow-sm ring-1 ring-slate-200 backdrop-blur sm:p-10">
      <h1 className="text-3xl font-bold tracking-tight text-slate-900">{title}</h1>
      <p className="mt-2 text-sm text-slate-500">Ultima actualizare: {UPDATED}</p>
      <div className="mt-8 space-y-6 leading-relaxed text-slate-700 [&_h2]:mt-8 [&_h2]:text-lg [&_h2]:font-semibold [&_h2]:text-slate-900 [&_li]:ml-5 [&_li]:list-disc [&_ul]:space-y-1.5">
        {children}
      </div>
    </article>
  )
}

const contact = (
  <a href={CONTACT_URL} target="_blank" rel="noreferrer" className="font-medium text-indigo-600 hover:underline">
    pagina proiectului de pe GitHub
  </a>
)

export function Privacy() {
  return (
    <LegalPage title="Politica de confidentialitate">
      <p>
        Licitatii.live este un proiect studentesc demonstrativ: o platforma de licitatii online in timp real. Aici explicam ce
        date colectam, de ce si ce poti face cu ele.
      </p>

      <h2>Ce date stocam</h2>
      <ul>
        <li>
          <b>Contul:</b> numele de utilizator, adresa de email si parola (doar sub forma criptata, cu BCrypt; parola in clar nu
          este pastrata niciodata).
        </li>
        <li>
          <b>Intrarea cu Google:</b> daca alegi „Continua cu Google”, primim de la Google adresa ta de email, confirmarea ca este
          verificata si numele afisat (numele nu il pastram). Folosim emailul ca sa iti cream contul sau sa te conectam la contul existent cu acelasi email. Nu
          primim parola Google si nu avem acces la alte date din contul tau Google.
        </li>
        <li>
          <b>Activitatea pe site:</b> obiectele puse la vanzare (cu poze si descrieri), ofertele, obiectele urmarite, intrebarile
          si raspunsurile, mesajele din chat dupa o vanzare, recenziile, raportarile, notificarile si colectiile tale (cele publice le poate vedea oricine are linkul si apar pe profilul tau).
        </li>
        <li>
          <b>Portofelul:</b> soldul virtual si istoricul tranzactiilor. Banii din portofel sunt credite de test, fara valoare
          reala; nu procesam plati si nu stocam date de card.
        </li>
        <li>
          <b>Notificarile push</b> (doar daca le activezi): adresa tehnica de abonare a browserului tau.
        </li>
      </ul>

      <h2>Ce date sunt publice</h2>
      <p>
        Numele de utilizator, obiectele, ofertele, intrebarile si recenziile sunt vizibile pentru toti vizitatorii. Adresa de
        email nu este afisata public: o vad doar tu si, dupa o vanzare, cumparatorul si vanzatorul obiectului respectiv, ca sa
        se poata contacta.
      </p>

      <h2>De ce folosim datele</h2>
      <ul>
        <li>ca sa functioneze contul, licitatiile, portofelul si notificarile;</li>
        <li>ca sa trimitem emailuri legate de cont si licitatii (confirmarea adresei, resetarea parolei, oferta depasita, obiect castigat sau vandut);</li>
        <li>ca sa prevenim abuzurile (limitarea incercarilor de autentificare, raportari, blocarea conturilor de catre administratori).</li>
      </ul>
      <p>Nu vindem datele, nu afisam reclame si nu folosim servicii de urmarire sau analiza a vizitatorilor.</p>

      <h2>Unde sunt pastrate</h2>
      <p>
        Aplicatia ruleaza pe Render, iar baza de date pe Neon, ambele in centre de date din Frankfurt (Uniunea Europeana).
        Emailurile sunt trimise printr-un furnizor SMTP. In browser pastram doar tokenul de autentificare si preferintele de
        afisare (tema si limba), in localStorage; nu folosim cookie-uri de urmarire.
      </p>

      <h2>Drepturile tale</h2>
      <p>
        Din <Link to="/account?tab=settings" className="font-medium text-indigo-600 hover:underline">Contul meu → Setari cont</Link>{' '}
        poti descarca oricand toate datele tale (fisier JSON) si iti poti sterge contul. La stergere, emailul, parola,
        notificarile si listele tale se sterg, iar ofertele si vanzarile raman pentru ceilalti participanti, fara numele tau.
        Pentru orice alta cerere (de exemplu corectarea datelor) scrie-ne prin {contact}. Daca te-ai conectat cu Google, poti retrage oricand accesul aplicatiei din setarile contului tau Google
        (Securitate → Aplicatii terte).
      </p>

      <p className="text-sm text-slate-500">
        Vezi si <Link to="/termeni" className="font-medium text-indigo-600 hover:underline">Termenii si conditiile</Link>.
      </p>
    </LegalPage>
  )
}

export function Terms() {
  return (
    <LegalPage title="Termeni si conditii">
      <p>
        Folosind Licitatii.live accepti termenii de mai jos. Site-ul este un proiect studentesc demonstrativ, oferit gratuit,
        „asa cum este”.
      </p>

      <h2>Contul</h2>
      <ul>
        <li>Esti responsabil pentru activitatea din contul tau si pentru pastrarea parolei in siguranta.</li>
        <li>Un cont poate fi blocat de administratori in caz de abuz, frauda sau incalcarea acestor termeni.</li>
      </ul>

      <h2>Licitatiile</h2>
      <ul>
        <li>O oferta plasata nu poate fi retrasa. Castiga oferta cea mai mare la finalul licitatiei sau primul care foloseste „Cumpara acum”.</li>
        <li>O oferta in ultimele 2 minute prelungeste licitatia cu 2 minute (protectie anti-sniping).</li>
        <li>
          Portofelul contine credite virtuale de test, fara valoare reala. Site-ul nu intermediaza plati reale; orice
          intelegere privind plata si livrarea se face direct intre cumparator si vanzator, pe raspunderea lor.
        </li>
      </ul>

      <h2>Ce nu ai voie sa publici</h2>
      <ul>
        <li>obiecte ilegale, furate, contrafacute sau periculoase;</li>
        <li>continut ofensator, inselator sau care incalca drepturile altor persoane (inclusiv fotografii fara drept de folosire);</li>
        <li>date personale ale altor persoane.</li>
      </ul>
      <p>Orice utilizator poate raporta un obiect; administratorii pot sterge obiectele care incalca aceste reguli.</p>

      <h2>Raspundere</h2>
      <p>
        Nu garantam ca site-ul functioneaza fara intreruperi sau erori si nu raspundem pentru tranzactiile dintre utilizatori.
        Datele demonstrative (utilizatorii „demo_...” si obiectele lor) sunt fictive.
      </p>

      <h2>Contact</h2>
      <p>Intrebari sau probleme: scrie-ne prin {contact}.</p>

      <p className="text-sm text-slate-500">
        Vezi si <Link to="/confidentialitate" className="font-medium text-indigo-600 hover:underline">Politica de confidentialitate</Link>.
      </p>
    </LegalPage>
  )
}

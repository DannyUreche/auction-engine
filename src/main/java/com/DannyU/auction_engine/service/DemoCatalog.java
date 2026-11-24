package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;

import java.util.List;

// Demo catalog: 10 listings per category with full details. In every category the 9th listing is a
// Dutch auction (buyNow = floor price) and the 10th is a scheduled auction that opens a few hours later.
// Pictures are drawn by DemoImagePainter from the shape and color of each listing.
final class DemoCatalog {

    enum Kind { ENGLISH, DUTCH, SCHEDULED }

    record Listing(String name, String description, Category category, DemoImagePainter.Shape shape,
                   int startPrice, int buyNowOrFloor, int color, Kind kind) {
    }

    private DemoCatalog() {
    }

    private static Listing en(String name, String description, Category category, DemoImagePainter.Shape shape,
                              int start, int buyNow, int color) {
        return new Listing(name, description, category, shape, start, buyNow, color, Kind.ENGLISH);
    }

    private static Listing dutch(String name, String description, Category category, DemoImagePainter.Shape shape,
                                 int start, int floor, int color) {
        return new Listing(name, description, category, shape, start, floor, color, Kind.DUTCH);
    }

    private static Listing later(String name, String description, Category category, DemoImagePainter.Shape shape,
                                 int start, int buyNow, int color) {
        return new Listing(name, description, category, shape, start, buyNow, color, Kind.SCHEDULED);
    }

    private static final Category EL = Category.ELECTRONICE;
    private static final Category CB = Category.CEASURI_BIJUTERII;
    private static final Category AC = Category.ARTA_COLECTII;
    private static final Category MO = Category.MODA;
    private static final Category CG = Category.CASA_GRADINA;
    private static final Category SP = Category.SPORT;
    private static final Category CM = Category.CARTI_MUZICA;
    private static final Category VE = Category.VEHICULE;
    private static final Category AL = Category.ALTELE;

    static final List<Listing> LISTINGS = List.of(
            // ---------- Electronics
            en("Samsung Galaxy S23 Ultra 512GB", "Culoare Phantom Black, baterie la 94%. Vine cu cutia originala, husa din piele si folie aplicata de la cumparare.",
                    EL, DemoImagePainter.Shape.PHONE, 2800, 3900, 0x1e293b),
            en("MacBook Air M2 13 inch", "8 GB RAM, SSD de 256 GB, doar 112 cicluri de incarcare. Tastatura US, fara zgarieturi, cu incarcatorul original.",
                    EL, DemoImagePainter.Shape.LAPTOP, 3500, 4900, 0x64748b),
            en("Casti Sony WH-1000XM5", "Anulare activa a zgomotului si 30 de ore de autonomie. Folosite cateva luni, perne ca noi, cu husa de transport.",
                    EL, DemoImagePainter.Shape.HEADPHONES, 900, 1350, 0x334155),
            en("Nintendo Switch OLED", "Ecran OLED de 7 inch, doi Joy-Con si dock. Include Mario Kart 8 si Zelda: Breath of the Wild.",
                    EL, DemoImagePainter.Shape.GAMEPAD, 1100, 1550, 0xdc2626),
            en("Aparat foto Sony A7 III", "Doar 18.000 de cadre, senzor full-frame curat. Cu obiectivul 28-70 mm, doua baterii si un card de 64 GB.",
                    EL, DemoImagePainter.Shape.CAMERA, 4200, 5800, 0x111827),
            en("iPad Pro 11 inch (2021)", "Cip M1, 128 GB, Wi-Fi. Vine cu Apple Pencil 2 si husa Smart Folio; ecranul nu are nicio zgarietura.",
                    EL, DemoImagePainter.Shape.PHONE, 2400, 3300, 0x6b7280),
            en("Boxa portabila JBL Charge 5", "Rezistenta la apa IP67, 20 de ore de muzica. Poate incarca si telefonul; culoare albastra.",
                    EL, DemoImagePainter.Shape.SPEAKER, 450, 650, 0x2563eb),
            en("Monitor Dell UltraSharp 27 4K", "Panou IPS 4K cu 99% sRGB si USB-C de 90 W. Picior reglabil pe inaltime, fara pixeli morti.",
                    EL, DemoImagePainter.Shape.MONITOR, 1300, 1800, 0x0f172a),
            dutch("Drona DJI Mini 3 Fly More", "Sub 249 g, filmare 4K HDR, trei baterii si hub de incarcare. Zburata de cateva ori, fara accidente.",
                    EL, DemoImagePainter.Shape.DRONE, 2600, 1600, 0x475569),
            later("Kindle Paperwhite (2023)", "Ecran de 6,8 inch fara reflexii, lumina calda reglabila. Rezistent la apa, 16 GB, cu husa magnetica.",
                    EL, DemoImagePainter.Shape.PHONE, 400, 600, 0x14532d),

            // ---------- Watches and jewelry
            en("Ceas Seiko Presage Cocktail Time", "Mecanism automatic 4R35, cadran albastru cu textura. Purtat rar, cu cutie si certificat de garantie.",
                    CB, DemoImagePainter.Shape.WATCH, 1400, 2100, 0x1d4ed8),
            en("Verigheta din aur alb 18k", "Marimea 56, latime 4 mm, greutate 5,2 grame. Gravura interioara se poate sterge la orice bijutier.",
                    CB, DemoImagePainter.Shape.RING, 900, 1400, 0x94a3b8),
            en("Colier cu perle de cultura", "Perle albe de 7-7,5 mm, inchizatoare din argint 925. Lungime 45 cm, in cutie de catifea.",
                    CB, DemoImagePainter.Shape.NECKLACE, 700, 1100, 0xe2e8f0),
            en("Ceas Casio G-Shock GA-2100", "Modelul negru \"CasiOak\", rezistent la apa pana la 200 m. Ca nou, cu toate foliile si cutia originala.",
                    CB, DemoImagePainter.Shape.WATCH, 350, 520, 0x111827),
            en("Inel de logodna cu diamant 0,5 ct", "Aur galben 14k, diamant culoare G, puritate VS2, cu certificat IGI. Marimea 52.",
                    CB, DemoImagePainter.Shape.RING, 4500, 6500, 0xeab308),
            en("Bratara Pandora cu 8 charmuri", "Bratara din argint 925 de 19 cm, cu charmuri originale. Toate au marcajul ALE si vin in cutia brandului.",
                    CB, DemoImagePainter.Shape.NECKLACE, 800, 1200, 0xcbd5e1),
            en("Ceas de buzunar Pobeda, 1960", "Mecanism manual sovietic, merge perfect dupa revizie. Capac gravat si lantisor inclus.",
                    CB, DemoImagePainter.Shape.WATCH, 300, 480, 0xb45309),
            en("Cercei din argint cu ametist", "Pietre naturale ovale de 8x6 mm, argint 925 rodiat. Inchidere cu tija, nepurtati.",
                    CB, DemoImagePainter.Shape.RING, 220, 340, 0x9333ea),
            dutch("Ceas Tissot PRX Powermatic 80", "Automatic cu 80 de ore rezerva de mers, cadran verde, bratara integrata. Garantie internationala pana in 2027.",
                    CB, DemoImagePainter.Shape.WATCH, 2800, 1900, 0x15803d),
            later("Set bijuterii vintage anii '60", "Colier, bratara si cercei din bronz aurit cu cristale. Gasite intr-un cufar de familie, stare foarte buna.",
                    CB, DemoImagePainter.Shape.NECKLACE, 600, 950, 0xca8a04),

            // ---------- Art and collectibles
            en("Tablou in ulei - Port la Constanta", "Pictura semnata, 50x70 cm, pe panza, cu rama aurie. Culori vii, fara crapaturi.",
                    AC, DemoImagePainter.Shape.PAINTING, 900, 1500, 0x0369a1),
            en("Vaza din portelan de Sevres", "Inaltime 32 cm, decor pictat manual si aurit. Marcaj pe fund, fara ciobituri sau restaurari.",
                    AC, DemoImagePainter.Shape.VASE, 1600, 2500, 0x1e40af),
            en("Moneda 20 lei aur, 1944", "Aur 900, 6,45 g, stare XF. Livrata in capsula de protectie, cu factura.",
                    AC, DemoImagePainter.Shape.COIN, 2200, 3000, 0xd97706),
            en("Acuarela - Strada din Sighisoara", "Lucrare originala de 30x40 cm pe hartie Arches, inramata sub sticla. Semnata si datata 2019.",
                    AC, DemoImagePainter.Shape.PAINTING, 450, 700, 0xc2410c),
            en("Colectie timbre Romania 1950-1990", "Peste 600 de timbre in doua clasoare, multe serii complete. Include colita Apollo 11.",
                    AC, DemoImagePainter.Shape.BOOK, 350, 550, 0x7f1d1d),
            en("Strachina de Horezu pictata", "Strachina traditionala cu cocosul de Horezu, diametru 30 cm. Lucrata manual de un mester local.",
                    AC, DemoImagePainter.Shape.VASE, 250, 400, 0x92400e),
            en("Icoana pe sticla, secolul XIX", "Icoana pe sticla din Transilvania, 35x28 cm, cu Sfantul Gheorghe. Rama originala din lemn.",
                    AC, DemoImagePainter.Shape.PAINTING, 1800, 2700, 0xb91c1c),
            en("Set 5 monede comemorative BNR", "Monede de argint proof, in capsule si cu certificate. Tema: domnitori romani.",
                    AC, DemoImagePainter.Shape.COIN, 600, 900, 0x94a3b8),
            dutch("Litografie Salvador Dali, semnata", "Litografie numerotata 112/300 din seria Divina Comedie, cu certificat de autenticitate. 40x55 cm, inramata.",
                    AC, DemoImagePainter.Shape.PAINTING, 3500, 2200, 0x7c3aed),
            later("Pereche de sfesnice din argint", "Doua sfesnice din argint 800, inaltime 24 cm, marcate. Lustruite recent, fara indoituri.",
                    AC, DemoImagePainter.Shape.VASE, 1100, 1600, 0x9ca3af),

            // ---------- Fashion
            en("Palton din lana Max Mara", "Model clasic camel, marimea 38, 100% lana. Purtat un sezon, curatat chimic.",
                    MO, DemoImagePainter.Shape.SHIRT, 1500, 2300, 0xb45309),
            en("Adidasi Nike Air Jordan 1 High", "Culorile \"Chicago\", marimea 43, purtati de doua ori. Cu cutia si sireturi de rezerva.",
                    MO, DemoImagePainter.Shape.SHOE, 800, 1250, 0xdc2626),
            en("Rucsac din piele Fossil", "Piele naturala maro, compartiment pentru laptop de 15 inch. Stare foarte buna, fermoare noi.",
                    MO, DemoImagePainter.Shape.BAG, 400, 600, 0x78350f),
            en("Rochie de seara din matase", "Marimea S, verde smarald, lungime pana la glezna. Purtata o singura data, la o nunta.",
                    MO, DemoImagePainter.Shape.SHIRT, 600, 900, 0x047857),
            en("Geaca de blugi Levi's vintage", "Model Trucker din anii '90, marimea L. Spalare naturala, nasturi originali.",
                    MO, DemoImagePainter.Shape.SHIRT, 250, 380, 0x1d4ed8),
            en("Pantofi Oxford din piele", "Marimea 42, piele box neagra, talpa din piele cusuta. Ideali pentru costum, purtati de cateva ori.",
                    MO, DemoImagePainter.Shape.SHOE, 350, 520, 0x1f2937),
            en("Geanta Louis Vuitton Speedy 30", "Canvas monogram, cu cod de data si factura. Manere cu patina uniforma, interior curat.",
                    MO, DemoImagePainter.Shape.BAG, 3800, 5200, 0x92400e),
            en("Ochelari de soare Ray-Ban Aviator", "Lentile verzi G-15, rama aurie, marimea 58. Cu tocul si laveta originale.",
                    MO, DemoImagePainter.Shape.GLASSES, 350, 520, 0xca8a04),
            dutch("Bocanci Dr. Martens 1460", "Piele neagra Smooth, marimea 40. Purtati putin, talpa fara uzura.",
                    MO, DemoImagePainter.Shape.SHOE, 700, 420, 0x111827),
            later("Sacou din tweed Harris", "Tesatura scotiana autentica, marimea 50, nasturi din corn. Captuseala intacta.",
                    MO, DemoImagePainter.Shape.SHIRT, 450, 650, 0x57534e),

            // ---------- Home and garden
            en("Fotoliu tapitat retro, anii '60", "Picioare din lemn de fag, retapitat cu stofa verde. Arcuri noi, foarte confortabil.",
                    CG, DemoImagePainter.Shape.CHAIR, 900, 1400, 0x15803d),
            en("Lampa de birou Anglepoise", "Model original englezesc cu brat articulat, culoare neagra. Cablu si dulie noi.",
                    CG, DemoImagePainter.Shape.LAMP, 400, 650, 0x1f2937),
            en("Ficus lyrata de 1,6 m", "Planta sanatoasa, crescuta in apartament, in ghiveci ceramic alb. Ridicare din Suceava sau prin curier.",
                    CG, DemoImagePainter.Shape.PLANT, 250, 400, 0x16a34a),
            en("Scaun Thonet nr. 14", "Lemn curbat, sezut din impletitura de rachita, restaurat. Marcaj Thonet sub sezut.",
                    CG, DemoImagePainter.Shape.CHAIR, 500, 750, 0x78350f),
            en("Candelabru din cristal Bohemia", "Sase brate, cristal Bohemia, diametru 60 cm. Toate prismele sunt intacte, gata de montat.",
                    CG, DemoImagePainter.Shape.LAMP, 1200, 1800, 0xa16207),
            en("Monstera deliciosa mare", "Inaltime 1,2 m, frunze mari cu taieturi, cu tutore de cocos. Ingrijita si sanatoasa.",
                    CG, DemoImagePainter.Shape.PLANT, 180, 280, 0x15803d),
            en("Set 4 scaune din stejar masiv", "Finisaj cu ulei natural, foarte stabile. La cerere se pot vinde si separat.",
                    CG, DemoImagePainter.Shape.CHAIR, 600, 900, 0xa16207),
            en("Lampadar cu abajur din in", "Inaltime 160 cm, picior din lemn de nuc, abajur bej. Intrerupator cu picior.",
                    CG, DemoImagePainter.Shape.LAMP, 300, 450, 0x92400e),
            dutch("Bonsai Juniperus de 15 ani", "Format in stil informal drept, in ghiveci japonez din ceramica. Instructiuni de ingrijire incluse.",
                    CG, DemoImagePainter.Shape.PLANT, 900, 500, 0x166534),
            later("Balansoar din rachita", "Impletit manual, cu perne crem incluse. Potrivit pentru terasa sau living.",
                    CG, DemoImagePainter.Shape.CHAIR, 450, 700, 0xb45309),

            // ---------- Sports
            en("Bicicleta MTB Trek Marlin 7", "Marimea M (roti de 29 inch), frane hidraulice, furca RockShox. Revizie completa luna trecuta.",
                    SP, DemoImagePainter.Shape.BIKE, 2400, 3300, 0xdc2626),
            en("Minge oficiala UEFA Champions League", "Adidas Finale, marimea 5, minge de meci certificata FIFA Quality Pro. Noua, nefolosita.",
                    SP, DemoImagePainter.Shape.BALL, 300, 450, 0x1e3a8a),
            en("Set gantere reglabile 2x24 kg", "Sistem rapid de schimbare a greutatii, cu suport inclus. Inlocuieste 15 perechi de gantere.",
                    SP, DemoImagePainter.Shape.DUMBBELL, 900, 1300, 0x334155),
            en("Racheta tenis Wilson Pro Staff 97", "Grip L3, racordata recent cu Luxilon. Folosita un sezon, fara fisuri in cadru.",
                    SP, DemoImagePainter.Shape.RACKET, 600, 900, 0x111827),
            en("Minge baschet Spalding NBA", "Piele compozita, marimea 7, pentru interior si exterior. Aderenta foarte buna.",
                    SP, DemoImagePainter.Shape.BALL, 150, 240, 0xea580c),
            en("Bicicleta de sosea Specialized Allez", "Cadru din aluminiu, grup Shimano 105, marimea 54. Usoara, ideala pentru antrenamente.",
                    SP, DemoImagePainter.Shape.BIKE, 3200, 4400, 0x0f172a),
            en("Kettlebell din fonta, 16 kg", "Maner larg, vopsit in camp electrostatic, baza plata. Folosit in sala de acasa.",
                    SP, DemoImagePainter.Shape.DUMBBELL, 120, 190, 0x111827),
            en("Racheta badminton Yonex Astrox", "Grafit, 83 g, racordaj nou. Vine cu husa si 6 fluturasi din pana.",
                    SP, DemoImagePainter.Shape.RACKET, 280, 420, 0x7c3aed),
            dutch("Trotineta electrica Xiaomi 4 Pro", "Autonomie 45 km, viteza maxima 25 km/h, anvelope tubeless. 600 km parcursi, cu incarcator.",
                    SP, DemoImagePainter.Shape.SCOOTER, 2000, 1300, 0x1f2937),
            later("Minge volei Mikasa V200W", "Minge oficiala FIVB, noua, in cutia originala. Ideala pentru competitii.",
                    SP, DemoImagePainter.Shape.BALL, 250, 380, 0x2563eb),

            // ---------- Books and music
            en("Chitara acustica Yamaha FG800", "Top din molid masiv, sunet cald, actiune joasa. Cu husa captusita si capodaster.",
                    CM, DemoImagePainter.Shape.GUITAR, 600, 900, 0xb45309),
            en("Vinil Pink Floyd - The Dark Side of the Moon", "Reeditare 2016 pe 180 g, ascultat de cateva ori. Cu posterele si stickerele originale.",
                    CM, DemoImagePainter.Shape.VINYL, 300, 480, 0x111827),
            en("Enciclopedia Britannica, 32 de volume", "Editia 2010, legatura cartonata, stare impecabila. Ocupa aproximativ 1,5 m de raft.",
                    CM, DemoImagePainter.Shape.BOOK, 700, 1100, 0x7f1d1d),
            en("Harry Potter - set ilustrat complet", "Cele 7 volume in limba romana, editura Arthur, ilustrate de Jim Kay. Ca noi.",
                    CM, DemoImagePainter.Shape.BOOK, 450, 650, 0x7c2d12),
            en("Pick-up Audio-Technica AT-LP120", "Direct drive, preamplificator integrat, doza noua. Testat, ruleaza stabil la 33 si 45 rpm.",
                    CM, DemoImagePainter.Shape.VINYL, 900, 1300, 0x334155),
            en("Chitara electrica Fender Stratocaster", "Made in Mexico, sunburst, doze Player Series. Reglaj facut de luthier, cu toc rigid.",
                    CM, DemoImagePainter.Shape.GUITAR, 3200, 4500, 0xc2410c),
            en("Colectie de 40 de viniluri jazz", "Miles Davis, John Coltrane, Ella Fitzgerald si altii, editii din anii '60-'80. Stare VG+ in medie.",
                    CM, DemoImagePainter.Shape.VINYL, 800, 1200, 0x1e3a8a),
            en("Mihai Eminescu - Poesii, editia 1884", "Editia postuma ingrijita de Titu Maiorescu, legatura refacuta. Piesa de colectie rara.",
                    CM, DemoImagePainter.Shape.BOOK, 2500, 3600, 0x78350f),
            dutch("Pian digital Yamaha P-125", "88 de clape cu actiune ciocan, 24 de voci, difuzoare stereo. Cu stativ si pedala de sustain.",
                    CM, DemoImagePainter.Shape.PIANO, 2400, 1500, 0x111827),
            later("Ukulele soprano Kala", "Lemn de mahon, corzi Aquila noi, sunet clar. Cu husa si acordor.",
                    CM, DemoImagePainter.Shape.GUITAR, 200, 320, 0xa16207),

            // ---------- Vehicles
            en("Volkswagen Golf 7 1.6 TDI, 2016", "185.000 km, istoric complet de service, un singur proprietar. Navigatie, senzori de parcare, ITP pana in 2027.",
                    VE, DemoImagePainter.Shape.CAR, 32000, 41000, 0x475569),
            en("Scuter Vespa Primavera 125", "An 2020, 6.800 km, culoare alb perlat. Revizie facuta, cu topcase.",
                    VE, DemoImagePainter.Shape.SCOOTER, 9500, 12500, 0x0d9488),
            en("Dacia Duster 4x4, 2019", "1.5 dCi, 98.000 km, tractiune integrala. Anvelope de iarna incluse, carte de service.",
                    VE, DemoImagePainter.Shape.CAR, 38000, 47000, 0xea580c),
            en("Motocicleta Honda CB500F, 2018", "22.000 km, ABS, potrivita pentru permis A2. Intretinuta in service autorizat, anvelope noi.",
                    VE, DemoImagePainter.Shape.MOTO, 17000, 22000, 0xdc2626),
            en("Bicicleta electrica de oras", "Motor de 250 W in butuc, baterie 36 V 13 Ah, autonomie 70 km. Cos fata si portbagaj.",
                    VE, DemoImagePainter.Shape.BIKE, 3800, 5000, 0x0e7490),
            en("BMW Seria 3 320d, 2015", "Cutie automata, 160.000 km, interior din piele. Faruri xenon, revizie la zi.",
                    VE, DemoImagePainter.Shape.CAR, 42000, 52000, 0x1e3a8a),
            en("ATV Polaris Sportsman 570", "An 2019, 1.900 km, troliu si carlig de remorcare. Inmatriculat, pentru teren si zapada.",
                    VE, DemoImagePainter.Shape.MOTO, 21000, 27000, 0x166534),
            en("Fiat 500 1.2 Lounge, 2017", "Trapa panoramica, 76.000 km, ideal pentru oras. Consum mic, intretinut.",
                    VE, DemoImagePainter.Shape.CAR, 24000, 30000, 0xbe123c),
            dutch("Mazda MX-5 NA, 1991", "Roadster clasic, 210.000 km, capota noua. Inmatriculat ca vehicul istoric.",
                    VE, DemoImagePainter.Shape.CAR, 45000, 32000, 0xb91c1c),
            later("Bicicleta pliabila Brompton M6L", "6 viteze, culoare verde. Se pliaza in 20 de secunde, cu geanta de transport.",
                    VE, DemoImagePainter.Shape.BIKE, 4500, 6000, 0x15803d),

            // ---------- Other
            en("LEGO Technic Porsche 911 GT3 RS", "Setul 42056 complet, construit o singura data, cu instructiuni si cutie. 2.704 piese.",
                    AL, DemoImagePainter.Shape.BOX, 1100, 1600, 0xea580c),
            en("Ursulet Steiff de colectie", "Editie limitata din mohair, 30 cm, cu buton si eticheta in ureche. Certificat inclus.",
                    AL, DemoImagePainter.Shape.TEDDY, 600, 900, 0xb45309),
            en("Telescop Celestron NexStar 130", "Montura computerizata GoTo cu 40.000 de obiecte in baza de date. Oculare de 25 mm si 10 mm.",
                    AL, DemoImagePainter.Shape.TELESCOPE, 1500, 2200, 0x1e293b),
            en("Joc de sah din lemn sculptat", "Tabla de 50x50 cm din nuc si artar, piese Staunton lestate. Lucrat manual.",
                    AL, DemoImagePainter.Shape.BOX, 300, 450, 0x78350f),
            en("Cort de camping pentru 4 persoane", "Doua camere, coloana de apa 4000 mm. Folosit de doua ori, complet.",
                    AL, DemoImagePainter.Shape.TENT, 400, 600, 0x15803d),
            en("Papusa Barbie de colectie, 1985", "In cutia originala sigilata, editia Day-to-Night. Cutia are mici urme de timp.",
                    AL, DemoImagePainter.Shape.TEDDY, 350, 520, 0xdb2777),
            en("Puzzle Ravensburger 3000 de piese", "Tema: harta lumii vintage, complet (piese numarate). Asamblat o singura data.",
                    AL, DemoImagePainter.Shape.BOX, 120, 190, 0x0369a1),
            en("Masina de cusut Singer, 1950", "Masina mecanica cu pedala si masa din lemn. Functionala, decorul auriu e bine pastrat.",
                    AL, DemoImagePainter.Shape.BOX, 650, 950, 0x1f2937),
            dutch("Trusa de scule Makita 18V", "Bormasina, surubelnita cu impact, polizor unghiular si doua baterii. In geanta originala.",
                    AL, DemoImagePainter.Shape.BOX, 1400, 850, 0x0d9488),
            later("Kit de pictura in ulei profesional", "48 de culori Winsor & Newton, pensule si sevalet de masa. Nefolosit, in cutie.",
                    AL, DemoImagePainter.Shape.BOX, 280, 420, 0x7c3aed));
}

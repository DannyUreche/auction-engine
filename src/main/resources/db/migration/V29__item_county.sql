-- The location filter uses Romania's 41 counties + Bucuresti instead of free-text cities:
-- rename the column and turn the existing cities into their county (unknown text is cleared)
DROP INDEX IF EXISTS idx_items_city;
ALTER TABLE items RENAME COLUMN city TO county;

UPDATE items SET county = CASE LOWER(TRIM(county))
    WHEN 'cluj-napoca' THEN 'Cluj'
    WHEN 'cluj napoca' THEN 'Cluj'
    WHEN 'timisoara' THEN 'Timis'
    WHEN 'craiova' THEN 'Dolj'
    WHEN 'oradea' THEN 'Bihor'
    WHEN 'ploiesti' THEN 'Prahova'
    WHEN 'pitesti' THEN 'Arges'
    WHEN 'targu mures' THEN 'Mures'
    WHEN 'baia mare' THEN 'Maramures'
    WHEN 'ramnicu valcea' THEN 'Valcea'
    WHEN 'piatra neamt' THEN 'Neamt'
    WHEN 'drobeta-turnu severin' THEN 'Mehedinti'
    WHEN 'targoviste' THEN 'Dambovita'
    WHEN 'targu jiu' THEN 'Gorj'
    WHEN 'miercurea ciuc' THEN 'Harghita'
    WHEN 'sfantu gheorghe' THEN 'Covasna'
    WHEN 'deva' THEN 'Hunedoara'
    WHEN 'resita' THEN 'Caras-Severin'
    WHEN 'zalau' THEN 'Salaj'
    WHEN 'slatina' THEN 'Olt'
    WHEN 'slobozia' THEN 'Ialomita'
    WHEN 'focsani' THEN 'Vrancea'
    WHEN 'alba iulia' THEN 'Alba'
    WHEN 'bistrita' THEN 'Bistrita-Nasaud'
    WHEN 'alexandria' THEN 'Teleorman'
    ELSE county END
WHERE county IS NOT NULL;

-- Whatever is not one of the 42 names now is dropped
UPDATE items SET county = NULL
WHERE county IS NOT NULL AND county NOT IN (
    'Alba', 'Arad', 'Arges', 'Bacau', 'Bihor', 'Bistrita-Nasaud', 'Botosani', 'Braila', 'Brasov', 'Bucuresti',
    'Buzau', 'Calarasi', 'Caras-Severin', 'Cluj', 'Constanta', 'Covasna', 'Dambovita', 'Dolj', 'Galati', 'Giurgiu',
    'Gorj', 'Harghita', 'Hunedoara', 'Ialomita', 'Iasi', 'Ilfov', 'Maramures', 'Mehedinti', 'Mures', 'Neamt', 'Olt',
    'Prahova', 'Salaj', 'Satu Mare', 'Sibiu', 'Suceava', 'Teleorman', 'Timis', 'Tulcea', 'Valcea', 'Vaslui', 'Vrancea');

CREATE INDEX idx_items_county ON items (county);

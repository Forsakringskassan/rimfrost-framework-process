# Krav — rimfrost-framework-process

## 1. Funktionella krav

### FPROC-FR-01 — Processvy-länk i regelanropet

- **FPROC-FR-01.1** När processen bygger ett regelanrop ska `processvyUrl` sättas från processens konfiguration, om den finns. Metodsignaturen för `RegelService.createRegelRequest` ändras inte, så att regelunderprocesserna inte behöver ändras.
- **FPROC-FR-01.2** Processvy-länken är valfri i processens konfiguration. Saknas den startar processen som vanligt, och regelanropen skickas utan `processvyUrl`.

### FPROC-FR-02 — Processbeskrivning

- **FPROC-FR-02.1** Ramverket ska läsa processens processbeskrivning från processens resurser. Beskrivningen listar processens steg i ordning, och varje steg har id, rubrik, valfri beskrivning och de aktivitets-ID:n som hör till steget.
- **FPROC-FR-02.2** Processbeskrivningen är valfri. Saknas den startar processen som vanligt. Finns den men är ogiltig ska processen inte starta.

### FPROC-FR-03 — Steg-endpoint

- **FPROC-FR-03.1** Ramverket ska implementera `GET /processvy/steg` enligt `rimfrost-framework-process-openapi`. Givet ett aktivitets-ID svarar den med processens steg i ordning: steget som innehåller aktiviteten markeras PAGAR, tidigare steg KLAR och senare steg KOMMANDE.
- **FPROC-FR-03.2** Endpointen ska svara 404 om aktivitets-ID:t inte finns i processbeskrivningen, eller om processen inte har någon processbeskrivning.
- **FPROC-FR-03.3** Endpointen ska ligga under prefixet `/processvy`, som inte krockar med Kogitos genererade endpoints under `/{processId}`.

### FPROC-FR-04 — Understeg i processbeskrivningen

- **FPROC-FR-04.1** Ett steg i processbeskrivningen ska kunna ha `steg` i stället för `aktiviteter`. Ett understeg har samma fält som ett steg och kan ha egna understeg, högst två nivåer under det översta steget.
- **FPROC-FR-04.2** Ett steg ska ha antingen `aktiviteter` eller `steg`, inte båda. Ett steg med `steg` ska ha minst ett understeg.
- **FPROC-FR-04.3** Valideringen enligt FPROC-FR-02.2 ska gälla hela trädet: id ska vara unika i hela beskrivningen, varje innersta steg ska ha minst en aktivitet, och en aktivitet får bara förekomma i ett steg. Finns en tredje nivå under det översta steget är beskrivningen ogiltig.
- **FPROC-FR-04.4** En processbeskrivning utan understeg ska läsas och valideras som idag.

### FPROC-FR-05 — Status för understeg i steg-endpointen

- **FPROC-FR-05.1** `GET /processvy/steg` ska returnera stegens understeg i fältet `steg` enligt `rimfrost-framework-process-openapi` 1.1.0, med status för varje steg och understeg.
- **FPROC-FR-05.2** De innersta stegen ska läsas i ordning genom hela trädet. Steget med den efterfrågade aktiviteten är PAGAR, de före är KLAR och de efter är KOMMANDE.
- **FPROC-FR-05.3** Ett steg med understeg ska vara PAGAR om något understeg pågår, KLAR om alla understeg är klara och annars KOMMANDE.
- **FPROC-FR-05.4** Fältet `steg` ska saknas i svaret för steg som inte har understeg. Svaret för en platt processbeskrivning ska vara detsamma som idag.

## 2. Icke-funktionella krav

### FPROC-NFR-01 — Testbarhet

- **FPROC-NFR-01.1** Ramverket ska erbjuda ett återanvändbart testhjälpmedel som kontrollerar att varje aktivitets-ID i en process BPMN-fil finns i processbeskrivningen.
- **FPROC-NFR-01.2** `ProcessbeskrivningVerifier` ska leta efter BPMN:ens aktivitets-ID:n i de innersta stegen på alla nivåer.

### FPROC-NFR-02 — Tester

- **FPROC-NFR-02.1** Enhetstester ska täcka läsning och validering av understeg (FPROC-FR-04.1–04.4) och statusberäkningen på varje nivå (FPROC-FR-05.2–05.4), också när den pågående aktiviteten ligger först eller sist i ett understeg.

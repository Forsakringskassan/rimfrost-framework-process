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

## 2. Icke-funktionella krav

### FPROC-NFR-01 — Testbarhet

- **FPROC-NFR-01.1** Ramverket ska erbjuda ett återanvändbart testhjälpmedel som kontrollerar att varje aktivitets-ID i en process BPMN-fil finns i processbeskrivningen.

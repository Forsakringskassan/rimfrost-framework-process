# rimfrost-framework-process

## Felhantering

`ProcessService` innehåller metoden `endProcessWithError(String handlaggningId, String felkod, String felmeddelande)`
som bygger ett `HandlaggningResponseMessageData` med `resultat: "FEL"` och ett populerat `error`-objekt.
Används av processer som behöver avsluta med ett strukturerat felmeddelande på Kafka.

## Processvy

En process kan ha en processvy (stegindikatorn) som visar handläggaren var i processen en uppgift befinner sig. Ramverket skickar processvy-länken i regelanropen och svarar på vilka steg processen har. Processen bidrar bara med sin konfiguration och sin processbeskrivning. Kraven finns i [docs/krav.md](docs/krav.md).

Både länken och processbeskrivningen är valfria. Saknas länken skickas regelanropen utan `processvyUrl`, och saknas processbeskrivningen svarar steg-endpointen `404`. Processen startar som vanligt i båda fallen, och loggar vid start om processvyn är konfigurerad. Finns processbeskrivningen men är ogiltig startar processen inte, och felmeddelandet säger vad som är fel.

### Processvy-länk

Länken till processens processvy anges (valfritt) i processens `application.properties`, i samma format som en uppgifts `url`:

```properties
processvy.url=/process/vah
```

`RegelService.createRegelRequest` sätter länken som `processvyUrl` i regelanropet. Saknas länken sätts inte `processvyUrl`.

### Processbeskrivning

Processbeskrivningen är den valfria filen `processbeskrivning.json` i processens resurser (`src/main/resources/processbeskrivning.json`, bredvid BPMN-filen). Den listar processens steg i den ordning de visas, och vilka aktivitets-ID:n som hör till varje steg:

```json
{
  "processId": "vah",
  "processnamn": "Vård av husdjur",
  "steg": [
    {
      "id": "kontrollera-ratt-till-forsakring",
      "rubrik": "Kontrollera rätt till försäkring",
      "beskrivning": "Handläggaren kontrollerar att den försäkrade har rätt till ersättning.",
      "aktiviteter": ["d4ab4820-68d9-41e0-abe1-cd8f9865d275"]
    }
  ]
}
```

| Fält | Obligatoriskt | Beskrivning |
|---|---|---|
| `processId` | ja | Processens ID, samma som i BPMN-filen |
| `processnamn` | ja | Processens namn så som det visas för handläggaren |
| `steg` | ja | Minst ett steg, i ordning |
| `steg[].id` | ja | Stegets ID, unikt i processbeskrivningen |
| `steg[].rubrik` | ja | Stegets rubrik så som den visas för handläggaren |
| `steg[].beskrivning` | nej | Valfri beskrivning av steget |
| `steg[].aktiviteter` | ja, om steget inte har [understeg](#understeg) | Minst ett aktivitets-ID (UUID). Samma värde som aktivitetens `aktivitetId` i BPMN-filen. Ett aktivitets-ID får bara höra till ett steg |

Okända fält ger fel, så att felstavningar upptäcks när processen startar.

#### Understeg

Ett steg kan bestå av understeg, till exempel en bedömning som består av flera kontroller. Ett sådant steg har `steg` i stället för `aktiviteter`, och ett understeg har samma fält som ett steg. Ett understeg kan i sin tur ha understeg, så att processvyn visar högst tre nivåer: steg, understeg och understegens understeg. Aktiviteterna ligger alltid i de innersta stegen:

```json
{
  "processId": "vah",
  "processnamn": "Vård av husdjur",
  "steg": [
    {
      "id": "bedom-ratt-till-ersattning",
      "rubrik": "Bedöm rätt till ersättning",
      "steg": [
        {
          "id": "kontrollera-forsakring",
          "rubrik": "Kontrollera försäkring",
          "aktiviteter": ["d4ab4820-68d9-41e0-abe1-cd8f9865d275"]
        },
        {
          "id": "kontrollera-villkor",
          "rubrik": "Kontrollera villkor",
          "steg": [
            {
              "id": "kontrollera-arbetsgivare",
              "rubrik": "Kontrollera arbetsgivare",
              "aktiviteter": ["1c9e7a54-3d2b-4f6a-8e1c-0a9b8c7d6e5f"]
            },
            {
              "id": "kontrollera-anstallning",
              "rubrik": "Kontrollera anställning",
              "aktiviteter": ["2b3c4d5e-6f70-4a1b-9c2d-3e4f5a6b7c8e"]
            }
          ]
        }
      ]
    },
    {
      "id": "berakna-ersattning",
      "rubrik": "Beräkna ersättning",
      "aktiviteter": ["5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d"]
    }
  ]
}
```

| Fält | Obligatoriskt | Beskrivning |
|---|---|---|
| `steg[].steg` | i stället för `aktiviteter` | Minst ett understeg, i ordning, med samma fält som ett steg. Ett understeg kan ha egna understeg, men inte djupare än två nivåer under det översta steget |

Reglerna gäller hela trädet: ett steg har antingen `aktiviteter` eller `steg`, aldrig båda, id är unika i hela processbeskrivningen och ett aktivitets-ID får bara höra till ett steg. Felmeddelandet anger var i trädet felet finns, till exempel `steg[0].steg[1]: 'aktiviteter' must contain at least one aktivitets-ID`. En processbeskrivning utan understeg fungerar som tidigare.

#### Schema och validering

Formatet beskrivs av JSON Schemat [`src/main/resources/schema/processbeskrivning.schema.json`](src/main/resources/schema/processbeskrivning.schema.json). Schemat beskriver alla fält och vilka som är obligatoriska, att aktivitets-ID:n är UUID, att ett steg har antingen `aktiviteter` eller `steg`, att inga andra fält är tillåtna och att det finns högst tre nivåer.

En process validerar sin `processbeskrivning.json` i sitt eget test med `ProcessbeskrivningVerifier`, se [Testhjälpmedel](#testhjälpmedel). Verifieraren läser filen med samma inläsare som när processen startar, så testet kontrollerar allt schemat kontrollerar och dessutom reglerna i tabellen nedan. Testet körs i processens bygge och i `maven-ci`, med ramverksversionen i processens `pom.xml`, så valideringen följer med när Renovate uppgraderar ramverket.

Ramverket har också ett återanvändbart workflow, `.github/workflows/validate-processbeskrivning.yaml`, som validerar `src/main/resources/processbeskrivning.json` mot schemat utan Maven. Saknas filen går valideringen igenom, eftersom processbeskrivningen är valfri. Processerna anropar inte workflowet: anropet anger ramverkets version en gång till (`uses:` och `framework-ref`), och Renovate uppdaterar bara `uses:`, så versionerna glider isär.

Schemat och ramverket delar på kontrollen:

| Kontroll | Schemat | Ramverket (när processen startar och i processens test) |
|---|---|---|
| Obligatoriska fält, okända fält, UUID-format | ja | ja |
| `aktiviteter` eller `steg`, högst tre nivåer | ja | ja |
| Unika id i hela processbeskrivningen | nej | ja |
| Ett aktivitets-ID hör bara till ett steg | nej | ja |
| Varje aktivitet i BPMN-filen finns i processbeskrivningen | nej | bara i testet, med `ProcessbeskrivningVerifier` |

Ett JSON Schema kan inte uttrycka unikhet över hela trädet, så de reglerna kontrolleras bara av ramverket. En processbeskrivning som går igenom ramverkets kontroll går också igenom schemat. Ramverkets test `ProcessbeskrivningSchemaTest` validerar README:ns exempel och testernas processbeskrivningar mot både schemat och ramverket, så att de inte glider isär.

Formatet och valideringen är desamma oavsett om `processbeskrivning.json` skrivs för hand eller genereras, till exempel från processens dokumentations-BPMN. Schemat är det format en sådan generator ska producera.

### Steg-endpoint

`GET /processvy/steg?aktivitetId={uuid}` implementerar kontraktet i [rimfrost-framework-process-openapi](https://github.com/Forsakringskassan/rimfrost-framework-process-openapi). Steget som innehåller aktiviteten är `PAGAR`, tidigare steg `KLAR` och senare steg `KOMMANDE`. Med understeg räknas det på de innersta stegen, i ordning genom hela trädet, och ett steg med understeg är `PAGAR` om något understeg pågår, `KLAR` om alla är klara och annars `KOMMANDE`. Understegen finns i svarets fält `steg`, som saknas för steg utan understeg. Ett aktivitets-ID som inte finns i processbeskrivningen ger `404`, liksom alla anrop om processen saknar processbeskrivning, och ett saknat eller ogiltigt aktivitets-ID ger `400`.

Endpointen ligger under `/processvy` och krockar därför inte med Kogitos genererade endpoints under `/{processId}`. Ingen process får därför heta `processvy`. Endpointen kommer med automatiskt i processer som indexerar ramverket (`quarkus.index-dependency.rimfrost-framework-process`).

### Testhjälpmedel

Ramverkets test-jar innehåller `ProcessbeskrivningVerifier`, som kontrollerar att varje aktivitets-ID i en BPMN-fil finns i processbeskrivningen. Den är till för processer som har en processbeskrivning. Lägg till test-jar:en i processens `pom.xml`:

```xml
<dependency>
  <groupId>se.fk.rimfrost.framework.process</groupId>
  <artifactId>rimfrost-framework-process</artifactId>
  <version>${rimfrost-framework-process.version}</version>
  <classifier>tests</classifier>
  <scope>test</scope>
</dependency>
```

och ett test i processen:

```java
@Test
void processbeskrivning_should_contain_all_aktiviteter()
{
   ProcessbeskrivningVerifier.assertAllAktiviteterInProcessbeskrivning("vah.bpmn");
}
```

Aktiviteterna letas upp i de innersta stegen på alla nivåer. Testet går sönder om en aktivitet i BPMN-filen saknas i processbeskrivningen, om `processId` inte är samma som processens id i BPMN-filen, om BPMN-filen saknar aktiviteter eller om en aktivitets `aktivitetId` inte tilldelas ett fast värde.

# rimfrost-framework-process

## Felhantering

`ProcessService` innehåller metoden `endProcessWithError(String handlaggningId, String felkod, String felmeddelande)`
som bygger ett `HandlaggningResponseMessageData` med `resultat: "FEL"` och ett populerat `error`-objekt.
Används av processer som behöver avsluta med ett strukturerat felmeddelande på Kafka.

## Processvy

Varje process har en processvy (stegindikatorn) som visar handläggaren var i processen en uppgift befinner sig. Ramverket skickar processvy-länken i alla regelanrop och svarar på vilka steg processen har. Processen bidrar bara med sin konfiguration och sin processbeskrivning. Kraven finns i [docs/krav.md](docs/krav.md).

Processen startar inte om länken saknas, eller om processbeskrivningen saknas eller är ogiltig. Felmeddelandet säger vad som saknas.

### Processvy-länk

Länken till processens processvy anges i processens `application.properties`, i samma format som en uppgifts `url`:

```properties
PROCESSVY_URL=/process/vah
```

`RegelService.createRegelRequest` sätter länken som `processvyUrl` i regelanropet.

### Processbeskrivning

Processbeskrivningen är filen `processbeskrivning.json` i processens resurser (`src/main/resources/processbeskrivning.json`, bredvid BPMN-filen). Den listar processens steg i den ordning de visas, och vilka aktivitets-ID:n som hör till varje steg:

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
| `steg[].aktiviteter` | ja | Minst ett aktivitets-ID (UUID). Samma värde som aktivitetens `aktivitetId` i BPMN-filen. Ett aktivitets-ID får bara höra till ett steg |

Okända fält ger fel, så att felstavningar upptäcks när processen startar.

### Steg-endpoint

`GET /processvy/steg?aktivitetId={uuid}` implementerar kontraktet i [rimfrost-framework-process-openapi](https://github.com/Forsakringskassan/rimfrost-framework-process-openapi). Steget som innehåller aktiviteten är `PAGAR`, tidigare steg `KLAR` och senare steg `KOMMANDE`. Ett aktivitets-ID som inte finns i processbeskrivningen ger `404`, och ett saknat eller ogiltigt aktivitets-ID ger `400`.

Endpointen ligger under `/processvy` och krockar därför inte med Kogitos genererade endpoints under `/{processId}`. Ingen process får därför heta `processvy`. Endpointen kommer med automatiskt i processer som indexerar ramverket (`quarkus.index-dependency.rimfrost-framework-process`).

### Testhjälpmedel

Ramverkets test-jar innehåller `ProcessbeskrivningVerifier`, som kontrollerar att varje aktivitets-ID i en BPMN-fil finns i processbeskrivningen. Lägg till test-jar:en i processens `pom.xml`:

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

Testet går sönder om en aktivitet i BPMN-filen saknas i processbeskrivningen, om `processId` inte är samma som processens id i BPMN-filen, om BPMN-filen saknar aktiviteter eller om en aktivitets `aktivitetId` inte tilldelas ett fast värde.

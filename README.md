# rimfrost-framework-process

## Felhantering

`ProcessService` innehåller metoden `endProcessWithError(String handlaggningId, String felkod, String felmeddelande)`
som bygger ett `HandlaggningResponseMessageData` med `resultat: "FEL"` och ett populerat `error`-objekt.
Används av processer som behöver avsluta med ett strukturerat felmeddelande på Kafka.
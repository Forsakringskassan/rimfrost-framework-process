package se.fk.framework.process;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.HandlaggningRequestMessageData;
import se.fk.rimfrost.framework.process.ProcessService;
import se.fk.rimfrost.framework.process.RegelProcessResult;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.RegelFelkod;
import se.fk.rimfrost.framework.regel.Utfall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class ProcessServiceTest
{
   @Inject
   ProcessService processService;

   @ParameterizedTest
   @CsvSource(
   {
         "684fe6fe-7337-4925-890b-22277633ba2b",
         "c3fa5bd4-8043-4104-8e6a-863fb05a8679"
   })
   public void start_process_should_return_handlaggning_id(String handlaggningId)
   {
      HandlaggningRequestMessageData handlaggningRequest = new HandlaggningRequestMessageData();
      handlaggningRequest.setHandlaggningId(handlaggningId);

      assertEquals(handlaggningId, processService.startProcess(handlaggningRequest));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "f398bf23-c074-4b64-bbf7-e0e2741c7e7e, JA, GODKÄND",
         "f398bf23-c074-4b64-bbf7-e0e2741c7e7e, NEJ, EJ GODKÄND",
         "f70da21d-3151-4ce2-9269-a6b52a161dd6, UTREDNING, EJ GODKÄND",
         "b39ef412-cf3c-4569-bc49-439cd1438558, ERROR, EJ GODKÄND",
   })
   public void end_process_should_return_expected_response(String handlaggningId, Utfall utfall, String expectedResultat)
   {
      var response = processService.endProcess(handlaggningId, new RegelProcessResult(utfall, null));

      assertEquals(handlaggningId, response.getHandlaggningId());
      assertEquals(expectedResultat, response.getResultat());
      assertNull(response.getError());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "bf4cc45c-b321-4826-b566-6b876ebb0e59, OTHER, Meddelande",
         "90dc79a7-34c4-49cc-991c-268ae5b3ff70, null, Meddelande",
         "2aa8b6cd-a1d3-49ab-9c38-1270d21ac9f7, OTHER, null"
   })
   public void end_process_with_error_should_return_expected_response(String handlaggningId, String felkod, String felmeddelande)
   {
      felmeddelande = "null".equals(felmeddelande) ? null : felmeddelande;
      felkod = "null".equals(felkod) ? null : felkod;

      var response = processService.endProcessWithError(handlaggningId,
            createRegelErrorInformation(toRegelFelkod(felkod), felmeddelande));

      var expectedFelkod = felkod != null ? felkod : "OKAND";
      var expectedFelmeddelande = felmeddelande != null ? felmeddelande : "Okant fel";

      assertEquals(handlaggningId, response.getHandlaggningId());
      assertEquals("FEL", response.getResultat());
      assertNotNull(response.getError());
      assertEquals(expectedFelkod, response.getError().getFelkod());
      assertEquals(expectedFelmeddelande, response.getError().getFelmeddelande());
   }

   private RegelFelkod toRegelFelkod(String felkod)
   {
      if(felkod==null){return null;}

      return switch(felkod){case"HANDLAGGNING_READ_FAILURE"->RegelFelkod.HANDLAGGNING_READ_FAILURE;case"HANDLAGGNING_WRITE_FAILURE"->RegelFelkod.HANDLAGGNING_WRITE_FAILURE;case"OTHER"->RegelFelkod.OTHER;default->throw new IllegalArgumentException("Unknown felkod: "+felkod);};
   }

   private RegelErrorInformation createRegelErrorInformation(RegelFelkod felkod, String felmeddelande)
   {
      var regelErrorInformation = new RegelErrorInformation();
      regelErrorInformation.setFelkod(felkod);
      regelErrorInformation.setFelmeddelande(felmeddelande);
      return regelErrorInformation;
   }
}

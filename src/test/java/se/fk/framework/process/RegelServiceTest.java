package se.fk.framework.process;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.process.RegelService;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.RegelFelkod;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayloadData;
import se.fk.rimfrost.framework.regel.Utfall;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.wildfly.common.Assert.assertNotNull;

@QuarkusTest
public class RegelServiceTest
{
   @Inject
   RegelService regelService;

   @Test
   public void should_return_expected_regel_request()
   {
      String handlaggningId = UUID.randomUUID().toString();
      String aktivitetId = UUID.randomUUID().toString();

      var request = regelService.createRegelRequest(handlaggningId, aktivitetId);

      assertNotNull(request);
      assertEquals(handlaggningId, request.getHandlaggningId());
      assertEquals(aktivitetId, request.getAktivitetId());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "b4c847f1-e2c2-4ce8-9a36-3814b6a14423, JA, false",
         "3514dd30-4f93-4713-ad60-e9e037a59eef, NEJ, false",
         "a008c53c-c739-45a8-b4c7-ace2a0047cab, UTREDNING, false",
         "d9fe5c40-70ce-4378-90ea-1f0f7f2ebe9b, ERROR, true"
   })
   public void should_return_expected_regel_process_result(String handlaggningId, Utfall utfall, boolean hasError)
   {
      RegelErrorInformation regelErrorInformation = null;

      if (hasError)
      {
         regelErrorInformation = new RegelErrorInformation();
         regelErrorInformation.setFelkod(RegelFelkod.OTHER);
         regelErrorInformation.setFelmeddelande("Felmeddelande");
      }

      RegelResponseMessagePayloadData response = new RegelResponseMessagePayloadData();
      response.setHandlaggningId(UUID.randomUUID().toString());
      response.setError(regelErrorInformation);
      response.setUtfall(utfall);

      var result = regelService.onRegelResponse(response);

      assertNotNull(result);
      assertEquals(utfall, result.getUtfall());
      assertEquals(regelErrorInformation, result.getError());
   }
}

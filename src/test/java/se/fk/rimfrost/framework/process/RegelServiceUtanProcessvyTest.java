package se.fk.rimfrost.framework.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(RegelServiceUtanProcessvyTest.UtanProcessvyUrl.class)
public class RegelServiceUtanProcessvyTest
{
   @Inject
   RegelService regelService;

   @Test
   @DisplayName("FPROC-FR-01.2: Utan processvy-länk skickas regelanropet utan processvyUrl")
   public void should_create_regel_request_without_processvy_url_when_not_configured()
   {
      String handlaggningId = UUID.randomUUID().toString();
      String aktivitetId = UUID.randomUUID().toString();
      String handlaggningInstanceBaseUrl = UUID.randomUUID().toString();

      var request = regelService.createRegelRequest(handlaggningId, aktivitetId, handlaggningInstanceBaseUrl);

      assertEquals(handlaggningId, request.getHandlaggningId());
      assertEquals(aktivitetId, request.getAktivitetId());
      assertEquals(handlaggningInstanceBaseUrl, request.getHandlaggningInstanceBaseUrl());
      assertNull(request.getProcessvyUrl());
   }

   public static class UtanProcessvyUrl implements QuarkusTestProfile
   {
      @Override
      public Map<String, String> getConfigOverrides()
      {
         return Map.of("processvy.url", "");
      }
   }
}

package se.fk.framework.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import se.fk.rimfrost.framework.process.RegelService;

@QuarkusTest
@TestProfile(RegelServiceUtanProcessvyTest.UtanProcessvyUrl.class)
public class RegelServiceUtanProcessvyTest
{
   @Inject
   RegelService regelService;

   @Test
   public void should_create_regel_request_without_processvy_url_when_not_configured()
   {
      String handlaggningId = UUID.randomUUID().toString();
      String aktivitetId = UUID.randomUUID().toString();

      var request = regelService.createRegelRequest(handlaggningId, aktivitetId, "RESPONSE_TOPIC_NAME");

      assertEquals(handlaggningId, request.getHandlaggningId());
      assertEquals(aktivitetId, request.getAktivitetId());
      assertNull(request.getProcessvyUrl());
   }

   public static class UtanProcessvyUrl implements QuarkusTestProfile
   {
      @Override
      public Map<String, String> getConfigOverrides()
      {
         return Map.of("PROCESSVY_URL", "");
      }
   }
}

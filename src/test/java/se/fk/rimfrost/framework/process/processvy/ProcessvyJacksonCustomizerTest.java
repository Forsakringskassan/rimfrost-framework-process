package se.fk.rimfrost.framework.process.processvy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ProcessvyJacksonCustomizerTest
{
   @Inject
   ObjectMapper objectMapper;

   @Test
   public void should_omit_steg_only_for_steg_without_understeg_on_every_level()
   {
      var service = new ProcessvyService();
      service.init(Optional.empty(),
            ProcessbeskrivningReader.readResource("processvy/processbeskrivning-med-understeg.json"));
      var response = service.getSteg(UUID.fromString("1c9e7a54-3d2b-4f6a-8e1c-0a9b8c7d6e5f")).orElseThrow();

      JsonNode steg = objectMapper.valueToTree(response).get("steg");

      assertThat(steg.get(0).has("steg")).isTrue();
      assertThat(steg.get(0).get("steg").get(0).has("steg")).isFalse();
      assertThat(steg.get(0).get("steg").get(1).get("steg").get(0).has("steg")).isFalse();
      assertThat(steg.get(0).get("steg").get(1).get("steg").get(1).get("status").asText()).isEqualTo("KOMMANDE");
      assertThat(steg.get(1).has("steg")).isFalse();
      assertThat(steg.get(1).has("beskrivning")).isTrue();
   }
}

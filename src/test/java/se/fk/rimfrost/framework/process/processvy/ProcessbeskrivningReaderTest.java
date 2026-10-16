package se.fk.rimfrost.framework.process.processvy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ProcessbeskrivningReaderTest
{
   private static final String STEG_A = "{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[\"d4ab4820-68d9-41e0-abe1-cd8f9865d275\"]}";

   @Test
   public void should_read_processbeskrivning_from_resources()
   {
      var processbeskrivning = ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE);

      assertThat(processbeskrivning.processId()).isEqualTo("test");
      assertThat(processbeskrivning.processnamn()).isEqualTo("Testprocess");
      assertThat(processbeskrivning.steg()).extracting(Processbeskrivning.Steg::id)
            .containsExactly("kontrollera-ratt-till-forsakring", "berakna-ersattning", "bekrafta-beslut");
      assertThat(processbeskrivning.steg().get(0).aktiviteter()).hasSize(2);
      assertThat(processbeskrivning.steg().get(1).beskrivning()).isNull();
   }

   @Test
   public void should_fail_when_processbeskrivning_is_missing()
   {
      assertThatThrownBy(() -> ProcessbeskrivningReader.readResource("saknas.json"))
            .isInstanceOf(ProcessbeskrivningException.class)
            .hasMessageContaining("Processbeskrivning is missing")
            .hasMessageContaining("saknas.json");
   }

   static Stream<Arguments> invalidProcessbeskrivningar()
   {
      return Stream.of(
            Arguments.of("",
                  "No content"),
            Arguments.of("null",
                  "the file is empty"),
            Arguments.of("{",
                  "is invalid"),
            Arguments.of("{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[],\"okant\":1}",
                  "okant"),
            Arguments.of("{\"processnamn\":\"VAH\",\"steg\":[" + STEG_A + "]}",
                  "'processId' is missing"),
            Arguments.of("{\"processId\":\"vah\",\"steg\":[" + STEG_A + "]}",
                  "'processnamn' is missing"),
            Arguments.of("{\"processId\":\"vah\",\"processnamn\":\"VAH\"}",
                  "'steg' must contain at least one steg"),
            Arguments.of("{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[]}",
                  "'steg' must contain at least one steg"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[{\"rubrik\":\"A\",\"aktiviteter\":[\"d4ab4820-68d9-41e0-abe1-cd8f9865d275\"]}]}",
                  "steg[0]: 'id' is missing"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[{\"id\":\"a\",\"aktiviteter\":[\"d4ab4820-68d9-41e0-abe1-cd8f9865d275\"]}]}",
                  "steg[0]: 'rubrik' is missing"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[]}]}",
                  "steg[0]: 'aktiviteter' must contain at least one aktivitets-ID"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[\"inte-ett-uuid\"]}]}",
                  "'inte-ett-uuid' is not a valid UUID"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[" + STEG_A
                        + ",{\"id\":\"a\",\"rubrik\":\"B\",\"aktiviteter\":[\"5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d\"]}]}",
                  "steg[1]: 'id' a is used by more than one steg"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[" + STEG_A
                        + ",{\"id\":\"b\",\"rubrik\":\"B\",\"aktiviteter\":[\"D4AB4820-68D9-41E0-ABE1-CD8F9865D275\"]}]}",
                  "is already used in steg[0]"));
   }

   @ParameterizedTest
   @MethodSource("invalidProcessbeskrivningar")
   public void should_fail_when_processbeskrivning_is_invalid(String json, String expectedMessage)
   {
      var inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      assertThatThrownBy(() -> ProcessbeskrivningReader.read(inputStream, "processbeskrivning.json"))
            .isInstanceOf(ProcessbeskrivningException.class)
            .hasMessageContaining("Processbeskrivning processbeskrivning.json is invalid")
            .hasMessageContaining(expectedMessage);
   }
}

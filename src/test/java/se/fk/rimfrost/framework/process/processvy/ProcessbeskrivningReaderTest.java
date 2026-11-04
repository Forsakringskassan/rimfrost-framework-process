package se.fk.rimfrost.framework.process.processvy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ProcessbeskrivningReaderTest
{
   private static final String STEG_A = "{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[\"d4ab4820-68d9-41e0-abe1-cd8f9865d275\"]}";

   private static final String STEG_B = "{\"id\":\"b\",\"rubrik\":\"B\",\"aktiviteter\":[\"5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d\"]}";

   private static String processbeskrivning(String steg)
   {
      return "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[" + steg + "]}";
   }

   private static String forelder(String id, String understeg)
   {
      return "{\"id\":\"" + id + "\",\"rubrik\":\"" + id + "\",\"steg\":[" + understeg + "]}";
   }

   @Test
   @DisplayName("FPROC-FR-02.1: Processbeskrivningen läses från processens resurser")
   public void should_read_processbeskrivning_from_resources()
   {
      var processbeskrivning = ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE).orElseThrow();

      assertThat(processbeskrivning.processId()).isEqualTo("test");
      assertThat(processbeskrivning.processnamn()).isEqualTo("Testprocess");
      assertThat(processbeskrivning.steg()).extracting(Processbeskrivning.Steg::id)
            .containsExactly("kontrollera-ratt-till-forsakring", "berakna-ersattning", "bekrafta-beslut");
      assertThat(processbeskrivning.steg().get(0).aktiviteter()).hasSize(2);
      assertThat(processbeskrivning.steg().get(1).beskrivning()).isNull();
   }

   @Test
   @DisplayName("FPROC-FR-04.1, FPROC-NFR-02.1: En processbeskrivning med understeg läses med alla nivåer")
   public void should_read_processbeskrivning_with_understeg()
   {
      var processbeskrivning = ProcessbeskrivningReader
            .readResource("processvy/processbeskrivning-med-understeg.json").orElseThrow();

      var forsta = processbeskrivning.steg().get(0);
      assertThat(forsta.harUndersteg()).isTrue();
      assertThat(forsta.aktiviteter()).isNull();
      assertThat(forsta.steg()).extracting(Processbeskrivning.Steg::id)
            .containsExactly("kontrollera-forsakring", "kontrollera-villkor");
      assertThat(forsta.steg().get(1).steg()).extracting(Processbeskrivning.Steg::id)
            .containsExactly("kontrollera-arbetsgivare", "kontrollera-anstallning");
      assertThat(processbeskrivning.steg().get(1).harUndersteg()).isFalse();
      assertThat(processbeskrivning.steg().get(1).steg()).isNull();
      assertThat(processbeskrivning.innerstaSteg()).extracting(Processbeskrivning.Steg::id)
            .containsExactly("kontrollera-forsakring", "kontrollera-arbetsgivare", "kontrollera-anstallning",
                  "berakna-ersattning", "bekrafta-beslut");
   }

   @Test
   @DisplayName("FPROC-FR-04.4: En processbeskrivning utan understeg läses som tidigare")
   public void should_read_processbeskrivning_without_understeg_as_before()
   {
      var processbeskrivning = ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE).orElseThrow();

      assertThat(processbeskrivning.steg()).noneMatch(Processbeskrivning.Steg::harUndersteg);
      assertThat(processbeskrivning.innerstaSteg()).containsExactlyElementsOf(processbeskrivning.steg());
   }

   @Test
   @DisplayName("FPROC-FR-04.1: Understeg två nivåer under det översta steget är tillåtna")
   public void should_accept_understeg_two_levels_below_top_steg()
   {
      var json = processbeskrivning(forelder("fas", forelder("delsteg", STEG_A)));
      var inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      var processbeskrivning = ProcessbeskrivningReader.read(inputStream, "processbeskrivning.json");

      assertThat(processbeskrivning.innerstaSteg()).extracting(Processbeskrivning.Steg::id).containsExactly("a");
   }

   @Test
   @DisplayName("FPROC-FR-02.2: En saknad processbeskrivning är tillåten")
   public void should_return_empty_when_processbeskrivning_is_missing()
   {
      assertThat(ProcessbeskrivningReader.readResource("saknas.json")).isEmpty();
   }

   @Test
   @DisplayName("FPROC-FR-02.2: En ogiltig processbeskrivning i resurserna ger fel")
   public void should_fail_when_processbeskrivning_resource_is_invalid()
   {
      assertThatThrownBy(() -> ProcessbeskrivningReader.readResource("processvy/ogiltig-processbeskrivning.json"))
            .isInstanceOf(ProcessbeskrivningException.class)
            .hasMessageContaining("Processbeskrivning processvy/ogiltig-processbeskrivning.json is invalid")
            .hasMessageContaining("'steg' must contain at least one steg");
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
                  "is already used in steg[0]"),
            Arguments.of("{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[null]}",
                  "steg[0] is empty"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[null]}]}",
                  "aktivitets-ID 'null' is not a valid UUID"),
            Arguments.of(
                  "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[\"1-2-3-4-5\"]}]}",
                  "'1-2-3-4-5' is not a valid UUID"),
            Arguments.of(
                  processbeskrivning(
                        "{\"id\":\"fas\",\"rubrik\":\"Fas\",\"aktiviteter\":[\"8d2f3c1e-1b7a-4c5e-9f0a-2b3c4d5e6f70\"],\"steg\":["
                              + STEG_A + "]}"),
                  "steg[0]: must have either 'aktiviteter' or 'steg', not both"),
            Arguments.of(processbeskrivning(forelder("fas", "")),
                  "steg[0]: 'steg' must contain at least one understeg"),
            Arguments.of(processbeskrivning(STEG_B + "," + forelder("fas", forelder("delsteg", "null"))),
                  "steg[1].steg[0].steg[0] is empty"),
            Arguments.of(processbeskrivning(forelder("fas", forelder("delsteg", forelder("for-djupt", STEG_A)))),
                  "steg[0].steg[0].steg[0]: 'steg' is not allowed, understeg can be at most 2 levels below the top steg"),
            Arguments.of(processbeskrivning(forelder("fas", "{\"id\":\"b\",\"rubrik\":\"B\"}")),
                  "steg[0].steg[0]: 'aktiviteter' must contain at least one aktivitets-ID"),
            Arguments.of(
                  processbeskrivning(
                        forelder("fas", "{\"id\":\"b\",\"aktiviteter\":[\"5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d\"]}")),
                  "steg[0].steg[0]: 'rubrik' is missing"),
            Arguments.of(
                  processbeskrivning(forelder("fas", "{\"id\":\"b\",\"rubrik\":\"B\",\"aktiviteter\":[\"inte-ett-uuid\"]}")),
                  "steg[0].steg[0]: aktivitets-ID 'inte-ett-uuid' is not a valid UUID"),
            Arguments.of(processbeskrivning(STEG_A + "," + forelder("fas", forelder("a", STEG_B))),
                  "steg[1].steg[0]: 'id' a is used by more than one steg"),
            Arguments.of(
                  processbeskrivning(STEG_A + ","
                        + forelder("fas",
                              "{\"id\":\"c\",\"rubrik\":\"C\",\"aktiviteter\":[\"D4AB4820-68D9-41E0-ABE1-CD8F9865D275\"]}")),
                  "steg[1].steg[0]: aktivitets-ID D4AB4820-68D9-41E0-ABE1-CD8F9865D275 is already used in steg[0]"),
            Arguments.of(processbeskrivning(forelder("fas",
                  "{\"id\":\"b\",\"rubrik\":\"B\",\"aktiviteter\":[\"5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d\"],\"okant\":1}")),
                  "okant"));
   }

   @ParameterizedTest
   @MethodSource("invalidProcessbeskrivningar")
   @DisplayName("FPROC-FR-02.2, FPROC-FR-04.2, FPROC-FR-04.3, FPROC-NFR-02.1: En ogiltig processbeskrivning ger fel som anger platsen i trädet")
   public void should_fail_when_processbeskrivning_is_invalid(String json, String expectedMessage)
   {
      var inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      assertThatThrownBy(() -> ProcessbeskrivningReader.read(inputStream, "processbeskrivning.json"))
            .isInstanceOf(ProcessbeskrivningException.class)
            .hasMessageContaining("Processbeskrivning processbeskrivning.json is invalid")
            .hasMessageContaining(expectedMessage);
   }
}

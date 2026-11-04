package se.fk.rimfrost.framework.process.processvy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/**
 * Kontrollerar att schemat för processbeskrivningen och ramverkets kontroll när processen startar inte glider isär:
 * README:ns exempel och testernas processbeskrivningar ska gå igenom båda, och strukturfel ska fångas av schemat.
 */
public class ProcessbeskrivningSchemaTest
{
   private static final String SCHEMA = "schema/processbeskrivning.schema.json";

   private static final List<String> TEST_PROCESSBESKRIVNINGAR = List.of(
         "processbeskrivning.json",
         "processvy/processbeskrivning-med-understeg.json");

   private static final Pattern JSON_BLOCK = Pattern.compile("```json\\r?\\n(.*?)```", Pattern.DOTALL);

   private static final String AKTIVITET_A = "\"d4ab4820-68d9-41e0-abe1-cd8f9865d275\"";

   private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

   private static final JsonSchema SCHEMA_VALIDATOR = loadSchema();

   private static JsonSchema loadSchema()
   {
      try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCHEMA))
      {
         JsonNode schemaNode = OBJECT_MAPPER.readTree(inputStream);
         return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
      }
      catch (IOException e)
      {
         throw new IllegalStateException("Could not read " + SCHEMA, e);
      }
   }

   private static Set<ValidationMessage> validate(String json)
   {
      try
      {
         return SCHEMA_VALIDATOR.validate(OBJECT_MAPPER.readTree(json));
      }
      catch (IOException e)
      {
         throw new IllegalStateException(e);
      }
   }

   private static String processbeskrivning(String steg)
   {
      return "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[" + steg + "]}";
   }

   private static String innersta(String id)
   {
      return "{\"id\":\"" + id + "\",\"rubrik\":\"" + id + "\",\"aktiviteter\":[" + AKTIVITET_A + "]}";
   }

   private static String forelder(String id, String understeg)
   {
      return "{\"id\":\"" + id + "\",\"rubrik\":\"" + id + "\",\"steg\":[" + understeg + "]}";
   }

   static Stream<Arguments> giltigaProcessbeskrivningar() throws IOException
   {
      List<Arguments> arguments = new ArrayList<>();
      for (String resource : TEST_PROCESSBESKRIVNINGAR)
      {
         try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
         {
            arguments.add(Arguments.of(resource, new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)));
         }
      }
      Matcher matcher = JSON_BLOCK.matcher(Files.readString(Path.of("README.md"), StandardCharsets.UTF_8));
      int exempel = 0;
      while (matcher.find())
      {
         arguments.add(Arguments.of("README.md exempel " + ++exempel, matcher.group(1)));
      }
      arguments.add(Arguments.of("platt", processbeskrivning(innersta("a"))));
      arguments.add(Arguments.of("understeg i två nivåer", processbeskrivning(forelder("fas", forelder("delsteg",
            innersta("a"))))));
      arguments.add(Arguments.of("aktivitets-ID med versaler", processbeskrivning(
            "{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[\"D4AB4820-68D9-41E0-ABE1-CD8F9865D275\"]}")));
      arguments.add(Arguments.of("aktivitets-ID med blanksteg runt", processbeskrivning(
            "{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[\" d4ab4820-68d9-41e0-abe1-cd8f9865d275 \"]}")));
      arguments.add(Arguments.of("fält med null", processbeskrivning(
            "{\"id\":\"fas\",\"rubrik\":\"Fas\",\"beskrivning\":null,\"aktiviteter\":null,\"steg\":["
                  + "{\"id\":\"a\",\"rubrik\":\"A\",\"beskrivning\":null,\"steg\":null,\"aktiviteter\":["
                  + AKTIVITET_A + "]}]}")));
      return arguments.stream();
   }

   @Test
   @DisplayName("FPROC-NFR-02.2: README:n innehåller exempel på processbeskrivningar att validera")
   public void readme_should_contain_processbeskrivning_examples() throws IOException
   {
      assertThat(giltigaProcessbeskrivningar().filter(a -> a.get()[0].toString().startsWith("README.md")))
            .hasSizeGreaterThanOrEqualTo(2);
   }

   @ParameterizedTest(name = "{0}")
   @MethodSource("giltigaProcessbeskrivningar")
   @DisplayName("FPROC-FR-06.3, FPROC-NFR-02.2: README:ns och testernas processbeskrivningar är giltiga enligt både schemat och ramverket")
   public void processbeskrivning_should_be_valid_according_to_schema_and_framework(String source, String json)
   {
      assertThat(validate(json)).as(source).isEmpty();
      ProcessbeskrivningReader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), source);
   }

   static Stream<Arguments> ogiltigaProcessbeskrivningar()
   {
      return Stream.of(
            Arguments.of("tom steg-lista", "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":[]}"),
            Arguments.of("processId saknas", "{\"processnamn\":\"VAH\",\"steg\":[" + innersta("a") + "]}"),
            Arguments.of("tom processnamn", "{\"processId\":\"vah\",\"processnamn\":\" \",\"steg\":["
                  + innersta("a") + "]}"),
            Arguments.of("okänt fält på toppnivån", "{\"processId\":\"vah\",\"processnamn\":\"VAH\",\"steg\":["
                  + innersta("a") + "],\"okant\":1}"),
            Arguments.of("id saknas", processbeskrivning("{\"rubrik\":\"A\",\"aktiviteter\":[" + AKTIVITET_A + "]}")),
            Arguments.of("rubrik saknas", processbeskrivning("{\"id\":\"a\",\"aktiviteter\":[" + AKTIVITET_A + "]}")),
            Arguments.of("varken aktiviteter eller steg", processbeskrivning("{\"id\":\"a\",\"rubrik\":\"A\"}")),
            Arguments.of("aktiviteter är null utan steg",
                  processbeskrivning("{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":null}")),
            Arguments.of("tom aktiviteter", processbeskrivning("{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[]}")),
            Arguments.of("aktivitets-ID är inte ett UUID",
                  processbeskrivning("{\"id\":\"a\",\"rubrik\":\"A\",\"aktiviteter\":[\"inte-ett-uuid\"]}")),
            Arguments.of("både aktiviteter och steg", processbeskrivning(
                  "{\"id\":\"fas\",\"rubrik\":\"Fas\",\"aktiviteter\":[" + AKTIVITET_A + "],\"steg\":["
                        + innersta("a") + "]}")),
            Arguments.of("tom steg i ett steg", processbeskrivning(forelder("fas", ""))),
            Arguments.of("tom steg i ett understeg", processbeskrivning(forelder("fas", forelder("delsteg", "")))),
            Arguments.of("fyra nivåer",
                  processbeskrivning(forelder("fas", forelder("delsteg", forelder("for-djupt", innersta("a")))))),
            Arguments.of("okänt fält i ett understeg", processbeskrivning(forelder("fas",
                  "{\"id\":\"b\",\"rubrik\":\"B\",\"aktiviteter\":[" + AKTIVITET_A + "],\"okant\":1}"))),
            Arguments.of("aktivitets-ID är inte ett UUID på tredje nivån", processbeskrivning(forelder("fas",
                  forelder("delsteg", "{\"id\":\"c\",\"rubrik\":\"C\",\"aktiviteter\":[\"1-2-3-4-5\"]}")))));
   }

   @ParameterizedTest(name = "{0}")
   @MethodSource("ogiltigaProcessbeskrivningar")
   @DisplayName("FPROC-FR-06.1: Schemat underkänner saknade fält, okända fält, ogiltiga UUID, både aktiviteter och steg och för många nivåer")
   public void processbeskrivning_should_be_invalid_according_to_schema(String fall, String json)
   {
      assertThat(validate(json)).as(fall).isNotEmpty();
   }

   @Test
   @DisplayName("FPROC-FR-06.1: Schemat godkänner tre nivåer")
   public void schema_should_accept_three_levels()
   {
      String json = processbeskrivning(forelder("fas", forelder("delsteg", innersta("a"))));

      assertThat(validate(json)).isEmpty();
   }
}

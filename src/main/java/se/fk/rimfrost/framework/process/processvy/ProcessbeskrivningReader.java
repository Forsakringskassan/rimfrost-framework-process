package se.fk.rimfrost.framework.process.processvy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Läser och validerar en processbeskrivning. Används av {@link ProcessvyService} när processen startar och av
 * testhjälpmedlet som kontrollerar att BPMN och processbeskrivning stämmer överens.
 */
public final class ProcessbeskrivningReader
{
   public static final String RESOURCE = "processbeskrivning.json";

   private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
         .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

   private ProcessbeskrivningReader()
   {
   }

   public static Processbeskrivning readResource(String resource)
   {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      try (InputStream inputStream = classLoader.getResourceAsStream(resource))
      {
         if (inputStream == null)
         {
            throw new ProcessbeskrivningException("Processbeskrivning is missing: could not find " + resource
                  + " in the process resources (src/main/resources)");
         }
         return read(inputStream, resource);
      }
      catch (IOException e)
      {
         throw new ProcessbeskrivningException("Processbeskrivning " + resource + " could not be read", e);
      }
   }

   public static Processbeskrivning read(InputStream inputStream, String source)
   {
      Processbeskrivning processbeskrivning;
      try
      {
         processbeskrivning = OBJECT_MAPPER.readValue(inputStream, Processbeskrivning.class);
      }
      catch (IOException | RuntimeException e)
      {
         throw new ProcessbeskrivningException("Processbeskrivning " + source + " is invalid: " + e.getMessage(), e);
      }

      List<String> errors = validate(processbeskrivning);
      if (!errors.isEmpty())
      {
         throw new ProcessbeskrivningException(
               "Processbeskrivning " + source + " is invalid: " + String.join("; ", errors));
      }
      return processbeskrivning;
   }

   private static List<String> validate(Processbeskrivning processbeskrivning)
   {
      List<String> errors = new ArrayList<>();
      if (processbeskrivning == null)
      {
         errors.add("the file is empty");
         return errors;
      }
      if (isBlank(processbeskrivning.processId()))
      {
         errors.add("'processId' is missing");
      }
      if (isBlank(processbeskrivning.processnamn()))
      {
         errors.add("'processnamn' is missing");
      }
      if (processbeskrivning.steg() == null || processbeskrivning.steg().isEmpty())
      {
         errors.add("'steg' must contain at least one steg");
         return errors;
      }

      Set<String> stegIds = new HashSet<>();
      Map<UUID, String> stegRefPerAktivitet = new HashMap<>();
      for (int i = 0; i < processbeskrivning.steg().size(); i++)
      {
         Processbeskrivning.Steg steg = processbeskrivning.steg().get(i);
         String stegRef = "steg[" + i + "]";
         if (steg == null)
         {
            errors.add(stegRef + " is empty");
            continue;
         }
         if (isBlank(steg.id()))
         {
            errors.add(stegRef + ": 'id' is missing");
         }
         else if (!stegIds.add(steg.id()))
         {
            errors.add(stegRef + ": 'id' " + steg.id() + " is used by more than one steg");
         }
         if (isBlank(steg.rubrik()))
         {
            errors.add(stegRef + ": 'rubrik' is missing");
         }
         if (steg.aktiviteter() == null || steg.aktiviteter().isEmpty())
         {
            errors.add(stegRef + ": 'aktiviteter' must contain at least one aktivitets-ID");
            continue;
         }
         for (String aktivitet : steg.aktiviteter())
         {
            UUID aktivitetId = parseUuid(aktivitet);
            if (aktivitetId == null)
            {
               errors.add(stegRef + ": aktivitets-ID '" + aktivitet + "' is not a valid UUID");
            }
            else if (stegRefPerAktivitet.putIfAbsent(aktivitetId, stegRef) != null)
            {
               errors.add(stegRef + ": aktivitets-ID " + aktivitet + " is already used in "
                     + stegRefPerAktivitet.get(aktivitetId));
            }
         }
      }
      return errors;
   }

   public static UUID parseUuid(String value)
   {
      if (value == null)
      {
         return null;
      }
      try
      {
         UUID uuid = UUID.fromString(value.trim());
         return uuid.toString().equalsIgnoreCase(value.trim()) ? uuid : null;
      }
      catch (IllegalArgumentException e)
      {
         return null;
      }
   }

   private static boolean isBlank(String value)
   {
      return value == null || value.isBlank();
   }
}

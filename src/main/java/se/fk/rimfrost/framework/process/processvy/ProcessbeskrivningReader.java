package se.fk.rimfrost.framework.process.processvy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

   /**
    * Hur många nivåer av understeg som får finnas under det översta steget.
    */
   static final int MAX_UNDERSTEG_NIVA = 2;

   private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
         .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

   private ProcessbeskrivningReader()
   {
   }

   /**
    * Läser processbeskrivningen från resursen. Saknas resursen returneras {@link Optional#empty()}, eftersom
    * processbeskrivningen är valfri. Finns den men är ogiltig kastas {@link ProcessbeskrivningException}.
    */
   public static Optional<Processbeskrivning> readResource(String resource)
   {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      try (InputStream inputStream = classLoader.getResourceAsStream(resource))
      {
         if (inputStream == null)
         {
            return Optional.empty();
         }
         return Optional.of(read(inputStream, resource));
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

      validateSteg(processbeskrivning.steg(), "", 0, new HashSet<>(), new HashMap<>(), errors);
      return errors;
   }

   /**
    * Validerar stegen på en nivå och deras understeg. Id och aktivitets-ID:n ska vara unika i hela trädet, så
    * {@code stegIds} och {@code stegRefPerAktivitet} delas mellan nivåerna. Felen anger stegets plats i trädet, till
    * exempel {@code steg[0].steg[1]}.
    */
   private static void validateSteg(List<Processbeskrivning.Steg> stegLista, String parentRef, int niva,
         Set<String> stegIds, Map<UUID, String> stegRefPerAktivitet, List<String> errors)
   {
      for (int i = 0; i < stegLista.size(); i++)
      {
         Processbeskrivning.Steg steg = stegLista.get(i);
         String stegRef = parentRef + "steg[" + i + "]";
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
         if (steg.harUndersteg())
         {
            validateUndersteg(steg, stegRef, niva, stegIds, stegRefPerAktivitet, errors);
            continue;
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
   }

   private static void validateUndersteg(Processbeskrivning.Steg steg, String stegRef, int niva,
         Set<String> stegIds, Map<UUID, String> stegRefPerAktivitet, List<String> errors)
   {
      if (steg.aktiviteter() != null)
      {
         errors.add(stegRef + ": must have either 'aktiviteter' or 'steg', not both");
      }
      if (steg.steg().isEmpty())
      {
         errors.add(stegRef + ": 'steg' must contain at least one understeg");
         return;
      }
      if (niva >= MAX_UNDERSTEG_NIVA)
      {
         errors.add(stegRef + ": 'steg' is not allowed, understeg can be at most " + MAX_UNDERSTEG_NIVA
               + " levels below the top steg");
         return;
      }
      validateSteg(steg.steg(), stegRef + ".", niva + 1, stegIds, stegRefPerAktivitet, errors);
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

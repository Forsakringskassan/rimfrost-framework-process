package se.fk.rimfrost.framework.process.processvy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Processens processbeskrivning: processens steg i ordning och vilka aktiviteter som hör till varje steg.
 * Läses från {@value ProcessbeskrivningReader#RESOURCE} i processens resurser.
 */
public final class Processbeskrivning
{
   private final String processId;
   private final String processnamn;
   private final List<Steg> steg;

   @JsonCreator
   public Processbeskrivning(@JsonProperty("processId") String processId,
         @JsonProperty("processnamn") String processnamn,
         @JsonProperty("steg") List<Steg> steg)
   {
      this.processId = processId;
      this.processnamn = processnamn;
      this.steg = steg == null ? null : List.copyOf(steg);
   }

   public String processId()
   {
      return processId;
   }

   public String processnamn()
   {
      return processnamn;
   }

   @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Listan är oföränderlig (List.copyOf)")
   public List<Steg> steg()
   {
      return steg;
   }

   public static final class Steg
   {
      private final String id;
      private final String rubrik;
      private final String beskrivning;
      private final List<String> aktiviteter;

      @JsonCreator
      public Steg(@JsonProperty("id") String id,
            @JsonProperty("rubrik") String rubrik,
            @JsonProperty("beskrivning") String beskrivning,
            @JsonProperty("aktiviteter") List<String> aktiviteter)
      {
         this.id = id;
         this.rubrik = rubrik;
         this.beskrivning = beskrivning;
         this.aktiviteter = aktiviteter == null ? null : List.copyOf(aktiviteter);
      }

      public String id()
      {
         return id;
      }

      public String rubrik()
      {
         return rubrik;
      }

      public String beskrivning()
      {
         return beskrivning;
      }

      public List<String> aktiviteter()
      {
         return aktiviteter;
      }
   }
}

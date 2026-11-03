package se.fk.rimfrost.framework.process.processvy;

import java.util.ArrayList;
import java.util.Collections;
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
      this.steg = steg == null ? null : Collections.unmodifiableList(new ArrayList<>(steg));
   }

   public String processId()
   {
      return processId;
   }

   public String processnamn()
   {
      return processnamn;
   }

   @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Listan är oföränderlig")
   public List<Steg> steg()
   {
      return steg;
   }

   /**
    * De innersta stegen, det vill säga stegen som har aktiviteter, i den ordning de läses genom hela trädet.
    */
   public List<Steg> innerstaSteg()
   {
      List<Steg> innersta = new ArrayList<>();
      addInnerstaSteg(steg, innersta);
      return innersta;
   }

   private static void addInnerstaSteg(List<Steg> steg, List<Steg> innersta)
   {
      for (Steg s : steg)
      {
         if (s.harUndersteg())
         {
            addInnerstaSteg(s.steg(), innersta);
         }
         else
         {
            innersta.add(s);
         }
      }
   }

   /**
    * Ett steg i processbeskrivningen. Ett steg har antingen aktiviteter eller understeg ({@link #steg()}), och ett
    * understeg har samma form som ett steg.
    */
   public static final class Steg
   {
      private final String id;
      private final String rubrik;
      private final String beskrivning;
      private final List<String> aktiviteter;
      private final List<Steg> steg;

      @JsonCreator
      public Steg(@JsonProperty("id") String id,
            @JsonProperty("rubrik") String rubrik,
            @JsonProperty("beskrivning") String beskrivning,
            @JsonProperty("aktiviteter") List<String> aktiviteter,
            @JsonProperty("steg") List<Steg> steg)
      {
         this.id = id;
         this.rubrik = rubrik;
         this.beskrivning = beskrivning;
         this.aktiviteter = aktiviteter == null ? null : Collections.unmodifiableList(new ArrayList<>(aktiviteter));
         this.steg = steg == null ? null : Collections.unmodifiableList(new ArrayList<>(steg));
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

      public List<Steg> steg()
      {
         return steg;
      }

      public boolean harUndersteg()
      {
         return steg != null;
      }
   }
}

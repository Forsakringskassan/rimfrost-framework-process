package se.fk.rimfrost.framework.process.processvy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.GetProcessvyStegResponse;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.ProcessSteg;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.StegStatus;

/**
 * Håller processens processvy-länk och processbeskrivning. Båda är valfria: saknas länken skickas regelanropen utan
 * {@code processvyUrl}, och saknas processbeskrivningen svarar {@link #getSteg(UUID)} tomt. En processbeskrivning som
 * finns men är ogiltig gör att processen inte startar.
 */
@Startup
@ApplicationScoped
public class ProcessvyService
{
   public static final String PROCESSVY_URL_PROPERTY = "processvy.url";

   private static final Logger LOGGER = LoggerFactory.getLogger(ProcessvyService.class);

   @ConfigProperty(name = PROCESSVY_URL_PROPERTY)
   Optional<String> processvyUrlConfig;

   private Optional<String> processvyUrl;

   private Optional<Processbeskrivning> processbeskrivning;

   private Map<UUID, Integer> stegIndexPerAktivitet;

   private Map<String, Integer> indexPerInnerstaSteg;

   @PostConstruct
   void init()
   {
      init(processvyUrlConfig, ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE));
   }

   void init(Optional<String> processvyUrlConfig, Optional<Processbeskrivning> processbeskrivning)
   {
      this.processvyUrl = processvyUrlConfig.filter(url -> !url.isBlank());
      this.processbeskrivning = processbeskrivning;
      this.stegIndexPerAktivitet = new HashMap<>();
      this.indexPerInnerstaSteg = new HashMap<>();
      processbeskrivning.ifPresent(beskrivning -> {
         List<Processbeskrivning.Steg> innersta = beskrivning.innerstaSteg();
         for (int i = 0; i < innersta.size(); i++)
         {
            indexPerInnerstaSteg.put(innersta.get(i).id(), i);
            for (String aktivitet : innersta.get(i).aktiviteter())
            {
               stegIndexPerAktivitet.put(ProcessbeskrivningReader.parseUuid(aktivitet), i);
            }
         }
      });

      if (this.processvyUrl.isPresent())
      {
         LOGGER.info("Processvy-länk is configured: {}", this.processvyUrl.get());
      }
      else
      {
         LOGGER.info("Processvy-länk is not configured ({} is not set): regel requests are sent without processvyUrl",
               PROCESSVY_URL_PROPERTY);
      }
      if (processbeskrivning.isPresent())
      {
         LOGGER.info("Loaded processbeskrivning for process {} with {} steg ({} innersta steg)",
               processbeskrivning.get().processId(), processbeskrivning.get().steg().size(),
               indexPerInnerstaSteg.size());
      }
      else
      {
         LOGGER.info("No processbeskrivning ({} not found in resources): GET /processvy/steg answers 404",
               ProcessbeskrivningReader.RESOURCE);
      }
   }

   public Optional<String> getProcessvyUrl()
   {
      return processvyUrl;
   }

   public Optional<GetProcessvyStegResponse> getSteg(UUID aktivitetId)
   {
      Integer aktivtStegIndex = stegIndexPerAktivitet.get(aktivitetId);
      if (aktivtStegIndex == null || processbeskrivning.isEmpty())
      {
         return Optional.empty();
      }

      Processbeskrivning beskrivning = processbeskrivning.get();
      return Optional.of(new GetProcessvyStegResponse()
            .processId(beskrivning.processId())
            .processnamn(beskrivning.processnamn())
            .steg(toProcessSteg(beskrivning.steg(), aktivtStegIndex)));
   }

   /**
    * De innersta stegen får status efter sin plats i förhållande till det innersta steget med aktiviteten. Ett steg
    * med understeg är PAGAR om något understeg pågår, KLAR om alla är klara och annars KOMMANDE. Fältet {@code steg}
    * sätts bara för steg som har understeg.
    */
   private List<ProcessSteg> toProcessSteg(List<Processbeskrivning.Steg> steg, int aktivtStegIndex)
   {
      List<ProcessSteg> processSteg = new ArrayList<>();
      for (Processbeskrivning.Steg s : steg)
      {
         ProcessSteg ps = new ProcessSteg()
               .id(s.id())
               .rubrik(s.rubrik())
               .beskrivning(s.beskrivning());
         if (s.harUndersteg())
         {
            List<ProcessSteg> understeg = toProcessSteg(s.steg(), aktivtStegIndex);
            ps.steg(understeg).status(toStatus(understeg));
         }
         else
         {
            Integer stegIndex = indexPerInnerstaSteg.get(s.id());
            if (stegIndex == null)
            {
               throw new IllegalStateException("Leaf steg id not in index: " + s.id());
            }
            ps.status(toStatus(stegIndex, aktivtStegIndex));
         }
         processSteg.add(ps);
      }
      return processSteg;
   }

   private static StegStatus toStatus(List<ProcessSteg> understeg)
   {
      if (understeg.stream().anyMatch(s -> s.getStatus() == StegStatus.PAGAR))
      {
         return StegStatus.PAGAR;
      }
      return understeg.stream().allMatch(s -> s.getStatus() == StegStatus.KLAR) ? StegStatus.KLAR
            : StegStatus.KOMMANDE;
   }

   private static StegStatus toStatus(int stegIndex, int aktivtStegIndex)
   {
      if (stegIndex < aktivtStegIndex)
      {
         return StegStatus.KLAR;
      }
      return stegIndex == aktivtStegIndex ? StegStatus.PAGAR : StegStatus.KOMMANDE;
   }
}

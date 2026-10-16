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
 * Håller processens processvy-länk och processbeskrivning. Båda läses och valideras när processen startar, så att en
 * process utan länk eller med en saknad eller ogiltig processbeskrivning inte startar.
 */
@Startup
@ApplicationScoped
public class ProcessvyService
{
   public static final String PROCESSVY_URL_PROPERTY = "PROCESSVY_URL";

   private static final Logger LOGGER = LoggerFactory.getLogger(ProcessvyService.class);

   @ConfigProperty(name = PROCESSVY_URL_PROPERTY)
   Optional<String> processvyUrlConfig;

   private String processvyUrl;

   private Processbeskrivning processbeskrivning;

   private Map<UUID, Integer> stegIndexPerAktivitet;

   @PostConstruct
   void init()
   {
      init(processvyUrlConfig, ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE));
   }

   void init(Optional<String> processvyUrlConfig, Processbeskrivning processbeskrivning)
   {
      this.processvyUrl = processvyUrlConfig
            .filter(url -> !url.isBlank())
            .orElseThrow(() -> new IllegalStateException("Processvy-länk is missing: set " + PROCESSVY_URL_PROPERTY
                  + " in the process application.properties, for example " + PROCESSVY_URL_PROPERTY + "=/process/vah"));
      this.processbeskrivning = processbeskrivning;
      this.stegIndexPerAktivitet = new HashMap<>();
      List<Processbeskrivning.Steg> steg = processbeskrivning.steg();
      for (int i = 0; i < steg.size(); i++)
      {
         for (String aktivitet : steg.get(i).aktiviteter())
         {
            stegIndexPerAktivitet.put(ProcessbeskrivningReader.parseUuid(aktivitet), i);
         }
      }
      LOGGER.info("Loaded processbeskrivning for process {} with {} steg, processvyUrl: {}",
            processbeskrivning.processId(), steg.size(), processvyUrl);
   }

   public String getProcessvyUrl()
   {
      return processvyUrl;
   }

   public Optional<GetProcessvyStegResponse> getSteg(UUID aktivitetId)
   {
      Integer aktivtStegIndex = stegIndexPerAktivitet.get(aktivitetId);
      if (aktivtStegIndex == null)
      {
         return Optional.empty();
      }

      List<ProcessSteg> processSteg = new ArrayList<>();
      List<Processbeskrivning.Steg> steg = processbeskrivning.steg();
      for (int i = 0; i < steg.size(); i++)
      {
         processSteg.add(new ProcessSteg()
               .id(steg.get(i).id())
               .rubrik(steg.get(i).rubrik())
               .beskrivning(steg.get(i).beskrivning())
               .status(toStatus(i, aktivtStegIndex)));
      }
      return Optional.of(new GetProcessvyStegResponse()
            .processId(processbeskrivning.processId())
            .processnamn(processbeskrivning.processnamn())
            .steg(processSteg));
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

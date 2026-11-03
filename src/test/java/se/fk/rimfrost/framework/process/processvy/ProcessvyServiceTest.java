package se.fk.rimfrost.framework.process.processvy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.ProcessSteg;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.StegStatus;

public class ProcessvyServiceTest
{
   private ProcessvyService processvyService;

   @BeforeEach
   public void setUp()
   {
      processvyService = new ProcessvyService();
      processvyService.init(Optional.of("/process/test"),
            ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE));
   }

   @Test
   public void should_return_processvy_url_from_config()
   {
      assertThat(processvyService.getProcessvyUrl()).contains("/process/test");
   }

   @ParameterizedTest
   @CsvSource(
   {
         "8d2f3c1e-1b7a-4c5e-9f0a-2b3c4d5e6f70, PAGAR, KOMMANDE, KOMMANDE",
         "1c9e7a54-3d2b-4f6a-8e1c-0a9b8c7d6e5f, PAGAR, KOMMANDE, KOMMANDE",
         "5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d, KLAR, PAGAR, KOMMANDE",
         "d4ab4820-68d9-41e0-abe1-cd8f9865d275, KLAR, KLAR, PAGAR"
   })
   public void should_return_steg_with_status_relative_to_aktivitet(UUID aktivitetId, StegStatus forsta,
         StegStatus andra, StegStatus tredje)
   {
      var response = processvyService.getSteg(aktivitetId).orElseThrow();

      assertThat(response.getProcessId()).isEqualTo("test");
      assertThat(response.getProcessnamn()).isEqualTo("Testprocess");
      assertThat(response.getSteg()).extracting(ProcessSteg::getStatus).containsExactly(forsta, andra, tredje);
      assertThat(response.getSteg()).extracting(ProcessSteg::getId)
            .containsExactly("kontrollera-ratt-till-forsakring", "berakna-ersattning", "bekrafta-beslut");
      assertThat(response.getSteg().get(0).getRubrik()).isEqualTo("Kontrollera rätt till försäkring");
      assertThat(response.getSteg().get(1).getBeskrivning()).isNull();
      assertThat(response.getSteg()).extracting(ProcessSteg::getSteg).containsOnlyNulls();
   }

   /**
    * Processbeskrivningen med understeg har två steg med understeg, varav det andra ligger på andra nivån. Statusarna
    * listas i trädets ordning: kontrollera-ratt-till-forsakring, kontrollera-forsakring, kontrollera-villkor,
    * kontrollera-arbetsgivare, kontrollera-anstallning, berakna-ersattning, bekrafta-beslut.
    */
   @ParameterizedTest
   @CsvSource(
   {
         // först i ett understeg
         "8d2f3c1e-1b7a-4c5e-9f0a-2b3c4d5e6f70, PAGAR, PAGAR, KOMMANDE, KOMMANDE, KOMMANDE, KOMMANDE, KOMMANDE",
         // först på andra nivån
         "1c9e7a54-3d2b-4f6a-8e1c-0a9b8c7d6e5f, PAGAR, KLAR, PAGAR, PAGAR, KOMMANDE, KOMMANDE, KOMMANDE",
         // sist på andra nivån och sist i det översta steget
         "2b3c4d5e-6f70-4a1b-9c2d-3e4f5a6b7c8e, PAGAR, KLAR, PAGAR, KLAR, PAGAR, KOMMANDE, KOMMANDE",
         // efter alla understeg
         "5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d, KLAR, KLAR, KLAR, KLAR, KLAR, PAGAR, KOMMANDE",
         "d4ab4820-68d9-41e0-abe1-cd8f9865d275, KLAR, KLAR, KLAR, KLAR, KLAR, KLAR, PAGAR"
   })
   public void should_return_understeg_with_status_on_every_level(UUID aktivitetId, StegStatus fas,
         StegStatus forsakring, StegStatus villkor, StegStatus arbetsgivare, StegStatus anstallning,
         StegStatus berakna, StegStatus bekrafta)
   {
      var service = new ProcessvyService();
      service.init(Optional.empty(),
            ProcessbeskrivningReader.readResource("processvy/processbeskrivning-med-understeg.json"));

      var response = service.getSteg(aktivitetId).orElseThrow();

      List<ProcessSteg> trad = flatten(response.getSteg());
      assertThat(trad).extracting(ProcessSteg::getId).containsExactly("kontrollera-ratt-till-forsakring",
            "kontrollera-forsakring", "kontrollera-villkor", "kontrollera-arbetsgivare", "kontrollera-anstallning",
            "berakna-ersattning", "bekrafta-beslut");
      assertThat(trad).extracting(ProcessSteg::getStatus)
            .containsExactly(fas, forsakring, villkor, arbetsgivare, anstallning, berakna, bekrafta);
   }

   @Test
   public void should_return_steg_only_for_steg_with_understeg()
   {
      var service = new ProcessvyService();
      service.init(Optional.empty(),
            ProcessbeskrivningReader.readResource("processvy/processbeskrivning-med-understeg.json"));

      var steg = service.getSteg(UUID.fromString("5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d")).orElseThrow().getSteg();

      assertThat(steg).hasSize(3);
      assertThat(steg.get(0).getSteg()).hasSize(2);
      assertThat(steg.get(0).getBeskrivning())
            .isEqualTo("Handläggaren kontrollerar att den försäkrade har rätt till ersättning.");
      assertThat(steg.get(0).getSteg().get(0).getSteg()).isNull();
      assertThat(steg.get(0).getSteg().get(1).getSteg()).extracting(ProcessSteg::getSteg).containsOnlyNulls();
      assertThat(steg.get(1).getSteg()).isNull();
      assertThat(steg.get(2).getSteg()).isNull();
   }

   private static List<ProcessSteg> flatten(List<ProcessSteg> steg)
   {
      List<ProcessSteg> trad = new ArrayList<>();
      for (ProcessSteg s : steg)
      {
         trad.add(s);
         if (s.getSteg() != null)
         {
            trad.addAll(flatten(s.getSteg()));
         }
      }
      return trad;
   }

   @Test
   public void should_return_empty_for_unknown_aktivitet()
   {
      assertThat(processvyService.getSteg(UUID.randomUUID())).isEmpty();
   }

   @ParameterizedTest
   @ValueSource(strings =
   {
         "", " "
   })
   public void should_start_without_processvy_url_when_blank(String processvyUrl)
   {
      var service = new ProcessvyService();
      service.init(Optional.of(processvyUrl),
            ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE));

      assertThat(service.getProcessvyUrl()).isEmpty();
   }

   @Test
   public void should_start_without_processvy_url_when_missing()
   {
      var service = new ProcessvyService();
      service.init(Optional.empty(), ProcessbeskrivningReader.readResource(ProcessbeskrivningReader.RESOURCE));

      assertThat(service.getProcessvyUrl()).isEmpty();
      assertThat(service.getSteg(UUID.fromString("5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d"))).isPresent();
   }

   @Test
   public void should_start_without_processbeskrivning_and_return_empty_steg()
   {
      var service = new ProcessvyService();
      service.init(Optional.of("/process/test"), Optional.empty());

      assertThat(service.getProcessvyUrl()).contains("/process/test");
      assertThat(service.getSteg(UUID.fromString("5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d"))).isEmpty();
   }

   @Test
   public void should_start_without_processvy_url_and_processbeskrivning()
   {
      var service = new ProcessvyService();
      service.init(Optional.empty(), Optional.empty());

      assertThat(service.getProcessvyUrl()).isEmpty();
      assertThat(service.getSteg(UUID.randomUUID())).isEmpty();
   }
}

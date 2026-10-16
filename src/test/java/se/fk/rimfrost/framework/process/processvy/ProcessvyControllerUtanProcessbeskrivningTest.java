package se.fk.rimfrost.framework.process.processvy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.ErrorResponse;

public class ProcessvyControllerUtanProcessbeskrivningTest
{
   @Test
   public void should_return_404_when_process_has_no_processbeskrivning()
   {
      var processvyService = new ProcessvyService();
      processvyService.init(Optional.of("/process/test"), Optional.empty());
      var controller = new ProcessvyController();
      controller.processvyService = processvyService;

      var aktivitetId = UUID.fromString("5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d");
      assertThatThrownBy(() -> controller.getProcessvySteg(aktivitetId))
            .isInstanceOf(ProcessvyController.AktivitetSaknasException.class);

      Response response = controller.mapAktivitetSaknas(new ProcessvyController.AktivitetSaknasException());
      assertThat(response.getStatus()).isEqualTo(404);
      assertThat(((ErrorResponse) response.getEntity()).getMessage()).isEqualTo(ProcessvyController.AKTIVITET_SAKNAS);
   }
}

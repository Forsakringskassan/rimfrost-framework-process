package se.fk.rimfrost.framework.process.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ProcessbeskrivningVerifierTest
{
   @Test
   public void should_read_all_aktivitet_ids_in_bpmn()
   {
      assertThat(ProcessbeskrivningVerifier.readAktivitetIds("processvy/test.bpmn")).containsExactly(
            "8d2f3c1e-1b7a-4c5e-9f0a-2b3c4d5e6f70",
            "5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d",
            "D4AB4820-68D9-41E0-ABE1-CD8F9865D275");
   }

   @Test
   public void should_pass_when_all_aktiviteter_are_in_processbeskrivning()
   {
      assertThatCode(() -> ProcessbeskrivningVerifier.assertAllAktiviteterInProcessbeskrivning("processvy/test.bpmn"))
            .doesNotThrowAnyException();
   }

   @Test
   public void should_fail_when_aktivitet_is_missing_in_processbeskrivning()
   {
      assertThatThrownBy(
            () -> ProcessbeskrivningVerifier.assertAllAktiviteterInProcessbeskrivning("processvy/okand-aktivitet.bpmn"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("missing in processbeskrivning.json")
            .hasMessageContaining("0f0f0f0f-0000-4000-8000-000000000001")
            .hasMessageNotContaining("8d2f3c1e-1b7a-4c5e-9f0a-2b3c4d5e6f70");
   }

   @Test
   public void should_fail_when_bpmn_has_no_aktiviteter()
   {
      assertThatThrownBy(
            () -> ProcessbeskrivningVerifier.assertAllAktiviteterInProcessbeskrivning("processvy/utan-aktivitet.bpmn"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Found no aktivitets-ID");
   }

   @Test
   public void should_fail_when_aktivitet_is_not_a_fixed_value()
   {
      assertThatThrownBy(() -> ProcessbeskrivningVerifier.readAktivitetIds("processvy/aktivitet-fran-variabel.bpmn"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("must be assigned a fixed aktivitets-ID")
            .hasMessageContaining("_IN_AKTIVITET_1")
            .hasMessageContaining("_IN_AKTIVITET_2");
   }

   @Test
   public void should_fail_when_process_id_differs_from_processbeskrivning()
   {
      assertThatThrownBy(
            () -> ProcessbeskrivningVerifier.assertAllAktiviteterInProcessbeskrivning("processvy/annan-process.bpmn"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("processId in processbeskrivning.json is test")
            .hasMessageContaining("is annan");
   }

   @Test
   public void should_read_process_id_in_bpmn()
   {
      assertThat(ProcessbeskrivningVerifier.readProcessId("processvy/test.bpmn")).isEqualTo("test");
   }

   @Test
   public void should_fail_when_bpmn_is_missing()
   {
      assertThatThrownBy(() -> ProcessbeskrivningVerifier.readAktivitetIds("processvy/saknas.bpmn"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Could not find processvy/saknas.bpmn");
   }
}

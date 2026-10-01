package br.gov.pmps.processoseletivo.domain.model.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SelectionProcessTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant START = NOW.plus(Duration.ofDays(1));
    private static final Instant END = NOW.plus(Duration.ofDays(10));

    @Test
    void newProcessStartsAsDraft() {
        assertThat(draft().getStatus()).isEqualTo(ProcessStatus.RASCUNHO);
    }

    @Test
    void rejectsPeriodEndingBeforeStart() {
        assertThatThrownBy(() -> new SelectionProcess(details(END, START), NOW))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void rejectsDuplicatePositionNameIgnoringCase() {
        SelectionProcess process = draft();
        process.addPosition("Auxiliar Administrativo", 10, NOW);

        assertThatThrownBy(() -> process.addPosition("auxiliar administrativo", 5, NOW))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void publishRequiresPositionNoticeAndFutureEnd() {
        SelectionProcess withoutPosition = draft();
        assertThatThrownBy(() -> withoutPosition.publish(true, NOW)).hasMessageContaining("cargo");

        SelectionProcess withoutNotice = draftWithPosition();
        assertThatThrownBy(() -> withoutNotice.publish(false, NOW)).hasMessageContaining("edital");

        SelectionProcess expired = draftWithPosition();
        assertThatThrownBy(() -> expired.publish(true, END.plusSeconds(1))).hasMessageContaining("já passou");
    }

    @Test
    void publishedProcessCannotBeEdited() {
        SelectionProcess process = published();

        assertThatThrownBy(() -> process.addPosition("Outro cargo", 1, NOW)).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> process.updateDetails(details(START, END), NOW)).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void opensAndClosesRegistrationByDates() {
        SelectionProcess process = published();

        assertThat(process.advanceBySchedule(START.minusSeconds(1))).isEmpty();
        assertThat(process.advanceBySchedule(START)).contains(
                new StatusChange(ProcessStatus.PUBLICADO, ProcessStatus.INSCRICOES_ABERTAS));
        assertThat(process.advanceBySchedule(END)).contains(
                new StatusChange(ProcessStatus.INSCRICOES_ABERTAS, ProcessStatus.INSCRICOES_ENCERRADAS));
        assertThat(process.advanceBySchedule(END.plus(Duration.ofDays(30)))).isEmpty();
    }

    @Test
    void acceptsApplicationsOnlyWhenOpenAndWithinPeriod() {
        SelectionProcess process = published();
        assertThat(process.acceptsApplications(START)).isFalse();

        process.advanceBySchedule(START);
        assertThat(process.acceptsApplications(START)).isTrue();
        // Mesmo antes da rotina automática encerrar, o fim do período já impede inscrições.
        assertThat(process.acceptsApplications(END)).isFalse();
    }

    @Test
    void extensionOnlyMovesEndForwardBeforeClosing() {
        SelectionProcess process = published();

        assertThatThrownBy(() -> process.extendRegistration(END.minusSeconds(1), NOW))
                .isInstanceOf(DomainRuleException.class);
        process.extendRegistration(END.plus(Duration.ofDays(5)), NOW);
        assertThat(process.getRegistrationEnd()).isEqualTo(END.plus(Duration.ofDays(5)));

        process.advanceBySchedule(START);
        process.advanceBySchedule(END.plus(Duration.ofDays(5)));
        assertThat(process.getStatus()).isEqualTo(ProcessStatus.INSCRICOES_ENCERRADAS);
        assertThatThrownBy(() -> process.extendRegistration(END.plus(Duration.ofDays(20)), NOW))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void suspendedProcessDoesNotAdvanceAndResumesToPreviousStatus() {
        SelectionProcess process = published();
        process.advanceBySchedule(START);

        process.suspend(NOW);
        assertThat(process.getStatus()).isEqualTo(ProcessStatus.SUSPENSO);
        assertThat(process.advanceBySchedule(END)).isEmpty();

        assertThat(process.resume(NOW).to()).isEqualTo(ProcessStatus.INSCRICOES_ABERTAS);
    }

    @Test
    void draftCannotBeSuspended() {
        assertThatThrownBy(() -> draft().suspend(NOW)).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void cancelledProcessIsTerminal() {
        SelectionProcess process = published();
        process.cancel(NOW);

        assertThatThrownBy(() -> process.cancel(NOW)).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> process.suspend(NOW)).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(process::requireNoticeRectifiable).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void archiveRequiresFinalResult() {
        assertThatThrownBy(() -> published().archive(NOW)).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void publicationStartsAtNoticeStageAndStageIsChosenManually() {
        SelectionProcess process = published();
        assertThat(process.getStage()).isEqualTo(ProcessStage.EDITAL_DISPONIVEL);

        assertThat(process.changeStage(ProcessStage.RESULTADO_PRELIMINAR, NOW)).isEqualTo(ProcessStage.EDITAL_DISPONIVEL);
        assertThat(process.changeStage(ProcessStage.GABARITO_DISPONIVEL, NOW)).isEqualTo(ProcessStage.RESULTADO_PRELIMINAR);
        assertThatThrownBy(() -> process.changeStage(ProcessStage.GABARITO_DISPONIVEL, NOW))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void draftHasNoStage() {
        assertThat(draft().getStage()).isNull();
        assertThatThrownBy(() -> draft().changeStage(ProcessStage.RESULTADO_FINAL, NOW))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void finalResultBlocksDecisionsAndAllowsArchivingAfterClosing() {
        SelectionProcess process = published();
        process.advanceBySchedule(START);
        process.advanceBySchedule(END);
        assertThat(process.allowsApplicationDecisions()).isTrue();

        process.changeStage(ProcessStage.RESULTADO_FINAL, NOW);

        assertThat(process.allowsApplicationDecisions()).isFalse();
        assertThat(process.archive(NOW).to()).isEqualTo(ProcessStatus.ARQUIVADO);
        assertThatThrownBy(() -> process.changeStage(ProcessStage.RESULTADO_PRELIMINAR, NOW))
                .isInstanceOf(DomainRuleException.class);
    }

    private static SelectionProcess draft() {
        return new SelectionProcess(details(START, END), NOW);
    }

    private static SelectionProcess draftWithPosition() {
        SelectionProcess process = draft();
        process.addPosition("Auxiliar Administrativo", 10, NOW);
        return process;
    }

    private static SelectionProcess published() {
        SelectionProcess process = draftWithPosition();
        process.publish(true, NOW);
        return process;
    }

    private static SelectionProcess.Details details(Instant start, Instant end) {
        return new SelectionProcess.Details("001", 2026, "Processo Teste", "Secretaria de Administração",
                start, end, false, false);
    }
}

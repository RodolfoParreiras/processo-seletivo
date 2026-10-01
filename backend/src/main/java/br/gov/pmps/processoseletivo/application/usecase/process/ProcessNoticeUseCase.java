package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.application.file.AllowedFileType;
import br.gov.pmps.processoseletivo.application.file.FileUploadService;
import br.gov.pmps.processoseletivo.application.file.IncomingFile;
import br.gov.pmps.processoseletivo.domain.model.StoredFile;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessNotice;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.ProcessNoticeRepository;
import br.gov.pmps.processoseletivo.domain.repository.StoredFileRepository;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edital em PDF (ESPECIFICACAO §15). Em rascunho, o arquivo pode ser trocado livremente.
 * Depois da publicação, cada envio é uma retificação: nova versão, com motivo, e a anterior fica no histórico.
 */
@Service
public class ProcessNoticeUseCase {

    private static final Set<ProcessNotice.Status> ACTIVE = EnumSet.of(ProcessNotice.Status.DRAFT, ProcessNotice.Status.CURRENT);

    private final ProcessNoticeRepository noticeRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadService fileUploadService;
    private final ProcessSupport support;
    private final long maxNoticeBytes;
    private final Clock clock;

    public ProcessNoticeUseCase(
            ProcessNoticeRepository noticeRepository,
            StoredFileRepository storedFileRepository,
            FileUploadService fileUploadService,
            ProcessSupport support,
            AppProperties appProperties,
            Clock clock) {
        this.noticeRepository = noticeRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileUploadService = fileUploadService;
        this.support = support;
        this.maxNoticeBytes = appProperties.files().noticeMaxSize().toBytes();
        this.clock = clock;
    }

    @Transactional
    public void upload(UUID processId, IncomingFile file, String reason, UUID actorId, String ipAddress) {
        SelectionProcess process = support.findForUpdate(processId);
        if (process.isDraft()) {
            uploadDraft(process, file, actorId, ipAddress);
        } else {
            rectify(process, file, reason, actorId, ipAddress);
        }
    }

    private void uploadDraft(SelectionProcess process, IncomingFile file, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        StoredFile storedFile = storePdf(file, actorId);
        Optional<ProcessNotice> existing = noticeRepository.findByProcessIdAndStatusIn(process.getId(), ACTIVE);
        if (existing.isPresent()) {
            UUID previousFileId = existing.get().replaceDraftFile(storedFile.getId());
            noticeRepository.flush();
            // Versão de rascunho nunca foi publicada: o arquivo anterior pode ser descartado.
            storedFileRepository.findById(previousFileId).ifPresent(fileUploadService::delete);
        } else {
            noticeRepository.save(ProcessNotice.draft(process.getId(), storedFile.getId(), actorId, now));
        }
        support.audit("NOTICE_UPLOADED", process, actorId, ipAddress, Map.of("fileId", storedFile.getId().toString()));
    }

    private void rectify(SelectionProcess process, IncomingFile file, String reason, UUID actorId, String ipAddress) {
        process.requireNoticeRectifiable();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Informe o motivo da retificação.");
        }
        if (reason.length() > 1000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "O motivo deve ter no máximo 1000 caracteres.");
        }
        Instant now = clock.instant();
        StoredFile storedFile = storePdf(file, actorId);
        ProcessNotice current = noticeRepository.findByProcessIdAndStatusIn(process.getId(), ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Processo publicado sem edital vigente"));
        current.supersede();
        // O índice único permite uma só versão vigente: grava a substituição antes de inserir a nova.
        noticeRepository.saveAndFlush(current);
        int newVersion = noticeRepository.findLatestVersion(process.getId()) + 1;
        noticeRepository.save(ProcessNotice.rectification(
                process.getId(), newVersion, storedFile.getId(), reason, actorId, now));
        support.audit("NOTICE_RECTIFIED", process, actorId, ipAddress, Map.of("version", newVersion));
    }

    private StoredFile storePdf(IncomingFile file, UUID actorId) {
        return fileUploadService.store(file, EnumSet.of(AllowedFileType.PDF), maxNoticeBytes, actorId);
    }
}

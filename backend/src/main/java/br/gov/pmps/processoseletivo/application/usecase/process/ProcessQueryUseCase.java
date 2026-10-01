package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.application.dto.PageResponse;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessDetailResponse;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessSummaryResponse;
import br.gov.pmps.processoseletivo.application.dto.process.StageHistoryResponse;
import br.gov.pmps.processoseletivo.application.dto.process.StatusHistoryResponse;
import br.gov.pmps.processoseletivo.application.file.FileDownload;
import br.gov.pmps.processoseletivo.application.file.FileUploadService;
import br.gov.pmps.processoseletivo.domain.model.Administrator;
import br.gov.pmps.processoseletivo.domain.model.StoredFile;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessNotice;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStageHistory;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatusHistory;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.AdministratorRepository;
import br.gov.pmps.processoseletivo.domain.repository.ProcessNoticeRepository;
import br.gov.pmps.processoseletivo.domain.repository.ProcessStageHistoryRepository;
import br.gov.pmps.processoseletivo.domain.repository.ProcessStatusHistoryRepository;
import br.gov.pmps.processoseletivo.domain.repository.SelectionProcessRepository;
import br.gov.pmps.processoseletivo.domain.repository.StoredFileRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de processos. A visão pública nunca mostra rascunhos nem versões de edital não publicadas.
 */
@Service
@Transactional(readOnly = true)
public class ProcessQueryUseCase {

    public static final int MAX_PAGE_SIZE = 50;

    private static final Set<ProcessStatus> PUBLIC_STATUSES = Arrays.stream(ProcessStatus.values())
            .filter(ProcessStatus::isPubliclyVisible)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ProcessStatus.class)));
    private static final Set<ProcessNotice.Status> PUBLISHED_NOTICES =
            EnumSet.of(ProcessNotice.Status.CURRENT, ProcessNotice.Status.SUPERSEDED);
    private static final Sort LIST_ORDER = Sort.by(Sort.Order.desc("createdAt"));

    private final SelectionProcessRepository processRepository;
    private final ProcessNoticeRepository noticeRepository;
    private final ProcessStatusHistoryRepository historyRepository;
    private final ProcessStageHistoryRepository stageHistoryRepository;
    private final AdministratorRepository administratorRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadService fileUploadService;

    public ProcessQueryUseCase(
            SelectionProcessRepository processRepository,
            ProcessNoticeRepository noticeRepository,
            ProcessStatusHistoryRepository historyRepository,
            ProcessStageHistoryRepository stageHistoryRepository,
            AdministratorRepository administratorRepository,
            StoredFileRepository storedFileRepository,
            FileUploadService fileUploadService) {
        this.processRepository = processRepository;
        this.noticeRepository = noticeRepository;
        this.historyRepository = historyRepository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.administratorRepository = administratorRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileUploadService = fileUploadService;
    }

    // ---- Visão pública ----

    public PageResponse<ProcessSummaryResponse> listPublic(ProcessStatus status, int page, int size) {
        Set<ProcessStatus> statuses = status == null
                ? PUBLIC_STATUSES
                : PUBLIC_STATUSES.contains(status) ? EnumSet.of(status) : EnumSet.noneOf(ProcessStatus.class);
        if (statuses.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        return PageResponse.from(
                processRepository.findByStatusIn(statuses, pageable(page, size)), ProcessSummaryResponse::from);
    }

    public ProcessDetailResponse getPublic(UUID processId) {
        SelectionProcess process = processRepository.findById(processId)
                .filter(found -> found.getStatus().isPubliclyVisible())
                .orElseThrow(ProcessSupport::notFound);
        return ProcessDetailResponse.from(process,
                noticeRepository.findByProcessIdAndStatusInOrderByNoticeVersionDesc(processId, PUBLISHED_NOTICES));
    }

    public FileDownload openPublicNotice(UUID processId, int version) {
        SelectionProcess process = processRepository.findById(processId)
                .filter(found -> found.getStatus().isPubliclyVisible())
                .orElseThrow(ProcessSupport::notFound);
        ProcessNotice notice = noticeRepository.findByProcessIdAndNoticeVersion(processId, version)
                .filter(ProcessNotice::isPublished)
                .orElseThrow(ProcessQueryUseCase::noticeNotFound);
        return open(process, notice);
    }

    // ---- Visão administrativa ----

    public PageResponse<ProcessSummaryResponse> listAdmin(ProcessStatus status, int page, int size) {
        Set<ProcessStatus> statuses = status == null ? EnumSet.allOf(ProcessStatus.class) : EnumSet.of(status);
        return PageResponse.from(
                processRepository.findByStatusIn(statuses, pageable(page, size)), ProcessSummaryResponse::from);
    }

    public ProcessDetailResponse getAdmin(UUID processId) {
        SelectionProcess process = processRepository.findById(processId).orElseThrow(ProcessSupport::notFound);
        return ProcessDetailResponse.from(process, noticeRepository.findByProcessIdOrderByNoticeVersionDesc(processId));
    }

    public FileDownload openAdminNotice(UUID processId, int version) {
        SelectionProcess process = processRepository.findById(processId).orElseThrow(ProcessSupport::notFound);
        ProcessNotice notice = noticeRepository.findByProcessIdAndNoticeVersion(processId, version)
                .orElseThrow(ProcessQueryUseCase::noticeNotFound);
        return open(process, notice);
    }

    public List<StatusHistoryResponse> history(UUID processId) {
        if (!processRepository.existsById(processId)) {
            throw ProcessSupport.notFound();
        }
        List<ProcessStatusHistory> entries = historyRepository.findByProcessIdOrderByChangedAtAscIdAsc(processId);
        // Uma consulta para todos os nomes, evitando N+1.
        Set<UUID> accountIds = entries.stream()
                .map(ProcessStatusHistory::getChangedByAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> names = administratorRepository.findByUserAccountIdIn(accountIds).stream()
                .collect(Collectors.toMap(Administrator::getUserAccountId, Administrator::getFullName));
        return entries.stream()
                .map(entry -> new StatusHistoryResponse(
                        entry.getFromStatus(), entry.getToStatus(), entry.getReason(),
                        entry.getChangedByAccountId() == null ? null : names.get(entry.getChangedByAccountId()),
                        entry.getChangedAt()))
                .toList();
    }

    public List<StageHistoryResponse> stageHistory(UUID processId) {
        if (!processRepository.existsById(processId)) {
            throw ProcessSupport.notFound();
        }
        List<ProcessStageHistory> entries = stageHistoryRepository.findByProcessIdOrderByChangedAtAscIdAsc(processId);
        Map<UUID, String> names = administratorRepository.findByUserAccountIdIn(
                        entries.stream().map(ProcessStageHistory::getChangedByAccountId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Administrator::getUserAccountId, Administrator::getFullName));
        return entries.stream()
                .map(entry -> new StageHistoryResponse(entry.getFromStage(), entry.getToStage(),
                        names.get(entry.getChangedByAccountId()), entry.getChangedAt()))
                .toList();
    }

    private FileDownload open(SelectionProcess process, ProcessNotice notice) {
        StoredFile file = storedFileRepository.findById(notice.getFileId())
                .orElseThrow(ProcessQueryUseCase::noticeNotFound);
        // Nome gerado pelo sistema: o nome enviado não é repetido em cabeçalhos HTTP.
        String fileName = "edital-%s-v%d.pdf".formatted(
                process.getDisplayNumber().replaceAll("[^0-9A-Za-z-]", "-"), notice.getNoticeVersion());
        return new FileDownload(fileName, file.getContentType(), file.getSizeBytes(), fileUploadService.open(file));
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), LIST_ORDER);
    }

    private static BusinessException noticeNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Edital não encontrado.");
    }
}

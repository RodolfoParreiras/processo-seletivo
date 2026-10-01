package br.gov.pmps.processoseletivo.domain.model.application;

import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.Address;
import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.Immutable;

/**
 * Cópia dos dados do candidato no momento da confirmação (ESPECIFICACAO §8, AI_RULES §17).
 * Alterações posteriores no cadastro não afetam a inscrição. O banco nega UPDATE/DELETE.
 */
@Entity
@Immutable
@Table(name = "application_snapshot")
public class ApplicationSnapshot {

    @Id
    @Column(name = "application_id")
    private UUID applicationId;

    @Column(nullable = false, columnDefinition = "bpchar(11)")
    private String cpf;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "mother_name", nullable = false)
    private String motherName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Embedded
    private Address address;

    @Column(name = "has_disability", nullable = false)
    private boolean hasDisability;

    @Column
    private String adaptations;

    @Column(name = "position_name", nullable = false)
    private String positionName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApplicationSnapshot() {
    }

    public ApplicationSnapshot(
            UUID applicationId, UserAccount account, Candidate candidate, String positionName, Instant now) {
        this.applicationId = applicationId;
        this.cpf = account.getCpf();
        this.fullName = candidate.getFullName();
        this.birthDate = candidate.getBirthDate();
        this.motherName = candidate.getMotherName();
        this.email = account.getEmail();
        this.phone = candidate.getPhone();
        this.address = candidate.getAddress();
        this.hasDisability = candidate.hasDisability();
        this.adaptations = candidate.getAdaptations().isEmpty()
                ? null
                : candidate.getAdaptations().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        this.positionName = positionName;
        this.createdAt = now;
    }

    public Set<Adaptation> getAdaptations() {
        if (adaptations == null) {
            return EnumSet.noneOf(Adaptation.class);
        }
        return Arrays.stream(adaptations.split(","))
                .map(Adaptation::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Adaptation.class)));
    }

    public String getCpf() {
        return cpf;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Address getAddress() {
        return address;
    }

    public boolean hasDisability() {
        return hasDisability;
    }

    public String getPositionName() {
        return positionName;
    }
}

package br.gov.pmps.processoseletivo.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "candidate")
public class Candidate {

    @Id
    private UUID id;

    @Column(name = "user_account_id", nullable = false, updatable = false)
    private UUID userAccountId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "mother_name", nullable = false)
    private String motherName;

    @Column(nullable = false)
    private String phone;

    @Embedded
    private Address address;

    @Column(name = "has_disability", nullable = false)
    private boolean hasDisability;

    @ElementCollection
    @CollectionTable(name = "candidate_adaptation", joinColumns = @JoinColumn(name = "candidate_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "adaptation", nullable = false)
    private Set<Adaptation> adaptations = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Candidate() {
    }

    public Candidate(
            UUID userAccountId,
            PersonalData personalData,
            Address address,
            boolean hasDisability,
            Set<Adaptation> adaptations,
            Instant now) {
        validateAdaptations(hasDisability, adaptations);
        this.id = UUID.randomUUID();
        this.userAccountId = userAccountId;
        this.fullName = personalData.fullName().trim();
        this.birthDate = personalData.birthDate();
        this.motherName = personalData.motherName().trim();
        this.phone = personalData.phone();
        this.address = address;
        this.hasDisability = hasDisability;
        this.adaptations = adaptations.isEmpty() ? new HashSet<>() : EnumSet.copyOf(adaptations);
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Pessoa com deficiência deve informar ao menos uma opção; "Nenhuma" não se combina com outras.
     * Quem não é pessoa com deficiência não informa adaptações.
     */
    static void validateAdaptations(boolean hasDisability, Set<Adaptation> adaptations) {
        if (!hasDisability) {
            if (!adaptations.isEmpty()) {
                throw new IllegalArgumentException("Adaptações só se aplicam a pessoa com deficiência.");
            }
            return;
        }
        if (adaptations.isEmpty()) {
            throw new IllegalArgumentException("Informe a necessidade de adaptações.");
        }
        if (adaptations.contains(Adaptation.NONE) && adaptations.size() > 1) {
            throw new IllegalArgumentException("A opção \"Nenhuma\" não pode ser combinada com outras.");
        }
    }

    public record PersonalData(String fullName, LocalDate birthDate, String motherName, String phone) {
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean hasDisability() {
        return hasDisability;
    }

    public Set<Adaptation> getAdaptations() {
        return Set.copyOf(adaptations);
    }
}

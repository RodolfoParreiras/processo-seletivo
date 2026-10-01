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
import java.util.TreeSet;
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
     * Atualiza o cadastro. Inscrições já realizadas não são afetadas, pois guardam snapshot próprio
     * (ESPECIFICACAO §8).
     *
     * @return nomes dos campos alterados, para auditoria sem registrar os valores
     */
    public Set<String> update(
            PersonalData personalData, Address newAddress, boolean newHasDisability,
            Set<Adaptation> newAdaptations, Instant now) {
        validateAdaptations(newHasDisability, newAdaptations);
        Set<String> changedFields = new TreeSet<>();
        String newFullName = personalData.fullName().trim();
        String newMotherName = personalData.motherName().trim();
        if (!fullName.equals(newFullName)) changedFields.add("fullName");
        if (!birthDate.equals(personalData.birthDate())) changedFields.add("birthDate");
        if (!motherName.equals(newMotherName)) changedFields.add("motherName");
        if (!phone.equals(personalData.phone())) changedFields.add("phone");
        if (!address.equals(newAddress)) changedFields.add("address");
        if (hasDisability != newHasDisability) changedFields.add("hasDisability");
        if (!adaptations.equals(newAdaptations)) changedFields.add("adaptations");

        fullName = newFullName;
        birthDate = personalData.birthDate();
        motherName = newMotherName;
        phone = personalData.phone();
        address = newAddress;
        hasDisability = newHasDisability;
        // Altera a coleção existente em vez de substituí-la, como o Hibernate espera.
        adaptations.clear();
        adaptations.addAll(newAdaptations);
        if (!changedFields.isEmpty()) {
            updatedAt = now;
        }
        return changedFields;
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

    public String getMotherName() {
        return motherName;
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

    public Set<Adaptation> getAdaptations() {
        return Set.copyOf(adaptations);
    }
}

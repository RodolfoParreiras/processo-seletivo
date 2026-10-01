package br.gov.pmps.processoseletivo.domain.model.process;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Cargo do processo. Alterado somente por meio de {@link SelectionProcess}. */
@Entity
@Table(name = "process_position")
public class ProcessPosition {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_id", nullable = false, updatable = false)
    private SelectionProcess process;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int vacancies;

    protected ProcessPosition() {
    }

    ProcessPosition(SelectionProcess process, String name, int vacancies) {
        this.id = UUID.randomUUID();
        this.process = process;
        change(name, vacancies);
    }

    void change(String newName, int newVacancies) {
        if (newVacancies <= 0) {
            throw new IllegalArgumentException("vacancies");
        }
        this.name = newName.trim();
        this.vacancies = newVacancies;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getVacancies() {
        return vacancies;
    }
}

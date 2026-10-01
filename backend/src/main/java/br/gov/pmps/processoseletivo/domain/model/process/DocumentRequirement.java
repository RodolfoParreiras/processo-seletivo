package br.gov.pmps.processoseletivo.domain.model.process;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Documento que o candidato deve (ou pode) enviar na inscrição. {@code title} indica documento
 * apresentado como título para avaliação. Alterado somente por meio de {@link SelectionProcess}.
 */
@Entity
@Table(name = "document_requirement")
public class DocumentRequirement {

    public record Definition(String name, String description, boolean mandatory, boolean title) {
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_id", nullable = false, updatable = false)
    private SelectionProcess process;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean mandatory;

    @Column(name = "is_title", nullable = false)
    private boolean title;

    protected DocumentRequirement() {
    }

    DocumentRequirement(SelectionProcess process, Definition definition) {
        this.id = UUID.randomUUID();
        this.process = process;
        change(definition);
    }

    void change(Definition definition) {
        this.name = definition.name().trim();
        this.description = definition.description() == null || definition.description().isBlank()
                ? null
                : definition.description().trim();
        this.mandatory = definition.mandatory();
        this.title = definition.title();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public boolean isTitle() {
        return title;
    }
}

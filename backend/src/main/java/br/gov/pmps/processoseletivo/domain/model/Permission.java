package br.gov.pmps.processoseletivo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Permissão granular. A lista é definida por migrations, pois cada código é verificado no código-fonte. */
@Entity
@Immutable
@Table(name = "permission")
public class Permission {

    @Id
    private String code;

    @Column(nullable = false)
    private String description;

    protected Permission() {
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

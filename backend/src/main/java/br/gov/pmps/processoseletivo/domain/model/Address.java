package br.gov.pmps.processoseletivo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Address(
        @Column(name = "cep", nullable = false, columnDefinition = "bpchar(8)") String cep,
        @Column(name = "street", nullable = false) String street,
        @Column(name = "address_number", nullable = false) String number,
        @Column(name = "complement") String complement,
        @Column(name = "neighborhood", nullable = false) String neighborhood,
        @Column(name = "city", nullable = false) String city,
        @Column(name = "uf", nullable = false, columnDefinition = "bpchar(2)") String uf) {
}

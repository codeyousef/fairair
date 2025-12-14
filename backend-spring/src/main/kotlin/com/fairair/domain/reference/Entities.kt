package com.fairair.domain.reference

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.relational.core.mapping.Column

@Table("airports")
data class Airport(
    @Id
    val code: String,
    @Column("name_en")
    val nameEn: String?,
    @Column("name_ar")
    val nameAr: String?
)

@Table("airport_aliases")
data class AirportAlias(
    @Id
    val id: Long? = null,
    @Column("airport_code")
    val airportCode: String,
    val alias: String
)

@Table("routes")
data class Route(
    @Id
    val id: Long? = null,
    val origin: String,
    val destination: String
)

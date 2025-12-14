package com.fairair.domain.reference

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface AirportRepository : ReactiveCrudRepository<Airport, String>

@Repository
interface AirportAliasRepository : ReactiveCrudRepository<AirportAlias, Long> {
    fun findAllBy(): Flux<AirportAlias>
}

@Repository
interface RouteRepository : ReactiveCrudRepository<Route, Long> {
    fun findAllBy(): Flux<Route>
    fun existsByOriginAndDestination(origin: String, destination: String): reactor.core.publisher.Mono<Boolean>
}

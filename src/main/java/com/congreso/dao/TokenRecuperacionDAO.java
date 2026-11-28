package com.congreso.dao;

import com.congreso.modelo.TokenRecuperacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacionDAO extends JpaRepository<TokenRecuperacion, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenRecuperacion t WHERE t.token = :token")
    Optional<TokenRecuperacion> bloquearPorToken(@Param("token") UUID token);
}

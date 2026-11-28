package com.congreso.dao;

import com.congreso.modelo.NotificacionEnviada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionEnviadaDAO extends JpaRepository<NotificacionEnviada, Long> {
}

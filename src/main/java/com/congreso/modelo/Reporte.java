package com.congreso.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @NotBlank(message = "El tipo de reporte es obligatorio")
    @Size(max = 100, message = "El tipo no puede exceder 100 caracteres")
    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "parametros", columnDefinition = "TEXT")
    private String parametros;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "codigo_generado_por")
    private Long codigoGeneradoPor;

    public Reporte() {
        this.fechaGeneracion = LocalDateTime.now();
    }

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getParametros() { return parametros; }
    public void setParametros(String parametros) { this.parametros = parametros; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public Long getCodigoGeneradoPor() { return codigoGeneradoPor; }
    public void setCodigoGeneradoPor(Long codigoGeneradoPor) { this.codigoGeneradoPor = codigoGeneradoPor; }
}

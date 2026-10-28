package com.congreso.modelo;

import com.congreso.modelo.enums.TipoReporte;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoReporte tipo;

    // Cambiado de Map<String, String> a String simple (JSON o texto)
    @Column(columnDefinition = "TEXT")
    private String filtros;

    // Cambiado de List<Object> a List<String> para que Hibernate pueda mapearlo
    @ElementCollection
    @CollectionTable(name = "reporte_resultados", joinColumns = @JoinColumn(name = "reporte_codigo"))
    @Column(name = "resultado_texto", columnDefinition = "TEXT")
    private List<String> resultados;

    private String periodo;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @ManyToOne
    @JoinColumn(name = "generado_por")
    private Administrador generadoPor;

    // Getters y Setters
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public TipoReporte getTipo() { return tipo; }
    public void setTipo(TipoReporte tipo) { this.tipo = tipo; }

    public String getFiltros() { return filtros; }
    public void setFiltros(String filtros) { this.filtros = filtros; }

    public List<String> getResultados() { return resultados; }
    public void setResultados(List<String> resultados) { this.resultados = resultados; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public Administrador getGeneradoPor() { return generadoPor; }
    public void setGeneradoPor(Administrador generadoPor) { this.generadoPor = generadoPor; }
}
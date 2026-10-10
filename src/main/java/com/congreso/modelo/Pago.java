package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Entidad que representa el pago realizado por un asistente.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_pago")
    private Long codigoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_asistente", nullable = false)
    private Asistente asistente;

    @Min(value = 0, message = "El importe no puede ser negativo")
    @Column(nullable = false)
    private double importe;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalidadPago modalidad;

    @Min(value = 1, message = "Debe haber al menos 1 cuota")
    private int cuotas;

    @Min(value = 0, message = "El saldo no puede ser negativo")
    private double saldo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(length = 255)
    private String observaciones;

    public Pago() {
        this.fechaPago = LocalDate.now();
        this.estado = EstadoPago.PENDIENTE;
    }

    public Pago(Asistente asistente, double importe, ModalidadPago modalidad, int cuotas) {
        this();
        this.asistente = asistente;
        this.importe = importe;
        this.modalidad = modalidad;
        this.cuotas = cuotas;
        this.saldo = importe;
    }

    // Getters y Setters
    public Long getCodigoPago() { return codigoPago; }
    public void setCodigoPago(Long codigoPago) { this.codigoPago = codigoPago; }

    public Asistente getAsistente() { return asistente; }
    public void setAsistente(Asistente asistente) { this.asistente = asistente; }

    public double getImporte() { return importe; }
    public void setImporte(double importe) { this.importe = importe; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public ModalidadPago getModalidad() { return modalidad; }
    public void setModalidad(ModalidadPago modalidad) { this.modalidad = modalidad; }

    public int getCuotas() { return cuotas; }
    public void setCuotas(int cuotas) { this.cuotas = cuotas; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public void registrarPago(double monto) {
        if (monto >= this.saldo) {
            this.saldo = 0;
            this.estado = EstadoPago.PAGADO;
        } else {
            this.saldo -= monto;
            this.estado = EstadoPago.PARCIAL;
        }
    }

    public boolean estaPagado() {
        return this.estado == EstadoPago.PAGADO;
    }
}
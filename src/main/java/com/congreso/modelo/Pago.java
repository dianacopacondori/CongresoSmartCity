package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_inscripcion", nullable = false)
    private Inscripcion inscripcion;

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

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CuotaPago> historialCuotas = new ArrayList<>();

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

    public Inscripcion getInscripcion() { return inscripcion; }
    public void setInscripcion(Inscripcion inscripcion) { this.inscripcion = inscripcion; }

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

    public List<CuotaPago> getHistorialCuotas() {
        return historialCuotas;
    }

    /**
     * Registra un abono simulado de forma segura: importe estrictamente positivo y
     * finito, sin sobrepago, sin reabrir pagos pagados o anulados, y dejando una
     * entrada por cuota en el historial.
     *
     * @return la cuota registrada
     */
    public CuotaPago registrarPago(double monto) {
        if (Double.isNaN(monto) || Double.isInfinite(monto) || monto <= 0) {
            throw new IllegalArgumentException("El importe del abono debe ser positivo y finito");
        }
        if (estado == EstadoPago.PAGADO || estado == EstadoPago.ANULADO) {
            throw new IllegalStateException("Este pago ya no admite abonos");
        }
        if (monto > saldo + 1e-9) {
            throw new IllegalArgumentException("El importe supera el saldo pendiente");
        }

        double abono = Math.min(monto, saldo);
        saldo = Math.max(0.0, saldo - abono);
        if (saldo < 1e-9) saldo = 0.0;
        estado = saldo == 0.0 ? EstadoPago.PAGADO : EstadoPago.PARCIAL;

        CuotaPago cuota = new CuotaPago(this, historialCuotas.size() + 1, abono,
                LocalDate.now(), observaciones);
        historialCuotas.add(cuota);
        return cuota;
    }

    public boolean estaPagado() {
        return this.estado == EstadoPago.PAGADO;
    }
}

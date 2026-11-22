package com.congreso.modelo;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Registro individual de cada abono simulado de un pago.
 * Permite consultar el historial sin recalcular importes a partir del saldo.
 */
@Entity
@Table(name = "cuotas_pago")
public class CuotaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_pago", nullable = false)
    private Pago pago;

    @Column(name = "numero_cuota", nullable = false)
    private int numeroCuota;

    @Column(nullable = false)
    private double importe;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(length = 255)
    private String observaciones;

    public CuotaPago() {
    }

    public CuotaPago(Pago pago, int numeroCuota, double importe, LocalDate fechaRegistro, String observaciones) {
        this.pago = pago;
        this.numeroCuota = numeroCuota;
        this.importe = importe;
        this.fechaRegistro = fechaRegistro;
        this.observaciones = observaciones;
    }

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }

    public int getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(int numeroCuota) { this.numeroCuota = numeroCuota; }

    public double getImporte() { return importe; }
    public void setImporte(double importe) { this.importe = importe; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}

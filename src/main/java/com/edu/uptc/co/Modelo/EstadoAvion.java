package com.edu.uptc.co.Modelo;

public enum EstadoAvion {
    ESPERANDO_PUERTA("Esperando puerta"),
    ESPERANDO_PISTA_ATERRIZAJE("Esperando pista (aterrizaje)"),
    ATERRIZANDO("Aterrizando"),
    EN_PUERTA("En puerta"),
    ESPERANDO_PISTA_DESPEGUE("Esperando pista (despegue)"),
    DESPEGANDO("Despegando"),
    FINALIZADO("Finalizado");
 
    private final String descripcion;
 
    EstadoAvion(String descripcion) {
        this.descripcion = descripcion;
    }
 
    public String getDescripcion() {
        return descripcion;
    }

}

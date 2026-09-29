package com.edu.uptc.co.Modelo;

public class Pista {
    private final int id;
    private boolean ocupada;
    private String avionActual; // nombre del avión que la ocupa, o null
 
    public Pista(int id) {
        this.id = id;
        this.ocupada = false;
        this.avionActual = null;
    }
 
    public int getId() {
        return id;
    }
 
    public boolean isOcupada() {
        return ocupada;
    }
 
    public String getAvionActual() {
        return avionActual;
    }
 
    public void ocupar(String nombreAvion) {
        this.ocupada = true;
        this.avionActual = nombreAvion;
    }
 
    public void liberar() {
        this.ocupada = false;
        this.avionActual = null;
    }
}



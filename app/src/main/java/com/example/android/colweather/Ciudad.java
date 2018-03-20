package com.example.android.colweather;

/**
 * Created by jdpp95 on 27/06/2017.
 */

public class Ciudad {
    protected String nombre, departamento;
    protected double latitud, longitud;

    public Ciudad(String nombre, String departamento, double latitud, double longitud)
    {
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
        //this.GMT = GMT;
        this.departamento = departamento;
        //this.temperatura = temperatura;
    }

    public String getNombre()
    {
        return nombre;
    }

    public double getLatitud()
    {
        return latitud;
    }

    public double getLongitud()
    {
        return longitud;
    }

    //public double getGMT(){     return GMT; }

    //public double getTemperatura(){return temperatura;}

    public String getDepartamento() {return departamento;}

    public String toString(){return nombre + ", " + departamento;}
}
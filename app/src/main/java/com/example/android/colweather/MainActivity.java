package com.example.android.colweather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.weightSum;
import static com.example.android.colweather.R.id.decor_content_parent;
import static com.example.android.colweather.R.id.place;
import static com.example.android.colweather.R.id.time;
import static com.example.android.colweather.R.raw.ciudades;

public class MainActivity extends AppCompatActivity {

    public double latitud, longitud, altitud, porcentaje, oscilacion, factorLluvia, sunrise, sunset,
            avgTemp, variacionDiurna, hora, osD, osN;
    public int año, mes, dia, sol = 0, seconds = 0;
    public Ciudad nearestC;
    public Estacion[] nearestS;
    public double temperatura;
    public boolean esDeDia, dayCycle;
    public double[] weights;

    RadioGroup clima, lluvia, escala;
    TextView textLugar, textTemperatura, textAltitud;
    EditText inputNubosidad;

    public ArrayList<Ciudad> lugares;
    public ArrayList<Estacion> estaciones;
    Calendar time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getWindow().setSoftInputMode(
          //      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            locationStart();
        }

        clima = (RadioGroup) findViewById(R.id.radioWeather);
        lluvia = (RadioGroup) findViewById(R.id.radioRain);
        escala = (RadioGroup) findViewById(R.id.radioScale);
        textLugar = (TextView) findViewById(place);
        textTemperatura = (TextView) findViewById(R.id.temperature);
        inputNubosidad = (EditText) findViewById(R.id.cloudiness);
        textAltitud = (TextView) findViewById(R.id.elevation);

        ((RadioButton) findViewById(R.id.rain0)).setChecked(true);
        ((RadioButton) findViewById(R.id.celsius)).setChecked(true);

        loadPlaces();
        nearestS = new Estacion[3];
        weights = new double[3];

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        double[] salidaPuestaSol;

                        /*
                        * tsnm
                        *           tsnm    OsD OsN
                        * Place 1   14.1    5.2 4.8
                        * Place 2   28.1    3.2 2.8
                        * Place 3   27.6    5.2 4.6
                        * Average   23.3    4.5 4.1
                         */
                        double temp[][] = new double[4][3];
                        updateTime();

                        /*hora = 22 + 48/60.0 + 33/3600.0;
                        latitud = 5.804;
                        longitud = -73.04;
                        altitud = 2491;*/


                        if (seconds == 0) nearestC = nearestCity(); // Updates place every 10 seconds
                        else if (seconds == 5) nearestS = nearestStations();

                        if(nearestS == null)
                            Log.e("Error","Nearest stations error");
                        else {

                            seconds = (seconds + 1) % 10;

                            textLugar.setText(nearestC.toString()); //Set text for place

                            //tsnm[0,1,2] = values, tsnm[3] = weighted average
                            //weights = new double[3];
                            double weightsDist[] = new double[3];
                            double weightsAlt[] = new double[3];
                            double altDiff[] = new double[3];

                            double weightsDistSum = 0, weightsAltSum = 0;
                            for (int i = 0; i < 3; i++) {
                                float results[] = new float[3];
                                if (nearestS[i] != null) {
                                    temp[i][0] = nearestS[i].getTemperatura() + nearestS[i].getAltitud() / 180;
                                    temp[i][1] = nearestS[i].getOsD();
                                    temp[i][2] = nearestS[i].getOsN();

                                    Location.distanceBetween(latitud, longitud, nearestS[i].getLatitud(), nearestS[i].getLongitud(), results);
                                    weightsDist[i] = 1 / results[0]; // 1/Dist
                                    weightsDistSum += weightsDist[i];

                                    altDiff[i] = Math.abs(altitud - nearestS[i].getAltitud());
                                    weightsAlt[i] = 1 / altDiff[i];
                                    weightsAltSum += weightsAlt[i];
                                }
                            }

                            for(int i=0;i<3;i++) {
                                weights[i] = (weightsAlt[i]/weightsAltSum + weightsDist[i]/weightsDistSum)/2;
                                //temp[3][i] = 0;
                            }

                            //Normalize weights

                            for (int i = 0; i < 3; i++) {
                                /*
                                if (weightSum > 0)
                                    weights[i] /= weightSum;
                                    */

                                //Log.v("Distance to " + nearestS[i], formatNumber(weights[i] * 100, 0) + "% from "+ estaciones.size() +" stations.");
                                for(int j=0;j<3;j++)
                                    temp[3][j] += temp[i][j] * weights[i];
                            }


                            //Log.v("\n ","");

                        }

                        avgTemp = temp[3][0] - altitud/180; //Computes average temp for a place
                        osD = temp[3][1];
                        osN = temp[3][2];

                        salidaPuestaSol = horaAnguloSolar(0); //Computes sunrise and sunset times
                        sunrise = salidaPuestaSol[0];
                        sunset = salidaPuestaSol[1];
                        variacionDiurna = factorTermico(salidaPuestaSol); //Computes temp variation with respect to hour
                        updateTemperature();
                        textAltitud.setText(formatNumber(altitud,0));
                    }
                });
            }
        };
        timer.schedule(timerTask, 1, 1000); //Runs this code every second
    }

    //Initializes the location
    private void locationStart() {
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Localizacion local = new Localizacion();
        local.setMainActivity(this);
        final boolean gpsEnabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        }
        mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) local);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) local);
    }

    public void selectorClima(View v) {
//        if(!((RadioButton)v).isChecked())
//            return;

        switch (clima.getCheckedRadioButtonId()) {
            case R.id.sunny:
                oscilacion = oscilacionTermica(0.02);
                break;
            case R.id.cloudy:
                String input = inputNubosidad.getText().toString();
                if (input.compareTo("") == 0)
                    oscilacion = 1;
                else
                    oscilacion = oscilacionTermica(0.01*Double.parseDouble(input));
                break;
            case R.id.clouds:
                if (dayCycle)
                    oscilacion = 3;
                else
                    oscilacion = 2;
                break;
            case R.id.clouds2:
                oscilacion = 2;
                break;
            case R.id.clouds3:
                oscilacion = 1; //Nigh os...
                break;
            default:
                oscilacion = 0;
        }
        updateTemperature();
    }

    public void viewStations(View v)
    {
        String text = "";
        //Estacion[] estaciones = nearestStations();

        /*
        double weights[] = new double[3];
        double weightSum = 0;

        for (int i = 0; i < 3; i++) {
            float results[] = new float[3];
            if (nearestS[i] != null) {
                Location.distanceBetween(latitud, longitud, nearestS[i].getLatitud(), nearestS[i].getLongitud(), results);
                weights[i] = 1 / results[0];
                weightSum += weights[i];
            }
        }


        //Normalize weights
        for (int i = 0; i < 3; i++) {
            if (weightSum > 0)
                weights[i] /= weightSum;
        }
        */

        for(int i=0;i<3;i++)
        {
            text += nearestS[i].toString();
            text += ": ";
            text += formatNumber(weights[i]*100,0) + "%";
            text += '\n';
        }

        Toast toast = Toast.makeText(this,text,Toast.LENGTH_SHORT);
        toast.show();
    }

    public void selectorLluvia(View v) {
        switch (lluvia.getCheckedRadioButtonId()) {
            case R.id.rain0:
                factorLluvia = 0;
                break;
            case R.id.rain1:
                factorLluvia = 1;
                break;
            case R.id.rain2:
                if (dayCycle)
                    factorLluvia = 2;
                else {
                    double hour = hora;
                    if (hour < sunset)
                        hour += 24;
                    factorLluvia = transicion(2, 1, sunset, sunrise + 24, hour);
                }
                break;
            case R.id.rain3:
                if (dayCycle)
                    factorLluvia = 2.5;
                else {
                    double hour = hora;
                    if (hour < sunset)
                        hour += 24;
                    factorLluvia = transicion(2.5, 1, sunset, sunrise + 24, hour);
                }
                break;
            default:
                factorLluvia = 0;
        }
        updateTemperature();
    }

    public void selectorEscala(View v) {
        switch (escala.getCheckedRadioButtonId()) {
            case R.id.celsius:
                textTemperatura.setText(formatNumber(temperatura, 0) + "°C");
                break;
            case R.id.fahrenheit:
                textTemperatura.setText(formatNumber(temperatura * 1.8 + 32, 0) + "°F");
                break;
            default:
                return;
        }
    }

    public void sunCheck(View v)
    {
        if (((CheckBox)findViewById(R.id.sunCheck)).isChecked())
            sol = 1;
        else
            sol = 0;

        updateTemperature();
    }

    //Loads cities database from text file
    public void loadPlaces() {
        String currentLine;

        lugares = new ArrayList<>();
        estaciones = new ArrayList<>();

        try {
            InputStream fraw = getResources().openRawResource(ciudades);
            InputStream graw = getResources().openRawResource(R.raw.estaciones);
            BufferedReader br = new BufferedReader(new InputStreamReader(fraw));
            BufferedReader br2 = new BufferedReader(new InputStreamReader(graw));

            while ((currentLine = br.readLine()) != null) {
                String tokens[] = currentLine.split("\t");
                //String tokens[] = new String[uncoded.length];
                /*
                for (int i=0;i < uncoded.length;i++)
                {
                    tokens[i] = new String(uncoded[i].getBytes("ANSI"),"UTF-8");
                }*/

                Ciudad ciudad = new Ciudad(
                        tokens[1],
                        tokens[0],
                        Double.parseDouble(tokens[2]),
                        Double.parseDouble(tokens[3])
                );

                lugares.add(ciudad);
            }

            while ((currentLine = br2.readLine()) != null) {
                String tokens[] = currentLine.split("\t");

                Estacion estacion = new Estacion(
                        tokens[0],
                        tokens[1],
                        tokens[2],
                        Double.parseDouble(tokens[3]),
                        Double.parseDouble(tokens[4]),
                        Double.parseDouble(tokens[5]),
                        Double.parseDouble(tokens[6]),
                        Double.parseDouble(tokens[7]),
                        Double.parseDouble(tokens[8])
                );

                estaciones.add(estacion);
            }

            if (br != null)
                br.close();

            if (br2 != null)
                br2.close();

            if (fraw != null)
                fraw.close();

            if(graw != null)
                graw.close();

        } catch (Exception e) {
            Log.e("Error!!", "No se pudo leer el archivo de origen.");
        }
    }

    //Rounds a decimal number to a given number of decimals
    public String formatNumber(double number, int decimalPlaces) {
        BigDecimal bd = new BigDecimal(number);
        return bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).toString();
    }

    //Updates variables to current time
    public void updateTime() {
        time = Calendar.getInstance();
        año = time.get(Calendar.YEAR);
        mes = time.get(Calendar.MONTH) + 1;
        dia = time.get(Calendar.DATE);
        hora = time.get(Calendar.HOUR_OF_DAY) + time.get(Calendar.MINUTE)/60 + time.get(Calendar.SECOND)/3600;
    }

    //Finds the nearest city in database
    public Ciudad nearestCity() {
        Ciudad nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        double newDistance, bearing;

        for (Ciudad x : lugares) {
            if (nearest == null)
                nearest = x;
            else {
                float[] results = new float[3];
                Location.distanceBetween(latitud, longitud, x.getLatitud(), x.getLongitud(), results);
                newDistance = results[0];
                //Log.v("Distance data: ", "Place :" + x + ", distance: " + formatNumber(newDistance / 1000, 1) +"km, initial bearing: " + results[1] + ", final bearing: " + results[2]);
                if (newDistance < distance) {
                    nearest = x;
                    distance = newDistance;
                }
            }
        }
        return nearest;
    }

    public Estacion[] nearestStations() {
        Estacion nearest[] = new Estacion[3];

        nearest[0] = null;
        nearest[1] = null;
        nearest[2] = null;

        double distances[] = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double newDistance, bearing;

        for (Estacion x : estaciones) {
            float[] results = new float[3];
            Location.distanceBetween(latitud, longitud, x.getLatitud(), x.getLongitud(), results);
            newDistance = results[0];
            //Log.v("Distance data: ", "Place :" + x + ", distance: " + formatNumber(newDistance / 1000, 1) + "km, initial bearing: " + results[1] + ", final bearing: " + results[2]);

            //Algoritmo chevere

            int i=2;

            if(newDistance > distances[2])
                continue;
            else {
                while(i > 0 && newDistance < distances[i-1])
                    i--;
                //newDistance will replace position 'i' in array

            }

            for(int j=2; j>i; j--)
            {
                distances[j] = distances[j-1];
                nearest[j] = nearest[j-1];
            }

            distances[i] = newDistance;
            nearest[i] = x;

            //Log.v("Distances"," "+distances[0]+", "+distances[1]+", "+distances[2]);
        }

        return nearest;
    }

    //Colors temperature box
    public void colorear(double temperatura) {
        int rgb[] = new int[3];
        final int[] FRIGID = {0, 0, 255};
        final int[] FREEZING = {0, 255, 255};
        final int[] CHILLY = {0, 192, 128};
        final int[] COLD = {0, 128, 0};
        final int[] COOL = {128, 192, 0};
        final int[] COMFORTABLE = {255, 255, 0};
        final int[] WARM = {255, 128, 0};
        final int[] HOT = {255, 0, 0};
        final int[] SWELTERING = {128, 0, 0};

        if (temperatura < 0) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = 255;
        } else if (temperatura < 21) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(255, FRIGID[i], 0, 21, temperatura) + 0.5);
        } else if (temperatura < 32) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(FRIGID[i], FREEZING[i], 21, 32, temperatura) + 0.5);
        } else if (temperatura < 41) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(FREEZING[i], CHILLY[i], 32, 41, temperatura) + 0.5);
        } else if (temperatura < 50) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(CHILLY[i], COLD[i], 40, 50, temperatura) + 0.5);
        } else if (temperatura < 60) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COLD[i], COOL[i], 50, 60, temperatura) + 0.5);
        } else if (temperatura < 70) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COOL[i], COMFORTABLE[i], 60, 70, temperatura) + 0.5);
        } else if (temperatura < 80) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COMFORTABLE[i], WARM[i], 70, 80, temperatura) + 0.5);
        } else if (temperatura < 90) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(WARM[i], HOT[i], 80, 90, temperatura) + 0.5);
        } else if (temperatura < 100) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(HOT[i], SWELTERING[i], 90, 100, temperatura) + 0.5);
        } else if (temperatura < 125) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(SWELTERING[i], 0, 100, 125, temperatura) + 0.5);
        } else {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = 0;
        }

        if(altitud != 0) {

            textTemperatura.setBackgroundColor(Color.rgb(rgb[0], rgb[1], rgb[2]));

            if (temperatura > 25 && temperatura < 35 || temperatura > 60 && temperatura < 80)
                textTemperatura.setTextColor(Color.BLACK);
            else
                textTemperatura.setTextColor(Color.WHITE);
        } else {
            textTemperatura.setBackgroundColor(Color.BLACK);
            textTemperatura.setTextColor(Color.WHITE);
        }
    }

    public double transicion(double start1, double end1, double start2, double end2, double value2) {
        //128, 192, 20, 30, 23

        double proporcion = (value2 - start2) / (end2 - start2);

        return start1 + (end1 - start1) * proporcion;
    }

    //Computes the hour of a given sun angle - In this case computes sunrise and sunset time based on the sun angle (0°)
    public double[] horaAnguloSolar(double anguloSolar) {
        int d = Calendar.DAY_OF_YEAR;
        int diasAño = 365;
        if (this.año % 4 == 0 && (this.año % 100 != 0 || this.año % 400 == 0))
            diasAño += 1;
        double delta = 23.45 * Math.sin((2 * Math.PI / diasAño) * (d - 81));
        double HRA = (180 / Math.PI) * Math.acos((Math.sin((Math.PI / 180) * anguloSolar) - Math.sin((Math.PI / 180) * delta) * Math.sin((Math.PI / 180) * latitud) / ((Math.cos((Math.PI / 180) * delta)) * Math.cos((Math.PI / 180) * latitud))));

        return new double[]{12 - (HRA / 15), 12 + (HRA / 15)};
    }

    //Computes the percentage of daytime or nightime taking as reference the sunset and sunrise times
    public double porcentajeDia(double amanecer, double atardecer) {
        if (esDeDia) //Dia
            return transicion(0, 1, amanecer, atardecer, hora);
        else //Noche
        {
            double hora = this.hora;

            if (hora < atardecer)
                hora += 24;

            return transicion(1, 0, atardecer, amanecer + 24, hora);
        }
    }

    //Computes temperature variation based on the hour, "ceroGrados" refers to the sun angle
    public double factorTermico(double[] ceroGrados) {
        double amanecer = ceroGrados[0];
        double atardecer = ceroGrados[1];
        esDeDia = (hora >= amanecer) && (hora <= atardecer);
        porcentaje = porcentajeDia(ceroGrados[0], ceroGrados[1]);
        dayCycle = (esDeDia && porcentaje > 0.202092708) || (!esDeDia && porcentaje > 0.885541319);

        if (esDeDia)
            return 6.25 * Math.pow(porcentaje, 4) - 12.83 * Math.pow(porcentaje, 3) + 3.17 * Math.pow(porcentaje, 2) + 4.78 * porcentaje - 1;
        else
            return 1.32 * Math.pow(porcentaje, 4) - 1.07 * Math.pow(porcentaje, 3) + 0.16 * Math.pow(porcentaje, 2) + 0.91 * porcentaje - 1;
    }

    //Computes the daytime thermal oscillation based on the cloudiness scaled from 0 to 1
    public double oscilacionTermica(double nubosidad) {
        if (dayCycle)
            return (-1.373 * Math.log(nubosidad) + 3.9037)*osD/5;
        else
            return (-1.657 * Math.log(nubosidad) + 2.8288)*osN/5;
    }

    //Shows and updates temperature on screen
    public void updateTemperature() {

        temperatura = avgTemp + variacionDiurna * oscilacion - factorLluvia + sol;
        colorear(temperatura * 1.8 + 32);
        if (escala.getCheckedRadioButtonId() == R.id.celsius)
            if(altitud != 0)
                textTemperatura.setText(formatNumber(temperatura, 0) + "°C");
            else
                textTemperatura.setText("-- °C");
        else
            if(altitud != 0)
                textTemperatura.setText(formatNumber(temperatura * 1.8 + 32, 0) + "°F");
            else
                textTemperatura.setText("-- °F");
    }

    /* Aqui empieza la Clase Localizacion */
    public class Localizacion implements LocationListener {

        MainActivity mainActivity;

        public MainActivity getMainActivity() {
            return mainActivity;
        }

        public void setMainActivity(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onLocationChanged(Location loc) {
            // Este metodo se ejecuta cada vez que el GPS recibe nuevas coordenadas
            // debido a la deteccion de un cambio de ubicacion

            latitud = loc.getLatitude();
            longitud = loc.getLongitude();
            altitud = loc.getAltitude();
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es desactivado
            Toast.makeText(mainActivity, "GPS Desactivado", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es activado
            Toast.makeText(mainActivity, "GPS Activado", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d("debug", "LocationProvider.AVAILABLE");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                    break;
            }
        }
    }
}

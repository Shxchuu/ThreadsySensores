package com.example.threadsysensores;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Sensores
    private SensorManager sensorManager;
    private Sensor rotationSensor;

    // Elementos de la interfaz
    private TextView txtSensorStatus;
    private TextView txtX;
    private TextView txtY;
    private TextView txtZ;
    private TextView txtThreadStatus;
    private TextView txtAsyncStatus;

    private Button btnStart;
    private Button btnStop;

    // Thread
    private Thread backgroundThread;
    private volatile boolean threadRunning = false;

    // Handler para actualizar la interfaz
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Handler
        handler = new Handler(Looper.getMainLooper());

        // Vincular componentes
        txtSensorStatus = findViewById(R.id.txtSensorStatus);
        txtX = findViewById(R.id.txtX);
        txtY = findViewById(R.id.txtY);
        txtZ = findViewById(R.id.txtZ);
        txtThreadStatus = findViewById(R.id.txtThreadStatus);
        txtAsyncStatus = findViewById(R.id.txtAsyncStatus);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        // Inicializar SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Obtener sensor de vector de rotación
        rotationSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR
        );

        // Comprobar disponibilidad del sensor
        if (rotationSensor != null) {
            txtSensorStatus.setText("● Sensor disponible");
        } else {
            txtSensorStatus.setText("● Sensor no disponible");
            btnStart.setEnabled(false);
        }

        // Botón iniciar
        btnStart.setOnClickListener(v -> iniciarProcesos());

        // Botón detener
        btnStop.setOnClickListener(v -> detenerProcesos());
    }

    /**
     * Inicia el sensor, Thread y AsyncTask.
     */
    private void iniciarProcesos() {

        // Registrar sensor
        if (rotationSensor != null) {
            sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }

        // Iniciar Thread
        iniciarThread();

        // Ejecutar AsyncTask
        new ProcesamientoAsyncTask().execute();

        txtThreadStatus.setText("● Thread ejecutándose");
        txtAsyncStatus.setText("Procesamiento iniciado");

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    /**
     * Detiene todos los procesos.
     */
    private void detenerProcesos() {

        // Detener sensor
        sensorManager.unregisterListener(this);

        // Detener Thread
        threadRunning = false;

        if (backgroundThread != null) {
            backgroundThread.interrupt();
            backgroundThread = null;
        }

        txtThreadStatus.setText("● Thread detenido");
        txtAsyncStatus.setText("Procesamiento detenido");

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    /**
     * Crea un Thread que trabaja en segundo plano.
     */
    private void iniciarThread() {

        threadRunning = true;

        backgroundThread = new Thread(() -> {

            while (threadRunning) {

                try {

                    Thread.sleep(1000);

                    // Actualizamos la interfaz desde el hilo principal
                    handler.post(() -> {

                        if (threadRunning) {
                            txtThreadStatus.setText(
                                    "● Thread activo • Procesando datos"
                            );
                        }

                    });

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();
                    break;
                }
            }

        });

        backgroundThread.start();
    }

    /**
     * Se ejecuta cuando cambia el valor del sensor.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            txtX.setText(
                    String.format(Locale.getDefault(), "%.3f", x)
            );

            txtY.setText(
                    String.format(Locale.getDefault(), "%.3f", y)
            );

            txtZ.setText(
                    String.format(Locale.getDefault(), "%.3f", z)
            );
        }
    }

    /**
     * Se ejecuta cuando cambia la precisión del sensor.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No es necesario realizar ninguna acción aquí.
    }

    /**
     * AsyncTask para realizar una operación asincrónica.
     *
     * La pauta solicita utilizar AsyncTask para gestionar
     * operaciones asincrónicas.
     */
    private class ProcesamientoAsyncTask
            extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {

            txtAsyncStatus.setText(
                    "Preparando procesamiento..."
            );
        }

        @Override
        protected String doInBackground(Void... voids) {

            try {

                // Simulación de procesamiento
                Thread.sleep(2000);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }

            return "Procesamiento completado correctamente";
        }

        @Override
        protected void onPostExecute(String resultado) {

            txtAsyncStatus.setText(resultado);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Liberar sensor cuando la aplicación pasa a segundo plano
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Volver a registrar el sensor si el proceso está activo
        if (threadRunning && rotationSensor != null) {

            sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        threadRunning = false;

        if (backgroundThread != null) {
            backgroundThread.interrupt();
        }

        sensorManager.unregisterListener(this);
    }
}
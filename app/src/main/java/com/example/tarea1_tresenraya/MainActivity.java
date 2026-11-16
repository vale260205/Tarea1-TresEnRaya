package com.example.tarea1_tresenraya;


import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // NUEVO: máximo de fichas por jugador
    private static final int MAX_FICHAS = 3;

    private boolean turnoX = true;          // true = juega X, false = juega O
    private boolean juegoTerminado = false;

    // NUEVO: cuántas fichas ha puesto cada jugador (de 0 a 3)
    private int fichasX = 0;
    private int fichasO = 0;

    // NUEVO: posición de la ficha seleccionada para mover (-1 = ninguna)
    private int seleccionada = -1;

    // Marcador (sin empates, porque aquí no puede haberlos)
    private int victoriasX = 0;
    private int victoriasO = 0;

    private Button[] casillas;
    private TextView estado;
    private TextView marcador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        estado = findViewById(R.id.estado);
        marcador = findViewById(R.id.marcador);
        Button btnReiniciar = findViewById(R.id.btnReiniciar);
        Button btnBorrarMarcador = findViewById(R.id.btnBorrarMarcador);

        casillas = new Button[]{
                findViewById(R.id.casilla1),
                findViewById(R.id.casilla2),
                findViewById(R.id.casilla3),
                findViewById(R.id.casilla4),
                findViewById(R.id.casilla5),
                findViewById(R.id.casilla6),
                findViewById(R.id.casilla7),
                findViewById(R.id.casilla8),
                findViewById(R.id.casilla9)
        };

        // NUEVO: for con índice, porque necesitamos saber la POSICIÓN pulsada
        // (para recordar qué ficha está seleccionada)
        for (int i = 0; i < casillas.length; i++) {
            final int indice = i; // la lambda necesita una variable final
            casillas[i].setOnClickListener(v -> jugar(indice));
        }

        btnReiniciar.setOnClickListener(v -> reiniciar());
        btnBorrarMarcador.setOnClickListener(v -> borrarMarcador());

        // Venimos de girar: recuperamos los datos
        if (savedInstanceState != null) {

            String[] tablero = savedInstanceState.getStringArray("tablero");
            if (tablero != null) {
                for (int i = 0; i < casillas.length; i++) {
                    casillas[i].setText(tablero[i]);
                    pintarFicha(casillas[i], tablero[i]);
                }
            }

            turnoX = savedInstanceState.getBoolean("turnoX", true);
            juegoTerminado = savedInstanceState.getBoolean("juegoTerminado", false);
            fichasX = savedInstanceState.getInt("fichasX", 0);           // NUEVO
            fichasO = savedInstanceState.getInt("fichasO", 0);           // NUEVO
            seleccionada = savedInstanceState.getInt("seleccionada", -1); // NUEVO
            estado.setText(savedInstanceState.getString("estado", "Turno de X"));

            victoriasX = savedInstanceState.getInt("victoriasX", 0);
            victoriasO = savedInstanceState.getInt("victoriasO", 0);

            // NUEVO: si había una ficha seleccionada, la volvemos a marcar
            if (seleccionada != -1) {
                casillas[seleccionada].setAlpha(0.4f);
            }

        } else {
            actualizarTurno();
        }

        actualizarMarcador();
    }

    // Guardamos todo antes de girar
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        String[] tablero = new String[casillas.length];
        for (int i = 0; i < casillas.length; i++) {
            tablero[i] = casillas[i].getText().toString();
        }

        outState.putStringArray("tablero", tablero);
        outState.putBoolean("turnoX", turnoX);
        outState.putBoolean("juegoTerminado", juegoTerminado);
        outState.putInt("fichasX", fichasX);           // NUEVO
        outState.putInt("fichasO", fichasO);           // NUEVO
        outState.putInt("seleccionada", seleccionada); // NUEVO
        outState.putString("estado", estado.getText().toString());

        outState.putInt("victoriasX", victoriasX);
        outState.putInt("victoriasO", victoriasO);
    }

    // Se ejecuta al pulsar cualquier casilla. Recibe su posición (0-8).
    private void jugar(int indice) {

        if (juegoTerminado) return;

        String ficha = turnoX ? "X" : "O";                        // ficha del jugador actual
        String texto = casillas[indice].getText().toString();     // lo que hay en la casilla pulsada
        int colocadas = turnoX ? fichasX : fichasO;               // fichas que ya ha puesto

        if (colocadas < MAX_FICHAS) {

            // Solo se puede poner en una casilla vacía
            if (!texto.isEmpty()) return;

            ponerFicha(indice, ficha);

            // Sumamos una ficha al jugador que ha jugado
            if (turnoX) {
                fichasX++;
            } else {
                fichasO++;
            }

            terminarJugada(ficha);
            return;
        }


        // a) Ha tocado una ficha SUYA -> la seleccionamos
        if (texto.equals(ficha)) {
            quitarSeleccion();                    // por si había otra seleccionada
            seleccionada = indice;
            casillas[indice].setAlpha(0.4f);      // transparente = seleccionada
            estado.setText("Turno de " + ficha + ": toca una casilla vacía");
            return;
        }

        // b) Ha tocado una casilla VACÍA y tenía una ficha seleccionada -> la movemos
        if (texto.isEmpty() && seleccionada != -1) {
            casillas[seleccionada].setText("");   // quitamos la ficha de su sitio
            quitarSeleccion();                    // la casilla vieja vuelve a verse normal
            ponerFicha(indice, ficha);            // la ponemos en la nueva casilla
            terminarJugada(ficha);
        }

        // Cualquier otro caso (ficha del rival, o vacía sin seleccionar) no hace nada
    }

    // NUEVO: escribe la ficha en la casilla con su color
    private void ponerFicha(int indice, String ficha) {
        casillas[indice].setText(ficha);
        pintarFicha(casillas[indice], ficha);
    }

    // NUEVO: después de poner o mover, comprobamos victoria o cambiamos turno
    private void terminarJugada(String ficha) {

        if (triunfo()) {
            juegoTerminado = true;

            if (turnoX) {
                victoriasX++;
            } else {
                victoriasO++;
            }
            actualizarMarcador();

            mostrarResultado("¡Ha ganado " + ficha + "!");
            return;
        }

        // Aquí no hay empate: nunca se llenan las 9 casillas (máximo 6 fichas)
        turnoX = !turnoX;
        actualizarTurno();
    }

    // NUEVO: deja de marcar la ficha seleccionada
    private void quitarSeleccion() {
        if (seleccionada != -1) {
            casillas[seleccionada].setAlpha(1f);   // opacidad normal
            seleccionada = -1;
        }
    }

    // Azul para X, violeta para O
    private void pintarFicha(Button casilla, String ficha) {
        if ("X".equals(ficha)) {
            casilla.setTextColor(getColor(R.color.azul_neon));
        } else if ("O".equals(ficha)) {
            casilla.setTextColor(getColor(R.color.violeta_neon));
        }
    }

    // NUEVO: el mensaje dice si toca colocar (y cuántas quedan) o mover
    private void actualizarTurno() {
        String ficha = turnoX ? "X" : "O";
        int colocadas = turnoX ? fichasX : fichasO;

        if (colocadas < MAX_FICHAS) {
            int quedan = MAX_FICHAS - colocadas;
            estado.setText("Turno de " + ficha + ": coloca (quedan " + quedan + ")");
        } else {
            estado.setText("Turno de " + ficha + ": elige una ficha para mover");
        }
    }

    private void actualizarMarcador() {
        marcador.setText("X: " + victoriasX + "   |   O: " + victoriasO);
    }

    private void borrarMarcador() {
        victoriasX = 0;
        victoriasO = 0;
        actualizarMarcador();
    }

    private void mostrarResultado(String mensaje) {
        estado.setText(mensaje);

        new AlertDialog.Builder(this)
                .setTitle("Fin de la partida")
                .setMessage(mensaje + "\n\n" + marcador.getText())
                .setPositiveButton("Jugar otra vez", (dialog, which) -> reiniciar())
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // Limpia el tablero (no el marcador)
    private void reiniciar() {
        quitarSeleccion();

        for (Button c : casillas) {
            c.setText("");
        }
        turnoX = true;
        juegoTerminado = false;
        fichasX = 0;   // NUEVO: cada jugador vuelve a tener 3 fichas por poner
        fichasO = 0;
        actualizarTurno();
    }

    // 3 filas, 3 columnas y 2 diagonales
    private boolean triunfo() {
        return linea(0, 1, 2) || linea(3, 4, 5) || linea(6, 7, 8)
                || linea(0, 3, 6) || linea(1, 4, 7) || linea(2, 5, 8)
                || linea(0, 4, 8) || linea(2, 4, 6);
    }

    // true si las 3 casillas tienen la misma ficha y no están vacías
    private boolean linea(int a, int b, int c) {
        String ta = casillas[a].getText().toString();
        String tb = casillas[b].getText().toString();
        String tc = casillas[c].getText().toString();
        return !ta.isEmpty() && ta.equals(tb) && tb.equals(tc);
    }
}

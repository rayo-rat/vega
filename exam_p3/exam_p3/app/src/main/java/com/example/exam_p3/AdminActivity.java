package com.example.exam_p3;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

public class AdminActivity extends AppCompatActivity {

    private DBLocal dbLocal;
    private LinearLayout layoutUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        layoutUsuarios = findViewById(R.id.layoutUsuarios);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        dbLocal = new DBLocal(this);

        cargarUsuariosRegistrados();

        btnCerrarSesion.setOnClickListener(v -> {
            // Regresar al MainActivity (login)
            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Cerrar esta actividad
        });
    }

    private void cargarUsuariosRegistrados() {
        SQLiteDatabase db = dbLocal.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT id, correo, usuario FROM usuarios ORDER BY id", null);

            if (cursor.getCount() == 0) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No hay usuarios registrados");
                tvEmpty.setTextSize(18);
                tvEmpty.setTextColor(Color.GRAY);
                tvEmpty.setPadding(20, 20, 20, 20);
                layoutUsuarios.addView(tvEmpty);
            } else {
                // Título
                TextView tvTitle = new TextView(this);
                tvTitle.setText("USUARIOS REGISTRADOS (" + cursor.getCount() + ")");
                tvTitle.setTextSize(20);
                tvTitle.setTextColor(Color.BLACK);
                tvTitle.setPadding(20, 20, 20, 30);
                tvTitle.setAllCaps(true);
                layoutUsuarios.addView(tvTitle);

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(0);
                    String correo = cursor.getString(1);
                    String usuario = cursor.getString(2);

                    // Crear contenedor para cada usuario
                    LinearLayout userLayout = new LinearLayout(this);
                    userLayout.setOrientation(LinearLayout.VERTICAL);
                    userLayout.setPadding(20, 15, 20, 15);
                    userLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 10);
                    userLayout.setLayoutParams(params);

                    // ID del usuario
                    TextView tvId = new TextView(this);
                    tvId.setText("ID: " + id);
                    tvId.setTextSize(14);
                    tvId.setTextColor(Color.DKGRAY);
                    userLayout.addView(tvId);

                    // Correo
                    TextView tvCorreo = new TextView(this);
                    tvCorreo.setText("Correo: " + correo);
                    tvCorreo.setTextSize(16);
                    tvCorreo.setTextColor(Color.BLACK);
                    userLayout.addView(tvCorreo);

                    // Usuario
                    TextView tvUsuario = new TextView(this);
                    tvUsuario.setText("Usuario: " + usuario);
                    tvUsuario.setTextSize(16);
                    tvUsuario.setTextColor(Color.BLACK);
                    userLayout.addView(tvUsuario);

                    // Separador
                    View separator = new View(this);
                    separator.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                    ));
                    separator.setBackgroundColor(Color.LTGRAY);

                    layoutUsuarios.addView(userLayout);
                    layoutUsuarios.addView(separator);
                }
            }
            cursor.close();

        } finally {
            db.close();
        }
    }

    class DBLocal extends SQLiteOpenHelper {
        public DBLocal(android.content.Context c){ super(c, "usuarios.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db){
            db.execSQL("CREATE TABLE IF NOT EXISTS usuarios(id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, usuario TEXT, contrasena TEXT)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){}
    }
}
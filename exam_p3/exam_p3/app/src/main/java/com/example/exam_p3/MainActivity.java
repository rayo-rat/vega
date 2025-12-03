package com.example.exam_p3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback {

    EditText edtCorreo, edtUsuario, edtPass;
    Button btnRegistrar, btnLogin, btnMostrarVideo, btnIrMapa, btnCerrarSesion;
    VideoView videoTutorial;
    Spinner spinnerBancos;
    ImageView imagenBienvenida;
    GoogleMap googleMap;

    SensorManager sensorManager;
    Sensor sensorGiroscopio;

    FusedLocationProviderClient fusedLocationClient;
    Location ubicacionActual;
    LocationCallback locationCallback;

    DBLocal dbLocal;

    String canalNotificacion = "cajeros_cercanos";
    boolean isLoggedIn = false;
    boolean isAdmin = false;

    // Constantes para guardar el estado
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_ADMIN = "isAdmin";

    static class Cajero {
        String banco;
        LatLng pos;
        String tipo;
        Cajero(String banco, LatLng pos, String tipo){ this.banco = banco; this.pos = pos; this.tipo = tipo; }
    }
    List<Cajero> cajerosSimulados = new ArrayList<>();

    static class BancoItem {
        String nombre;
        int resId;
        BancoItem(String nombre, int resId){ this.nombre = nombre; this.resId = resId; }
        public String toString(){ return nombre; }
    }

    static class BancoAdapter extends ArrayAdapter<BancoItem> {
        Context ctx;
        List<BancoItem> items;
        public BancoAdapter(Context c, List<BancoItem> items){
            super(c, android.R.layout.simple_spinner_item, items);
            this.ctx = c;
            this.items = items;
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
        @Override
        public View getView(int pos, View convertView, android.view.ViewGroup parent){
            return makeItemView(pos);
        }
        @Override
        public View getDropDownView(int pos, View convertView, android.view.ViewGroup parent){
            return makeItemView(pos);
        }
        private View makeItemView(int pos){
            BancoItem item = items.get(pos);
            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setPadding(12,12,12,12);
            ImageView iv = new ImageView(ctx);
            iv.setImageResource(item.resId);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(90,90);
            p.setMargins(0,0,16,0);
            iv.setLayoutParams(p);

            TextView tv = new TextView(ctx);
            tv.setText(item.nombre);
            tv.setTextSize(16);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setGravity(Gravity.CENTER_VERTICAL);

            ll.addView(iv);
            ll.addView(tv);
            return ll;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtCorreo = findViewById(R.id.edtCorreo);
        edtUsuario = findViewById(R.id.edtUsuario);
        edtPass = findViewById(R.id.edtPass);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnLogin = findViewById(R.id.btnLogin);
        btnMostrarVideo = findViewById(R.id.btnVideo);
        btnIrMapa = findViewById(R.id.btnMapa);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        videoTutorial = findViewById(R.id.videoTutorial);
        spinnerBancos = findViewById(R.id.spinnerBancos);
        imagenBienvenida = findViewById(R.id.imgBienvenida);

        // Restaurar estado si existe
        if (savedInstanceState != null) {
            isLoggedIn = savedInstanceState.getBoolean(KEY_IS_LOGGED_IN, false);
            isAdmin = savedInstanceState.getBoolean(KEY_IS_ADMIN, false);
        }

        // Configurar interfaz basada en el estado de login
        configurarInterfaz();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorGiroscopio = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        dbLocal = new DBLocal(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        pedirPermisos();

        ArrayList<BancoItem> bancos = new ArrayList<>();
        bancos.add(new BancoItem("Todos", R.drawable.ic_all));
        bancos.add(new BancoItem("BBVA", R.drawable.bbva));
        bancos.add(new BancoItem("Banamex", R.drawable.banamex));
        bancos.add(new BancoItem("Santander", R.drawable.santander));
        bancos.add(new BancoItem("HSBC", R.drawable.hsbc));
        bancos.add(new BancoItem("Banorte", R.drawable.banorte));

        BancoAdapter adapter = new BancoAdapter(this, bancos);
        spinnerBancos.setAdapter(adapter);

        prepararCajerosReales();
        crearCanalNotificaciones();

        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        btnLogin.setOnClickListener(v -> {
            loginUsuario();
            if(isLoggedIn){
                if(isAdmin) {
                    // Redirigir a la actividad de administrador
                    Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                    startActivity(intent);
                    finish(); // Cerrar esta actividad
                } else {
                    // Usuario normal - mostrar interfaz normal
                    configurarInterfaz();
                }
            }
        });

        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        btnMostrarVideo.setOnClickListener(v -> mostrarVideo());
        btnIrMapa.setOnClickListener(v -> cargarMapa());

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult r){
                if(r == null) return;
                ubicacionActual = r.getLastLocation();
                if(googleMap != null && ubicacionActual != null){
                    LatLng yo = new LatLng(ubicacionActual.getLatitude(), ubicacionActual.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(yo, 15));
                    actualizarCajerosEnMapa();
                }
            }
        };
    }

    // Método para configurar la interfaz basada en el estado de login
    private void configurarInterfaz() {
        if (isLoggedIn && !isAdmin) {
            // Usuario normal logueado
            imagenBienvenida.setVisibility(View.VISIBLE);
            spinnerBancos.setVisibility(View.VISIBLE);
            btnIrMapa.setVisibility(View.VISIBLE);
            btnCerrarSesion.setVisibility(View.VISIBLE);
            findViewById(R.id.mapa).setVisibility(View.VISIBLE);

            edtCorreo.setVisibility(View.GONE);
            edtUsuario.setVisibility(View.GONE);
            edtPass.setVisibility(View.GONE);
            btnRegistrar.setVisibility(View.GONE);
            btnLogin.setVisibility(View.GONE);
            btnMostrarVideo.setVisibility(View.GONE);
            videoTutorial.setVisibility(View.GONE);
        } else {
            // No logueado o admin (admin va a otra actividad)
            spinnerBancos.setVisibility(View.GONE);
            btnIrMapa.setVisibility(View.GONE);
            btnCerrarSesion.setVisibility(View.GONE);
            findViewById(R.id.mapa).setVisibility(View.GONE);
            imagenBienvenida.setVisibility(View.GONE);

            edtCorreo.setVisibility(View.VISIBLE);
            edtUsuario.setVisibility(View.VISIBLE);
            edtPass.setVisibility(View.VISIBLE);
            btnRegistrar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);
            btnMostrarVideo.setVisibility(View.VISIBLE);
            videoTutorial.setVisibility(View.VISIBLE);
        }
    }

    // Método para cerrar sesión
    private void cerrarSesion() {
        isLoggedIn = false;
        isAdmin = false;
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        configurarInterfaz();

        // Limpiar campos
        edtCorreo.setText("");
        edtUsuario.setText("");
        edtPass.setText("");
    }

    // Guardar estado para rotación
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        outState.putBoolean(KEY_IS_ADMIN, isAdmin);
    }

    private void registrarUsuario(){
        String correo = edtCorreo.getText().toString().trim();
        String usuario = edtUsuario.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if(correo.isEmpty() || usuario.isEmpty() || pass.isEmpty()){
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar longitud mínima de contraseña
        if(pass.length() < 6){
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbLocal.getWritableDatabase();

        // Verificar si el usuario ya existe
        Cursor cursor = db.rawQuery("SELECT id FROM usuarios WHERE usuario=? OR correo=?",
                new String[]{usuario, correo});
        if(cursor.moveToFirst()){
            Toast.makeText(this, "El usuario o correo ya existe", Toast.LENGTH_SHORT).show();
            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        db.execSQL("INSERT INTO usuarios(correo,usuario,contrasena) VALUES(?,?,?)",
                new Object[]{correo, usuario, pass});
        db.close();

        new Thread(() -> {
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://tu-servidor/bancos","usuario","password");
                PreparedStatement pst = con.prepareStatement("INSERT INTO usuarios(correo,usuario,contrasena) VALUES(?,?,?)");
                pst.setString(1, correo);
                pst.setString(2, usuario);
                pst.setString(3, pass);
                pst.executeUpdate();
                con.close();
            } catch (Exception ignored){}
        }).start();

        Toast.makeText(this, "Usuario registrado (local)", Toast.LENGTH_SHORT).show();

        // Limpiar campos después del registro
        edtCorreo.setText("");
        edtUsuario.setText("");
        edtPass.setText("");
    }

    private void loginUsuario(){
        String correo = edtCorreo.getText().toString().trim();
        String usuario = edtUsuario.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if(usuario.isEmpty() || pass.isEmpty()){
            Toast.makeText(this, "Introduce usuario y contraseña", Toast.LENGTH_SHORT).show();
            isLoggedIn = false;
            isAdmin = false;
            return;
        }

        // Verificar credenciales de administrador
        if(usuario.equals("admin") && pass.equals("admin123") && correo.equals("admin@ceti.mx")){
            isLoggedIn = true;
            isAdmin = true;
            Toast.makeText(this, "Bienvenido Administrador", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbLocal.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT id FROM usuarios WHERE usuario=? AND contrasena=?", new String[]{usuario, pass});
            boolean ok = c.moveToFirst();
            c.close();
            if(ok) {
                isLoggedIn = true;
                isAdmin = false;
                Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show();
            } else {
                isLoggedIn = false;
                isAdmin = false;
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        } finally {
            db.close();
        }
    }

    private void mostrarVideo(){
        Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.tutorial);
        videoTutorial.setVideoURI(video);
        videoTutorial.start();
        sensorManager.registerListener(this, sensorGiroscopio, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent e){
        if(Math.abs(e.values[1]) > 2.0f){
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
    @Override public void onAccuracyChanged(Sensor s, int a){}

    private void cargarMapa(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        if(mapFragment != null) mapFragment.getMapAsync(this);
        startLocationUpdates();
    }

    @Override
    public void onMapReady(GoogleMap gm){
        googleMap = gm;
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
            return;
        }
        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if(loc != null){
                ubicacionActual = loc;
                LatLng yo = new LatLng(loc.getLatitude(), loc.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(yo, 15));
                actualizarCajerosEnMapa();
            }
        });
    }

    private void prepararCajerosReales(){
        cajerosSimulados.clear();
        cajerosSimulados.add(new Cajero("BBVA", new LatLng(20.63070169552964, -103.26649851202039), "cajero"));
        cajerosSimulados.add(new Cajero("BBVA", new LatLng(20.61833225607396, -103.25162629582336), "cajero"));
        cajerosSimulados.add(new Cajero("BBVA", new LatLng(20.623036505475717, -103.24277643875743), "cajero"));
        cajerosSimulados.add(new Cajero("Santander", new LatLng(20.62850052276907, -103.25778416257305), "sucursal"));
        cajerosSimulados.add(new Cajero("Santander", new LatLng(20.62512223869445, -103.24119180588424), "sucursal"));
        cajerosSimulados.add(new Cajero("HSBC", new LatLng(20.628374551225452, -103.25776739267867), "cajero"));
        cajerosSimulados.add(new Cajero("HSBC", new LatLng(20.619383420533236, -103.24433830980017), "cajero"));
        cajerosSimulados.add(new Cajero("Banamex", new LatLng(20.631198406011666, -103.26571933196266), "sucursal"));
        cajerosSimulados.add(new Cajero("Banorte", new LatLng(20.64063105202782, -103.27469363434436), "sucursal"));
        cajerosSimulados.add(new Cajero("Banorte", new LatLng(20.64053665916049, -103.27864050165066), "cajero"));
        cajerosSimulados.add(new Cajero("Banorte", new LatLng(20.630236027280997, -103.25141426647605), "cajero"));
    }

    private void actualizarCajerosEnMapa(){
        if(googleMap == null || ubicacionActual == null) return;

        googleMap.clear();
        BancoItem sel = (BancoItem) spinnerBancos.getSelectedItem();
        String seleccionado = (sel != null ? sel.nombre : "Todos");

        for(Cajero c : cajerosSimulados){
            float[] r = new float[1];
            Location.distanceBetween(
                    ubicacionActual.getLatitude(), ubicacionActual.getLongitude(),
                    c.pos.latitude, c.pos.longitude,
                    r
            );
            float dist = r[0];

            boolean match = seleccionado.equals("Todos") || seleccionado.equalsIgnoreCase(c.banco);
            if(match && dist <= 5000){
                googleMap.addMarker(new MarkerOptions()
                        .position(c.pos)
                        .title(c.banco + " - " + c.tipo)
                        .snippet("Distancia: " + Math.round(dist) + " m")
                );

                if(dist <= 300) enviarNotificacion("Cajero de " + c.banco + " a " + Math.round(dist) + " m");
            }
        }
    }

    private void crearCanalNotificaciones(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel canal = new NotificationChannel(
                    canalNotificacion,"Avisos de cajeros",NotificationManager.IMPORTANCE_HIGH
            );
            Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            canal.setSound(sonido, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            NotificationManager man = getSystemService(NotificationManager.class);
            if(man != null) man.createNotificationChannel(canal);
        }
    }

    private void enviarNotificacion(String mensaje){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS},2001);
                return;
            }
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, canalNotificacion)
                .setSmallIcon(R.drawable.ic_cajero)
                .setContentTitle("Cajero cercano")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(this).notify((int)System.currentTimeMillis(), b.build());
    }

    private void startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            pedirPermisos();
            return;
        }
        LocationRequest req = LocationRequest.create();
        req.setInterval(5000);
        req.setFastestInterval(2000);
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(req, locationCallback, null);
    }

    private void stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void pedirPermisos(){
        List<String> p = new ArrayList<>();
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            p.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                p.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if(!p.isEmpty()) ActivityCompat.requestPermissions(this, p.toArray(new String[0]),1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        sensorManager.unregisterListener(this);
    }

    class DBLocal extends SQLiteOpenHelper {
        public DBLocal(Context c){ super(c, "usuarios.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db){
            db.execSQL("CREATE TABLE IF NOT EXISTS usuarios(id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, usuario TEXT, contrasena TEXT)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){}
    }
}
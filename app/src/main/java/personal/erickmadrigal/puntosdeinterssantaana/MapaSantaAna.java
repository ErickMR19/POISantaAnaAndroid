package personal.erickmadrigal.puntosdeinterssantaana;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.os.AsyncTask;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.geojson.GeoJsonLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Vector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MapaSantaAna extends AppCompatActivity implements OnMapReadyCallback {
    private final int iconos_marcadores[] = {
            R.drawable.markercultura,
            R.drawable.markerdeporte,
            R.drawable.markereducacion,
            R.drawable.markersalud,
            R.drawable.markerbancos,
            R.drawable.markerrestaurantes,
            R.drawable.markerspublicos,
            R.drawable.markerautomotriz
    };

    private class CrearMarcadores extends AsyncTask<Void, Void, String> {

        private Vector<ControlMarcador> marcadores;


        public CrearMarcadores(Vector<ControlMarcador> vectorExistente) {
            marcadores = vectorExistente;
            Log.d("CrearMarcadores", "Constructor CrearMarcadores()");
        }


        protected String doInBackground(Void... v) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://erickmadrigal.me/MapaUbicaciones/public/ubicaciones/json")
                    .build();
            String respuesta = " ";
            try {
                Log.d("CrearMarcadores", "BEFORE EXECUTE");
                Response response = client.newCall(request).execute();
                respuesta = response.body().string();
                Log.d("CrearMarcadores", "Respuesta obtenida");
            } catch (IOException /*| JSONException*/ e) {

            }
            return respuesta;
        }

        protected void onPostExecute(String resultado) {
            Log.d("CrearMarcadores", "Resultado obtenido");
            Log.d("CrearMarcadores", resultado);
            try {
                JSONArray array_json = new JSONArray(resultado);
                Log.d("CrearMarcadores", array_json.toString());

                final int array_json_length = array_json.length();// Moved  array_json.length() call out of the loop to local variable array_json_length
                for (int i = 0; i < array_json_length; i++) {
                    Log.d("CrearMarcadores", i + ": " + (array_json.getJSONObject(i).toString()));
                    marcadores.add(new ControlMarcador(array_json.getJSONObject(i)));
                }
            } catch (JSONException e) {
                Log.e("CrearMarcadores", e.toString());
            }
        }
    }

    private void cambiarVisibilidadMarcadores() {
        int seleccionCategoriaC = seleccionCategoria - 1;

        if (seleccionCategoria == 0)  // Si esta seleccionada todas las categorias
            if (seleccionActualRegion == 0) {  // Si además esta seleccionadas todas las regiones
                // muestre todos los marcadores
                for (ControlMarcador controlMarcador : marcadores) {
                    controlMarcador.marcador.setVisible(true);
                }
            } else {
                // muestre los marcadores de la actual region
                for (ControlMarcador controlMarcador : marcadores) {
                    controlMarcador.marcador.setVisible(
                            controlMarcador.region == seleccionActualRegion);
                }
            }
        else {   // Si esta seleccionada alguna categoria
            if (seleccionActualRegion == 0) {  // Si además esta seleccionadas todas las regiones
                // muestre los marcadores de la categoria seleccionada
                for (ControlMarcador controlMarcador : marcadores) {
                    controlMarcador.marcador.setVisible(
                            controlMarcador.categoria == seleccionCategoriaC
                    );
                }
            } else {
                // muestre los marcadores de la actual region y categoria
                for (ControlMarcador controlMarcador : marcadores) {
                    controlMarcador.marcador.setVisible(
                            controlMarcador.categoria == seleccionCategoriaC &&
                                    controlMarcador.region == seleccionActualRegion
                    );
                }
            }
        }
    }

    private class ControlMarcador {
        public Marker marcador;
        public int region;
        public int categoria;

        public ControlMarcador(Marker marker, int reg, int cat) {
            marcador = marker;
            region = reg;
            categoria = cat;
        }

        public ControlMarcador(JSONObject objeto_json) {
            try {
                LatLng posicion = new LatLng(Double.parseDouble(objeto_json.getString("X")), Double.parseDouble(objeto_json.getString("Y")));
                region = Integer.parseInt(objeto_json.getString("distrito"));
                categoria = Integer.parseInt(objeto_json.getString("categoria"));
                MarkerOptions MO = new MarkerOptions().position(posicion).title(objeto_json.getString("nombre")).snippet(objeto_json.getString("telefono"));
                MO = MO.icon(BitmapDescriptorFactory.fromResource(iconos_marcadores[categoria]));
                marcador = mMap.addMarker(MO);
            } catch (JSONException | ArrayIndexOutOfBoundsException e) {

            }
        }
    }

    private GoogleMap mMap;
    private FloatingActionsMenu fabMenuFilter;
    private FloatingActionsMenu fabMenuConfig;
    private Marker marcadorActivo;
    private final Context context = this;
    private ListView listaRegiones;
    private ListView listaCategorias;
    private Dialog dialogSelectionRegion;
    private Dialog dialogSelectionCategory;
    private int seleccionActualRegion;
    private int nuevaSeleccionRegion;
    private int seleccionCategoria;
    private int tipoInicialMap;
    private GeoJsonLayer layerMascaraDistrito[];
    private boolean traficoActivado = false;
    private Vector<ControlMarcador> marcadores;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_canton);
        marcadores = new Vector<>();
        dialogSelectionRegion = new Dialog(context);
        dialogSelectionCategory = new Dialog(context);
        dialogSelectionRegion.setContentView(R.layout.custom_dialog_single_choice);
        dialogSelectionCategory.setContentView(R.layout.custom_dialog_single_choice);
        dialogSelectionRegion.setTitle("Escoger Región");
        dialogSelectionCategory.setTitle("Escoger Categoría");

        /*******************************************************************************************/
        listaRegiones = (ListView) dialogSelectionRegion.findViewById(R.id.control_regiones);
        listaCategorias = (ListView) dialogSelectionCategory.findViewById(R.id.control_regiones);
        final String[] regionsName = getResources().getStringArray(R.array.sectores_array);
        final String[] categoriesName = getResources().getStringArray(R.array.categorias_array);
        final ArrayAdapter<String> adapterRegions = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, regionsName);
        final int coloresCategorias[] =
                {
                        Color.BLACK,
                        Color.parseColor("#B700FF"),
                        Color.parseColor("#FF9911"),
                        Color.BLUE,
                        Color.RED,
                        Color.parseColor("#00ff15"),
                        Color.parseColor("#ffd500"),
                        Color.parseColor("#44AAFF"),
                        Color.parseColor("#444444")
                };
        final ArrayAdapter adapterCategories = new ColorArrayAdapter(this, android.R.layout.simple_list_item_single_choice, categoriesName, coloresCategorias);

        listaRegiones.setAdapter(adapterRegions);
        listaRegiones.setItemsCanFocus(false);
        listaRegiones.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listaRegiones.setEnabled(true);

        listaCategorias.setAdapter(adapterCategories);
        listaCategorias.setItemsCanFocus(false);
        listaCategorias.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listaCategorias.setEnabled(true);
        nuevaSeleccionRegion = 0;

        if (savedInstanceState != null) {
            seleccionActualRegion = savedInstanceState.getInt("region actual");
            tipoInicialMap = savedInstanceState.getInt("tipo mapa actual");
            seleccionCategoria = savedInstanceState.getInt("seleccion categoria");

            if (savedInstanceState.getBoolean("mostrando dialogo")) {
                nuevaSeleccionRegion = savedInstanceState.getInt("region nueva");
                Log.d("MAPAS", "onCreate: | region nueva->" + nuevaSeleccionRegion);
                listaRegiones.setItemChecked(nuevaSeleccionRegion, true);
                dialogSelectionRegion.show();
            }
        } else {
            seleccionActualRegion = 0;
            seleccionCategoria = 0;
            tipoInicialMap = GoogleMap.MAP_TYPE_NORMAL;
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fabMenuFilter = (FloatingActionsMenu) findViewById(R.id.fab_menu_filter);
        fabMenuConfig = (FloatingActionsMenu) findViewById(R.id.fab_menu_config);
        fabMenuFilter.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuCollapsed() {
                Toast.makeText(MapaSantaAna.this, "Menu Filtrado Cerrado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuExpanded() {
                fabMenuConfig.collapse();
            }
        });
        fabMenuConfig.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuCollapsed() {
                Toast.makeText(MapaSantaAna.this, "Menu Config Cerrado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuExpanded() {
                fabMenuFilter.collapse();
            }
        });

        FloatingActionButton botonEscogerRegion = (FloatingActionButton) findViewById(R.id.fab_button_filter_region);
        assert botonEscogerRegion != null;
        botonEscogerRegion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listaRegiones.setItemChecked(seleccionActualRegion, true);
                dialogSelectionRegion.show();
            }
        });

        FloatingActionButton botonEscogerCategoria = (FloatingActionButton) findViewById(R.id.fab_button_filter_category);
        assert botonEscogerCategoria != null;
        botonEscogerCategoria.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listaCategorias.setItemChecked(seleccionCategoria, true);
                dialogSelectionCategory.show();
            }
        });

        FloatingActionButton botonVistaSatelite = (FloatingActionButton) findViewById(R.id.fab_button_config_satelite);
        assert botonVistaSatelite != null;
        botonVistaSatelite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            }
        });

        FloatingActionButton botonVistaMapa = (FloatingActionButton) findViewById(R.id.fab_button_config_mapa);
        assert botonVistaMapa != null;
        botonVistaMapa.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });
        FloatingActionButton botonVistaTrafico = (FloatingActionButton) findViewById(R.id.fab_button_config_traffic);
        assert botonVistaTrafico != null;
        botonVistaTrafico.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                traficoActivado = !traficoActivado;
                mMap.setTrafficEnabled(traficoActivado);
            }
        });

        listaRegiones.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                nuevaSeleccionRegion = position;
            }
        });
        /*******************************************************************************************/
        final Button botonAceptarRegion = (Button) dialogSelectionRegion.findViewById(R.id.boton_aceptar);
        final Button botonCancelarRegion = (Button) dialogSelectionRegion.findViewById(R.id.boton_cancelar);

        botonAceptarRegion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (seleccionActualRegion != nuevaSeleccionRegion) {

                    if (seleccionActualRegion != 0) { // si no esta seleccionado el canton completo
                        // remueva el distrito seleccionado
                        layerMascaraDistrito[seleccionActualRegion - 1].removeLayerFromMap();
                    }
                    if (nuevaSeleccionRegion != 0) { // si no se selcciono el canton completo
                        layerMascaraDistrito[nuevaSeleccionRegion - 1].addLayerToMap();
                    }
                    seleccionActualRegion = nuevaSeleccionRegion;
                    //Toast.makeText(MapaCanton.this, "Nueva seleccion hecha: " + nuevaSeleccionRegion, Toast.LENGTH_SHORT).show();
                    cambiarVisibilidadMarcadores();
                }
                dialogSelectionRegion.dismiss();
            }
        });

        botonCancelarRegion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSelectionRegion.cancel();
            }
        });

        dialogSelectionRegion.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                nuevaSeleccionRegion = seleccionActualRegion;
                Log.d("MAPAS", "se invoco onCancel de region");
            }
        });

        /*******************************************************************************************/
        final Button botonAceptarCategory = (Button) dialogSelectionCategory.findViewById(R.id.boton_aceptar);
        final Button botonCancelarCategory = (Button) dialogSelectionCategory.findViewById(R.id.boton_cancelar);

        botonAceptarCategory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (seleccionCategoria != listaCategorias.getCheckedItemPosition()) {
                    seleccionCategoria = listaCategorias.getCheckedItemPosition();
                    cambiarVisibilidadMarcadores();
                }
                Log.d("MAPAS", "La categoria seleccionada es: " + listaCategorias.getCheckedItemPosition());
                dialogSelectionCategory.dismiss();
            }
        });

        botonCancelarCategory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSelectionCategory.cancel();
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("region actual", seleccionActualRegion);
        outState.putInt("tipo mapa actual", mMap.getMapType());

        outState.putInt("seleccion categoria", seleccionCategoria);
        if (dialogSelectionRegion.isShowing()) {
            outState.putBoolean("mostrando dialogo", true);
            outState.putInt("region nueva", nuevaSeleccionRegion);
            Log.d("MAPAS", "onSaveInstanceState: | region nueva->" + nuevaSeleccionRegion);
        } else {
            outState.putBoolean("mostrando dialogo", false);
            outState.putInt("region nueva", -1);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(tipoInicialMap);
        mMap.setPadding(130, 0, 130, 0);
        GeoJsonLayer layerMascaraCanton;
        layerMascaraDistrito = new GeoJsonLayer[6];
        final int recursos[] = {R.raw.santaana, R.raw.salitral, R.raw.pozos, R.raw.lauruca, R.raw.piedades, R.raw.brasil};
        try {
            layerMascaraCanton = new GeoJsonLayer(googleMap, R.raw.canton, context);
            for (int i = 0; i < 6; ++i) {
                layerMascaraDistrito[i] = new GeoJsonLayer(googleMap, recursos[i], context);
                layerMascaraDistrito[i].getDefaultPolygonStyle().setFillColor(Color.argb(125, 0, 0, 0));

            }
            // GeoJsonPolygonStyle estiloCanton = new GeoJsonPolygonStyle();
            layerMascaraCanton.getDefaultPolygonStyle().setFillColor(Color.argb(125, 0, 0, 0));
            layerMascaraCanton.addLayerToMap();
        } catch (IOException | JSONException e) {
            Log.e("MAPAS", "Error Archivo");
        }

        // Add a marker in Sydney and move the camera
        LatLng santaAna = new LatLng(9.913558506393061, -84.19467810541391);
        crearMarcadores();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(santaAna, 12.274366f));
        Log.d("MAPAS", "LISTO");
        mMap.setOnMapClickListener(new OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                fabMenuFilter.collapse();
                fabMenuConfig.collapse();
            }
        });
        mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marcadorActivo = marker;
                fabMenuFilter.collapse();
                fabMenuConfig.collapse();
                return false;
            }
        });
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                boolean modification = false;
                Log.d("MAPAS", "Current Zoom: " + cameraPosition.zoom);
                double x = cameraPosition.target.latitude;
                double y = cameraPosition.target.longitude;
                double zoom = cameraPosition.zoom;
                Log.d("MAPAS", "Current X: " + x);
                Log.d("MAPAS", "Current Y: " + y);
                if (cameraPosition.zoom > 18.0) {
                    zoom = 18.0;
                    modification = true;
                    Log.d("MAPAS", "CAMBIO ZOOM");
                } else if (cameraPosition.zoom < 11.0) {
                    zoom = 11.0;
                    modification = true;
                    Log.d("MAPAS", "CAMBIO ZOOM");
                }

                if (x < 9.6) {
                    x = 9.735;
                    modification = true;
                    Log.d("MAPAS", "CAMBIO X < 9.85");
                } else if (x > 10.22) {
                    x = 10.093;
                    modification = true;
                    Log.d("MAPAS", "CAMBIO X > 10.22");
                }

                if (y < -84.379) {
                    y = -84.379;
                    modification = true;
                    Log.d("MAPAS", "CAMBIO Y");
                } else if (y > -84.013) {
                    y = -84.013;
                    modification = true;
                    Log.d("MAPAS", "CAMBIO Y");
                }
                if (modification) {
                    Log.d("MAPAS", "REQUIERE MODIFICACION");
                    mMap.stopAnimation();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(x, y), (float) zoom));
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (fabMenuConfig.isExpanded()) {
            fabMenuConfig.collapse();
        } else if (fabMenuFilter.isExpanded()) {
            fabMenuFilter.collapse();
        } else if (marcadorActivo.isInfoWindowShown()) {
            marcadorActivo.hideInfoWindow();
        } else {
            super.onBackPressed();
        }
    }

    public void crearMarcadores() {
        LatLng coordenadas = new LatLng(9.913558506393061, -84.19467810541391);
        LatLng coordenadas2 = new LatLng(9.916558506393061, -84.19467810541391);
        float coloresMarcadores[] = {283.0f, 34.0f, 240.0f, 0.0f, 125.0f};

        new CrearMarcadores(marcadores).execute();


    }

}
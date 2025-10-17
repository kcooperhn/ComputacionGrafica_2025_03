package hn.uth.miscodigosbarra.ui.dashboard;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import hn.uth.miscodigosbarra.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private static final int REQUEST_PERMISSIONS = 1000;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    private FragmentDashboardBinding binding;
    private static String IMAGE_CAMERA_TAG = "IMAGEN_CAMARA";
    private String directorioImagenes;
    private Bitmap imagenSeleccionada;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        directorioImagenes = "";
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.buttonSelectPicture.setOnClickListener(v -> {
            abrirGaleria();
        });

        binding.buttonTakePicture.setOnClickListener(v -> {
            tomarFotografia();
        });
        

        return root;
    }

    private void tomarFotografia() {
        if(revisarPermisos()){
            Log.d(IMAGE_CAMERA_TAG,"Permisos concedidos");
            Intent tomarFoto = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if(tomarFoto.resolveActivity(this.getActivity().getPackageManager()) != null){
                Log.d(IMAGE_CAMERA_TAG,"Si hay app para tomar fotos");
                File archivoFoto = null;
                try{
                    archivoFoto = createImageFile();
                }catch (Exception e){
                    Log.d(IMAGE_CAMERA_TAG,"Error al crear el archivo");
                    e.printStackTrace();
                }
                if(archivoFoto != null){
                    Uri fotoUri = FileProvider.getUriForFile(this.getContext(), "hn.uth.miscodigosbarra.fileprovider", archivoFoto);
                    tomarFoto.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fotoUri);
                }
                startActivityForResult(tomarFoto, REQUEST_IMAGE_CAPTURE);
                Log.d(IMAGE_CAMERA_TAG,"Intent de camara ejecutado");
            }else{
                Log.d(IMAGE_CAMERA_TAG,"No se encontró camara disponible");
            }
        }else{
            Log.d(IMAGE_CAMERA_TAG,"No se  tienen los permisos");
        }
    }

    private File createImageFile() throws IOException {
        String fechaHoy = new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis());
        String nombreArchivo = "JPEG_" + fechaHoy + "_";
        File folderImagenes = this.getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File archivoImagen = File.createTempFile(nombreArchivo, ".jpg", folderImagenes);
        directorioImagenes = archivoImagen.getAbsolutePath();

        return archivoImagen;
    }

    private void abrirGaleria() {
        if(revisarPermisos()){
            Intent galeria = new Intent(Intent.ACTION_GET_CONTENT);
            galeria.setType("image/*");

            Intent selecionIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            selecionIntent.setType("image/*");

            Intent menuSeleccion = Intent.createChooser(galeria, "Seleccione una imagen");
            menuSeleccion.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{selecionIntent});

            startActivityForResult(menuSeleccion, REQUEST_PICK_IMAGE);
        }else{
            Log.d(IMAGE_CAMERA_TAG,"No se  tienen los permisos");
        }
    }
    
    private boolean revisarPermisos(){
        int cameraPermission = ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.CAMERA);
        Log.d(IMAGE_CAMERA_TAG,"Evaluando permisos de camara");
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            int storagePermission = ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(cameraPermission != PackageManager.PERMISSION_GRANTED || storagePermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
                Log.d(IMAGE_CAMERA_TAG,"No se  tienen los permisos");
                return false;
            }
        }else{
            if(cameraPermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
                Log.d(IMAGE_CAMERA_TAG,"No se  tienen los permisos");
                return false;
            }
        }
        Log.d(IMAGE_CAMERA_TAG,"Permisos concedidos");
        
        return true;
    }
    
    private void AnalizarCodigoBarras(){
        Log.d(IMAGE_CAMERA_TAG,"Comenzando analisis con ML Kit");

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                .enableAllPotentialBarcodes()
                .build();
        int gradosRotacion = 90;
        InputImage imagen = InputImage.fromBitmap(imagenSeleccionada, gradosRotacion);
        Log.d(IMAGE_CAMERA_TAG,"Imagen rotada "+gradosRotacion+" grados");

        BarcodeScanner scanner = BarcodeScanning.getClient();
        scanner.process(imagen).addOnSuccessListener(barcodes -> {
            Log.d(IMAGE_CAMERA_TAG,"Comenzando lectura de codigo de barra");
            if (barcodes.isEmpty()) {
                showMessage("No se encontraron códigos de barras en la imagen");
                Log.d(IMAGE_CAMERA_TAG,"No se encontraron códigos de barras en la imagen");
            }else{
                Log.d(IMAGE_CAMERA_TAG,"Se detectaron codigos de barra en la imagen");
            }
            for (Barcode barcode : barcodes) {
                Log.d(IMAGE_CAMERA_TAG,"Codigo de barras detectado, leyendo...");
                Rect bounds = barcode.getBoundingBox();
                Point[] corners = barcode.getCornerPoints();
                String rawValue = barcode.getRawValue();

                int valueType = barcode.getValueType();

                switch (valueType) {
                    case Barcode.TYPE_WIFI:
                        Log.d(IMAGE_CAMERA_TAG,"Codigo de barras de tipo WIFI");
                        String ssid = barcode.getWifi().getSsid();
                        String password = barcode.getWifi().getPassword();
                        int type = barcode.getWifi().getEncryptionType();
                        binding.textViewMlkitResult.setText("SSID: " + ssid + "\nPassword: " + password + "\nTipo: " + type);
                        break;
                    case Barcode.TYPE_URL:
                        Log.d(IMAGE_CAMERA_TAG,"Codigo de barras de tipo URL");
                        String title = barcode.getUrl().getTitle();
                        String url = barcode.getUrl().getUrl();
                        binding.textViewMlkitResult.setText("Titulo: " + title + "\nURL: " + url);
                        break;
                    default:
                        Log.d(IMAGE_CAMERA_TAG,"Otro tipo de Codigo de barras");
                        binding.textViewMlkitResult.setText("Valor: " + rawValue);
                        break;
                }
            }

        }).addOnFailureListener(error -> {
            Log.d(IMAGE_CAMERA_TAG,"Fallo al procesar escaneo de codigos de barra");
            String mensajeError = "Error al obtener información de códigos de barra de la imagen: "  + error.getLocalizedMessage() ;
            binding.textViewMlkitResult.setText(mensajeError);
            showMessage("Error al obtener la información de la imagen");
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            //ABRI LA APLICACION DE CAMARA Y TOME BIEN LA FOTO
            if(!"".equals(directorioImagenes) && directorioImagenes != null){
                File imagen = new File(directorioImagenes);
                if(imagen.exists()){
                    Bitmap bitmap = BitmapFactory.decodeFile(imagen.getAbsolutePath());
                    int megapixeles = bitmap.getWidth() * bitmap.getHeight() / 1000000;
                    int factor = megapixeles / 2; // CALCULANDO FACTOR DE REDUCCIÓN DE LA IMAGEN. 10MX -> 5MX
                    Log.d(IMAGE_CAMERA_TAG,"Imagen tomada es de tamaño: " +bitmap.getWidth()+"x"+bitmap.getHeight()+" = " + megapixeles + " megapixeles");

                    //REDIMENSIONANDO IMAGEN
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / factor, bitmap.getHeight() / factor, false);
                    imagenSeleccionada = bitmap;
                    megapixeles = bitmap.getWidth() * bitmap.getHeight() / 1000000;
                    Log.d(IMAGE_CAMERA_TAG,"Imagen redimensionada es de tamaño: " +bitmap.getWidth()+"x"+bitmap.getHeight()+" = " + megapixeles + " megapixeles");
                    binding.imageViewCaptured.setImageBitmap(imagenSeleccionada);
                    AnalizarCodigoBarras();
                }
            }
        }
        if(requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK){
            //ABRI LA APLICACION DE GALERIA Y SELECCIONE UNA FOTO
            if(data == null){
                Log.d(IMAGE_CAMERA_TAG,"No hay foto seleccionada");
                showMessage("No selecciono ninguna foto, favor seleccione una");
            }else{
                Uri fotoUri = data.getData();
                try {
                    InputImage imagen = InputImage.fromFilePath(this.getContext(), fotoUri);
                    binding.imageViewCaptured.setImageURI(fotoUri);
                    imagenSeleccionada = imagen.getBitmapInternal();
                    AnalizarCodigoBarras();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void showMessage(String message){
        Toast.makeText(this.getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
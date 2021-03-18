package com.example.photoencrypter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ScrollingActivity extends AppCompatActivity {

    protected PackageManager packageManager;
    protected FloatingActionButton cameraButton;
    protected ImageView contentImageView;
    protected ListView contentListView;
    protected Button encryptButton;
    protected Button decryptButton;
    protected CustomListAdapter customListAdapter;

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 0;
    private static final int REQUEST_TAKE_PHOTO = 1;

    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    protected LinkedList unEncryptedPhotoPaths = new LinkedList();
    protected LinkedList encryptedPhotoPaths = new LinkedList();
    protected String USER_KEY = "";

    protected String selectedFile;

    protected File encryptedImage;
    protected File decryptedImage;
    protected File unEncryptedImage;
    protected File unDecryptedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        packageManager = this.getPackageManager();

        checkPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent newData) {
        super.onActivityResult(requestCode, resultCode, newData);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            unEncryptedImage = new File((String) unEncryptedPhotoPaths.get(unEncryptedPhotoPaths.size() -1));
            Uri contentUri = Uri.fromFile(unEncryptedImage);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
            contentImageView.setImageURI(contentUri);
            encryptButton.setEnabled(true);

        } else {
            Toast.makeText(ScrollingActivity.this, "Action cancelled.", Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED){
            switch (requestCode) {
                case REQUEST_CODE_ASK_PERMISSIONS:

                    if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
                        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                        setSupportActionBar(toolbar);

                        contentImageView = (ImageView) findViewById(R.id.contentImageView);
                        contentListView = (ListView) findViewById(R.id.contentListView);
                        contentListView.setLongClickable(true);
                        contentListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                                //TODO
                                contentListView.setSelection(position);
                                selectedFile = contentListView.getItemAtPosition(position).toString();
                                unDecryptedImage = new File(selectedFile);
                                Toast.makeText(ScrollingActivity.this, "Encrypted item selected.", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        });

                        cameraButton = (FloatingActionButton) findViewById(R.id.fab);
                        cameraButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dispatchTakePictureIntent();
                            }
                        });

                        encryptButton = (Button) findViewById(R.id.contentEncryptButton);
                        encryptButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                USER_KEY = ((EditText) findViewById(R.id.contentEditText)).getText().toString();
                                if (USER_KEY.length() < 32) {
                                    int MAX_LENGTH = 32 - USER_KEY.length();
                                    Random generator = new Random();
                                    StringBuilder randomStringBuilder = new StringBuilder();
                                    char tempChar;
                                    for (int i = 0; i < MAX_LENGTH; i++){
                                        tempChar = (char) (generator.nextInt(96) + 32);
                                        randomStringBuilder.append(tempChar);
                                    }
                                    USER_KEY = randomStringBuilder.toString() + USER_KEY;
                                }
                                Toast.makeText(ScrollingActivity.this, "User Key: " + USER_KEY, Toast.LENGTH_LONG).show();

                                byte[] rawData;
                                byte[] data;
                                byte[] key = new byte[0];
                                byte[] base64Encoded;

//                                byte[] originalData;
//                                byte[] base64Decoded;

                                unEncryptedImage = new File((String) unEncryptedPhotoPaths.get(unEncryptedPhotoPaths.size() -1));
                                FileInputStream fis = null;
                                try {
                                    fis = new FileInputStream(unEncryptedImage);
                                    key = USER_KEY.getBytes(StandardCharsets.UTF_8);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                Bitmap bm = BitmapFactory.decodeStream(fis);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                rawData = baos.toByteArray();

//                                int byteCounter = 0;
//                                int byteArrayPartSize = rawData.length % (rawData.length / 8);
//                                LinkedList<byte[]> rawDataParts = new LinkedList<>();
//                                byte[] encryptedDataParts = new byte[];
//                                for (int i = 0; i < byteArrayPartSize; i++) {
//                                    rawDataParts.add(Arrays.copyOfRange(rawData, byteCounter, byteArrayPartSize));
//                                    byteCounter += byteArrayPartSize;
//
//                                    data = AES.encrypt_byte_array(rawDataParts.get(i), key);
//                                    base64Encoded = Base64.getEncoder().encode(data);
//                                    encryptedDataParts += base64Encoded;
//                                }

                                data = AES.encrypt_byte_array(rawData, key);
                                base64Encoded = Base64.getEncoder().encode(data);

//                                base64Decoded = Base64.getDecoder().decode(base64Encoded);
//                                originalData = AES.decrypt_byte_array(base64Decoded, key);
//
//                                if (!Arrays.equals(rawData, originalData)) {
//                                    Log.e("Encryption", "That didn't worked. Fuck...");
//                                }

                                FileOutputStream fileOutputStream = null;
                                try {
                                    encryptedImage = createImageFile("Encrypted");
                                    if (encryptedImage != null) {
                                        fileOutputStream = new FileOutputStream(encryptedImage);
                                        fileOutputStream.write(base64Encoded);
                                        ScrollingActivity.this.getContentResolver().delete(FileProvider.getUriForFile(ScrollingActivity.this,
                                                "com.example.photoencrypter.fileprovider",
                                                unEncryptedImage), null, null);
                                        unEncryptedPhotoPaths.remove(unEncryptedImage.getAbsolutePath());
                                        encryptedPhotoPaths.add(encryptedImage.getAbsolutePath());
                                        customListAdapter = new CustomListAdapter(ScrollingActivity.this, android.R.layout.simple_list_item_1, encryptedPhotoPaths);
                                        customListAdapter.notifyDataSetChanged();
                                        contentListView.setAdapter(customListAdapter);
                                        if (unEncryptedPhotoPaths.size() == 0) {
                                            contentImageView.setImageDrawable(null);
                                            encryptButton.setEnabled(false);
                                        } else {
                                            contentImageView.setImageURI(FileProvider.getUriForFile(ScrollingActivity.this,
                                                    "com.example.photoencrypter.fileprovider",
                                                    unEncryptedImage));
                                        }
                                        Toast.makeText(ScrollingActivity.this, "Image encrypted successfully.", Toast.LENGTH_LONG).show();
                                    }
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                    Toast.makeText(ScrollingActivity.this, "Image not encrypted.", Toast.LENGTH_LONG).show();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(ScrollingActivity.this, "Image not encrypted.", Toast.LENGTH_LONG).show();
                                } finally {
                                    if (fileOutputStream != null) {
                                        try {
                                            fileOutputStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        });

                        decryptButton = (Button) findViewById(R.id.contentDecryptButton);
                        decryptButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                USER_KEY = ((EditText) findViewById(R.id.contentEditText)).getText().toString();
                                if (USER_KEY.length() < 32) {
                                    int MAX_LENGTH = 32 - USER_KEY.length();
                                    Random generator = new Random();
                                    StringBuilder randomStringBuilder = new StringBuilder();
                                    char tempChar;
                                    for (int i = 0; i < MAX_LENGTH; i++){
                                        tempChar = (char) (generator.nextInt(96) + 32);
                                        randomStringBuilder.append(tempChar);
                                    }
                                    USER_KEY = randomStringBuilder.toString() + USER_KEY;
                                }
                                Toast.makeText(ScrollingActivity.this, "User Key: " + USER_KEY, Toast.LENGTH_LONG).show();

                                byte[] rawData;
                                byte[] data;
                                byte[] key;
                                byte[] base64Decoded = new byte[0];
                                try {
                                    data = Files.readAllBytes(Paths.get(selectedFile));
                                    base64Decoded = Base64.getDecoder().decode(data);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                key = USER_KEY.getBytes(StandardCharsets.UTF_8);
                                rawData = AES.decrypt_byte_array(base64Decoded, key);
                                FileOutputStream fileOutputStream = null;
                                try {
                                    decryptedImage = createImageFile(Environment.DIRECTORY_PICTURES);
                                    if (decryptedImage != null) {
                                        fileOutputStream = new FileOutputStream(decryptedImage);
                                        fileOutputStream.write(rawData);
                                        // TODO
                                        ScrollingActivity.this.getContentResolver().delete(FileProvider.getUriForFile(ScrollingActivity.this,
                                                "com.example.photoencrypter.fileprovider",
                                                unDecryptedImage), null, null);
                                        encryptedPhotoPaths.remove(unDecryptedImage.getAbsolutePath());
                                        unEncryptedPhotoPaths.add(decryptedImage.getAbsolutePath());
                                        customListAdapter.remove(selectedFile);
                                        customListAdapter = new CustomListAdapter(ScrollingActivity.this, android.R.layout.simple_list_item_1, encryptedPhotoPaths);
                                        customListAdapter.notifyDataSetChanged();
                                        contentListView.setAdapter(customListAdapter);
                                        if (unEncryptedPhotoPaths.size() == 0) {
                                            contentImageView.setImageDrawable(null);
                                            encryptButton.setEnabled(false);
                                        } else {
                                            contentImageView.setImageURI(FileProvider.getUriForFile(ScrollingActivity.this,
                                                    "com.example.photoencrypter.fileprovider", decryptedImage));
                                            encryptButton.setEnabled(true);
                                        }
                                        Toast.makeText(ScrollingActivity.this, "Image decrypted successfully.", Toast.LENGTH_LONG).show();
                                    }
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                    Toast.makeText(ScrollingActivity.this, "Image not decrypted.", Toast.LENGTH_LONG).show();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(ScrollingActivity.this, "Image not decrypted.", Toast.LENGTH_LONG).show();
                                } finally {
                                    if (fileOutputStream != null) {
                                        try {
                                            fileOutputStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            }
                        });

//                        encryptedListView = (ListView) findViewById(R.id.contentListView);
//                        encryptedListView.setAdapter(customListAdapter);

                    } else {
                        Toast.makeText(ScrollingActivity.this, "A Camera is necessary in order to use the app.\n" +
                                "App will close.", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    break;

                case 1:
                    break;

                default:
                    break;
            }
        } else {
            Toast.makeText(this, "Permissions not granted. App will close.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();

        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(Environment.DIRECTORY_PICTURES);
                unEncryptedPhotoPaths.add(unEncryptedPhotoPaths.size(), photoFile.getAbsolutePath());
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(ScrollingActivity.this,
                        "com.example.photoencrypter.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile(String dir) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = dir + "_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(dir);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }
}

package com.logitech.btchatapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity{

    private LinearLayout startedLayout;
    private ExpandableListView chatListView;
    private Button scanButton;
    private LinearLayout chatLayout;

    private ListView ConversationView;
    private EditText OutEditText;
    private Button SendButton;
    private ImageView add_files, selectedImg;

    private Dialog dialog;

    private BluetoothDevice connectingDevice;
    private String connectingDeviceAddress;

    private BluetoothAdapter myBluetoothAdapter;

    private ArrayAdapter<String> myNewDevicesArrayAdapter;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    static final int RESULT_LOAD_FILE = 1;
    private String path;
    private static final String[] INITIAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION};

    private static final int INITIAL_REQUEST = 1337;
    private static final int REQUEST_WRITE_STORAGE = INITIAL_REQUEST+4;

    private ChatController chatController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewsByIds();

        if (myBluetoothAdapter == null) {
            Toast.makeText(getApplication(), "Cet appareil ne supporte pas la connexion par bluetooth!", Toast.LENGTH_LONG).show();
        }

        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        ConversationView.setAdapter(chatAdapter);
        ConversationView.setSelection(chatMessages.size()-1);

        if (!canAccessLocation() || !canAccessCamera() || !canAccessWriteStorage() || !canAccessReadStorage()
                || !canAccessReadContacts() || !canAccessWriteContacts()){
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
            }
        }

        //Ajout de l'action liés au boutton d'ajout de fichiers
        add_files = findViewById(R.id.add_files);
        add_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                 intent.setType("*/*");
                 startActivityForResult(intent, RESULT_LOAD_FILE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_WRITE_STORAGE:
                if (canAccessWriteStorage()){
                    Toast.makeText(this,"Permission obtenue!", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(this, "Cette application n'est pas autorisée à avoir accès à votre gestionnaire de fichier; " +
                            "ce qui pourrait l'empêcher de fonctionner normalement", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void findViewsByIds(){

        selectedImg = findViewById(R.id.sendImage);
        chatLayout = findViewById(R.id.chat_layout);
//        startedLayout = findViewById(R.id.started_layout);
//        chatListView = findViewById(R.id.chat_list_view);
//        scanButton = findViewById(R.id.scan_btn);

        ConversationView = findViewById(R.id.in);
        OutEditText = findViewById(R.id.edit_text_out);
        SendButton = findViewById(R.id.button_send);

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

//        scanButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!myBluetoothAdapter.isEnabled()) {
//                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
//                    // Otherwise, setup the chat session
//                }else{
//                   showPrinterPickDialog();
//                }
//            }
//        });

        SendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OutEditText.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Entrez du texte s'il vous plaît !", Toast.LENGTH_SHORT).show();
                } else {

                    sendMessage(OutEditText.getText().toString());
                    OutEditText.setText("");
                }
            }
        });
    }

    private void showPrinterPickDialog(){

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_device_list);
        dialog.setTitle("Bluetooth Devices");

        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        myNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        ListView pairedListView = dialog.findViewById(R.id.paired_devices);
        ListView newDevicesListView = dialog.findViewById(R.id.new_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        newDevicesListView.setAdapter(myNewDevicesArrayAdapter);

        if (myBluetoothAdapter.isDiscovering()) {
            myBluetoothAdapter.cancelDiscovery();
        }
        myBluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }

        pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                myBluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                if (info.length()>17){
                    String address = info.substring(info.length() - 17);
                    connectingDeviceAddress = address;
                    connectToDevice(address);
                    dialog.dismiss();
                }else{
                    Toast.makeText(getApplication(), "Pas d'appareil sélectionné !!! Veuillez réessayer", Toast.LENGTH_LONG).show();
                }
            }
        });

        newDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                myBluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                if (info.length()>17){
                    String address = info.substring(info.length() - 17);

                    connectToDevice(address);
                    dialog.dismiss();
                }else{
                    Toast.makeText(getApplication(), "Pas d'appareil sélectionné !!! Veuillez réessayer", Toast.LENGTH_LONG).show();
                }
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void connectToDevice(String deviceAddress) {
        myBluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = myBluetoothAdapter.getRemoteDevice(deviceAddress);
        chatController.connect(device);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.scan_btn:
                if (!myBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
                    // Otherwise, setup the chat session
                }else{
                    showPrinterPickDialog();
                }
                break;
            case R.id.ecoute:
                chatController.start();
                Toast.makeText(getApplication(), "En écoute", Toast.LENGTH_LONG).show();
                break;
            case R.id.profil:
                String current_name = myBluetoothAdapter.getName();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Change Device Name");

                //Current Name
                final TextView textView = new TextView(this);
                textView.setText("Votre Nom : "+current_name);
                builder.setView(textView);

                // Set up the input
                final EditText input = new EditText(this);
                // Specify the type of input expected; this, for aduech, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT );
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Changer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = "";
                        m_Text = input.getText().toString();
                        myBluetoothAdapter.setName(m_Text);
                        Toast.makeText(getApplication(), "Votre nom a été mis à jour !", Toast.LENGTH_LONG).show();

                    }
                });
                builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                break;
            case R.id.visible:
                if (myBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                }
                break;
            case R.id.about:
                Intent intent = new Intent(this,
                        About.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        chatLayout.setVisibility(View.VISIBLE);
        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        }else{
            chatController = new ChatController(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatController != null)
            chatController.stop();
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                //Au niveau de la vérification de l'établissement de la connexion
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        //lorsque l'application est déja connecté
                        case ChatController.STATE_CONNECTED:
//                            Toast.makeText(getApplicationContext(), "Connecté à : " + connectingDevice.getName(), Toast.LENGTH_SHORT).show();
//                            scanButton.setEnabled(false);
//                            LinearLayout linear = findViewById(R.id.chat_layout);
//                            LinearLayout back = findViewById(R.id.started_layout);
//                            back.setVisibility(View.GONE);
//                            linear.setVisibility(View.VISIBLE);
                            break;
                        //Pendant la connexion de l'application
                        case ChatController.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(), "Connection en cours ...", Toast.LENGTH_SHORT).show();

//                            scanButton.setEnabled(false);
                            break;
                        case ChatController.STATE_LISTEN:
                            //Apres echec de la connexion ou lorsque l'on est du tout pas connecté
                            break;
                        case ChatController.STATE_NONE:
                            Toast.makeText(getApplicationContext(), "Echec de la Connection", Toast.LENGTH_SHORT).show();
//                            scanButton.setEnabled(true);
//                            LinearLayout lay = findViewById(R.id.chat_layout);
//                            LinearLayout bak = findViewById(R.id.started_layout);

//                            bak.setVisibility(View.VISIBLE);
//                            lay.setVisibility(View.GONE);
                            break;
                    }
                    break;
                //Processus d'ecriture de message
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    chatMessages.add("Me: " + writeMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case Constants.MESSAGE_WRITE_FILE:
                    byte[] writeFileBuf = (byte[]) msg.obj;

                    Bitmap bitmap = BitmapFactory.decodeByteArray(writeFileBuf, 0, msg.arg1);
                    selectedImg.setImageBitmap(bitmap);
                    break;
                case Constants.MESSAGE_RECEIVE_FILE:
                    byte[] readBuffFile = (byte[]) msg.obj;

                    Bitmap bitmape = BitmapFactory.decodeByteArray(readBuffFile, 0, msg.arg1);
                    selectedImg.setImageBitmap(bitmape);
                    break;
                //Processus de reception de message
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);
                    chatMessages.add(connectingDevice.getName() + ":  " + readMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                //Affichage de l'appareil à qui l'on est connecté
                case Constants.MESSAGE_DEVICE_NAME:
                    connectingDevice = msg.getData().getParcelable(Constants.DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connecté à " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void sendMessage(String message) {
        if (chatController.getState() != ChatController.STATE_CONNECTED) {
            Toast.makeText(this, "Oups connexion perdue!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatController.write(send);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    chatController = new ChatController(this, handler);
                } else {
                    Toast.makeText(this, "Bluetooth n'a pas été activé!", Toast.LENGTH_SHORT).show();
                }
               break;

            case RESULT_LOAD_FILE:
                if (resultCode==RESULT_OK){
                    final Uri uriPath = data.getData();
                    path = getPath(this, uriPath);

//                    File f = new File(path);
//                    Intent intent = new Intent();
//                    intent.setType("*/*");
//                    intent.putExtra(Intent.EXTRA_STREAM, uriPath);
//
//                    PackageManager pm = getPackageManager();
//                    List<ResolveInfo> applist = pm.queryIntentActivities(intent,0);
//
//                    if (applist.size()>0){
//                        String packageName = null;
//                        String className = null;
//                        boolean found = false;
//
//                        for (ResolveInfo info:applist){
//                            packageName = info.activityInfo.packageName;
//                            if (packageName.equals("com.android.bluetooth")){
//                                className = info.activityInfo.name;
//                                found = true;
//                                break;
//                            }
//                        }
//                        if (!found){
//                            Toast.makeText(this, "not found", Toast.LENGTH_LONG).show();
//                        }else {
//                            intent.setClassName(packageName,className);
//                            startActivity(intent);
//                        }
//                    }
//                    try {
//                        ParcelFileDescriptor inputPFD = getContentResolver().openFileDescriptor(uriPath, "r");
//                        FileDescriptor fd = inputPFD.getFileDescriptor();
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
                    sendMessage(path);

                }else {
                    Toast.makeText(getApplicationContext(), "Vous n'avez pas choisi d'image", Toast.LENGTH_LONG).show();
                }
        }
    }

    private boolean canAccessWriteStorage(){
        return (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }
    private boolean canAccessReadContacts(){
        return (hasPermission(Manifest.permission.READ_CONTACTS));
    }
    private boolean canAccessReadStorage(){
        return (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
    }
    private boolean canAccessWriteContacts(){
        return (hasPermission(Manifest.permission.WRITE_CONTACTS));
    }
    private boolean canAccessCamera(){
        return (hasPermission(Manifest.permission.CAMERA));
    }
    private boolean canAccessLocation(){
        return (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    }
    private boolean hasPermission(String perm){
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));
    }

    public static String getPath(final Context context, final Uri uri){
        final boolean isKitKatOrAbove = Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT;

        if (isKitKatOrAbove && DocumentsContract.isDocumentUri(context,uri)){

            if (isExternalStorageDocument(uri)){
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)){
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }else if (isDownloadsDocuments(uri)){
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);

            }else if (isMediaDocument(uri)){
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)){
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }else if ("video".equals(type)){
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }else if ("audio".equals(type)){
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }

        else if("content".equalsIgnoreCase(uri.getScheme())){
            return getDataColumn(context,uri,null,null);
        }
        else if("file".equalsIgnoreCase(uri.getScheme())){
            return uri.getPath();
        }
        return null;
    }
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs){

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try{
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }
    public static boolean isExternalStorageDocument(Uri uri){
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    public static boolean isDownloadsDocuments(Uri uri){
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
    public static boolean isMediaDocument(Uri uri){
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    myNewDevicesArrayAdapter.add(device .getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (myNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    myNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    @Override
    public  void onBackPressed(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Voulez-vous vraiment quitter? ");
        builder.setCancelable(true);

        // Set up the buttons
        builder.setPositiveButton("Quitter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}



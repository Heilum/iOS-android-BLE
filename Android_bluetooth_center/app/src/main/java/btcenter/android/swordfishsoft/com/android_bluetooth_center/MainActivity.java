package btcenter.android.swordfishsoft.com.android_bluetooth_center;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static final String Peripheral_service_Uuid = "14FB2349-72FE-4CA2-94D6-1F3CB16331EE";
    public static final String Peripheral_characteristic_Uuid = "1A3E4B28-522D-4B3B-82A9-D5E2004534FC";
    public static final String Peripheral_characteristic_Uuid_for_writting = "2A3E4B28-522D-4B3B-82A9-D5E2004534FC";

    private BluetoothManager bluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;


    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCharacteristic  characteristicForRead;
    private BluetoothGattCharacteristic  characteristicForWrite;


    private EditText logger;

    private Timer timer;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        logger = (EditText) this.findViewById(R.id.logger);

        bluetoothManager =
        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothAdapter.enable();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {

                    appendLog("find BT device = " + device.getName());
                    mBluetoothGatt = device.connectGatt(MainActivity.this,false,mGattCallback);

                }
            };


    private void appendLog(final String log){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logger.append("\n"+log);

            }

        });

    }
    private void clearLog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logger.setText("");
            }
        });


    }

    private  void closeBluetooth(){

        if (timer != null){
            timer.cancel();
            timer = null;
        }

        if (mBluetoothGatt != null){
            mBluetoothGatt.close();
            mBluetoothGatt = null;

        }



        mBluetoothAdapter.stopLeScan(mLeScanCallback);


    }




    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {

                    if (newState == BluetoothProfile.STATE_CONNECTED) {

                        appendLog("Connected to GATT server.");
                        mBluetoothGatt.discoverServices();


                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        closeBluetooth();
                        rescan();

                    }
                }



                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {



                        List<BluetoothGattService> services  = gatt.getServices();
                        for (BluetoothGattService service : services){

                            String serviceId = service.getUuid().toString().toUpperCase();



                            if (serviceId.equals(Peripheral_service_Uuid)){

                                appendLog("total characteristics = " + service.getCharacteristics().size());

                                characteristicForRead =   service.getCharacteristic(UUID.fromString(Peripheral_characteristic_Uuid));
                                characteristicForWrite =   service.getCharacteristic(UUID.fromString(Peripheral_characteristic_Uuid_for_writting));



                                if (timer == null){
                                    timer = new Timer(true);
                                }
                                TimerTask read_task = new TimerTask() {
                                    public void run() {
                                        if (mBluetoothGatt != null){
                                            mBluetoothGatt.readCharacteristic(characteristicForRead);

                                        }
                                    }
                                };
                                TimerTask write_task = new TimerTask() {
                                    public void run() {
                                        if (mBluetoothGatt != null){
                                            //mBluetoothGatt.readCharacteristic(characteristicForRead);

                                            //characteristicForWrite.setValue("Response from central");

                                            characteristicForWrite.setValue(("Response from central").getBytes());
                                            mBluetoothGatt.writeCharacteristic(characteristicForWrite);
                                        }
                                    }
                                };

                                timer.schedule(read_task,0,1000);
                                timer.schedule(write_task,0,1500);

                                break;
                            }
                        }



                    } else {
                        appendLog("Find service false. status = " + status );
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        byte[] bs = characteristic.getValue();
                        String value = new String(bs);
                        appendLog(">>>read value:"+value);



                        //boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                        //appendLog("setCharacteristicNotification result = " + b);
                    }else{
                        appendLog(">>>read value fail");
                    }
                }

                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    appendLog("onCharacteristicChanged");

                    mBluetoothGatt.readCharacteristic(characteristic);

                }

                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic, int status) {

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        byte[] bs = characteristic.getValue();
                        String value = new String(bs);
                        appendLog(">>>write value:"+value);



                        //boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                        //appendLog("setCharacteristicNotification result = " + b);
                    }else{
                        appendLog(">>>write value fail");
                    }
                }


            };


    private  void rescan(){
        UUID[] serviceUuids = { UUID.fromString(Peripheral_service_Uuid) };
        mBluetoothAdapter.startLeScan(serviceUuids, mLeScanCallback);
    }

    public void onStart(View v) {
        rescan();

    }

    public void onRead(View v) {

    }

    public void onWrite(View v) {

    }

    public void onClose(View v) {
        clearLog();
        closeBluetooth();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}

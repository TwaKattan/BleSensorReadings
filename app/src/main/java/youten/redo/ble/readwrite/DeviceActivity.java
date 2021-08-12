package youten.redo.ble.readwrite;
import java.util.Arrays;
import java.util.UUID;
import youten.redo.ble.util.BleUtil;
import youten.redo.ble.util.BleUuid;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

// when click on any sensor to open its info.
public class DeviceActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "BLEDevice";

	public static final String EXTRA_BLUETOOTH_DEVICE = "BT_DEVICE";
	// characterstics related to BLE that must be defined to jave and objects, to establish BLE connection
	private BluetoothAdapter mBTAdapter;
	private BluetoothDevice mDevice;
	private BluetoothGatt mConnGatt;
	private int mStatus;

    // buttons name
	private Button mReadHumidityButton;
	private Button mReadTemperatureButton;


	// mGattcallback, fuction call based on certin event, ex/ start device scanning
	private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
		@Override
		//onConnectionStateChange: Callback indicating when the app has connected/disconnected to/from a the sensor.
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
											int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				mStatus = newState;
				mConnGatt.discoverServices();

				// if disconeection happen update the state and disabled the humidity and temp. buttons (THE OLD PROJECT)
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				mStatus = newState;
				runOnUiThread(new Runnable() {
					public void run() {
						mReadHumidityButton.setEnabled(false);
						mReadTemperatureButton.setEnabled(false);
					};
				});
			}
		};

		@Override
		//Callback invoked when the list of remote services, characteristics and descriptors for the remote device have been updated, ie new services have been discovered.
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			for (BluetoothGattService service : gatt.getServices()) {
				// IF there is no service or have no name
				if ((service == null) || (service.getUuid() == null)) {
					continue;
				}
				// xiomi sensor detected (SENSOR_DEVICE_INFORMATION) "ebe0ccb0-7a0a-4b0c-8a1a-6ff2997da3a6"
				if (BleUuid.SENSOR_DEVICE_INFORMATION.equalsIgnoreCase(service
						.getUuid().toString())) {
					//enable_notification, enable the sensor notifications to send data to the application
					BluetoothGattCharacteristic enable_notification=service.getCharacteristic(UUID
							.fromString(BleUuid.Sensor_data));
					// true, is a flag to enable sending sensor information
					gatt.setCharacteristicNotification(enable_notification, true);
					// sensor information (00002902-0000-1000-8000-00805f9b34fb)
					UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
					BluetoothGattDescriptor desc = enable_notification.getDescriptor(CONFIG_DESCRIPTOR);
					desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					gatt.writeDescriptor(desc);


					// after getting the notification, provide the humidity and temp. data
					mReadHumidityButton
							.setTag(service.getCharacteristic(UUID
									.fromString(BleUuid.Sensor_data)));
					mReadTemperatureButton
							.setTag(service.getCharacteristic(UUID
									.fromString(BleUuid.Sensor_data2)));
					runOnUiThread(new Runnable() {
						public void run() {
							mReadHumidityButton.setEnabled(true);
							mReadTemperatureButton.setEnabled(true);
						};
					});
				}

			}

			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
				};
			});
		};

		// how to covert the temp. value from the sensor format to LittleEndian(final readable) format
		private static final String HEXES = "0123456789ABCDEF";

		public String getHex(byte[] raw) {
			final StringBuilder hex = new StringBuilder(2 * raw.length);
			for (final byte b : raw) {
				hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
			}
			return hex.toString();
		}

		public  int toLittleEndian(final String hex) {
			int ret = 0;
			String hexLittleEndian = "";
			if (hex.length() % 2 != 0) return ret;
			for (int i = hex.length() - 2; i >= 0; i -= 2) {
				hexLittleEndian += hex.substring(i, i + 2);
			}
			ret = Integer.parseInt(hexLittleEndian, 16);
			return ret;
		}

		@Override
		// onCharacteristicChanged : important characterstic in BLE that dettedct the temp and humidity value changes.
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {


			// ebe0ccc1-7a0a-4b0c-8a1a-6ff2997da3a6, xiomi humidity and sensor data characterstics
			if (BleUuid.Sensor_data
					.equalsIgnoreCase(characteristic.getUuid().toString())) {

				String ff= getHex(characteristic.getValue());
				// tempreture, reading first 4 bytes
				String sub=ff.substring(0, 4);
				int littlesub=toLittleEndian(sub);
				double t = littlesub/100.0;
				// reading  humidity value, 2 index in the aray is the humidity
				float h = characteristic.getValue()[2];
				final String name = "Humidity: "+h;
				final  String name2= "Temperature: "+t;
				runOnUiThread(new Runnable() {
					public void run() {
						mReadHumidityButton.setText(name);
						setProgressBarIndeterminateVisibility(false);
						mReadTemperatureButton.setText(name2);
						setProgressBarIndeterminateVisibility(false);
					};
				});
			}

		}


		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
										 BluetoothGattCharacteristic characteristic, int status) {

		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
										  BluetoothGattCharacteristic characteristic, int status) {

			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
				};
			});
		};
	};

	@Override
	// onCreate, function that used on navigate between the app layout
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.layout2);

		// state
		mStatus = BluetoothProfile.STATE_DISCONNECTED;
		// bring the buttons ID when access this layout
		mReadHumidityButton = (Button) findViewById(R.id.button4);
		mReadHumidityButton.setOnClickListener(this);
		mReadTemperatureButton = (Button) findViewById(R.id.button2);
		mReadTemperatureButton.setOnClickListener(this);

	}

	//onResume, if the activity comes to the foreground مدام عي بوجعي اعمل تنفيذ
	protected void onResume() {
		super.onResume();

		init();
	}

	@Override
	// when go out of layout
	protected void onDestroy() {
		super.onDestroy();
		if (mConnGatt != null) {
			if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
					&& (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
				mConnGatt.disconnect();
			}
			mConnGatt.close();
			mConnGatt = null;
		}
	}

	@Override
	// when clicking the temp amd huidity buttons
	public void onClick(View v) {
		if (v.getId() == R.id.button4) {
			if ((v.getTag() != null)
					&& (v.getTag() instanceof BluetoothGattCharacteristic)) {
				BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v
						.getTag();
				if (mConnGatt.readCharacteristic(ch)) {
					setProgressBarIndeterminateVisibility(true);
				}
			}
		} else if (v.getId() == R.id.button2) {
			if ((v.getTag() != null)
					&& (v.getTag() instanceof BluetoothGattCharacteristic)) {
				BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v
						.getTag();
				if (mConnGatt.readCharacteristic(ch)) {
					setProgressBarIndeterminateVisibility(true);
				}
			}

		}
	}

	private void init() {
		// BLE check
		if (!BleUtil.isBLESupported(this)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// BT check
		// if not null  get bluetooth adapter

		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBTAdapter = manager.getAdapter();
		}
		// if null  show toast msg that it is not available
		if (mBTAdapter == null) {
			Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		// check BluetoothDevice
		if (mDevice == null) {
			mDevice = getBTDeviceExtra();
			if (mDevice == null) {
				finish();
				return;
			}
		}

		// button disable
		mReadHumidityButton.setEnabled(false);
		mReadTemperatureButton.setEnabled(false);

		// connect to Gatt
		// null means no connection established yet
		if ((mConnGatt == null)
				&& (mStatus == BluetoothProfile.STATE_DISCONNECTED)) {
			// try to connect
			mConnGatt = mDevice.connectGatt(this, false, mGattcallback);
			mStatus = BluetoothProfile.STATE_CONNECTING;
		} else {
			if (mConnGatt != null) {
				// re-connect and re-discover Services
				mConnGatt.connect();
				mConnGatt.discoverServices();
			} else {
				Log.e(TAG, "state error");
				finish();
				return;
			}
		}
		setProgressBarIndeterminateVisibility(true);
	}
// pass data from one activity to other
	private BluetoothDevice getBTDeviceExtra() {
		Intent intent = getIntent();
		if (intent == null) {
			return null;
		}

		Bundle extras = intent.getExtras();
		if (extras == null) {
			return null;
		}

		return extras.getParcelable(EXTRA_BLUETOOTH_DEVICE);
	}

}

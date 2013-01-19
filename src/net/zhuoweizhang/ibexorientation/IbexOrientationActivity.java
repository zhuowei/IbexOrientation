package net.zhuoweizhang.ibexorientation;

import java.net.*;

import android.app.Activity;
import android.hardware.*;
import android.os.Bundle;
import android.widget.*;
import android.view.*;

public class IbexOrientationActivity extends Activity implements View.OnClickListener, SensorEventListener {

	public static final int DEFAULT_PORT = 1982;

	private SensorManager sensorManager;
	private Sensor rotationVectorSensor;
	private TextView addressText;
	private Button connectButton;
	private Button recenterButton;
	private float[] originalRotation = null;
	private float[] lastRotation = null;
	private float[] rotationMatrix = new float[9];
	private float[] angleChange = new float[3];
	private float[] angleChangeMatrix = new float[9];
	private float[] rotatedAngleChangeMatrix = new float[9];
	private SocketAddress remoteAddress;
	private DatagramSocket datagramSocket;
	private boolean resetRotation = true;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addressText = (TextView) findViewById(R.id.address_text);
		connectButton = (Button) findViewById(R.id.connect_button);
		connectButton.setOnClickListener(this);
		recenterButton = (Button) findViewById(R.id.recenter_button);
		recenterButton.setOnClickListener(this);
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
	}

	public void onClick(View v) {
		if (v == connectButton) {
			connect();
		} else if (v == recenterButton) {
			resetRotation = true;
		}
	}

	public void onPause() {
		super.onPause();
		stopSendingEvents();
	}

	public void connect() {
		try {
			remoteAddress = new InetSocketAddress(addressText.getText().toString(), DEFAULT_PORT);
			addressText.setError(null);
			connectButton.setEnabled(false);
			startSendingEvents();
		} catch (Exception e) {
			e.printStackTrace();
			addressText.setError("Invalid");
		}
	}
		

	protected void startSendingEvents() {
		if (remoteAddress == null) return;
		sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
		try {
			datagramSocket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void stopSendingEvents() {
		sensorManager.unregisterListener(this);
		try {
			if (datagramSocket != null) {
				datagramSocket.close();
				datagramSocket = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
			
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
			if (originalRotation == null || resetRotation) {
				originalRotation = rotationMatrix.clone();
				resetRotation = false;
			} else {
				SensorManager.getAngleChange(angleChange, originalRotation, rotationMatrix);//, originalRotation);
				//angleChange[2] += (Math.PI / 3);
				SensorManager.getRotationMatrixFromVector(angleChangeMatrix, angleChange);
				//SensorManager.remapCoordinateSystem(angleChangeMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rotatedAngleChangeMatrix);
			rotatedAngleChangeMatrix = angleChangeMatrix;
				sendNewOrientation();
			}
		}
	}

	protected void sendNewOrientation() {
		if (remoteAddress == null || datagramSocket == null) return;
		try {
			byte[] sendBytes = new byte[9 * 8];
			int sI = 0;
			for (int i = 0; i < 9; i++) {
				//System.out.println(rotationMatrix[i]);
				long rawBytes = Double.doubleToLongBits(rotatedAngleChangeMatrix[i]);
				for (int b = 0; b < 8; b++) {
					sendBytes[sI + b] = (byte) ((rawBytes >>> (8 * b)) & 0xff);
				}
				sI += 8;
			}
			DatagramPacket pack = new DatagramPacket(sendBytes, sendBytes.length, remoteAddress);
			datagramSocket.send(pack);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

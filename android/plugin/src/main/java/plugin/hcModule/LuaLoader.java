//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.library"
package plugin.hcModule;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;

import com.ansca.corona.CoronaRuntimeTask;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;


import static android.content.ContentValues.TAG;


/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings("WeakerAccess")
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	/** Lua registry ID to the Lua function to be called when the ad request finishes. */
	private int fListener;
	private int initLis;
	private List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
	private LuaState initL;
	private List<Integer> mBuffer = new ArrayList<>();
	private List<String> mResponseBuffer = new ArrayList<>();
	private ArrayAdapter<String> mResponsesAdapter;
	private CoronaRuntimeTaskDispatcher initDispatcher = null;


	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	BluetoothSocket tmp = null;
	private String[] axisData;
	private CountDownTimer timer;



	/**
	 * Creates a new Lua interface to this plugin.
	 * <p>
	 * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
	 * That is, only one instance of this class will be created for the lifetime of the application process.
	 * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
	 */
	@SuppressWarnings("unused")
	public LuaLoader() {
		// Initialize member variables.
		fListener = CoronaLua.REFNIL;

		// Set up this plugin to listen for Corona runtime events to be received by methods
		// onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
		CoronaEnvironment.addRuntimeListener(this);
	}

	/**
	 * Called when this plugin is being loaded via the Lua require() function.
	 * <p>
	 * Note that this method will be called every time a new CoronaActivity has been launched.
	 * This means that you'll need to re-initialize this plugin here.
	 * <p>
	 * Warning! This method is not called on the main UI thread.
	 * @param L Reference to the Lua state that the require() function was called from.
	 * @return Returns the number of values that the require() function will return.
	 *         <p>
	 *         Expected to return 1, the library that the require() function is loading.
	 */
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
			new init(), new connect(), new disconnect(), new isConnected(),
		};
		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);

		// Returning 1 indicates that the Lua require() function will return the above Lua library.
		return 1;
	}

	/**
	 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
	 *                Provides a LuaState object that allows the application to extend the Lua API.
	 */
	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Note that this method will not be called the first time a Corona activity has been launched.
		// This is because this listener cannot be added to the CoronaEnvironment until after
		// this plugin has been required-in by Lua, which occurs after the onLoaded() event.
		// However, this method will be called when a 2nd Corona activity has been created.

	}

	/**
	 * Called just after the Corona runtime has executed the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been started.
	 */
	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
	 * and other Corona related operations. This can happen when another Android activity (ie: window) has
	 * been displayed, when the screen has been powered off, or when the screen lock is shown.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
	 */
	@Override
	public void onSuspended(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been resumed after a suspend.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
	 */
	@Override
	public void onResumed(CoronaRuntime runtime) {
	}

	/**
	 * Called just before the Corona runtime terminates.
	 * <p>
	 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
	 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
	 * method is called. This does not mean that the application is exiting.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that is being terminated.
	 */
	@Override
	public void onExiting(CoronaRuntime runtime) {
		// Remove the Lua listener reference.
		CoronaLua.deleteRef( runtime.getLuaState(), fListener );
		fListener = CoronaLua.REFNIL;
	}

	private class disconnect implements NamedJavaFunction {
		@Override
		public String getName() {
			return "disconnect";
		}
		@Override
		public int invoke(LuaState L) {
			if (tmp.isConnected()){
				try {
					tmp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}


			return 0;

		}
	}
	private class isConnected implements NamedJavaFunction {
		@Override
		public String getName() {
			return "isConnected";
		}
		@Override
		public int invoke(LuaState L) {
			if (tmp != null && tmp.isConnected()){
				L.pushBoolean(true);
			}else{
				L.pushBoolean(false);
			}


			return 1;

		}
	}
	private class init implements NamedJavaFunction {
		@Override
		public String getName() {
			return "init";
		}
		@Override
		public int invoke(LuaState L) {
			initLis = CoronaLua.newRef(L, 1);
			final int frequency = ( L.toInteger(2) == 0 ? 500 : L.toInteger(2) ); // default to 1/2 a second

			initL = L;
			initDispatcher = new CoronaRuntimeTaskDispatcher(L);
			CoronaEnvironment.getCoronaActivity().runOnUiThread(new Runnable() {
				public void run() {
					IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
					filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
					filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
					filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
					CoronaEnvironment.getCoronaActivity().registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							String action = intent.getAction();
							if (BluetoothDevice.ACTION_FOUND.equals(action)) {

							}

							else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

							}
							else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
							}
							else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
							}
							else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
							}
							if (BluetoothDevice.ACTION_FOUND.equals(action)) {
								final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
								initDispatcher.send( new CoronaRuntimeTask() {
									@Override
									public void executeUsing(CoronaRuntime runtime) {
										LuaState L = runtime.getLuaState();
										CoronaLua.newEvent(L, "hcModule");
										L.pushString("device found");
										L.setField(-2, "type");
										L.pushString(device.getAddress());
										L.setField(-2, "address");
										L.pushString(device.getName());
										L.setField(-2, "name");
										try {
											CoronaLua.dispatchEvent(L, initLis, 1);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								} );
								devices.add(device);
							}
						}
					}, filter);
					BluetoothAdapter.getDefaultAdapter().startDiscovery();
					timer  =  new CountDownTimer(frequency, 1000) {

						@Override
						public void onTick(long millisUntilFinished) {

						}

						@Override
						public void onFinish() {
							if (axisData != null){
								initDispatcher.send( new CoronaRuntimeTask() {
									@Override
									public void executeUsing(CoronaRuntime runtime) {
										LuaState L = runtime.getLuaState();
										CoronaLua.newEvent(L, "hcModule");
										L.pushNumber(Double.parseDouble(axisData[0]));
										L.setField(-2, "x");
										L.pushNumber(Double.parseDouble(axisData[1]));
										L.setField(-2, "y");
										L.pushNumber(Double.parseDouble(axisData [2]));
										L.setField(-2, "z");
										L.pushString("data");
										L.setField(-2, "type");
										try {
											CoronaLua.dispatchEvent(L, initLis, 1);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								} );
							}
							timer.start();
						}
					}.start();
				}
			});

			return 0;

		}
	}
	private class connect implements NamedJavaFunction {
		@Override
		public String getName() {
			return "connect";
		}
		@SuppressLint("MissingPermission")
		@Override
		public int invoke(LuaState L) {
			final BluetoothDevice device= BluetoothAdapter.getDefaultAdapter().getRemoteDevice(L.toString(1));

			CoronaEnvironment.getCoronaActivity().runOnUiThread(new Runnable() {
				public void run() {
					Method m2 = null;
					try {
						m2 = device.getClass()
								.getMethod("removeBond", (Class[]) null);
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					try {
						m2.invoke(device, (Object[]) null);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}

					mmDevice = device;


					try {
						final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
						tmp = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
						Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
						tmp = (BluetoothSocket) m.invoke(device, 1);
						tmp.connect();

					} catch (IOException e) {

					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
					mmSocket = tmp;
					(new beginListenForData()).execute();
				}
			});

			return 0;

		}
	}
	public class beginListenForData extends AsyncTask<Void,Void,Void> {
		@Override
		protected Void doInBackground(Void... arg0){
			byte[] tempInputBuffer = new byte[1024];
			Queue<Byte> queueBuffer = new LinkedList<Byte>();
			byte[] packBuffer = new byte[11];
			InputStream mmInStream = null;
			try {
				mmInStream = mmSocket.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			int acceptedLen = 0;
			byte sHead;
			// Keep listening to the InputStream while connected
			long lLastTime = System.currentTimeMillis();
			String strDate,strTime;
			Log.e("run124444", "hello112444");
			float [] fData=new float[31];
			while (true) {

				try {

					acceptedLen = mmInStream.read(tempInputBuffer);

					for (int i = 0; i < acceptedLen; i++) queueBuffer.add(tempInputBuffer[i]);


					while (queueBuffer.size() >= 11) {
						if ((queueBuffer.poll()) != 0x55) continue;
						sHead = queueBuffer.poll();
						for (int j = 0; j < 9; j++) packBuffer[j] = queueBuffer.poll();
						switch (sHead) {//
							case 0x50:
								int ms = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff));
								strDate = String.format("20%02d-%02d-%02d",packBuffer[0],packBuffer[1],packBuffer[2]);
								strTime = String.format(" %02d:%02d:%02d.%03d",packBuffer[3],packBuffer[4],packBuffer[5],ms);
								RecordData(sHead,strDate+strTime);
								break;
							case 0x51:
								fData[0] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 16;
								fData[1] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 16;
								fData[2] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 16;
								fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
								RecordData(sHead,String.format("% 10.2f", fData[0])+String.format("% 10.2f", fData[1])+String.format("% 10.2f", fData[2])+" ");
								break;
							case 0x52:
								fData[3] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 2000;
								fData[4] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 2000;
								fData[5] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 2000;
								fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
								RecordData(sHead,String.format("% 10.2f", fData[3])+String.format("% 10.2f", fData[4])+String.format("% 10.2f", fData[5])+" ");
								break;
							case 0x53:
								fData[6] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 180;
								fData[7] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 180;
								fData[8] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 180;
								fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
								RecordData(sHead,String.format("% 10.2f", fData[6])+String.format("% 10.2f", fData[7])+String.format("% 10.2f", fData[8]));
								break;
							case 0x54://磁场
								fData[9] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff));
								fData[10] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff));
								fData[11] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff));
								fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
								RecordData(sHead,String.format("% 10.2f", fData[9])+String.format("% 10.2f", fData[10])+String.format("% 10.2f", fData[11]));
								break;
							case 0x55://端口
								fData[12] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff));
								fData[13] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff));
								fData[14] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff));
								fData[15] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff));
								RecordData(sHead,String.format("% 7.0f", fData[12])+String.format("% 7.0f", fData[13])+String.format("% 7.0f", fData[14])+String.format("% 7.0f", fData[15]));
								break;
							case 0x56://气压、高度
								fData[16] = ((((long) packBuffer[3]) << 24) |(((long) packBuffer[2]) << 16) |(((long) packBuffer[1]) << 8) | (((long) packBuffer[0])));
								fData[17] = ((((long) packBuffer[7]) << 24) |(((long) packBuffer[6]) << 16) |(((long) packBuffer[5]) << 8) | (((long) packBuffer[4])));
								fData[17]/=100;
								RecordData(sHead,String.format("% 10.2f", fData[16])+String.format("% 10.2f", fData[17]));;
								break;
							case 0x57://经纬度
								long Longitude = ((((long) packBuffer[3]) << 24) |(((long) packBuffer[2]) << 16) |(((long) packBuffer[1]) << 8) | (((long) packBuffer[0])));
								fData[18]=(float) ((float)Longitude / 10000000 + ((float)(Longitude % 10000000) / 100000.0 / 60.0));
								long Latitude = ((((long) packBuffer[7]) << 24) |(((long) packBuffer[6]) << 16) |(((long) packBuffer[5]) << 8) | (((long) packBuffer[4])));
								fData[19]=(float) ((float)Latitude / 10000000 + ((float)(Latitude % 10000000) / 100000.0 / 60.0));
								RecordData(sHead,String.format("% 14.6f", fData[18])+String.format("% 14.6f", fData[19]));;
								break;
							case 0x58://海拔、航向、地速
								fData[20] = ((((long) packBuffer[3]) << 24) |(((long) packBuffer[2]) << 16) |(((long) packBuffer[1]) << 8) | (((long) packBuffer[0])))/10;
								fData[21]=((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff))/10;
								fData[22]=((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff))/1000;
								RecordData(sHead,String.format("% 10.2f", fData[20])+String.format("% 10.2f", fData[21])+String.format("% 10.2f", fData[22]));;
								break;
							case 0x59://四元数
								fData[23] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f;
								fData[24] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff))/32768.0f;
								fData[25] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff))/32768.0f;
								fData[26] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff))/32768.0f;
								RecordData(sHead,String.format("% 7.3f", fData[23])+String.format("% 7.3f", fData[24])+String.format("% 7.3f", fData[25])+String.format("% 7.3f", fData[26]));
								break;
							case 0x5a://卫星数
								fData[27] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f;
								fData[28] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff))/32768.0f;
								fData[29] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff))/32768.0f;
								fData[30] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff))/32768.0f;
								RecordData(sHead,String.format("% 5.0f", fData[27])+String.format("% 7.1f", fData[28])+String.format("% 7.1f", fData[29])+String.format("% 7.1f", fData[30]));
								break;
						}//switch
					}//while (queueBuffer.size() >= 11)

					long lTimeNow = System.currentTimeMillis(); // 获取开始时间
					if (lTimeNow - lLastTime > 80) {
						lLastTime = lTimeNow;

					}

				} catch (IOException e) {

					break;
				}
			}
			return null;
		}
	}

	public void RecordData(byte ID,String str) throws IOException
	{
		String[] parts = str.split("\\s+");
		int n=parts.length-1;
		final String[] newArray=new String[n];
		System.arraycopy(parts,1,newArray,0,n);
		axisData = newArray;

	}
}

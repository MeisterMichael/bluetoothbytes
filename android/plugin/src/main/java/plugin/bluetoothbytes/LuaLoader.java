package plugin.bluetoothbytes;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaValueProxy;
import com.naef.jnlua.Converter;
import com.naef.jnlua.LuaType;
import com.naef.jnlua.JavaReflector.Metamethod;
import com.naef.jnlua.NamedJavaFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import android.bluetooth.BluetoothAdapter;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.bluetooth.BluetoothDevice;
import java.lang.reflect.Method;
import me.aflak.bluetooth.Bluetooth;

import static android.content.ContentValues.TAG;

public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	private List<Integer> mBuffer = new ArrayList<>();
	private List<String> mResponseBuffer = new ArrayList<>();
	private ArrayAdapter<String> mResponsesAdapter;
	// private CoronaRuntimeTaskDispatcher initDispatcher = null;
	private CoronaActivity coronaActivity = null;
	Bluetooth bluetooth;
	public LuaLoader() {
		CoronaEnvironment.addRuntimeListener(this);
	}
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
				new init( ), new isEnabled (), new search(), new send(), new enable(), new connect(), new disconnect(), new getDevices(),
		};
		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);
		return 1;
	}

	@Override
	public void onLoaded(CoronaRuntime runtime) {

	}

	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	@Override
	public void onSuspended(CoronaRuntime runtime) {
	}

	@Override
	public void onResumed(CoronaRuntime runtime) {
	}

	@Override
	public void onExiting(CoronaRuntime runtime) {
		;
	}

	public CoronaRuntimeTaskDispatcher getInitDispatcher() {
		return coronaActivity.getRuntimeTaskDispatcher();
		// return initDispatcher;
	}


	private class init implements NamedJavaFunction {
		@Override
		public String getName() {
			return "init";
		}
		@Override
		public int invoke(final LuaState L) {
			final int myRef = CoronaLua.newRef( L, 1 );
			final LuaState initL = L;
			// initDispatcher = new CoronaRuntimeTaskDispatcher(L);
			coronaActivity = CoronaEnvironment.getCoronaActivity();
			bluetooth = new Bluetooth(coronaActivity);
			bluetooth.setDiscoveryCallback(new Bluetooth.DiscoveryCallback() {

				@Override
				public void onFinish() {
					getInitDispatcher().send( new CoronaRuntimeTask() {
						@Override
						public void executeUsing(CoronaRuntime runtime) {
							LuaState L = runtime.getLuaState();
							CoronaLua.newEvent(L, "bluetoothbytes");
							L.pushString("discovery finished");
							L.setField(-2, "type");
							try {
								CoronaLua.dispatchEvent(L, myRef, 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} );

				}

				@Override
				public void onDevice(BluetoothDevice mydevice) {
					final BluetoothDevice device = mydevice;
					if ( device.getName() != null){
						getInitDispatcher().send( new CoronaRuntimeTask() {
							@Override
							public void executeUsing(CoronaRuntime runtime) {
								LuaState L = runtime.getLuaState();
								CoronaLua.newEvent(L, "bluetoothbytes");
								L.pushString(device.getName());
								L.setField(-2, "deviceName");
								L.pushString(device.getAddress());

								L.setField(-2, "deviceID");
								if (device.getBondState() == BluetoothDevice.BOND_BONDED){
									L.pushString("connected");
								}
								if (device.getBondState() == BluetoothDevice.BOND_BONDING){
									L.pushString("connecting");
								}
								if (device.getBondState() == BluetoothDevice.BOND_NONE){
									L.pushString("not connected");
								}
								L.setField(-2, "deviceState");
								L.pushString("device found");
								L.setField(-2, "type");
								try {
									CoronaLua.dispatchEvent(L, myRef, 1);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} );

					}


				}

				@Override
				public void onPair(BluetoothDevice mydevice) {
					final BluetoothDevice device = mydevice;
					if ( device.getName() != null){
						getInitDispatcher().send( new CoronaRuntimeTask() {
							@Override
							public void executeUsing(CoronaRuntime runtime) {
								LuaState L = runtime.getLuaState();
								CoronaLua.newEvent(L, "bluetoothbytes");
								L.pushString(device.getName());
								L.setField(-2, "deviceName");
								L.pushString(device.getAddress());
								L.setField(-2, "deviceID");
								if (device.getBondState() == BluetoothDevice.BOND_BONDED){
									L.pushString("connected");
								}
								if (device.getBondState() == BluetoothDevice.BOND_BONDING){
									L.pushString("connecting");
								}
								if (device.getBondState() == BluetoothDevice.BOND_NONE){
									L.pushString("not connected");
								}
								L.setField(-2, "deviceState");
								L.pushString("device paired");
								L.setField(-2, "type");
								try {
									CoronaLua.dispatchEvent(L, myRef, 1);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} );

					};

				}

				@Override
				public void onUnpair(BluetoothDevice mydevice) {
					final BluetoothDevice device = mydevice;
					if ( device.getAddress() != null){
						getInitDispatcher().send( new CoronaRuntimeTask() {
							@Override
							public void executeUsing(CoronaRuntime runtime) {
								LuaState L = runtime.getLuaState();
								CoronaLua.newEvent(L, "bluetoothbytes");
								if (device.getName() == null){
									L.pushNil();
								}else{
									L.pushString(device.getName());
								}
								L.setField(-2, "deviceName");
								L.pushString(device.getAddress());
								L.setField(-2, "deviceID");
								if (device.getBondState() == BluetoothDevice.BOND_BONDED){
									L.pushString("connected");
								}
								if (device.getBondState() == BluetoothDevice.BOND_BONDING){
									L.pushString("connecting");
								}
								if (device.getBondState() == BluetoothDevice.BOND_NONE){
									L.pushString("not connected");
								}
								L.setField(-2, "deviceState");
								L.pushString("device unpaired");
								L.setField(-2, "type");
								try {
									CoronaLua.dispatchEvent(L, myRef, 1);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} );

					}

				}

				@Override
				public void onError(String mymessage) {
					final String message = mymessage;
					getInitDispatcher().send( new CoronaRuntimeTask() {
						@Override
						public void executeUsing(CoronaRuntime runtime) {
							LuaState L = runtime.getLuaState();
							CoronaLua.newEvent(L, "bluetoothbytes");
							L.pushString(message);
							L.setField(-2, "error");
							L.pushString("error");
							L.setField(-2, "type");
							try {
								CoronaLua.dispatchEvent(L, myRef, 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} );

				}
			});
			bluetooth.setCommunicationCallback(new Bluetooth.CommunicationCallback() {
				@Override
				public void onConnect(BluetoothDevice mydevice) {
					final BluetoothDevice device = mydevice;
					getInitDispatcher().send( new CoronaRuntimeTask() {
						@Override
						public void executeUsing(CoronaRuntime runtime) {
							LuaState L = runtime.getLuaState();
							CoronaLua.newEvent(L, "bluetoothbytes");
							if (device.getName() == null){
								L.pushNil();
							}else{
								L.pushString(device.getName());
							}
							L.setField(-2, "deviceName");
							L.pushString(device.getAddress());
							L.setField(-2, "deviceID");
							if (device.getBondState() == BluetoothDevice.BOND_BONDED){
								L.pushString("connected");
							}
							if (device.getBondState() == BluetoothDevice.BOND_BONDING){
								L.pushString("connecting");
							}
							if (device.getBondState() == BluetoothDevice.BOND_NONE){
								L.pushString("not connected");
							}
							L.setField(-2, "deviceState");
							L.pushString("connected");
							L.setField(-2, "type");
							try {
								CoronaLua.dispatchEvent(L, myRef, 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} );
				}

				@Override
				public void onDisconnect(BluetoothDevice mydevice, String mymessage) {
					final BluetoothDevice device = mydevice;
					final String message = mymessage;
					getInitDispatcher().send( new CoronaRuntimeTask() {
						@Override
						public void executeUsing(CoronaRuntime runtime) {
							LuaState L = runtime.getLuaState();
							CoronaLua.newEvent(L, "bluetoothbytes");
							if (device.getName() == null){
								L.pushNil();
							}else{
								L.pushString(device.getName());
							}
							L.setField(-2, "deviceName");
							L.pushString(device.getAddress());
							L.setField(-2, "deviceID");
							if (device.getBondState() == BluetoothDevice.BOND_BONDED){
								L.pushString("connected");
							}
							if (device.getBondState() == BluetoothDevice.BOND_BONDING){
								initL.pushString("connecting");
							}
							if (device.getBondState() == BluetoothDevice.BOND_NONE){
								L.pushString("not connected");
							}
							L.setField(-2, "deviceState");
							L.pushString(message);
							L.setField(-2, "error");
							L.pushString("disconnect");
							L.setField(-2, "type");
							try {
								CoronaLua.dispatchEvent(L, myRef, 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} );
				}

				@Override
				public void onMessage(byte[] mybytes) {
					System.out.println("onMessage(byte[] mybytes) START");
					final byte[] bytes = mybytes;
					getInitDispatcher().send( new CoronaRuntimeTask() {
						@Override
						public void executeUsing(CoronaRuntime runtime) {
							System.out.println("onMessage(byte[] mybytes) getInitDispatcher().send START");

							LuaState L = runtime.getLuaState();

							System.out.println("onMessage(byte[] mybytes) getInitDispatcher().send before newEvent");
							CoronaLua.newEvent(L, "bluetoothbytes");

							System.out.println("onMessage(byte[] mybytes) getInitDispatcher().send before message");

							if ( bytes != null && bytes.length > 0 ) {
								L.newTable(bytes.length, 0);

								for(int i = 0; i < bytes.length; i++ ) {
									L.pushInteger( (int) (byte) bytes[i] );
									L.rawSet(-2, i + 1 );
								}

								L.setField(-2, "bytes");
							}

							System.out.println("onMessage(byte[] mybytes) getInitDispatcher().send before bytes");

							L.pushString("bytes");
							L.setField(-2, "type");

							System.out.println("onMessage(byte[] mybytes) getInitDispatcher().send before dispatchEvent");
							try {
								CoronaLua.dispatchEvent(L, myRef, 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
							System.out.println("onMessage(byte[] mybytes) getInitDispatcher().send END");
						}
					} );

					System.out.println("onMessage(byte[] mybytes) END");

				}

				@Override
				public void onError(String mymessage) {
					final String message = mymessage;
					getInitDispatcher().send( new CoronaRuntimeTask() {
						@Override
						public void executeUsing(CoronaRuntime runtime) {
							LuaState L = runtime.getLuaState();
							CoronaLua.newEvent(L, "bluetoothbytes");
							L.pushString(message);
							L.setField(-2, "error");
							L.pushString("error");
							L.setField(-2, "type");
							try {
								CoronaLua.dispatchEvent(L, myRef, 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} );

				}

				@Override
				public void onConnectError(BluetoothDevice mydevice, String mymessage) {
					final BluetoothDevice device = mydevice;
					final String message = mymessage;
					getInitDispatcher().send( new CoronaRuntimeTask() {
						@Override
						public void executeUsing(CoronaRuntime runtime) {
							LuaState L = runtime.getLuaState();
							CoronaLua.newEvent(L, "bluetoothbytes");
							if ( device.getName() != null){
								L.pushString(device.getName());
							}else{
								L.pushNil();
							}
							L.setField(-2, "deviceName");
							L.pushString(device.getAddress());
							L.setField(-2, "deviceID");
							if (device.getBondState() == BluetoothDevice.BOND_BONDED){
								L.pushString("connected");
							}
							if (device.getBondState() == BluetoothDevice.BOND_BONDING){
								L.pushString("connecting");
							}
							if (device.getBondState() == BluetoothDevice.BOND_NONE){
								L.pushString("not connected");
							}
							L.setField(-2, "deviceState");
							L.pushString(message);
							L.setField(-2, "error");
							L.pushString("connection error");
							L.setField(-2, "type");
							try {
								CoronaLua.dispatchEvent(L, myRef, 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} );

				}
			});
			return 0;

		}
	}
	private class isEnabled implements NamedJavaFunction {
		@Override
		public String getName() {
			return "isEnabled";
		}
		@Override
		public int invoke(LuaState L) {
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				L.pushBoolean(true);
			} else {
				if (!mBluetoothAdapter.isEnabled()) {
					L.pushBoolean(false);
				}
			}

			return 1;

		}
	}
	private class search implements NamedJavaFunction {
		@Override
		public String getName() {
			return "search";
		}
		@Override
		public int invoke(LuaState L) {
			bluetooth.scanDevices();
			return 0;

		}
	}
	private class send implements NamedJavaFunction {
		@Override
		public String getName() {
			return "send";
		}
		@Override
		public int invoke(LuaState L) {

			// Convert a lua array (1 byte chars) to java string (2 byte chars)
			/*
			String message = L.toString(1);
			byte[] bytes = new byte[message.length()];

			for(int i = 0; i < message.length(); i++ ) {
				bytes[i] = (byte) ( message.charAt(i) & 0xff );
			}*/

			// Convert lua table array of ints into bytes
			int count = L.tableSize(1);
			L.getTable(1);
			byte[] bytes = new byte[count];
			for( int i = 0; i < count; i++ ) {
				bytes[i] = (byte) ( L.toInteger( i+1 ) & 0xff );
			}



			bluetooth.send( new String( bytes ) );

			return 0;

		}
	}
	private class enable implements NamedJavaFunction {
		@Override
		public String getName() {
			return "enable";
		}
		@Override
		public int invoke(LuaState L) {
			if (L.isBoolean(1) && L.toBoolean(1) == false){
				BluetoothAdapter.getDefaultAdapter().disable();
			}else{
				BluetoothAdapter.getDefaultAdapter().enable();
			}
			return 0;

		}
	}
	private class connect implements NamedJavaFunction {
		@Override
		public String getName() {
			return "connect";
		}
		@Override
		public int invoke(LuaState L) {
			bluetooth.connectToAddress(L.toString(1));

			return 0;

		}
	}
	private class disconnect implements NamedJavaFunction {
		@Override
		public String getName() {
			return "disconnect";
		}
		@Override
		public int invoke(LuaState L) {
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(L.toString(1));
			try {
				Method method = device.getClass().getMethod("removeBond", (Class[]) null);
				method.invoke(device, (Object[]) null);
				L.pushBoolean(false);
				L.pushNil();
			} catch (Exception e) {
				L.pushBoolean(true);
				L.pushString(e.getLocalizedMessage());
			}
			return 2;

		}
	}
	private class getDevices implements NamedJavaFunction {
		@Override
		public String getName() {
			return "getDevices";
		}
		@Override
		public int invoke(LuaState L) {
			try {
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
				ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(adapter, null);

				if(uuids != null) {
					Log.e(TAG, "/////////");
					for (ParcelUuid uuid : uuids) {
						Log.e(TAG, "UUID: " + uuid.getUuid().toString());
					}
				}else{
					Log.e(TAG, "Uuids not found, be sure to enable Bluetooth!");
				}

			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			List<BluetoothDevice> devices = bluetooth.getPairedDevices();
			L.newTable(devices.size(),0);
			int index = 1;
			int luaTableStackIndex = L.getTop();
			for (BluetoothDevice device: devices){
				L.newTable(0, 3);
				if ( device.getName() != null){
					L.pushString(device.getName());
				}else{
					L.pushString("");
				}
				Method method = null;
				try {
					method = device.getClass().getMethod("getUuids", null);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
				ParcelUuid[] phoneUuids= null;
				try {
					phoneUuids = (ParcelUuid[]) method.invoke(device, null);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				if (phoneUuids != null){
					Log.e("run23423",phoneUuids[0].toString() + "//"+device.getName());
				}else{
					Log.e("run23423",device.getAddress());
				}

				L.setField(-2, "deviceName");
				L.pushString(device.getAddress());
				L.setField(-2, "deviceID");
				Boolean isConnected = false;
				Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
				if (pairedDevices.size() != 0) {
					for (BluetoothDevice myDevice : pairedDevices) {
						if (device.getAddress().contains(myDevice.getAddress())) {
							isConnected= true;
						}
					}
				}
				if (isConnected == true){
					L.pushString("connected");
				}
				else{
					L.pushString("not connected");
				}
				L.setField(-2, "deviceState");
				L.rawSet(luaTableStackIndex, index);
				index = index+1;
			}
			return 1;

		}
	}

}

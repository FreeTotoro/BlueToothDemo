package com.example.bttelephone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.bttelephone.adapter.DevicesAdapter;
import com.example.bttelephone.util.BTApplication;
import com.example.bttelephone.util.ReceiveDataService;
import com.example.bttelephone.util.ReflectDeviceMethod;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "ShowToast", "HandlerLeak", "NewApi" })
public class BluetoothFragment extends Fragment {
	//���������
	private static final int REQUEST_ENABLE_BT = 0;
	//�����豸�ɱ�����
	protected static final int REQUEST_DISCOVERABLE = 1;
	//������б����ı�
	private static final int BONDED_DEVICES_CHANGED = 2;
	//�豸�ɱ�����ʱ���þ���2min��
	private static final int DISCOVERABLE_TIME_OUT = 3;
	//ɾ���ظ��豸
	private static final int CANCEL_DUPLICATE_DEVICE = 4;
	//�����豸�ɱ������ɹ�
	private static final int RESULT_DEVICE_DISCOVERABLE = 120;
	//����Խ���
	private static final int NEW_CONNECTION_SETUP = 5;
	//�յ��µĶ���Ϣ
	public static final int RECEIVE_NEW_MESSAGE = 6;
	//���ʧ��
	public static final int CONNECT_FAILED = 7;
	//���豸����
	TextView deviceName;
	//�����豸
	TextView searchDevice;
	//�������豸�б�
	ListView connectedList;
	//�������豸������
	DevicesAdapter connectedAdapter;
	//�����豸�б�
	ListView connectingList;
	//�����豸�б�������
	DevicesAdapter connectingAdapter;
	//����������
	BluetoothAdapter btAdapter;
	//Զ�������豸
	BluetoothDevice btDevice;
	//�������豸��Ϣ����
	List<BluetoothDevice> connectedDeviceList;
	//�����豸��Ϣ����
	List<BluetoothDevice> connectingDeviceList;
	//�ͻ�����������߳�
	ClientSocketThread csThread;
	//���������߳�
	ServerSocketThread ssThread;
	BluetoothSocket mSocket;
	//��activity
	Activity activity;
	BTApplication app;
	boolean serverStop = false;
	boolean clientStop = false;
	
	Handler _handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case BONDED_DEVICES_CHANGED:
				showConnectedDevices();
				break;
			case DISCOVERABLE_TIME_OUT:
				deviceName.setText(Build.MODEL);
				deviceName.setClickable(true);
				break;
			case CANCEL_DUPLICATE_DEVICE:
				showConnectingDevices();
				break;
			case NEW_CONNECTION_SETUP:
				showConnectedDevices();
				break;
			case RECEIVE_NEW_MESSAGE:
				Toast.makeText(activity, (String)msg.obj, Toast.LENGTH_LONG).show();
				break;
			case CONNECT_FAILED:
				Toast.makeText(activity, "���ʧ��", Toast.LENGTH_LONG).show();
				break;
			}
		}
	};
	
	BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				/**
				 * �����豸
				 */
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				connectingDeviceList.add(device);

				sendMsg(CANCEL_DUPLICATE_DEVICE);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				/**
				 * �����豸���
				 */
				searchDevice.setText(R.string.search_device_text);
				searchDevice.setClickable(true);
				btAdapter.cancelDiscovery();
			}
		}
	};
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
			
		init();	
		showConnectedDevices();
		ssThread = new ServerSocketThread(btAdapter);
		ssThread.start();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.bluetooth_fragment, null);
	}

	/**
	 * 2013-12-30
	 * 
	 * ��ʼ��views
	 * @author totoro
	 */
	void init() {
		activity = getActivity();
		app = (BTApplication) activity.getApplication();
		
		deviceName = (TextView) activity.findViewById(R.id.device_name);
		deviceName.setText(Build.MODEL);
		deviceName.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {		
				Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				startActivityForResult(intent, REQUEST_DISCOVERABLE);
			}
		});
		
		searchDevice = (TextView) activity.findViewById(R.id.search_device);
		searchDevice.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				searchDevice.setClickable(false);
				searchDevice.setText(R.string.searching);
				if (!connectingDeviceList.isEmpty()) {
					connectingDeviceList.clear();
					connectingAdapter.notifyDataSetChanged();
				}	
				new SearchThread().start();
			}
		});		
		
		connectedList = (ListView) activity.findViewById(R.id.connected_list);
		connectedList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position,
					long id) {
				AlertDialog.Builder builder = new Builder(activity);
				builder.setMessage("ȡ�����");
				builder.setPositiveButton("������", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						/**
						 * ʹ�����ص�api
						 */
						btDevice = connectedDeviceList.get(position);
						try {
							ReflectDeviceMethod.removeBond(btDevice.getClass(), btDevice);
						} catch (Exception e) {
							e.printStackTrace();
						}
						/**
						 * �ر�socket
						 */				
//						csThread.cancel();
						
						connectedDeviceList.remove(position);
						connectedAdapter.notifyDataSetChanged();
					}
				});
				builder.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {		
						dialog.dismiss();
					}
				});
				builder.create().show();
				
			}
		});
		connectingList = (ListView) activity.findViewById(R.id.connecting_list);
		connectingList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position,
					long id) {
				AlertDialog.Builder builder = new Builder(activity);
				builder.setMessage("�豸���");
				builder.setPositiveButton("���", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						btDevice = connectingDeviceList.get(position);
						if(btAdapter.isDiscovering()) {
							btAdapter.cancelDiscovery();
							searchDevice.setText(R.string.search_device_text);
							searchDevice.setClickable(true);
						}
						csThread = new ClientSocketThread(btDevice);
						csThread.start();
//						try {
//							boolean isBond = ReflectDeviceMethod.createBond(btDevice.getClass(), btDevice);
//							if (isBond) {
//								showConnectedDevices();
//							} else {
//								Toast.makeText(MainActivity.this, "���ʧ��", Toast.LENGTH_LONG);
//							}
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
					}
				});
				builder.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {		
						dialog.dismiss();
					}
				});
				builder.create().show();		
			}
		});
		connectedDeviceList = new ArrayList<BluetoothDevice>();
		connectedAdapter = new DevicesAdapter(activity,connectedDeviceList);
		connectedList.setAdapter(connectedAdapter);
		connectingDeviceList = new ArrayList<BluetoothDevice>();
		connectingAdapter = new DevicesAdapter(activity, connectingDeviceList);
		connectingList.setAdapter(connectingAdapter);
		
		initReceiver();
		initBluetooth();
	}
	
	/**
	 * 2013-12-30
	 * 
	 * ��ʼ���㲥������
	 * @author totoro
	 */
	void initReceiver() {
		IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		activity.registerReceiver(deviceReceiver, filter1);
		IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		activity.registerReceiver(deviceReceiver, filter2);
	}
	
	/**
	 * 2013-12-30
	 * 
	 * ��ʼ���������
	 * @author totoro
	 */
	void initBluetooth() {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			Toast.makeText(activity, "�豸��֧��������", 3000).show();
		}
		if (!btAdapter.isEnabled()) {
			/**
			 * ��ʾ�û��ֶ�������
			 */
//			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			/**
			 * �Զ���������������ʾ
			 */
			btAdapter.enable();
			Toast.makeText(activity, "���������Ѵ�", Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * 2013-12-30
	 * 
	 * ��ʾ�����豸�б�
	 * @author totoro
	 */
	void showConnectingDevices() {
		//���ظ��豸ɾ��
		for (int position = 0 ; position < connectingDeviceList.size(); position++) {
			for (int i = position + 1 ; i < connectingDeviceList.size() ; i++) {
				String desAddress = connectingDeviceList.get(position).getAddress();
				String opAddress = connectingDeviceList.get(position).getAddress();
				if (opAddress.equals(desAddress)) {
					connectingDeviceList.remove(i);
				}
			}
		}
		connectingAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 2013-12-31
	 * 
	 * ��ʾ������豸�б�
	 * @author totoro
	 */
	void showConnectedDevices() {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> devicesSet = btAdapter.getBondedDevices();
		Log.v("Totoro:showConnectedDevices","devicesSet:" + devicesSet.size());
		if(devicesSet.size()>0){  
            for(Iterator<BluetoothDevice> iterator = devicesSet.iterator();iterator.hasNext();){  
                BluetoothDevice device = (BluetoothDevice) iterator.next();  
                connectedDeviceList.add(device); 
            }  
        }  
		for (BluetoothDevice btDevice:connectedDeviceList) {
			if (connectingDeviceList.contains(btDevice)) {
				connectingDeviceList.remove(btDevice);
			}
		}
		connectedAdapter.notifyDataSetChanged();
		connectingAdapter.notifyDataSetChanged();
	}
			
	/**
	 * 2013-12-30
	 * 
	 * �����߳�
	 * @author totoro
	 */
	class SearchThread extends Thread {

		@Override
		public void run() {
			if (btAdapter.isDiscovering()) {
				btAdapter.cancelDiscovery();
			}
			btAdapter.startDiscovery();		
		}
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_DISCOVERABLE) {
			Log.v("Totoro:onActivityResult","resultCode:" + resultCode);
			if (resultCode == RESULT_DEVICE_DISCOVERABLE) {
				deviceName.append("(�����ɱ�����)");
				deviceName.setClickable(false);
				new Thread(new Runnable(){

					@Override
					public void run() {
						try {
							Thread.sleep(120*1000);
							sendMsg(DISCOVERABLE_TIME_OUT);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}		
					}
				}).start();
			} else {
				Toast.makeText(activity, "�Ѿܾ�������", Toast.LENGTH_LONG);
			}		
		}
	}

	/**
	 * 2013-12-30
	 * 
	 * ���������߳�
	 * @author totoro
	 *
	 */
	class ServerSocketThread extends Thread{
		public static final String _UUID = "00001101-0000-1000-8000-00805F9B34FB";
		private BluetoothServerSocket mServerSocket;
		private BluetoothAdapter mAdapter;
		
		public ServerSocketThread(BluetoothAdapter adapter) {
			mAdapter = adapter;
			listen();
		}

		@Override
		public void run() {
			int attempt = 0;
			if (mServerSocket == null && attempt < 10) {
				Log.v("Totoro:run","������������ʧ�ܣ�");
				listen();
				attempt++;
			} 
			if (mServerSocket != null){
				Log.v("Totoro:run","�������������ɹ���");
				while (!serverStop) {
					try {
						Log.v("Totoro:ServerSocketThread","i am accepting!");
				        app.mSocket = mServerSocket.accept();
					} catch (IOException e) {
						e.printStackTrace();
				        break;
					}
					if (app.mSocket != null) {
						sendMsg(NEW_CONNECTION_SETUP);		
						
						Intent serviceIntent = new Intent(activity,ReceiveDataService.class);
						activity.startService(serviceIntent);
					    break;
					}
				}
			}	
	    }
		
		/**
		 * �򿪷�������
		 * 
		 * 2014-01-14
		 * @author totoro
		 */
		void listen() {
			try {
				mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord("SERVER", UUID.fromString(_UUID));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 2013-12-30
	 * 
	 * �ͻ��˷����������
	 * @author totoro
	 *
	 */
	public class ClientSocketThread extends Thread {
		
		public ClientSocketThread(BluetoothDevice device) {
			try {
				app.mSocket = device.createRfcommSocketToServiceRecord(
						UUID.fromString(BluetoothFragment.ServerSocketThread._UUID));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				app.mSocket.connect();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (app.mSocket != null && app.mSocket.isConnected()) {
				sendMsg(NEW_CONNECTION_SETUP);
				
				Intent serviceIntent = new Intent(activity,ReceiveDataService.class);
				activity.startService(serviceIntent);
			} else {
				sendMsg(CONNECT_FAILED);
			}
		}
	}
	
	/**
	 * 2014-01-03
	 * 
	 * ������Ϣ
	 * @param what
	 */
	public void sendMsg(int what) {
		Message msg = Message.obtain();
		msg.what = what;
		_handler.sendMessage(msg);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		activity.unregisterReceiver(deviceReceiver);
		serverStop = true;
	}
}

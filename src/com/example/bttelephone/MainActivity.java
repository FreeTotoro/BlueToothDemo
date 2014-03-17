/**
 * ƽ��˵�������
 * 
 * 2014-01-06
 * @author totoro
 */
package com.example.bttelephone;

import com.example.bttelephone.util.BTApplication;
import com.example.bttelephone.util.ReceiveDataService;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	private static final int BLUETOOTH_FRAGMENT_INDEX = 0;
	private static final int PHONE_BOOK_FRAGMENT_INDEX = 1;
	//���ƽ�������ģ��İ�ť
	TextView btBtn;
	//����绰���İ�ť
	TextView pbBtn;
	//����ģ��
	BluetoothFragment btFragment;
	//��ϵ��ģ��
	PhonebookFragment pkFragment;
	//fragment����
	FragmentTransaction ft;
	//fragment����
	FragmentManager fm;
	
	BTApplication app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main_activity);
		
		init();
		setCurFragment(BLUETOOTH_FRAGMENT_INDEX);
	}
	
	/**
	 * ��ʼ��
	 * 
	 * 2014-01-06
	 * @author totoro
	 */
	void init() {
		app = (BTApplication) getApplication();
		
		btBtn = (TextView) findViewById(R.id.connect_bt);
		btBtn.setOnClickListener(this);
		pbBtn = (TextView) findViewById(R.id.contacts_book);
		pbBtn.setOnClickListener(this);
		
		btFragment = new BluetoothFragment();
		pkFragment = new PhonebookFragment();
		
		fm = getFragmentManager();
	}

	@Override
	public void onClick(View v) {
		if (v == btBtn) {
			setCurFragment(BLUETOOTH_FRAGMENT_INDEX);
		} else if (v == pbBtn){
			setCurFragment(PHONE_BOOK_FRAGMENT_INDEX);
		}
	}
	
	/**
	 * ���õ�ǰ��ʾ����Ƭ����
	 * 
	 * 2014-01-07
	 * @param index
	 */
	void setCurFragment(int index) {
		switch(index) {
		case BLUETOOTH_FRAGMENT_INDEX:
			ft = fm.beginTransaction();
			ft.replace(R.id.content_layout, btFragment);
			ft.addToBackStack(null);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
			break;
		case PHONE_BOOK_FRAGMENT_INDEX:
			ft = fm.beginTransaction();
			ft.replace(R.id.content_layout, pkFragment);
			ft.addToBackStack(null);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Intent serviceIntent = new Intent(this,ReceiveDataService.class);
		stopService(serviceIntent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "�˳�");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 0 ) {
			finish();
		}
		return true;
	}	
	
}

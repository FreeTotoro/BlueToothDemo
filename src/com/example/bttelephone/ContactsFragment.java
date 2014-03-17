package com.example.bttelephone;

import java.util.ArrayList;
import java.util.List;

import com.example.bttelephone.util.Person;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class ContactsFragment extends Fragment{
	private static final int QUERY_ALL = 0;
	ListView personList;
	PersonAdapter listAdapter; 
	QueryContacts queryContacts;
	Cursor mCursor;
	List<Person> pList;
	ContentResolver mResolver;
	ProgressDialog dialog;
	Activity activity;
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		activity = getActivity();
		init();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.contacts_fragment, null);
	}

	/**
	 * ��ʼ��
	 * 
	 * @author totoro
	 */
	void init() {
		dialog = new ProgressDialog(activity);
		pList = new ArrayList<Person>();

		mResolver = activity.getContentResolver();
		personList = (ListView) activity.findViewById(R.id.person_list);
		personList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				
			}
		});
		startQuery();
	}
	
	/**
	 * ��ʼ��ȡ��ϵ���б�
	 * 
	 * @author totoro
	 */
	void startQuery() {
		displayDialog();
		queryContacts = new QueryContacts(mResolver);
		
		queryContacts.startQuery(QUERY_ALL, null, ContactsContract.Contacts.CONTENT_URI, 
				null, null, null, ContactsContract.Contacts.DISPLAY_NAME);
	}
	
	/**
	 * ��ʾ���ȶԻ���
	 * 
	 * 2014-01-06
	 * @author totoro
	 */
	void displayDialog() {
		dialog.setMessage("��ϵ����Ϣ������......");
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		
		Window window = dialog.getWindow();   
        WindowManager.LayoutParams lp = window.getAttributes();   
        lp.alpha = 0.5f;// ͸����   
        lp.dimAmount = 0.0f;// �ڰ���   
        window.setAttributes(lp);   
        
        dialog.show();
	}
	
	/**
	 * ��ȡ��ϵ�˵��첽handler
	 * 
	 * @author totoro
	 *
	 */
	@SuppressLint("HandlerLeak")
	class QueryContacts extends AsyncQueryHandler {
		private ContentResolver mContentResolver;
		
		public QueryContacts(ContentResolver cr) {
			super(cr);
			mContentResolver = cr;
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			super.onQueryComplete(token, cookie, cursor);
			if (QUERY_ALL == token) {
				mCursor = cursor;
				handleCursor();
				dialog.dismiss();
				
				listAdapter = new PersonAdapter(activity);
				personList.setAdapter(listAdapter);
			}
		}
	}
	
	/**
	 * ����ϵ�˴�cursor��ȡ����װ��person������ӵ�pList
	 * 
	 * @author totoro
	 */
	void handleCursor() {
		Person p ;
		while (mCursor.moveToNext()) {
			p = new Person();
			String id = mCursor.getString(
					mCursor.getColumnIndex(ContactsContract.Contacts._ID));
			
			//��ȡ��ϵ������
			String name= mCursor.getString(
					mCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			p.setStrName(name);

			//��ȡ��ϵ���Ƿ��е绰����
			int hasPhoneNumber = Integer.parseInt(
					mCursor.getString(mCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
			if(hasPhoneNumber > 0){
				//��ѯ����
				Cursor c = mResolver.query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,null,null);
				ArrayList<String> pNumber = new ArrayList<String>();
				while(c.moveToNext()){
					String number = c.getString(c.getColumnIndex(
							ContactsContract.CommonDataKinds.Phone.NUMBER));
					pNumber.add(number);
				}
				p.setAlPhoneNumber(pNumber);
				c.close();
			}
			
			//��ȡ��ϵ����֯
			String orgWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";   
		    String[] orgWhereParams = new String[]{id,   
		        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};   
		    Cursor cur = mResolver.query(ContactsContract.Data.CONTENT_URI,   
		                null, orgWhere, orgWhereParams, null);  
		    if (cur.moveToFirst()) {   
		        String orgName = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DATA));  
		        String title = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)); 
		        p.setStrAddress(orgName);
		    }   
		    cur.close();  
		    
		    pList.add(p);
		}
	}
		
	/**
	 * ��ϵ��������
	 * 
	 * @author totoro
	 *
	 */
	class PersonAdapter extends BaseAdapter {
		LayoutInflater inflater;
		class ViewHolder {
			TextView tvName;
		}
		
		public PersonAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}
		
		@Override
		public int getCount() {
			return pList.size();
		}

		@Override
		public Object getItem(int position) {
			return pList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.contact_item, null);
				
				holder.tvName = (TextView) convertView.findViewById(R.id.person_name);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder)convertView.getTag();
			}
			holder.tvName.setText(pList.get(position).getStrName());
			return convertView;
		}
	}
}

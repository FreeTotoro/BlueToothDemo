package com.example.bttelephone.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import com.example.bttelephone.PhonebookFragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * ��ȡ�����߳�
 * @author totoro
 *
 */
public class ReadThread extends Thread { 
	public static final String CONTACTS_BUNDLE_KEY = "CONTACTS";
	public boolean isStop = false;
	public BTApplication app;
	
	public ReadThread(BTApplication app) {
		this.app = app;		
	}
	
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        InputStream mmInStream = null;
        
		try {
			mmInStream = app.mSocket.getInputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
		}	
        while (!isStop) {
            try {
                // ����������ȡ����
                if( (bytes = mmInStream.read(buffer)) > 0 )
                {
                    byte[] buf_data = new byte[bytes];
			    	for(int i=0; i<bytes; i++)
			    	{
			    		buf_data[i] = buffer[i];
			    	}
			    	String temp = new String(buf_data);
			    	/**
			    	 * ���е绰
			    	 */
			    	if (temp.contains("tel")) {
			    		temp = temp.substring(1);
			    		Log.v("Totoro:readthread","temp:" + temp);
			    		Intent intent = new Intent(Intent.ACTION_CALL,Uri.parse(temp));
			    		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    		app.startActivity(intent);
			    	} 
			    	/**
			    	 * ���͵���ϢΪ��ϵ���б�
			    	 */
			    	if (temp.contains("contacts")){
			    		Intent broadcastIntent = new Intent(PhonebookFragment.ACTION_CONTACTS);
			    		Bundle extras = new Bundle();
			    		extras.putSerializable(CONTACTS_BUNDLE_KEY, convert2Contacts(temp));
			    		broadcastIntent.putExtras(extras);
			    		app.sendBroadcast(broadcastIntent);
			    	}
                }
            } catch (IOException e) {
            	try {
					mmInStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
                break;
            }
        }
    }
    
    /**
     * ���ַ�����������ϵ���б�
     * 
     * @param str ��ϵ����Ϣ��
     * @return ArrayList<Person>
     */
    public ArrayList<Person> convert2Contacts(String str) {
    	ArrayList<Person> list = new ArrayList<Person>();
    	/**
    	 * ȥͷȥβ����ȡ��ϵ����Ϣ�Ӵ�
    	 * ��Ϣ��ʽ��#contacts��name-number,number$name-number%
    	 */
    	String temp1 = str.substring(str.indexOf(':') + 1, str.length() - 2);
    	/**
    	 * ��ȡ������ϵ����Ϣ��
    	 * name-number
    	 */
    	String[] temp2 = temp1.split("$");
    	for (String temp3 : temp2) {
    		/**
    		 * ���������͵绰����
    		 */
    		String[] temp4 = temp3.split("-");
    		/**
    		 * temp4��0λ�������
    		 */
    		Person p = new Person();
    		p.setStrName(temp4[0]);
    		/**
    		 * temp4��1λ��ź��봮
    		 */
    		ArrayList<String> numberList = new ArrayList<String>();
    		/**
    		 * number,number
    		 * �������
    		 */
    		String[] temp5 = temp4[1].split(",");
    		for (String temp6 : temp5) {
    			numberList.add(temp6);
    		}
    		
    		p.setAlPhoneNumber(numberList);
    		p.setStrGroup("");
    		list.add(p);
    	}
		return list;	
    }
}
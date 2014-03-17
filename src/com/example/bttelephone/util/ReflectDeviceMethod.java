package com.example.bttelephone.util;

import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;

/**
 * 2014-01-02
 * 
 * ͨ������ȡ��SDK���صķ���
 * �ο���ַ��http://www.eoeandroid.com/thread-52461-1-1.html
 * 
 * ʹ�ø��෽�����ף���Ч�ﵽĿ�Ĺ��ܣ������ڼ��������޷����κα�֤
 * @author totoro
 *
 */
public class ReflectDeviceMethod {
	
	/**
	* ���豸��� �ο�Դ�룺platform/packages/apps/Settings.git
	* \Settings\src\com\android\settings\bluetooth\CachedBluetoothDevice.java
	*/
	public static boolean createBond(Class btClass,BluetoothDevice btDevice) throws Exception {
		Method createBondMethod = btClass.getMethod("createBond");
		Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}
	
	/**
	* ���豸������ �ο�Դ�룺platform/packages/apps/Settings.git
	* \Settings\src\com\android\settings\bluetooth\CachedBluetoothDevice.java
	*/
	public static boolean removeBond(Class btClass,BluetoothDevice btDevice) throws Exception {
		Method removeBondMethod = btClass.getMethod("removeBond");
		Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}
}

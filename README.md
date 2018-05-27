# AndroidBluetoothTest
檢查藍芽連線狀態

1. 使用BluetoothManager.instance.initBluetooth,初始化變且註冊監聽手機藍牙狀態
 ```
val filter = IntentFilter()
filter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + BluetoothAssignedNumbers.PLANTRONICS)
filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
//耳機連結狀態
filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
//藍牙開啟關閉狀態
filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
mContext!!.registerReceiver(this, filter)
 ```
2. 當藍芽開啟後
 可以使用BluetoothAdapter.isEnabled來確認藍牙狀態
 ```
mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled) {
	//do something
}
 ```
3. 監聽藍牙裝置連接斷線狀態
可以透過BluetoothProfile.ServiceListener 這個interface來監聽手機藍牙裝置連結狀態
使用 mBluetoothAdapter!!.getProfileProxy()來註冊listener
 ```
var mProfileListener = object : BluetoothProfile.ServiceListener {
override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
//當有藍芽裝置連接就會觸發這個動作
	if (profile == BluetoothProfile.HEADSET) {
		mBluetoothHeadset = proxy as BluetoothHeadset
		isBluetoothConnected = true
	}
}

override fun onServiceDisconnected(profile: Int) {
	//當有藍芽裝置斷線就會觸發這個動作
	if (profile == BluetoothProfile.HEADSET) {
		mBluetoothHeadset = null
		isBluetoothConnected = false
		android.util.Log.d("BluetoothManager", "[Bluetooth] Headset disconnected")
	}
}
val success = mBluetoothAdapter!!.getProfileProxy(mContext, mProfileListener, BluetoothProfile.HEADSET)
 ```
 解除註冊
 ```
if (mBluetoothAdapter != null && mProfileListener != null && mBluetoothHeadset != null) {
	mBluetoothAdapter!!.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset)
	mProfileListener = null
}
 ```

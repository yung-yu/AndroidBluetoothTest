package com.andy.bluetoothtest


import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAssignedNumbers
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class BluetoothManager : BroadcastReceiver() {

	private var mContext: Context? = null
	private val mAudioManager: AudioManager? = null
	private var mBluetoothAdapter: BluetoothAdapter? = null
	private var mBluetoothHeadset: BluetoothHeadset? = null
	private var mBluetoothDevice: BluetoothDevice? = null
	private var mProfileListener: BluetoothProfile.ServiceListener? = null
	private var isBluetoothConnected: Boolean = false
	private var isScoConnected: Boolean = false
	var bluetoothListener: OnBluetoothListener? = null

	interface OnBluetoothListener{
		fun onActionChange(action:String, state:Int)

	}

	val isUsingBluetoothAudioRoute: Boolean
		get() = mBluetoothHeadset != null && mBluetoothHeadset!!.isAudioConnected(mBluetoothDevice) && isScoConnected

	val isBluetoothHeadsetAvailable: Boolean
		get() {
			if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled && mAudioManager != null && mAudioManager.isBluetoothScoAvailableOffCall) {
				var isHeadsetConnected = false
				if (mBluetoothHeadset != null) {
					val devices = mBluetoothHeadset!!.connectedDevices
					mBluetoothDevice = null
					for (dev in devices) {
						if (mBluetoothHeadset!!.getConnectionState(dev) == BluetoothHeadset.STATE_CONNECTED) {
							mBluetoothDevice = dev
							isHeadsetConnected = true
							break
						}
					}
					android.util.Log.d("BluetoothManager", if (isHeadsetConnected) "[Bluetooth] Headset found, bluetooth audio route available" else "[Bluetooth] No headset found, bluetooth audio route unavailable")
				}
				return isHeadsetConnected
			}

			return false
		}

	init {
		isBluetoothConnected = false
	}

	fun initBluetooth(context: Context) {
		mContext = context
		val filter = IntentFilter()
		filter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + BluetoothAssignedNumbers.PLANTRONICS)
		filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
		filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
		filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
		mContext!!.registerReceiver(this, filter)
		android.util.Log.d("BluetoothManager", "[Bluetooth] Receiver started")

		startBluetooth()
	}

	private fun startBluetooth() {
		if (isBluetoothConnected) {
			android.util.Log.e("BluetoothManager", "[Bluetooth] Already started, skipping...")
			return
		}

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

		if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled) {
			if (mProfileListener != null) {
				android.util.Log.w("BluetoothManager", "[Bluetooth] Headset profile was already opened, let's close it")
				mBluetoothAdapter!!.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset)
			}

			mProfileListener = object : BluetoothProfile.ServiceListener {
				override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
					if (profile == BluetoothProfile.HEADSET) {
						android.util.Log.d("BluetoothManager", "[Bluetooth] Headset connected")
						mBluetoothHeadset = proxy as BluetoothHeadset
						isBluetoothConnected = true
					}
				}

				override fun onServiceDisconnected(profile: Int) {
					if (profile == BluetoothProfile.HEADSET) {
						mBluetoothHeadset = null
						isBluetoothConnected = false
						android.util.Log.d("BluetoothManager", "[Bluetooth] Headset disconnected")
					}
				}
			}
			val success = mBluetoothAdapter!!.getProfileProxy(mContext, mProfileListener, BluetoothProfile.HEADSET)
			if (!success) {
				android.util.Log.e("BluetoothManager", "[Bluetooth] getProfileProxy failed !")
			}
		} else {
			android.util.Log.w("BluetoothManager", "[Bluetooth] Interface disabled on device")
		}
	}


	fun routeAudioToBluetooth(): Boolean {


		if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled && mAudioManager != null && mAudioManager.isBluetoothScoAvailableOffCall) {
			if (isBluetoothHeadsetAvailable) {
				if (mAudioManager != null && !mAudioManager.isBluetoothScoOn) {
					android.util.Log.d("BluetoothManager", "[Bluetooth] SCO off, let's start it")
					mAudioManager.isBluetoothScoOn = true
					mAudioManager.startBluetoothSco()
				}
			} else {
				return false
			}

			// Hack to ensure bluetooth sco is really running
			var ok = isUsingBluetoothAudioRoute
			var retries = 0
			while (!ok && retries < 5) {
				retries++

				try {
					Thread.sleep(200)
				} catch (e: InterruptedException) {
				}

				if (mAudioManager != null) {
					mAudioManager.isBluetoothScoOn = true
					mAudioManager.startBluetoothSco()
				}

				ok = isUsingBluetoothAudioRoute
			}
			if (ok) {
				if (retries > 0) {
					android.util.Log.d("BluetoothManager", "[Bluetooth] Audio route ok after $retries retries")
				} else {
					android.util.Log.d("BluetoothManager", "[Bluetooth] Audio route ok")
				}
			} else {
				android.util.Log.d("BluetoothManager", "[Bluetooth] Audio route still not ok...")
			}

			return ok
		}

		return false
	}

	fun disableBluetoothSCO() {
		if (mAudioManager != null && mAudioManager.isBluetoothScoOn) {
			mAudioManager.stopBluetoothSco()
			mAudioManager.isBluetoothScoOn = false

			// Hack to ensure bluetooth sco is really stopped
			var retries = 0
			while (isScoConnected && retries < 10) {
				retries++

				try {
					Thread.sleep(200)
				} catch (e: InterruptedException) {
				}

				mAudioManager.stopBluetoothSco()
				mAudioManager.isBluetoothScoOn = false
			}
			android.util.Log.w("BluetoothManager", "[Bluetooth] SCO disconnected!")
		}
	}

	fun stopBluetooth() {
		android.util.Log.w("BluetoothManager", "[Bluetooth] Stopping...")
		isBluetoothConnected = false

		disableBluetoothSCO()

		if (mBluetoothAdapter != null && mProfileListener != null && mBluetoothHeadset != null) {
			mBluetoothAdapter!!.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset)
			mProfileListener = null
		}
		mBluetoothDevice = null

		android.util.Log.w("BluetoothManager", "[Bluetooth] Stopped!")

	}

	fun destroy() {
		try {
			stopBluetooth()

			try {
				mContext!!.unregisterReceiver(this)
				android.util.Log.d("BluetoothManager", "[Bluetooth] Receiver stopped")
			} catch (e: Exception) {
			}

		} catch (e: Exception) {
			android.util.Log.e("BluetoothManager", e.message)
		}

	}

	override fun onReceive(context: Context, intent: Intent) {

		val action = intent.action

		if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED == action) {
			val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0)
			bluetoothListener?.onActionChange(action, state)
			if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
				android.util.Log.d("BluetoothManager", "[Bluetooth] SCO state: connected")

				isScoConnected = true
			} else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
				android.util.Log.d("BluetoothManager", "[Bluetooth] SCO state: disconnected")
				isScoConnected = false
			} else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
				android.util.Log.d("BluetoothManager", "[Bluetooth] SCO state: connecting")
				isScoConnected = true
			} else {
				android.util.Log.d("BluetoothManager", "[Bluetooth] SCO state: $state")
			}

		} else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED == action) {
			val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
			bluetoothListener?.onActionChange(action, state)
			if (state == BluetoothAdapter.STATE_DISCONNECTED) {
				android.util.Log.d("BluetoothManager", "[Bluetooth] State: disconnected")
				stopBluetooth()
			} else if (state == BluetoothAdapter.STATE_CONNECTED) {
				android.util.Log.d("BluetoothManager", "[Bluetooth] State: connected")
				startBluetooth()
			} else {
				android.util.Log.d("BluetoothManager", "[Bluetooth] State: $state")
			}
		} else if (intent.action == BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT) {
			val command = intent.extras!!.getString(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
			//int type = intent.getExtras().getInt(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE);

			val args = intent.extras!!.get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS) as Array<Any>
			if (args.size <= 0) {
				android.util.Log.d("BluetoothManager", "[Bluetooth] Event: $command, no args")
				return
			}
			val eventName = args[0].toString()
			if (eventName == "BUTTON" && args.size >= 3) {
				val buttonID = args[1].toString()
				val mode = args[2].toString()
				android.util.Log.d("BluetoothManager", "[Bluetooth] Event: $command : $eventName, id = $buttonID ($mode)")
			} else {
				android.util.Log.d("BluetoothManager", "[Bluetooth] Event: $command : $eventName")
			}
		}
	}

	companion object {
		 val instance: BluetoothManager by lazy { BluetoothManager() }

	}
}

package com.andy.bluetoothtest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.andy.bluetoothtest.BluetoothManager.OnBluetoothListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		BluetoothManager.instance.initBluetooth(this)
		BluetoothManager.instance.bluetoothListener = object :OnBluetoothListener{
			override fun onActionChange(action: String, state:Int) {
				text.setText(action+"\n"+state)
			}

		}
	}

	override fun onDestroy() {
		super.onDestroy()
		BluetoothManager.instance.destroy()
	}
}

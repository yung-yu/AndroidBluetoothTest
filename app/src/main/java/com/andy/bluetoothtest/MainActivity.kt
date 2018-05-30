package com.andy.bluetoothtest

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import com.andy.bluetoothtest.BluetoothManager.OnBluetoothListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileInputStream
import java.io.IOException
import android.media.RingtoneManager


class MainActivity : AppCompatActivity() {
	var msg: StringBuilder = StringBuilder()
	lateinit var mAudioManager: AudioManager;
	lateinit var mVibrator: Vibrator;
	var mRingerPlayer: MediaPlayer? = null;
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
		mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		BluetoothManager.instance.initBluetooth(this)
		BluetoothManager.instance.bluetoothListener = object : OnBluetoothListener {
			override fun onActionChange(action: String, state: Int, time: Long) {
				if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED == action && state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
					//BluetoothManager.instance.routeAudioToBluetooth()
				} else if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED == action && state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
					//BluetoothManager.instance.disableBluetoothSCO()
				}
				msg.append(action)
				msg.append("\n")
				msg.append(state)
				msg.append("\n")
				msg.append(time)
				msg.append("\n")
				text.setText(msg.toString())
			}

		}
		button.setOnClickListener({
			if (isRinging) {
				stopRinging()
			} else {
				startRinging()
			}
		})
	}

	override fun onDestroy() {
		super.onDestroy()
		BluetoothManager.instance.destroy()
	}

	var isRinging = false;

	@Synchronized
	private fun startRinging() {

		//this.mAudioManager.isSpeakerphoneOn = true
		this.mAudioManager.setMode(AudioManager.MODE_RINGTONE)

		try {
			if ((this.mAudioManager.getRingerMode() == 1 || this.mAudioManager.getRingerMode() == 2) && this.mVibrator != null) {
				val patern = longArrayOf(0L, 1000L, 1000L)
				this.mVibrator.vibrate(patern, 1)
			}

			if (mRingerPlayer == null) {
				this.requestAudioFocus(AudioManager.STREAM_RING)
				this.mRingerPlayer = MediaPlayer()
				this.mRingerPlayer?.setAudioStreamType(2)

				val defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE)
				var ringtone = defaultRingtoneUri.toString()
				try {
					if (ringtone.startsWith("content://")) {
						this.mRingerPlayer?.setDataSource(this, Uri.parse(ringtone))
					} else {
						val fis = FileInputStream(ringtone)
						this.mRingerPlayer?.setDataSource(fis.getFD())
						fis.close()
					}
				} catch (var3: IOException) {
				}

				this.mRingerPlayer?.prepare()
				this.mRingerPlayer?.setLooping(true)
				this.mRingerPlayer?.start()
			} else {

			}
		} catch (var4: Exception) {

		}
		this.isRinging = true
	}

	@Synchronized
	private fun stopRinging() {
		if (this.mRingerPlayer != null) {
			this.mRingerPlayer?.stop()
			this.mRingerPlayer?.release()
			this.mRingerPlayer = null
		}

		this.mVibrator.cancel()
		isRinging = false
	}

	var mAudioFocused = false
	private fun requestAudioFocus(stream: Int) {
		if (!this.mAudioFocused) {
			val res = this.mAudioManager.requestAudioFocus(null as AudioManager.OnAudioFocusChangeListener?, stream, 2)
			if (res == 1) {
				this.mAudioFocused = true
			}
		}

	}
}

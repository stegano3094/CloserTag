package com.stegano.closertag

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    val TAG: String = "MainActivity"

    // NFC의 필수 변수 선언
    private lateinit var adapter: NfcAdapter  // 안드로이드 단말기의 NFC 정보를 가져오는 변수
    private lateinit var pendingIntent: PendingIntent  // NFC로 전송받은 데이터를 Intent를 통해 다른 액티비티로 넘겨주는 역할

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 단말기의 NFC 사용이 불가능 할 때 null을 반환함
        adapter = NfcAdapter.getDefaultAdapter(this)

        // 현재 액티비티에서 처리하는 방법
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // 다른 액티비티에서 처리하는 방법
//        val intent = Intent(this, second::class.java)

        // 태그의 세부 정보를 채움 (해당 액티비티에 intent 정보를 넣는다)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

    }

    // 생명주기 콜백 정의 -----------------------------------------------------------------------------
    // 포커스를 얻음
    override fun onResume() {
        super.onResume()
        // enableForegroundDispatch()는 기본 스레드에서 호출해야 하며, 활동이 포그라운드에 있을 경우에만 호출해야 합니다.
        // (this, pendingIntent, null, null)로 선언 시 필터링 없이 모든 데이터를 읽고 전송한다.
        adapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    // 포커스를 잃음
    override fun onPause() {
        super.onPause()
        // 화면에 보이지 않을 경우 데이터 수신을 종료한다.
        adapter.disableForegroundDispatch(this)  // 수신
    }

    // 검사된 NFC 태그에서 데이터 처리 시 콜백 구현
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val tagFromIntent : Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

        val writeValue = "https://github.com/stegano3094";
        val message: NdefMessage = NdefMessage(NdefRecord.createUri(writeValue))

        if(tagFromIntent != null) {
            writeTag(message, tagFromIntent)
        } else {
            Toast.makeText(applicationContext, "태그를 인식하지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeTag(message: NdefMessage, tag: Tag) {
        // 스니펫 (재사용 가능한 소스코드, 루틴 등)
        val size = message.toByteArray().size
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    Toast.makeText(applicationContext, "NFC 태그에 정보를 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                if (ndef.maxSize < size) {
                    Toast.makeText(applicationContext, "NFC 태그에 저장할 정보를 줄여주세요.", Toast.LENGTH_SHORT).show()
                }
                ndef.writeNdefMessage(message)
                Toast.makeText(applicationContext, "NFC 태그에 저장하였습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.printStackTrace().toString())
        }
    }
}
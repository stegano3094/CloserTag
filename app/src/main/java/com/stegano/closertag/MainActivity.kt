package com.stegano.closertag

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_drawerlayout.*
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    val TAG: String = "MainActivity"

    // NFC의 필수 변수 선언
    private var adapter: NfcAdapter? = null  // 안드로이드 단말기의 NFC 정보를 가져오는 변수
    private lateinit var pendingIntent: PendingIntent  // NFC로 전송받은 데이터를 Intent를 통해 다른 액티비티로 넘겨주는 역할

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_drawerlayout)  // 레이아웃 세팅
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)  // 화면 꺼짐 방지

        naviView.setNavigationItemSelectedListener(this)  // 드로어 리스너 세팅

        // 단말기의 NFC 사용이 불가능 할 때 null을 반환함
        if(NfcAdapter.getDefaultAdapter(this) != null) {
            adapter = NfcAdapter.getDefaultAdapter(this)
        } else {
            Toast.makeText(this, "이 기기에 NFC 기능이 없어 앱을 종료합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 현재 액티비티에서 처리하는 방법
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // 태그의 세부 정보를 채움 (해당 액티비티에 intent 정보를 넣는다)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
    }

    override fun onStart() {
        super.onStart()

        drawerlayout.closeDrawers()  // 액티비티가 완전히 안보이고 다시 보일 때 드로어 닫음
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.item1 -> {
                print("1번 아이템 선택됨")
            }
            R.id.item2 -> {
                print("2번 아이템 선택됨")
            }
        }

        drawerlayout.closeDrawers()  // 아이템 선택 시 드로어 닫음
        return false
    }

    override fun onBackPressed() {
        if (drawerlayout.isDrawerOpen(GravityCompat.START))  // 네비드로어가 열려있을 경우 닫음
            drawerlayout.closeDrawers()
        else
            super.onBackPressed()
    }

    // 생명주기 콜백 정의 -----------------------------------------------------------------------------
    // 포커스를 얻음
    override fun onResume() {
        super.onResume()
        // enableForegroundDispatch()는 기본 스레드에서 호출해야 하며, 활동이 포그라운드에 있을 경우에만 호출해야 합니다.
        // (this, pendingIntent, null, null)로 선언 시 필터링 없이 모든 데이터를 읽고 전송한다.
        adapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    // 포커스를 잃음
    override fun onPause() {
        super.onPause()
        // 화면에 보이지 않을 경우 데이터 수신을 종료한다.
        adapter?.disableForegroundDispatch(this)  // 수신
    }

    // 검사된 NFC 태그에서 데이터 처리 시 콜백 구현
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // 페이로드를 포함하고 태그의 기술을 열거할 수 있는 Tag 객체를 인텐트에서 가져옴
        val tagFromIntent : Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

        // 참고 사이트 : https://developer.android.com/guide/topics/connectivity/nfc/nfc?hl=ko

        if(tagFromIntent != null) {
            if(toggleButton.isChecked) {  // 토글 버튼 ON 시 NFC 태그로 전송함
                writeTag(makeMessage(), tagFromIntent)
            } else {
                readUriTag(tagFromIntent)
            }
        } else {
            Toast.makeText(applicationContext, "태그를 인식하지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeMessage() : NdefMessage {
        // NFC로 URI 전송 시
        var dataValue = editUserInputData.text.toString()

        // http:// 미입력 시 넣어주는 코드 (NFC 태그를 읽을 때 http://가 없으면 에러 발생하므로 입력할 때 넣어줌)
        if(!dataValue.contains("http://")) {
            dataValue = "http://$dataValue"
        }

        val uriField = dataValue.toByteArray(Charset.forName("UTF-8"))  // 방법 3
        val payload = ByteArray(uriField.size + 1)  // add 1 for the URI Prefix
        payload [0] = 0x01  // prefixes http://www. to the URI
        System.arraycopy(uriField, 0, payload, 1, uriField.size)  // appends URI to payload
        val rtdUriRecord = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, ByteArray(0), payload)

//        val rtdUriRecord1 = NdefRecord.createUri("http://example.com")  // 방법 1
//
//        val rtdUriRecord2 = Uri.parse("http://example.com").let { uri ->  // 방법 2
//            NdefRecord.createUri(uri)
//        }
//        val makeMessage: NdefMessage = NdefMessage(uriValue.let { uri ->
//            NdefRecord.createUri(uri)
//        })

        // NFC로 텍스트 전송 시  (US-ASCII, UTF-8, UTF-16, ...)
//        val makeMessage: NdefMessage = NdefMessage(NdefRecord.createTextRecord("UTF-16", "테스트입니다~!!"))

        val makeMessage: NdefMessage = NdefMessage(rtdUriRecord)
        Log.e(TAG, "makeMessage: $makeMessage")

        return makeMessage
    }

    private fun readUriTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            val data = ndef.cachedNdefMessage
            Toast.makeText(applicationContext, "NFC 태그에 저장된 값 : $data", Toast.LENGTH_SHORT).show()

            Log.e(TAG, "readUriTag()")
            recsLog(data.records)

            for (i in data.records) {
                readDataText.text = String(i.payload)

                // payload [0] = 0x01 데이터로 인해 제대로 읽지 못해서 이 데이터를 지우는 작업을 하고 인터넷으로 이동한다.
                val resultUriData = String(i.payload).subSequence(1, i.payload.size)
                Log.e(TAG, "readUriTag: resultUriData: ${resultUriData}")
                val intent2 = Intent(Intent.ACTION_VIEW, Uri.parse(resultUriData.toString()))
                startActivity(intent2)
            }
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
                    return
                }
                if (ndef.maxSize < size) {
                    Toast.makeText(applicationContext, "NFC 태그에 저장할 정보를 줄여주세요.", Toast.LENGTH_SHORT).show()
                    return
                }
                ndef.writeNdefMessage(message)
                Toast.makeText(applicationContext, "NFC 태그에 저장하였습니다.", Toast.LENGTH_SHORT).show()

                Log.e(TAG, "writeTag()")
                recsLog(message.records)
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, e.printStackTrace().toString())
        }
    }

    private fun recsLog(data: Array<NdefRecord>) {
        for (i in data) {
            Log.e(TAG, "recsLog: NFC 태그 id : ${String(i.id)}")
            Log.e(TAG, "recsLog: NFC 태그 tnf : ${i.tnf}")
            Log.e(TAG, "recsLog: NFC 태그 type : ${String(i.type)}")
            Log.e(TAG, "recsLog: NFC 태그 payload : ${String(i.payload)}")
        }
    }
}
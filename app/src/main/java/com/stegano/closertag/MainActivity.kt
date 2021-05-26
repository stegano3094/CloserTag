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
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    val TAG: String = "MainActivity"

    // NFC의 필수 변수 선언
    private lateinit var adapter: NfcAdapter  // 안드로이드 단말기의 NFC 정보를 가져오는 변수
    private lateinit var pendingIntent: PendingIntent  // NFC로 전송받은 데이터를 Intent를 통해 다른 액티비티로 넘겨주는 역할

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_drawerlayout)  // 레이아웃 세팅
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)  // 화면 꺼짐 방지

        naviView.setNavigationItemSelectedListener(this)  // 드로어 리스너 세팅

        // 단말기의 NFC 사용이 불가능 할 때 null을 반환함
        adapter = NfcAdapter.getDefaultAdapter(this)

        // 현재 액티비티에서 처리하는 방법
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // 태그의 세부 정보를 채움 (해당 액티비티에 intent 정보를 넣는다)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        readDataText.setOnClickListener {
            val link = "" + readDataText.text
            Log.e(TAG, "onCreate: link: $link")
            val intent2 = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(intent2)
        }
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

        // 페이로드를 포함하고 태그의 기술을 열거할 수 있는 Tag 객체를 인텐트에서 가져옴
        val tagFromIntent : Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val writeValue = "http://steganowork.ipdisk.co.kr/apps/xe/"
        // NFC로 메시지 전송 시
        val makeMessage: NdefMessage = NdefMessage(Uri.parse(writeValue).let { uri ->
            NdefRecord.createUri(uri)
        })

        // NFC로 텍스트 전송 시  (US-ASCII, UTF-8, UTF-16, ...)
//        val makeMessage: NdefMessage = NdefMessage(NdefRecord.createTextRecord("UTF-16", "테스트입니다~!!"))

        Log.e(TAG, "onNewIntent: makeMessage: $makeMessage")

        if(tagFromIntent != null) {
            if(toggleButton.isChecked) {  // 토글 버튼 ON 시 NFC 태그로 전송함
                writeTag(makeMessage, tagFromIntent)
            } else {
                readTag(tagFromIntent)
            }
        } else {
            Toast.makeText(applicationContext, "태그를 인식하지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readTag(tag: Tag) {
        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED){
            val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if(messages == null){
                return
            }

            for (msgIndex in messages.indices){
                val msg: NdefMessage = messages[msgIndex] as NdefMessage
                val recs = msg.records

                for (recsIndex in recs.indices) {
                    val record = recs[recsIndex]
                    if (Arrays.equals(record.type, NdefRecord.RTD_URI)) {
                        val intent2 = Intent(Intent.ACTION_VIEW, record.toUri())
                        Log.e(TAG, "readTag: record.toUri(): $record.toUri()")
                        startActivity(intent2)
                    }
                }
            }
        }

        // URI 링크 이상한데 수정해야함

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            val data = ndef.cachedNdefMessage
            Toast.makeText(applicationContext, "NFC 태그에 저장된 값 : $data", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "onNewIntent: NFC 태그에 저장된 값 : $data")

            for (i in data.records) {
                Log.e(TAG, "onNewIntent: NFC 태그에 저장된 값 id : ${String(i.id)}")
                Log.e(TAG, "onNewIntent: NFC 태그에 저장된 값 tnf : ${i.tnf}")
                Log.e(TAG, "onNewIntent: NFC 태그에 저장된 값 type : ${i.type}")
                Log.e(TAG, "onNewIntent: NFC 태그에 저장된 값 payload : ${String(i.payload)}")
                readDataText.text = String(i.payload)
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
            }
        } catch (e: Exception) {
            Log.e(TAG, e.printStackTrace().toString())
        }
    }
}
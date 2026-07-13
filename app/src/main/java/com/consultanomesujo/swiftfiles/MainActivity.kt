package com.consultanomesujo.swiftfiles

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile

class MainActivity : AppCompatActivity() {
    private lateinit var list: ListView
    private lateinit var pathLabel: TextView
    private var current: DocumentFile? = null
    private var rootUri: Uri? = null
    private var entries: List<DocumentFile> = emptyList()

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            rootUri = uri
            getSharedPreferences("state", MODE_PRIVATE).edit().putString("tree", uri.toString()).apply()
            open(DocumentFile.fromTreeUri(this, uri))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }
        root.addView(title(getString(R.string.app_name)))
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        val choose = Button(this).apply { text = "Escolher pasta"; setOnClickListener { picker.launch(rootUri) } }
        val up = Button(this).apply { text = "Subir"; setOnClickListener { current?.parentFile?.let { open(it) } } }
        bar.addView(choose, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(up, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(bar)
        pathLabel = TextView(this).apply { setPadding(dp(12), dp(6), dp(12), dp(6)); setTextColor(Color.DKGRAY) }
        root.addView(pathLabel)
        list = ListView(this)
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        attachAd(root)
        setContentView(root)
        list.setOnItemClickListener { _, _, position, _ ->
            val item = entries[position]
            if (item.isDirectory) open(item) else openFile(item)
        }
        val saved = getSharedPreferences("state", MODE_PRIVATE).getString("tree", null)
        if (saved != null) {
            rootUri = Uri.parse(saved)
            open(DocumentFile.fromTreeUri(this, rootUri!!))
        } else picker.launch(null)
    }

    private fun open(folder: DocumentFile?) {
        if (folder == null || !folder.isDirectory) return
        current = folder
        pathLabel.text = folder.name ?: "Armazenamento"
        entries = folder.listFiles().sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
        val labels = entries.map { (if (it.isDirectory) "📁 " else "📄 ") + (it.name ?: "Sem nome") }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }

    private fun openFile(file: DocumentFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, file.type ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }.onFailure { Toast.makeText(this, "Nenhum app compatível", Toast.LENGTH_SHORT).show() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun title(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(Color.WHITE)
        setPadding(dp(16), dp(16), dp(16), dp(12))
        setBackgroundColor(Color.parseColor(getString(R.string.color_primary)))
    }

    private fun attachAd(root: LinearLayout) {
        val adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = getString(R.string.admob_banner_id)
        }
        root.addView(adView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL })
        MobileAds.initialize(this) { adView.loadAd(AdRequest.Builder().build()) }
    }

}

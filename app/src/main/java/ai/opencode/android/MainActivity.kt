package ai.opencode.android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnStartTerminal: Button
    private lateinit var btnSetup: Button
    private lateinit var btnSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnStartTerminal = findViewById(R.id.btnStartTerminal)
        btnSetup = findViewById(R.id.btnSetup)
        btnSettings = findViewById(R.id.btnSettings)

        btnStartTerminal.setOnClickListener {
            startActivity(Intent(this, TerminalActivity::class.java))
        }

        btnSetup.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        checkSetupStatus()
    }

    override fun onResume() {
        super.onResume()
        checkSetupStatus()
    }

    private fun checkSetupStatus() {
        lifecycleScope.launch {
            val isSetupComplete = withContext(Dispatchers.IO) {
                checkIfSetupComplete()
            }
            if (isSetupComplete) {
                statusText.text = getString(R.string.status_setup_complete)
                statusText.setTextColor(getColor(R.color.terminal_green))
                btnStartTerminal.isEnabled = true
            } else {
                statusText.text = "Setup required — tap Settings to configure"
                statusText.setTextColor(getColor(R.color.terminal_yellow))
                btnStartTerminal.isEnabled = false
            }
        }
    }

    private fun checkIfSetupComplete(): Boolean {
        val baseDir = File(filesDir, "opencode")
        val nodeDir = File(baseDir, "node")
        val nodeBin = File(nodeDir, "bin/node")
        val opencodeDir = File(baseDir, "opencode-cli")
        val opencodeBin = File(opencodeDir, "node_modules/.bin/opencode")
        return nodeBin.exists() && (opencodeBin.exists() || File(opencodeDir, "package.json").exists())
    }
}

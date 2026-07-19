package ai.opencode.android

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class TerminalActivity : AppCompatActivity() {

    private lateinit var terminalOutput: TextView
    private lateinit var terminalScroll: ScrollView
    private lateinit var commandInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var promptView: TextView
    private lateinit var sessionInfo: TextView

    private var shellProcess: Process? = null
    private var shellInput: OutputStream? = null
    private var shellReaderJob: Job? = null

    private val outputBuilder = SpannableStringBuilder()
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_OpenCode_Terminal)
        setContentView(R.layout.activity_terminal)

        terminalOutput = findViewById(R.id.terminalOutput)
        terminalScroll = findViewById(R.id.terminalScroll)
        commandInput = findViewById(R.id.commandInput)
        btnSend = findViewById(R.id.btnSend)
        promptView = findViewById(R.id.promptView)
        sessionInfo = findViewById(R.id.sessionInfo)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { finish() }

        btnSend.setOnClickListener { sendCommand() }

        commandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendCommand()
                true
            } else false
        }

        startShell()
    }

    override fun onDestroy() {
        super.onDestroy()
        killShell()
    }

    private fun startShell() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val env = mutableMapOf<String, String>()
                    env["TERM"] = "xterm-256color"
                    env["HOME"] = filesDir.absolutePath
                    env["OPENCODE_HOME"] = File(filesDir, "opencode").absolutePath

                    // Add opencode and node to PATH
                    val opencodeDir = File(filesDir, "opencode")
                    val nodeBinDir = File(opencodeDir, "node/bin")
                    val opencodeBinDir = File(opencodeDir, "opencode-cli/node_modules/.bin")
                    val pathEnv = buildString {
                        append(nodeBinDir.absolutePath)
                        append(":")
                        append(opencodeBinDir.absolutePath)
                        append(":")
                        append("/system/bin")
                        append(":")
                        append("/system/xbin")
                    }
                    env["PATH"] = pathEnv

                    val pb = ProcessBuilder("/system/bin/sh")
                    pb.environment().putAll(env)
                    pb.directory(filesDir)
                    pb.redirectErrorStream(true)

                    shellProcess = pb.start()
                    shellInput = shellProcess!!.outputStream

                    appendOutput("OpenCode Terminal v1.0.0\n", ForegroundColorSpan(getColor(R.color.terminal_green)))
                    appendOutput("Type 'opencode --help' to get started\n", ForegroundColorSpan(getColor(R.color.terminal_blue)))
                    appendOutput("Type 'exit' to close the terminal\n\n", ForegroundColorSpan(getColor(R.color.terminal_text)))

                    isRunning = true
                    sessionInfo.text = "● Running"
                    sessionInfo.setTextColor(getColor(R.color.terminal_green))
                    commandInput.isEnabled = true
                    btnSend.isEnabled = true

                    // Read shell output
                    shellReaderJob = launch(Dispatchers.IO) {
                        try {
                            val reader = shellProcess!!.inputStream.bufferedReader()
                            val buffer = CharArray(4096)
                            while (isRunning) {
                                val charsRead = reader.read(buffer, 0, buffer.size)
                                if (charsRead == -1) break
                                val text = String(buffer, 0, charsRead)
                                withContext(Dispatchers.Main) {
                                    appendOutput(text, ForegroundColorSpan(getColor(R.color.terminal_text)))
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                appendOutput("\n[Session ended: ${e.message}]\n", ForegroundColorSpan(getColor(R.color.terminal_yellow)))
                            }
                        }
                    }

                    shellProcess!!.waitFor()
                    isRunning = false
                    withContext(Dispatchers.Main) {
                        appendOutput("\n[Process exited with code ${shellProcess!!.exitValue()}]\n", ForegroundColorSpan(getColor(R.color.terminal_yellow)))
                        sessionInfo.text = "● Stopped"
                        sessionInfo.setTextColor(getColor(R.color.terminal_yellow))
                        commandInput.isEnabled = false
                        btnSend.isEnabled = false
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        appendOutput("\n[Error starting shell: ${e.message}]\n", ForegroundColorSpan(getColor(R.color.terminal_yellow)))
                        isRunning = false
                        sessionInfo.text = "● Error"
                        sessionInfo.setTextColor(getColor(R.color.terminal_yellow))
                    }
                }
            }
        }
    }

    private fun sendCommand() {
        val cmd = commandInput.text.toString().trim()
        if (cmd.isEmpty()) return
        commandInput.text.clear()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                shellInput?.write("$cmd\n".toByteArray())
                shellInput?.flush()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendOutput("\n[Error: ${e.message}]\n", ForegroundColorSpan(getColor(R.color.terminal_yellow)))
                }
            }
        }
    }

    private fun appendOutput(text: String, color: ForegroundColorSpan) {
        val start = outputBuilder.length
        outputBuilder.append(text)
        outputBuilder.setSpan(color, start, outputBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        terminalOutput.text = outputBuilder

        // Auto-scroll to bottom
        terminalScroll.post {
            terminalScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun killShell() {
        isRunning = false
        shellReaderJob?.cancel()
        try {
            shellInput?.write("exit\n".toByteArray())
            shellInput?.flush()
        } catch (_: Exception) {}
        shellProcess?.destroy()
        shellProcess = null
        shellInput = null
    }
}

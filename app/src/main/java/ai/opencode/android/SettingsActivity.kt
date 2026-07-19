package ai.opencode.android

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class SettingsActivity : AppCompatActivity() {

    private lateinit var setupCard: CardView
    private lateinit var setupProgressText: TextView
    private lateinit var setupProgressBar: ProgressBar
    private lateinit var setupLog: TextView
    private lateinit var btnRunSetup: Button

    private var isRunning = false

    companion object {
        private const val NODE_URL = "https://github.com/termux/termux-packages/releases/download/nodejs-22.14.0/nodejs_22.14.0_arm64.deb"
        private const val NODE_VERSION = "22.14.0"

        // Fallback: Use prebuilt Node.js binary for ARM64 Linux (works on Android via proot)
        private const val NODE_BINARY_URL = "https://nodejs.org/dist/v22.14.0/node-v22.14.0-linux-arm64.tar.xz"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupCard = findViewById(R.id.setupCard)
        setupProgressText = findViewById(R.id.setupProgressText)
        setupProgressBar = findViewById(R.id.setupProgressBar)
        setupLog = findViewById(R.id.setupLog)
        btnRunSetup = findViewById(R.id.btnRunSetup)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        btnRunSetup.setOnClickListener { runSetup() }
    }

    private fun runSetup() {
        if (isRunning) return
        isRunning = true
        btnRunSetup.isEnabled = false
        setupCard.visibility = View.VISIBLE
        setupLog.visibility = View.VISIBLE
        setupLog.text = ""
        appendLog("Starting setup…\n")

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val baseDir = File(filesDir, "opencode")
                    baseDir.mkdirs()

                    val nodeDir = File(baseDir, "node")
                    val opencodeDir = File(baseDir, "opencode-cli")

                    // Step 1: Download and extract Node.js
                    updateProgress("Downloading Node.js v$NODE_VERSION for ARM64…")
                    appendLog("→ Downloading Node.js…\n")

                    val nodeArchive = File(baseDir, "node.tar.xz")
                    downloadFile(NODE_BINARY_URL, nodeArchive)

                    appendLog("  Downloaded (${nodeArchive.length()} bytes)\n")
                    updateProgress("Extracting Node.js…")
                    appendLog("→ Extracting Node.js…\n")

                    // Extract tar.xz
                    extractTarXz(nodeArchive, nodeDir)

                    // Find the actual node binary
                    val extractedNode = findNodeBinary(nodeDir)
                    if (extractedNode != null) {
                        extractedNode.setExecutable(true)
                        appendLog("  Node.js binary: ${extractedNode.absolutePath}\n")
                    }

                    // Verify Node.js works
                    updateProgress("Verifying Node.js…")
                    appendLog("→ Verifying Node.js…\n")

                    val nodeBin = File(nodeDir, "bin/node")
                    if (nodeBin.exists()) {
                        val version = runCommand(nodeBin.absolutePath, "--version")
                        appendLog("  Node.js version: $version\n")
                    } else {
                        appendLog("  ⚠ Node.js binary not found at expected location\n")
                    }

                    // Step 2: Install opencode via npm
                    updateProgress("Installing OpenCode CLI…")
                    appendLog("→ Installing OpenCode CLI via npm…\n")

                    val npmBin = File(nodeDir, "bin/npm")
                    if (npmBin.exists()) {
                        opencodeDir.mkdirs()

                        // Create a minimal package.json for opencode
                        val pkgJson = File(opencodeDir, "package.json")
                        pkgJson.writeText("""{"name":"opencode-android","private":true}""")

                        val npmInstall = runCommand(
                            npmBin.absolutePath,
                            "install", "opencode@latest",
                            "--prefix", opencodeDir.absolutePath,
                            "--no-optional",
                            "--no-audit",
                            "--no-fund"
                        )
                        appendLog("  npm install output:\n  $npmInstall\n")
                    } else {
                        appendLog("  ⚠ npm not found, trying alternative…\n")
                        // Try using npx
                        if (nodeBin.exists()) {
                            val npxResult = runCommand(
                                nodeBin.absolutePath,
                                "-e",
                                "const { execSync } = require('child_process'); console.log(execSync('npm install -g opencode --prefix ${opencodeDir.absolutePath}').toString())"
                            )
                            appendLog("  $npxResult\n")
                        }
                    }

                    // Step 3: Verify installation
                    updateProgress("Verifying installation…")
                    appendLog("→ Verifying OpenCode installation…\n")

                    val opencodeBin = File(opencodeDir, "node_modules/.bin/opencode")
                    if (opencodeBin.exists()) {
                        val version = if (nodeBin.exists()) {
                            runCommand(nodeBin.absolutePath, opencodeBin.absolutePath, "--version")
                        } else {
                            "unknown"
                        }
                        appendLog("  OpenCode CLI version: $version\n")
                    } else {
                        // Also check for opencode in node_modules
                        val opencodeModule = File(opencodeDir, "node_modules/opencode")
                        if (opencodeModule.exists()) {
                            appendLog("  OpenCode module found at $opencodeModule\n")
                        } else {
                            appendLog("  ⚠ OpenCode binary not found. Checking directory contents…\n")
                            appendLog("  Contents: ${opencodeDir.list()?.joinToString() ?: "empty"}\n")
                        }
                    }

                    // Clean up archive
                    nodeArchive.delete()

                    updateProgress("Setup complete!")
                    appendLog("\n✓ Setup completed successfully!\n")
                    appendLog("You can now use OpenCode CLI in the terminal.\n")
                    appendLog("Type 'opencode --help' to get started.\n")

                } catch (e: Exception) {
                    appendLog("\n✗ Error: ${e.message}\n")
                    appendLog("Please check your internet connection and try again.\n")
                    updateProgress("Setup failed: ${e.message}")
                } finally {
                    isRunning = false
                    withContext(Dispatchers.Main) {
                        btnRunSetup.isEnabled = true
                        setupProgressBar.isIndeterminate = false
                        setupProgressBar.progress = 100
                    }
                }
            }
        }
    }

    private fun updateProgress(text: String) {
        lifecycleScope.launch {
            setupProgressText.text = text
        }
    }

    private fun appendLog(text: String) {
        lifecycleScope.launch {
            setupLog.append(text)
        }
    }

    private fun downloadFile(urlStr: String, targetFile: File) {
        val url = URL(urlStr)
        url.openStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractTarXz(archive: File, targetDir: File) {
        // First decompress xz, then extract tar
        val decompressed = File(archive.parentFile, archive.name.replace(".xz", ""))
        try {
            // Try using tar command if available
            val result = runCommand("tar", "-xJf", archive.absolutePath, "-C", targetDir.absolutePath)
            appendLog("  tar extraction: $result\n")
        } catch (e: Exception) {
            // If tar is not available, use Java-based extraction
            appendLog("  tar not available, trying alternative extraction…\n")
            extractUsingJava(archive, targetDir)
        }
    }

    private fun extractUsingJava(archive: File, targetDir: File) {
        // For tar.xz, we need to decompress xz first
        // Since we can't easily do this in pure Java without dependencies,
        // try using busybox or other available tools
        try {
            val result = runCommand("busybox", "tar", "-xJf", archive.absolutePath, "-C", targetDir.absolutePath)
            appendLog("  busybox extraction: $result\n")
        } catch (e2: Exception) {
            throw RuntimeException("Cannot extract tar.xz archive. Please install tar or busybox.", e2)
        }
    }

    private fun findNodeBinary(nodeDir: File): File? {
        // Search for node binary in extracted directory
        val candidates = listOf(
            File(nodeDir, "bin/node"),
            File(nodeDir, "node-v22.14.0-linux-arm64/bin/node"),
        )
        for (candidate in candidates) {
            if (candidate.exists()) return candidate
        }
        // Search recursively
        return nodeDir.walkTopDown().find { it.name == "node" && it.isFile }
    }

    private fun runCommand(vararg cmd: String): String {
        val pb = ProcessBuilder(*cmd)
        pb.redirectErrorStream(true)
        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Command '${cmd.joinToString(" ")}' failed with exit code $exitCode: $output")
        }
        return output.trim()
    }
}

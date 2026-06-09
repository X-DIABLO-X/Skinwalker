package com.example.skinwalker

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.skinwalker.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var inventory: AppInventory
    private lateinit var cloneRepository: CloneRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var storageMeter: StorageMeter
    private lateinit var virtualEngine: VirtualEngine
    private lateinit var updateManager: UpdateManager
    private lateinit var activeProfile: SkinwalkerProfile
    private var availableApps: List<AppEntry> = emptyList()
    private var clones: List<CloneEntry> = emptyList()
    private var inventoryLoading = false
    private var startupUpdateCheckRunning = false
    private val cloneAppCache = mutableMapOf<String, AppEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(UiPreferences.savedNightMode(this))
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inventory = AppInventory(this)
        cloneRepository = CloneRepository(this)
        profileRepository = ProfileRepository(this)
        storageMeter = StorageMeter(this)
        virtualEngine = VirtualEngineProvider.get(this)
        updateManager = UpdateManager(this)
        activeProfile = profileRepository.activeProfile()
        binding.menuButton.setOnClickListener { showSettingsSheet() }
        binding.profileButton.setOnClickListener { showProfileSheet() }
        binding.profileStrip.setOnClickListener { showProfileSheet() }

        render()
        loadInventoryIfNeeded()
        checkForUpdatesOnStartup()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeProfile = profileRepository.activeProfile()
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            showMainLoading(false)
            render()
        }
    }

    private fun render() {
        activeProfile = profileRepository.activeProfile()
        clones = cloneRepository.getClones(activeProfile.id)
        renderStats()
        renderHomeGrid()
    }

    private fun loadInventoryIfNeeded() {
        if (availableApps.isNotEmpty() || inventoryLoading) return
        inventoryLoading = true
        Thread {
            val apps = inventory.launchableApps()
            runOnUiThread {
                availableApps = apps
                inventoryLoading = false
                renderStats()
                renderHomeGrid()
            }
        }.start()
    }

    private fun renderStats() {
        binding.cloneMetric.text = getString(R.string.profile_apps_count, clones.size)
        binding.activeProfileName.text = activeProfile.name
        binding.activeProfileMeta.text = listOf(activeProfile.displaySlot, activeProfile.phone)
            .filter { it.isNotBlank() }
            .joinToString(" | ")
        binding.emptyClonesText.visibility = if (clones.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderHomeGrid() {
        binding.homeGrid.removeAllViews()
        clones.forEach { clone ->
            val app = availableApps.firstOrNull { it.packageName == clone.packageName }
                ?: cloneAppCache[clone.packageName]
                ?: inventory.appEntryForPackage(clone.packageName)?.also {
                    cloneAppCache[clone.packageName] = it
                }
            binding.homeGrid.addView(appTile(clone, app))
        }
        binding.homeGrid.addView(addTile())
    }

    private fun appTile(clone: CloneEntry, app: AppEntry?): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), dp(8))
            layoutParams = GridLayout.LayoutParams().apply {
                width = resources.displayMetrics.widthPixels / 4 - dp(14)
                height = dp(126)
                setMargins(dp(4), dp(6), dp(4), dp(10))
            }
            setOnClickListener { launchVirtualApp(clone.packageName) }
            setOnLongClickListener {
                showCloneDetails(clone, app)
                true
            }
        }

        tile.addView((app?.let { iconFor(it, 58) } ?: fallbackIcon(58)))
        tile.addView(TextView(this).apply {
            text = clone.displayName ?: app?.label ?: clone.packageName
            gravity = Gravity.CENTER
            maxLines = 2
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.graphite_900))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(10), 0, 0) }
        })
        return tile
    }

    private fun addTile(): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), dp(8))
            layoutParams = GridLayout.LayoutParams().apply {
                width = resources.displayMetrics.widthPixels / 4 - dp(14)
                height = dp(126)
                setMargins(dp(4), dp(6), dp(4), dp(10))
            }
            setOnClickListener { openAddAppsPage() }
        }

        tile.addView(TextView(this).apply {
            text = "+"
            gravity = Gravity.CENTER
            textSize = 38f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.parallel_purple))
            background = ContextCompat.getDrawable(context, R.drawable.add_tile_background)
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
        })
        tile.addView(TextView(this).apply {
            text = getString(R.string.add)
            gravity = Gravity.CENTER
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.graphite_900))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(10), 0, 0) }
        })
        return tile
    }

    private fun openAddAppsPage() {
        showMainLoading(true)
        binding.root.post {
            startActivity(Intent(this, AddAppsActivity::class.java))
        }
    }

    private fun launchVirtualApp(packageName: String) {
        val profileId = activeProfile.id
        showMainLoading(true)
        Thread {
            val result = virtualEngine.launch(packageName, profileId)
            if (result.success) {
                cloneRepository.markLaunched(packageName, profileId)
            } else {
                cloneRepository.updateStatus(packageName, profileId, CloneStatus.FAILED, result.message)
            }
            runOnUiThread {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                if (!result.success) {
                    showMainLoading(false)
                    render()
                }
            }
        }.start()
    }

    private fun showProfileSheet() {
        val dialog = BottomSheetDialog(this)
        val content = sheetContainer()
        content.addView(sheetTitle(getString(R.string.switch_profile)))

        profileRepository.getProfiles().forEach { profile ->
            content.addView(profileRow(profile) {
                profileRepository.setActiveProfileId(profile.id)
                dialog.dismiss()
                render()
            })
        }

        content.addView(primaryAction(getString(R.string.add_profile)) {
            dialog.dismiss()
            showProfileEditor(null)
        })

        content.addView(secondaryAction(getString(R.string.edit_profile)) {
            dialog.dismiss()
            showProfileEditor(activeProfile)
        })

        dialog.setContentView(content)
        dialog.show()
    }

    private fun showSettingsSheet() {
        val dialog = BottomSheetDialog(this)
        val content = sheetContainer()
        content.addView(sheetTitle(getString(R.string.settings)))
        content.addView(settingsRow(getString(R.string.profiles), "${profileRepository.getProfiles().size} configured") {
            dialog.dismiss()
            showProfileSheet()
        })

        content.addView(sectionTitle(getString(R.string.appearance)))
        val isDark = UiPreferences.savedNightMode(this) == AppCompatDelegate.MODE_NIGHT_YES
        content.addView(SwitchMaterial(this).apply {
            text = if (isDark) getString(R.string.dark_mode) else getString(R.string.light_mode)
            isChecked = isDark
            setTextColor(color(R.color.graphite_900))
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnCheckedChangeListener { button, checked ->
                button.text = if (checked) getString(R.string.dark_mode) else getString(R.string.light_mode)
                val mode = if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                UiPreferences.saveNightMode(this@MainActivity, mode)
                AppCompatDelegate.setDefaultNightMode(mode)
                recreate()
            }
        })

        content.addView(sectionTitle(getString(R.string.storage)))
        val hostStorageRow = settingsRow(getString(R.string.host_storage), getString(R.string.calculating), null)
        val cloneStorageRow = settingsRow(getString(R.string.clone_storage), getString(R.string.calculating), null)
        val totalStorageRow = settingsRow(getString(R.string.total_storage), getString(R.string.calculating), null)
        content.addView(hostStorageRow)
        content.addView(cloneStorageRow)
        content.addView(totalStorageRow)
        Thread {
            val storage = storageMeter.snapshot()
            runOnUiThread {
                setSettingsDetail(hostStorageRow, storage.appText())
                setSettingsDetail(cloneStorageRow, storage.cloneText())
                setSettingsDetail(totalStorageRow, storage.totalText())
            }
        }.start()

        content.addView(sectionTitle(getString(R.string.updates)))
        content.addView(settingsRow(getString(R.string.current_version), updateManager.currentVersionText(), null))
        content.addView(settingsRow(getString(R.string.check_updates), getString(R.string.check_updates_detail)) {
            dialog.dismiss()
            checkForUpdates()
        })

        dialog.setContentView(ScrollView(this).apply { addView(content) })
        dialog.show()
    }

    private fun checkForUpdates() {
        Toast.makeText(this, getString(R.string.checking_updates), Toast.LENGTH_SHORT).show()
        Thread {
            val result = updateManager.checkForUpdate()
            runOnUiThread {
                when (result) {
                    is UpdateCheckResult.Available -> showUpdateAvailableDialog(result.update)
                    is UpdateCheckResult.UpToDate -> Toast.makeText(
                        this,
                        getString(R.string.no_update_available),
                        Toast.LENGTH_LONG
                    ).show()
                    is UpdateCheckResult.Error -> Toast.makeText(
                        this,
                        getString(R.string.update_check_failed, result.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun checkForUpdatesOnStartup() {
        if (startupUpdateCheckRunning) return
        startupUpdateCheckRunning = true
        Thread {
            val result = updateManager.checkForUpdate()
            runOnUiThread {
                startupUpdateCheckRunning = false
                if (result is UpdateCheckResult.Available) {
                    showUpdateAvailableDialog(result.update)
                }
            }
        }.start()
    }

    private fun showUpdateAvailableDialog(update: UpdateInfo) {
        val detail = buildString {
            appendLine(getString(R.string.update_available_detail, update.versionName, update.versionCode))
            if (update.notes.isNotBlank()) {
                appendLine()
                append(update.notes)
            }
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_available))
            .setMessage(detail)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                downloadUpdate(update)
            }
            .show()
    }

    private fun downloadUpdate(update: UpdateInfo) {
        showMainLoading(true)
        Toast.makeText(this, getString(R.string.downloading_update), Toast.LENGTH_SHORT).show()
        Thread {
            runCatching { updateManager.downloadApk(update) }
                .onSuccess { apk ->
                    runOnUiThread {
                        showMainLoading(false)
                        updateManager.installApk(apk)
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        showMainLoading(false)
                        Toast.makeText(
                            this,
                            getString(R.string.update_download_failed, error.message ?: "unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }.start()
    }

    private fun showProfileEditor(profile: SkinwalkerProfile?) {
        val nameInput = EditText(this).apply {
            hint = getString(R.string.profile_name)
            setText(profile?.name.orEmpty())
            setSingleLine()
        }
        val phoneInput = EditText(this).apply {
            hint = getString(R.string.profile_phone)
            setText(profile?.phone.orEmpty())
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine()
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), 0)
            addView(nameInput)
            addView(phoneInput)
        }

        AlertDialog.Builder(this)
            .setTitle(if (profile == null) getString(R.string.add_profile) else getString(R.string.edit_profile))
            .setView(form)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val saved = if (profile == null) {
                    profileRepository.createProfile(nameInput.text.toString(), phoneInput.text.toString())
                } else {
                    profile.copy(name = nameInput.text.toString().ifBlank { profile.name }, phone = phoneInput.text.toString())
                        .also { profileRepository.upsertProfile(it) }
                }
                profileRepository.setActiveProfileId(saved.id)
                render()
            }
            .show()
    }

    private fun profileRow(profile: SkinwalkerProfile, onClick: () -> Unit): View {
        val isActive = profile.id == activeProfile.id
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundResource(if (isActive) R.drawable.info_background else R.drawable.profile_lane_background)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
            setOnClickListener { onClick() }

            addView(TextView(context).apply {
                text = profile.name.take(1).uppercase()
                gravity = Gravity.CENTER
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.white))
                background = ContextCompat.getDrawable(context, R.drawable.header_badge_background)
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(dp(12), 0, dp(12), 0) }
                addView(TextView(context).apply {
                    text = profile.name
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(color(R.color.graphite_900))
                })
                addView(TextView(context).apply {
                    text = listOf(profile.displaySlot, profile.phone).filter { it.isNotBlank() }.joinToString(" | ")
                    textSize = 12f
                    setTextColor(color(R.color.graphite_500))
                })
            })
            addView(TextView(context).apply {
                text = if (isActive) getString(R.string.active) else getString(R.string.switch_profile_short)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(if (isActive) R.color.android_green else R.color.parallel_purple))
            })
        }
    }

    private fun settingsRow(title: String, detail: String, onClick: (() -> Unit)?): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundResource(R.drawable.profile_lane_background)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
            isClickable = onClick != null
            if (onClick != null) setOnClickListener { onClick() }
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.graphite_900))
            })
            addView(TextView(context).apply {
                text = detail
                textSize = 12f
                setTextColor(color(R.color.graphite_500))
                maxLines = 2
            })
        }
    }

    private fun setSettingsDetail(row: LinearLayout, detail: String) {
        (row.getChildAt(1) as? TextView)?.text = detail
    }

    private fun sheetContainer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
        }
    }

    private fun sheetTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.graphite_900))
            setPadding(0, 0, 0, dp(14))
        }
    }

    private fun sectionTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.graphite_500))
            setPadding(0, dp(12), 0, dp(8))
        }
    }

    private fun primaryAction(textValue: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = textValue
            isAllCaps = false
            setOnClickListener { onClick() }
        }
    }

    private fun secondaryAction(textValue: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = textValue
            gravity = Gravity.CENTER
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.parallel_purple))
            setPadding(0, dp(12), 0, 0)
            setOnClickListener { onClick() }
        }
    }

    private fun showCloneDetails(clone: CloneEntry, app: AppEntry?) {
        val dialog = BottomSheetDialog(this)
        val lastLaunch = clone.lastLaunchAt?.let {
            DateFormat.getDateTimeInstance().format(Date(it))
        } ?: getString(R.string.never)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(22))
        }
        content.addView(TextView(this).apply {
            text = clone.displayName ?: app?.label ?: clone.packageName
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.graphite_900))
        })
        content.addView(TextView(this).apply {
            text = buildString {
                appendLine(clone.packageName)
                appendLine()
                appendLine(getString(R.string.last_launch, lastLaunch))
                appendLine(getString(R.string.status_line, clone.lastMessage.ifBlank { clone.status.message }))
                appendLine(getString(R.string.virtual_engine_note))
            }
            textSize = 13f
            setPadding(0, dp(10), 0, dp(16))
            setTextColor(ContextCompat.getColor(context, R.color.graphite_600))
        })
        content.addView(Button(this).apply {
            text = getString(R.string.remove)
            isAllCaps = false
            setOnClickListener {
                cloneRepository.removeClone(clone.packageName, activeProfile.id)
                dialog.dismiss()
                render()
            }
        })
        dialog.setContentView(content)
        dialog.show()
    }

    private fun iconFor(app: AppEntry, size: Int): ImageView {
        return ImageView(this).apply {
            setImageDrawable(app.icon)
            background = ContextCompat.getDrawable(context, R.drawable.app_icon_background)
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        }
    }

    private fun fallbackIcon(size: Int): ImageView {
        return ImageView(this).apply {
            setImageResource(android.R.drawable.sym_def_app_icon)
            background = ContextCompat.getDrawable(context, R.drawable.app_icon_background)
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        }
    }

    private fun showMainLoading(show: Boolean) {
        binding.mainLoadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun color(colorId: Int): Int = ContextCompat.getColor(this, colorId)

    companion object {
        init {
            System.loadLibrary("skinwalker")
        }
    }
}

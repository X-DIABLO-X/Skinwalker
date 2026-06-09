package com.example.skinwalker

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.skinwalker.databinding.ActivityAddAppsBinding

class AddAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAppsBinding
    private lateinit var inventory: AppInventory
    private lateinit var cloneRepository: CloneRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var virtualEngine: VirtualEngine
    private lateinit var activeProfile: SkinwalkerProfile
    private var availableApps: List<AppEntry> = emptyList()
    private var clones: List<CloneEntry> = emptyList()
    private var isAdding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(UiPreferences.savedNightMode(this))
        super.onCreate(savedInstanceState)
        binding = ActivityAddAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inventory = AppInventory(this)
        cloneRepository = CloneRepository(this)
        profileRepository = ProfileRepository(this)
        virtualEngine = VirtualEngineProvider.get(this)
        activeProfile = profileRepository.activeProfile()
        clones = cloneRepository.getClones(activeProfile.id)

        binding.backButton.setOnClickListener { finish() }
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderAppList()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        loadApps()
    }

    private fun loadApps() {
        showLoading(true)
        Thread {
            val apps = inventory.launchableApps()
            runOnUiThread {
                availableApps = apps
                showLoading(false)
                renderAppList()
            }
        }.start()
    }

    private fun renderAppList() {
        if (!::binding.isInitialized || isAdding) return
        val query = binding.searchInput.text?.toString().orEmpty()
        val clonePackages = clones.map { it.packageName }.toSet()
        val filteredApps = availableApps
            .filterNot { it.packageName in clonePackages }
            .filter { AppFilters.matchesQuery(it.label, it.packageName, query) }

        binding.appList.removeAllViews()
        filteredApps.forEach { app ->
            binding.appList.addView(appRow(app))
        }
        binding.emptyText.visibility = if (!binding.loadingSpinner.isShown && filteredApps.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun appRow(app: AppEntry): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundResource(R.drawable.profile_lane_background)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
            setOnClickListener { addApp(app) }

            addView(iconFor(app, 46))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(dp(12), 0, dp(12), 0) }
                addView(TextView(context).apply {
                    text = app.label
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(color(R.color.graphite_900))
                    maxLines = 1
                })
                addView(TextView(context).apply {
                    text = app.packageName
                    textSize = 12f
                    setTextColor(color(R.color.graphite_500))
                    maxLines = 1
                })
            })
            addView(TextView(context).apply {
                text = getString(R.string.add)
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.parallel_purple))
            })
        }
    }

    private fun addApp(app: AppEntry) {
        if (isAdding) return
        isAdding = true
        showAdding(true)
        Thread {
            val installResult = virtualEngine.installInstalledPackage(app.packageName, activeProfile.id)
            val status = if (installResult.success) CloneStatus.INSTALLED else CloneStatus.ENGINE_MISSING
            cloneRepository.upsertClone(
                CloneEntry(
                    packageName = app.packageName,
                    displayName = app.label,
                    createdAt = System.currentTimeMillis(),
                    status = status,
                    lastMessage = installResult.message,
                    profileId = activeProfile.id
                )
            )
            runOnUiThread {
                isAdding = false
                showAdding(false)
                clones = cloneRepository.getClones(activeProfile.id)
                Toast.makeText(this, installResult.message, Toast.LENGTH_LONG).show()
                renderAppList()
            }
        }.start()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
        binding.appScroll.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.emptyText.visibility = View.GONE
    }

    private fun showAdding(show: Boolean) {
        binding.addingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun iconFor(app: AppEntry, size: Int): ImageView {
        return ImageView(this).apply {
            setImageDrawable(app.icon)
            background = ContextCompat.getDrawable(context, R.drawable.app_icon_background)
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun color(colorId: Int): Int = ContextCompat.getColor(this, colorId)
}

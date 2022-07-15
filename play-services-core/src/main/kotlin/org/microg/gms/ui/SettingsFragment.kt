/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.mgoogle.android.gms.R
import org.microg.gms.checkin.CheckinPrefs
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        val pm = requireActivity().packageManager

        findPreference<Preference>(PREF_CHECKIN)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openCheckinSettings)
            true
        }
        findPreference<Preference>(PREF_GCM)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openGcmSettings)
            true
        }
        findPreference<Preference>(PREF_ABOUT)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAbout)
            true
        }
        findPreference<Preference>(PREF_ABOUT)!!.summary = getString(R.string.about_version_str, AboutFragment.getSelfVersion(context))
        findPreference<SwitchPreferenceCompat>(PREF_CAST_HIDE_LAUNCHER_ICON)?.apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                setOnPreferenceChangeListener { _, newValue ->
                    pm.setComponentEnabledSetting(
                        ComponentName.createRelative(requireActivity(), "org.microg.gms.ui.SettingsActivityLauncher"),
                        when (newValue) {
                            true -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            else -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        },
                        PackageManager.DONT_KILL_APP
                    )
                    true
                }
            } else {
                preferenceScreen.removePreference(this)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        lifecycleScope.launchWhenResumed {
            updateDetails(context)
        }
    }

    private suspend fun updateDetails(context: Context) {
        val gcmServiceInfo = getGcmServiceInfo(context)
        if (gcmServiceInfo.configuration.enabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            database.close()
            findPreference<Preference>(PREF_GCM)!!.summary = context.getString(R.string.service_status_enabled_short) + " - " + context.resources.getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount)
        } else {
            findPreference<Preference>(PREF_GCM)!!.setSummary(R.string.service_status_disabled_short)
        }

        findPreference<Preference>(PREF_CHECKIN)!!.setSummary(if (CheckinPrefs.isEnabled(context)) R.string.service_status_enabled_short else R.string.service_status_disabled_short)
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_CAST_HIDE_LAUNCHER_ICON = "pref_hide_launcher_icon"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}

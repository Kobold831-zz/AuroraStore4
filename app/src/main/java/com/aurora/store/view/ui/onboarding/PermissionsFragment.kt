/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.databinding.FragmentOnboardingPermissionsBinding
import com.aurora.store.view.epoxy.views.preference.PermissionViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions


class PermissionsFragment : BaseFragment() {

    private lateinit var B: FragmentOnboardingPermissionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentOnboardingPermissionsBinding.bind(
            inflater.inflate(
                R.layout.fragment_onboarding_permissions,
                container,
                false
            )
        )

        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateController()
    }

    private fun updateController() {

        val installerList: List<Permission> = listOf(
            Permission(
                0,
                getString(R.string.onboarding_permission_esa),
                getString(R.string.onboarding_permission_esa_desc)
            )
        )

        B.epoxyRecycler.withModels {
            val writeExternalStorage = ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val storageManager = if (isRAndAbove()) Environment.isExternalStorageManager() else true
            canGoForward = writeExternalStorage
            if (canGoForwardInitial == null) {
                canGoForwardInitial = canGoForward
            }
            if (canGoForward && canGoForwardInitial == false) {
                if (activity is OnboardingActivity) {
                    (activity!! as OnboardingActivity).refreshButtonState()
                }
            }
            setFilterDuplicates(true)
            installerList.forEach {
                add(
                    PermissionViewModel_()
                        .id(it.id)
                        .permission(it)
                        .isGranted(
                            when (it.id) {
                                0 -> writeExternalStorage
                                else -> false
                            }
                        )
                        .click { _ ->
                            when (it.id) {
                                0 -> checkStorageAccessPermission()
                            }
                        }
                )
            }
        }
    }

    private fun checkStorageAccessPermission() = runWithPermissions(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) {
        toast(R.string.toast_permission_granted)
        B.epoxyRecycler.requestModelBuild()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            99, 999 -> {
                toast(R.string.toast_permission_granted)
                B.epoxyRecycler.requestModelBuild()
            }
        }
    }

    private var canGoForward = false
    private var canGoForwardInitial: Boolean? = null

    fun canGoForward(): Boolean {
        return canGoForward
    }
}
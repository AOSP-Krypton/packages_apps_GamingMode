/*
 * Copyright (C) 2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exthmui.game.fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import org.exthmui.game.R

/**
 * [Fragment] that hosts a [RecyclerView] with a vertical
 * list of application info. Items display an icon, name
 * and package name of the application, along with a [CheckBox]
 * indicating whether the item is selected or not.
 */
abstract class AppListFragment: Fragment(R.layout.app_list_layout) {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageManager: PackageManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var packageList: List<PackageInfo>

    private var searchText = ""
    private var category: Int = CATEGORY_USER_ONLY
    private var customFilter: ((PackageInfo) -> Boolean)? = null
    private var comparator: ((PackageInfo, PackageInfo) -> Int)? = null

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        setHasOptionsMenu(true)
        requireActivity().setTitle(getTitle())
        packageManager = requireContext().packageManager
        packageList = packageManager.getInstalledPackages(0)
    }

    /**
     * Override this function to set the title res id of this fragment.
     */
    protected abstract fun getTitle(): Int

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AppListAdapter()
        recyclerView = view.findViewById<RecyclerView>(R.id.apps_list).also {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
        refreshList()
    }

    /**
     * @return an initial list of packages that should appear as selected.
     */
    private fun getInitialCheckedList(): List<String> {
        val flattenedString = if (getPreferenceDataStore() != null) getPreferenceDataStore()!!.getString(getKey(), null)
            else sharedPreferences.getString(getKey(), null)
        return flattenedString?.takeIf {
            it.isNotBlank()
        }?.split(";")?.toList() ?: emptyList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_list_menu, menu)
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_apps)
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(newText: String): Boolean {
                searchText = newText
                refreshList()
                return true
            }
        })
    }

    /**
     * Called when user selects an item.
     *
     * @param list a [List<String>] of selected items.
     */
    private fun onListUpdate(list: List<String>) {
        val flattenedString = list.fold("") { r, t ->
            "$r$t;"
        }
        getPreferenceDataStore()?.putString(getKey(), flattenedString) ?: run {
            sharedPreferences.edit().putString(getKey(), flattenedString).commit()
        }
    }

    protected open fun getPreferenceDataStore(): PreferenceDataStore? = null

    protected abstract fun getKey(): String

    private fun refreshList() {
        var list = packageList.filter {
            when(category) {
                CATEGORY_SYSTEM_ONLY -> it.applicationInfo.isSystemApp()
                CATEGORY_USER_ONLY -> !it.applicationInfo.isSystemApp()
                else -> true
            }
        }.filter {
            getLabel(it).contains(searchText, true)
        }
        list = customFilter?.let { customFilter ->
            list.filter {
                customFilter(it)
            }
        } ?: list
        list = comparator?.let {
            list.sortedWith(it)
        } ?: list.sortedWith { a, b ->
            getLabel(a).compareTo(getLabel(b))
        }
        if (::adapter.isInitialized) adapter.submitList(list.map { appInfoFromPackageInfo(it) })
    }

    private fun appInfoFromPackageInfo(packageInfo: PackageInfo) =
        AppInfo(
            packageInfo.packageName,
            getLabel(packageInfo),
            packageInfo.applicationInfo.loadIcon(packageManager),
        )
    
    private fun getLabel(packageInfo: PackageInfo) =
        packageInfo.applicationInfo.loadLabel(packageManager).toString()

    private inner class AppListAdapter: ListAdapter<AppInfo, AppListViewHolder>(itemCallback) {
        private val selectedIndices = mutableSetOf<Int>()
        private val initialList = getInitialCheckedList().toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AppListViewHolder(layoutInflater.inflate(
                R.layout.app_list_item, parent, false))

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            getItem(position).let {
                holder.label.text = it.label
                holder.packageName.text = it.packageName
                holder.icon.setImageDrawable(it.icon)
                holder.itemView.setOnClickListener {
                    if (selectedIndices.contains(position)){
                        selectedIndices.remove(position)
                    } else {
                        selectedIndices.add(position)
                    }
                    notifyItemChanged(position)
                    onListUpdate(getSelectedPackages())
                }
                if (initialList.contains(it.packageName)) {
                    initialList.remove(it.packageName)
                    selectedIndices.add(position)
                }
                holder.checkBox.isChecked = selectedIndices.contains(position)
            }
        }

        override fun submitList(list: List<AppInfo>?) {
            selectedIndices.clear()
            super.submitList(list)
        }

        private fun getSelectedPackages(): List<String> =
            selectedIndices.map {
                getItem(it)
            }.map {
                it.packageName
            }.toList()
    }

    private class AppListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val label: TextView = itemView.findViewById(R.id.label)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    private data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable,
    )

    companion object {
        const val CATEGORY_SYSTEM_ONLY = 0
        const val CATEGORY_USER_ONLY = 1

        private val itemCallback = object: DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo.packageName == newInfo.packageName
            
            override fun areContentsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo == newInfo
        }
    }
}
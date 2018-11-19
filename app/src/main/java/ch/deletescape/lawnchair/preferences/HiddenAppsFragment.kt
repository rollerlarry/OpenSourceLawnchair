package ch.deletescape.lawnchair.preferences

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import ch.deletescape.lawnchair.LauncherAppState
import ch.deletescape.lawnchair.MultiSelectRecyclerViewAdapter
import ch.deletescape.lawnchair.R
import ch.deletescape.lawnchair.compat.LauncherActivityInfoCompat
import ch.deletescape.lawnchair.compat.LauncherAppsCompat
import ch.deletescape.lawnchair.config.FeatureFlags

class HiddenAppsFragment : Fragment(), MultiSelectRecyclerViewAdapter.ItemClickListener {

    private lateinit var installedApps: List<LauncherActivityInfoCompat>
    private lateinit var adapter: MultiSelectRecyclerViewAdapter
    var passWordTrue = FeatureFlags.PASSWORD_HIDDEN_APP;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_selectable_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = view.context
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        installedApps = getAppsList(context).apply { sortBy { it.label.toString() } }
        adapter = MultiSelectRecyclerViewAdapter(installedApps, this)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val hiddenApps = PreferenceProvider.getPreferences(context).hiddenAppsSet
        if (!hiddenApps.isEmpty()) {
            activity!!.title = hiddenApps.size.toString() + getString(R.string.hidden_app_selected)
        } else {
            activity!!.title = getString(R.string.hidden_app)
        }
    }

    override fun onItemClicked(position: Int) {
        val title = adapter.toggleSelection(position, installedApps[position].componentName.flattenToString())
        activity!!.title = title
    }

    private fun getAppsList(context: Context?) =
            LauncherAppsCompat.getInstance(context).getActivityList(null, Process.myUserHandle())

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        return inflater.inflate(R.menu.menu_hide_apps, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_apply -> {
                showCreateCategoryDialogHiddenApp()
                true
            }
            R.id.action_reset -> {
                activity!!.title = adapter.clearSelection()
                true
            }
            R.id.action_change_password -> {
                if (passWordTrue.equals("")){
                    Toast.makeText(context, "Password not installed", Toast.LENGTH_SHORT).show()
                } else{
                    showDialogChangePasswordHiddenApp()
                }
                true
            }
            R.id.action_delete_password -> {
                if (passWordTrue.equals("")){
                    Toast.makeText(context, "Password not installed", Toast.LENGTH_SHORT).show()
                } else{
                    showDialogDeletePasswordHiddenApp()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showCreateCategoryDialogHiddenApp() {
        val context = getContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Device security")

        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.dialog_password_hidden_app, null)

        val edtPassword = view.findViewById(R.id.edtPassword) as EditText
        val edtRePassword = view.findViewById(R.id.edtRePassword) as EditText
        val tvTitle = view.findViewById(R.id.tvTitle) as TextView

        if (passWordTrue.equals("")){
            edtRePassword.visibility = View.VISIBLE
            tvTitle.visibility = View.VISIBLE
        }

        builder.setView(view);

        // set up the ok button
        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            val passWord = edtPassword.text.toString()
            val rePassWord = edtRePassword.text.toString()
            var isValid = true

            if (passWordTrue.equals("")){
                if (passWord.isBlank() || rePassWord.isBlank()){
                    Toast.makeText(context, "Password and repassword not empty", Toast.LENGTH_SHORT).show()
                    isValid = false
                } else{
                    if (passWord.equals(rePassWord)){
                        FeatureFlags.PASSWORD_HIDDEN_APP = passWord
                        isValid = true
                    } else{
                        Toast.makeText(context, "Two password not same", Toast.LENGTH_SHORT).show()
                        isValid = false
                    }
                }
            } else{
                if (passWord.isBlank()){
                    Toast.makeText(context, "Password not empty", Toast.LENGTH_SHORT).show()
                    isValid = false
                } else {
                    if (passWord.equals(passWordTrue)){
                        isValid = true
                    } else {
                        Toast.makeText(context, "Wrong password", Toast.LENGTH_SHORT).show()
                        isValid = false
                    }
                }
            }

            if (isValid) {
                adapter.addSelectionsToList(activity)
                LauncherAppState.getInstanceNoCreate().reloadAllApps()
                activity!!.onBackPressed()
                Toast.makeText(context, "Successful", Toast.LENGTH_SHORT).show()
            }

            if (isValid) {
                dialog.dismiss()
            }
        }

        builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
            dialog.cancel()
        }

        builder.show()
    }

    fun showDialogChangePasswordHiddenApp() {
        val context = getContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Device security")

        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.dialog_change_password_hidden_app, null)

        val edtOldPassword = view.findViewById(R.id.edtOldPassword) as EditText
        val edtPassword = view.findViewById(R.id.edtPassword) as EditText
        val edtRePassword = view.findViewById(R.id.edtRePassword) as EditText

        builder.setView(view);

        // set up the ok button
        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            val passWord = edtPassword.text.toString()
            val rePassWord = edtRePassword.text.toString()
            val oldPassword = edtOldPassword.text.toString()
            var isValid = true

            if (oldPassword.equals(passWordTrue)){
                if (passWord.isBlank() || rePassWord.isBlank()){
                    Toast.makeText(context, "Password and repassword not empty", Toast.LENGTH_SHORT).show()
                } else{
                    if (passWord.equals(rePassWord)){
                        FeatureFlags.PASSWORD_HIDDEN_APP = passWord
                        Toast.makeText(context, "Change password successful", Toast.LENGTH_SHORT).show()
                        activity!!.onBackPressed()
                    } else{
                        Toast.makeText(context, "Two password not same", Toast.LENGTH_SHORT).show()
                        isValid = false
                    }
                }
            } else {
                Toast.makeText(context, "Old password not true", Toast.LENGTH_SHORT).show()
            }

        }

        builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
            dialog.cancel()
        }

        builder.show()
    }

    fun showDialogDeletePasswordHiddenApp() {
        val context = getContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete password hidden app")

        val view = layoutInflater.inflate(R.layout.dialog_password_hidden_app, null)

        val edtPassword = view.findViewById(R.id.edtPassword) as EditText

        builder.setView(view);

        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            val passWord = edtPassword.text.toString()
            if (passWord.isBlank()){
                Toast.makeText(context, "Password not empty", Toast.LENGTH_SHORT).show()
            } else{
                if (passWord.equals(passWordTrue)){
                    FeatureFlags.PASSWORD_HIDDEN_APP = ""
                    Toast.makeText(context, "Delete password successful", Toast.LENGTH_SHORT).show()
                    activity!!.onBackPressed()
                } else{
                    Toast.makeText(context, "Old password not true", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
            dialog.cancel()
        }

        builder.show()
    }
}
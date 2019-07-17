package com.example.firebasechat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager

import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.firebasechat.ActivityRequestCode.*
import kotlinx.android.synthetic.main.activity_main.*

import java.util.Arrays
import java.util.HashMap

const val CHILD_MESSAGES = "messages"
const val CHILD_PHOTOS = "chat_photos"
const val DEFAULT_MSG_LENGTH_LIMIT = 1000
const val FRIENDLY_MSG_LENGTH_LIMIT = "friendly_message_length"
const val TAG = "MainActivity"
const val ANONYMOUS = "anonymous"
const val ERROR_CANNOT_UPLOAD_IMAGE = "Image upload task was not successful."


class MainActivity : AppCompatActivity() {

    private var username: String? = null

    private lateinit var fireBaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var childEventListener: ChildEventListener
    private lateinit var fireBaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var fireBaseStorage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var fireBaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var adapter: MessageAdapter
    private var listMess = arrayListOf<FriendlyMessage>()
    private var cacheExpiration = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        username = ANONYMOUS

        initInstances()
        initListeners()

        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        adapter = MessageAdapter()
        recyclerView.adapter = adapter

        progressBar.visibility = ProgressBar.INVISIBLE
        messageEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT))

        val fireBaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()

        fireBaseRemoteConfig.setConfigSettingsAsync(fireBaseRemoteConfigSettings)

        val defaults = HashMap<String, Any>()
        defaults[FRIENDLY_MSG_LENGTH_LIMIT] = DEFAULT_MSG_LENGTH_LIMIT
        fireBaseRemoteConfig.setDefaults(defaults)

        fetchConfig()
        val editTextViewLengthLimit = fireBaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_LIMIT)
        messageEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(editTextViewLengthLimit.toInt()))
    }

    private fun initListeners() {
        photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.complete_action)), RC_PHOTO_PICKER.state)
        }

        sendButton.setOnClickListener {
            val friendlyMessage = FriendlyMessage(messageEditText.text.toString(), username, null)
            databaseReference.push().setValue(friendlyMessage)
            messageEditText.setText("")
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                sendButton.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        authStateListener = FirebaseAuth.AuthStateListener { fireBaseAuth ->
            if (fireBaseAuth.currentUser != null) {
                onSignedInInitialize(fireBaseAuth.currentUser!!.displayName)

            } else {
                onSignedOutInitialize()
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
                                .setAvailableProviders(Arrays.asList<AuthUI.IdpConfig>(
                                        AuthUI.IdpConfig.GoogleBuilder().build(),
                                        AuthUI.IdpConfig.EmailBuilder().build()))
                                .build(),
                        RC_SIGN_IN.state)
            }
        }

        childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val friendlyMessage = dataSnapshot.getValue<FriendlyMessage>(FriendlyMessage::class.java)
                if (friendlyMessage != null) {
                    listMess.add(friendlyMessage)
                }
                adapter.setItems(listMess)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}

            override fun onCancelled(databaseError: DatabaseError) {}
        }
    }

    private fun initInstances() {
        fireBaseDatabase = FirebaseDatabase.getInstance()
        fireBaseAuth = FirebaseAuth.getInstance()
        fireBaseStorage = FirebaseStorage.getInstance()
        fireBaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        databaseReference = FirebaseDatabase.getInstance().reference.child(CHILD_MESSAGES)
        storageReference = FirebaseStorage.getInstance().reference.child(CHILD_PHOTOS)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sign_out_menu) {
            AuthUI.getInstance().signOut(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onResume() {
        fireBaseAuth.addAuthStateListener(authStateListener)
        detachDatabaseReadListener()
        messageEditText.text.clear()
        super.onResume()
    }

    override fun onPause() {
        fireBaseAuth.removeAuthStateListener(authStateListener)
        super.onPause()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN.state -> {
                if (resultCode == RESULT_OK) {
                    makeToast(getString(R.string.signed_in))
                } else if (resultCode == RESULT_CANCELED) {
                    makeToast(getString(R.string.sign_cancelled))
                    finish()
                }
            }
            RC_PHOTO_PICKER.state -> if (resultCode == RESULT_OK) {
                data?.let {
                    val photoRef = storageReference.child(it.data!!.lastPathSegment!!)
                    putImageInStorage(photoRef, it.data, it.data!!.lastPathSegment)
                }
            }
        }
    }

    private fun makeToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri?, key: String?) {
        uri?.let {
            storageReference.putFile(it)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            throw task.exception!!
                        }
                        storageReference.downloadUrl
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUrl = task.result.toString()
                            databaseReference.child(key!!).setValue(FriendlyMessage(null, username, downloadUrl))
                        } else {
                            Log.w(TAG, ERROR_CANNOT_UPLOAD_IMAGE, task.exception)
                        }
                    }
        }
    }


    private fun onSignedInInitialize(username: String?) {
        this.username = username
        attachDatabaseReadListener()
    }

    private fun onSignedOutInitialize() {
        username = ANONYMOUS
        detachDatabaseReadListener()
    }

    private fun attachDatabaseReadListener() {
        databaseReference.addChildEventListener(childEventListener)
    }

    private fun detachDatabaseReadListener() {
        databaseReference.removeEventListener(childEventListener)
    }

    private fun fetchConfig() {
        if (fireBaseRemoteConfig.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }

        fireBaseRemoteConfig.fetch(cacheExpiration)
                .addOnFailureListener { e ->
                    Log.w(TAG, e)
                    applyRetrievedLengthLimit()
                }
                .addOnSuccessListener {
                    fireBaseRemoteConfig.activate()
                    applyRetrievedLengthLimit()
                }
    }

    private fun applyRetrievedLengthLimit() {
        val editTextViewLengthLimit = fireBaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_LIMIT)
        messageEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(editTextViewLengthLimit.toInt()))
    }
}

package com.dung.baitapnodejs

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import com.androidadvance.topsnackbar.TSnackbar
import com.dung.baitapnodejs.adapter.UserAdapter
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.app_bar_main2.*
import kotlinx.android.synthetic.main.content_main2.*
import kotlinx.android.synthetic.main.dialog_user_register.view.*
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException
import android.widget.TextView
import com.dung.baitapnodejs.adapter.UserChatAdapter
import com.dung.baitapnodejs.model.Chat

class Main2Activity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var localHost = "https://ddonlinechat.herokuapp.com/"
    lateinit var mSocket: Socket

    // register
    var appRegisterSend = "app-register-send"
    var serverRegisterSend = "server-register-send"

    //Inform
    var serverInform = "server-inform"

    //Send user list
    var serverUserListSend = "server-send-user-list"

    //Chat
    var appMessageSend = "app-message-send"
    var serverMessageSend = "server_message-send"

    // Typing
    var appTypingSend = "app-typing-send"
    var serverTypingSend = "server-typing-send"

    lateinit var userList: ArrayList<String>
    lateinit var userAdapter: UserAdapter

    lateinit var chatList: ArrayList<Chat>
    lateinit var chatAdapter: UserChatAdapter

    var rUsername = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main2)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        initView()
        createRegisterDialog()
        getAllUser()
        userDisconnect()
        sendMessage()
        getMessageFromServer()
        userOnTyping()
        getTypingStatus()

    }

    private fun getTypingStatus() {
        mSocket.on(serverTypingSend, object : Emitter.Listener {
            override fun call(vararg args: Any?) {
                runOnUiThread(object : Runnable {
                    override fun run() {
                        var jsonObject = args[0] as JSONObject
                        try {
                            var name = jsonObject.getString("ten")
                            var nhan = jsonObject.getBoolean("nhan")
                            if (nhan) {
                                if (!name.equals(rUsername)) {
                                    chatList.add(Chat(name, "is typing..."))
                                }
                            } else {
                                if (!name.equals(rUsername)) {
                                    chatList.removeAt(chatList.size - 1)
                                }
                            }
                            chatAdapter.notifyDataSetChanged()
                        } catch (e: JSONException) {
                            Log.d("Loi JSON:", e.toString())
                        }
                    }
                })
            }
        })
    }

    private fun userOnTyping() {
        edtChat.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s!!.length == 1 && start == 0) {
                    mSocket.emit(appTypingSend, true)
                } else if (s.length == 0) {
                    mSocket.emit(appTypingSend, false)
                }
            }

        })
    }

    private fun getMessageFromServer() {
        mSocket.on(serverMessageSend, object : Emitter.Listener {
            override fun call(vararg args: Any?) {
                runOnUiThread(object : Runnable {
                    override fun run() {
                        var jsonObject = args[0] as JSONObject
                        try {
                            var username = jsonObject.getString("ten")
                            var message = jsonObject.getString("tinnhan")
                            chatList.add(Chat(username, message))
                        } catch (e: JSONException) {
                        }
                        chatAdapter.notifyDataSetChanged()
                    }
                })
            }
        })
    }

    private fun sendMessage() {
        btnChat.setOnClickListener {
            sendMessageToServer()
        }
    }

    private fun sendMessageToServer() {
        var message = edtChat.text.toString()
        if (message.isEmpty()) {
            showMessage("Please fill out message box before sending", clMain)
        } else {
            edtChat.setText("")
            mSocket.emit(appMessageSend, message)
        }
    }

    private fun userDisconnect() {
        mSocket.on(serverInform, object : Emitter.Listener {

            override fun call(vararg args: Any?) {
                runOnUiThread(object : Runnable {
                    override fun run() {
                        var jsonObject = args[0] as JSONObject
                        try {
                            var nameInform = jsonObject.getString("ten")
                            announceDisconnect("$nameInform has logged out", Snackbar.LENGTH_LONG)
                        } catch (e: JSONException) {
                        }
                    }
                })
            }

        })
    }

    fun announceDisconnect(message: String, mode: Int) {
        var snack = TSnackbar.make(clMain, message, mode)
        val snackbarView = snack.view
        val textView =
            snackbarView.findViewById(com.androidadvance.topsnackbar.R.id.snackbar_text) as TextView
        textView.setTextColor(Color.WHITE)
        snack.show()
    }

    private fun getAllUser() {
        mSocket.on(serverUserListSend, object : Emitter.Listener {
            override fun call(vararg args: Any?) {

                runOnUiThread(object : Runnable {
                    override fun run() {
                        var jsonObject = args[0] as JSONObject
                        try {
                            var array = jsonObject.getJSONArray("array")
                            userList.clear()
                            for (i in 0..array.length()) {
                                var name = array.getString(i)
                                userList.add(name)
                            }
                        } catch (e: JSONException) {
                            Log.e("Loi JSON ", e.toString())
                        }
                        userAdapter.notifyDataSetChanged()
                    }

                })
            }

        })
    }

    private fun createRegisterDialog() {
        var alertDialog = AlertDialog.Builder(this)

        alertDialog.setIcon(R.drawable.registration_icon)
        alertDialog.setTitle("Registration")

        alertDialog.setCancelable(false)
        var view = layoutInflater.inflate(R.layout.dialog_user_register, null)

        var edtRegister = view.edtRegister
        alertDialog.setView(view)
        alertDialog.setPositiveButton("OK", null)

        var dialog = alertDialog.create()

        dialog.setOnShowListener {
            var pButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            pButton.setOnClickListener {

                if (edtRegister.text.toString().isEmpty()) {
                    showMessage("Please fill out the name box", view)
                } else {
                    mSocket.emit(appRegisterSend, edtRegister.text.toString())
                    mSocket.on(serverRegisterSend, object : Emitter.Listener {

                        override fun call(vararg args: Any?) {
                            runOnUiThread(object : Runnable {

                                override fun run() {
                                    var jsonObject = args[0] as JSONObject
                                    try {
                                        var exist = jsonObject.getBoolean("ketqua")
                                        if (exist) {
                                            showMessage(
                                                "Username has been coincided with other users",
                                                view
                                            )
                                        } else {
                                            showMessage("Sign up Succesfully!", clMain)
                                            rUsername = edtRegister.text.toString()
                                            dialog.dismiss()
                                            chatList = ArrayList()
                                            chatAdapter = UserChatAdapter(
                                                this@Main2Activity,
                                                chatList,
                                                rUsername
                                            )
                                            var linearLayoutManager1 =
                                                LinearLayoutManager(this@Main2Activity)
                                            rvChat.layoutManager = linearLayoutManager1
                                            rvChat.adapter = chatAdapter
                                        }
                                    } catch (e: JSONException) {
                                        showMessage("Loi JSON $e", view)
                                    }
                                }

                            })
                        }

                    })
                }
            }
        }

        dialog.show()

    }

    fun showMessage(message: String, view: View) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    fun initView() {

        try {
            mSocket = IO.socket(localHost)
        } catch (e: URISyntaxException) {
            showMessage("Loi Link $e", clMain)
        }

        mSocket.connect()

        userList = ArrayList()
        userAdapter = UserAdapter(this, userList)

        var linearLayoutManager = LinearLayoutManager(this@Main2Activity)
        rvUserList.layoutManager = linearLayoutManager
        rvUserList.adapter = userAdapter

        chatList = ArrayList()
        chatAdapter = UserChatAdapter(this@Main2Activity, chatList, rUsername)
        var linearLayoutManager1 = LinearLayoutManager(this@Main2Activity)
        rvChat.layoutManager = linearLayoutManager1
        rvChat.adapter = chatAdapter

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return true
    }
}

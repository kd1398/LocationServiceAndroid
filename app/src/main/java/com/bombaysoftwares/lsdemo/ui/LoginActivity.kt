package com.bombaysoftwares.lsdemo.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bombaysoftwares.lsdemo.R
import com.bombaysoftwares.lsdemo.customs.Constant
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnLogin.setOnClickListener {
            if (isValid()) {
                startMainActivity()
            } else {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(Constant.KEY_USER, etName.text.toString())
        startActivity(intent)
        finish()
    }

    fun isValid(): Boolean {
        var isValid = true
        if (etName.text.isEmpty()) {
            etName.error = "Email is required"
            isValid = false
        }
        return isValid
    }
}
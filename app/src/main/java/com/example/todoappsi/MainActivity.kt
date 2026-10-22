package com.example.todoappsi

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val urlUsuario = "http://52.91.244.133:5000/usuario"
    private val urlLogin = "http://52.91.244.133:5000/login"

    private lateinit var tvEstado: TextView
    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvRut: TextView
    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var layoutLogin: LinearLayout
    private lateinit var layoutDatos: LinearLayout

    private var loginActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvEstado = findViewById(R.id.tvEstado)
        tvNombre = findViewById(R.id.tvNombre)
        tvCorreo = findViewById(R.id.tvCorreo)
        tvRut = findViewById(R.id.tvRut)
        etLogin = findViewById(R.id.etLogin)
        etPassword = findViewById(R.id.etPassword)
        layoutLogin = findViewById(R.id.layoutLogin)
        layoutDatos = findViewById(R.id.layoutDatos)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            iniciarSesion()
        }

        findViewById<Button>(R.id.btnObtenerUsuario).setOnClickListener {
            obtenerUsuario()
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        loginActual = ""

        etLogin.text.clear()
        etPassword.text.clear()

        tvNombre.text = "Nombre: -"
        tvCorreo.text = "Correo: -"
        tvRut.text = "RUT: -"
        tvEstado.text = "Sesión cerrada"

        layoutDatos.visibility = View.GONE
        layoutLogin.visibility = View.VISIBLE
    }

    private fun obtenerUsuario() {
        tvEstado.text = "Cargando..."

        val request = Request.Builder()
            .url("$urlUsuario?login=$loginActual")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tvEstado.text = "Error de conexión: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    runOnUiThread {
                        tvEstado.text = "Error del servidor: ${response.code}"
                    }
                    return
                }

                try {
                    val json = JSONObject(cuerpo)
                    val nombre = json.getString("nombre")
                    val correo = json.getString("correo")
                    val rut = json.getString("rut")

                    runOnUiThread {
                        tvEstado.text = "Datos cargados"
                        tvNombre.text = "Nombre: $nombre"
                        tvCorreo.text = "Correo: $correo"
                        tvRut.text = "RUT: $rut"
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvEstado.text = "Error al leer la respuesta: ${e.message}"
                    }
                }
            }
        })
    }

    private fun iniciarSesion() {
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()

        if (login.isEmpty() || password.isEmpty()) {
            tvEstado.text = "Ingresa usuario y contraseña"
            return
        }

        tvEstado.text = "Verificando..."

        val json = JSONObject()
        json.put("login", login)
        json.put("password", password)

        val cuerpo = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(urlLogin)
            .post(cuerpo)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tvEstado.text = "Error de conexión: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val texto = response.body?.string() ?: ""
                try {
                    val respuesta = JSONObject(texto)
                    val valido = respuesta.getBoolean("valido")
                    val mensaje = respuesta.getString("mensaje")

                    runOnUiThread {
                        if (valido) {
                            loginActual = login
                            tvEstado.text = "✔ $mensaje"
                            layoutLogin.visibility = View.GONE
                            layoutDatos.visibility = View.VISIBLE
                        } else {
                            loginActual = ""
                            tvEstado.text = "✘ $mensaje"
                            layoutDatos.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvEstado.text = "Error al leer la respuesta: ${e.message}"
                    }
                }
            }
        })
    }
}
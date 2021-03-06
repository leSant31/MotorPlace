package com.example.motorplace.activitys

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.motorplace.R
import com.example.motorplace.model.Servico
import com.example.motorplace.model.Usuario
import com.example.motorplace.util.MoneyTextWatcher
import com.example.motorplace.util.Permissao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_cadastro_servico.*
import java.io.ByteArrayOutputStream
import java.util.*

class CadastroServicoActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var imagemPerfil: ImageView
    private val permisssaoCamera= arrayOf(Manifest.permission.CAMERA) //array com as permições que o app precisará (camera)
    private val permisssaoGaleria = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) //array com as permições que o app precisará (Galeria)
    private val SELECAO_CAMERA = 100
    private val SELECAO_GALERIA = 200
    private var imagem: Bitmap? = null
    private lateinit var storageReference : StorageReference
    private var pgtCategoria: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_servico)
        supportActionBar!!.title ="Cadastrar Serviço"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        //criacao do spinner
        val spinnerFiltro = resources.getStringArray(R.array.categoria) //recupera o array do string.xml
        spinner_cadastro_servicos.setAdapter(
            ArrayAdapter(
                this,
                R.layout.support_simple_spinner_dropdown_item, spinnerFiltro
            )
        ) //seta o adapter no spinner

        inicializar()

        image_servicos.setOnClickListener {
            mudarFoto()
        }

        btn_cadastrar_servico.setOnClickListener {
            veririfcaCampos()
        }
    }
    fun inicializar(){
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        imagemPerfil = findViewById(R.id.image_servicos)
        storageReference = FirebaseStorage.getInstance().reference
        //progressImage = findViewById(R.id.progressImage)

        spinner_cadastro_servicos.setOnItemSelectedListener(object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                //(parent.getChildAt(0) as TextView).setTextColor(Color.parseColor("#bdbdbd"))
                (parent.getChildAt(0) as TextView).setTypeface(Typeface.DEFAULT)
                pgtCategoria = spinner_cadastro_servicos.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        })

        var mLocale = Locale("pt", "BR")

        txt_valor.addTextChangedListener(MoneyTextWatcher(txt_valor,mLocale))
        txt_custo.addTextChangedListener(MoneyTextWatcher(txt_custo,mLocale))



    }

    fun veririfcaCampos(){
        var titulo = txt_titulo_servico.text.toString()
        var descricao = txt_descricao.text.toString()
        var valor = txt_valor.text.toString()
        var custo = txt_custo.text.toString()


        var valido = true

        if(titulo.isEmpty()){
            txt_titulo_servico.error = "Campo obrigatório"
            valido = false
        }
        if(descricao.isEmpty()){
            txt_descricao.error = "Campo obrigatório"
            valido = false
        }
        if(valor.isEmpty()){
            txt_valor.error = "Campo obrigatório"
            valido = false
        }
        if(custo.isEmpty()){
            txt_custo.error = "Campo obrigatório"
            valido = false
        }

        if(valido && pgtCategoria.isEmpty()){
            Toast.makeText(this, "Escolha uma categoria", Toast.LENGTH_SHORT).show()
        }else if(valido && imagem == null){
            Toast.makeText(this, "Selecione uma Foto", Toast.LENGTH_SHORT).show()
        }else if(valido && !pgtCategoria.isEmpty() && !(imagem == null)){
            val servico  = Servico()
            servico.titulo = titulo
            servico.descricao = descricao
            servico.categoria = pgtCategoria
            servico.valor = valor
            servico.custo = custo

           salvar(servico)
        }

    }

    fun mudarFoto() {
        val modes = arrayOf("Tirar foto da câmera", "Selecionar foto da galeria")

        //Cria uma AlertDialog
        val builder = AlertDialog.Builder(this)

        //Seta o título
        builder.setTitle("Selecione uma opção") //Seta os itens de opção
            .setItems(modes) { dialogInterface, i ->

                //Verifica o índice do item
                when (i) {
                    0 -> //abre as permissoes para camera/ se o usuario tiver permissao irá abrir a camera
                        if (Permissao.validarPermissao(permisssaoCamera, this, SELECAO_CAMERA)) {
                            if (baseContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                if (intent.resolveActivity(packageManager) != null) {
                                    startActivityForResult(intent, SELECAO_CAMERA)
                                }
                            } else {
                                //caso não tenha acesso aos recursos da camera
                            }
                        }
                    1 -> {
                        //abre as permissoes para galeria/ se o usuario tiver permissao irá abrir a galeria
                        if (Permissao.validarPermissao(permisssaoGaleria, this, SELECAO_GALERIA)) {
                            val intent =
                                Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivityForResult(intent, SELECAO_GALERIA)
                            }
                        }
                    }
                }
            }
            .create()
            .show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (permissaoResultado in grantResults) {
            if (permissaoResultado == PackageManager.PERMISSION_DENIED) {
                alertaPermissao()

            }else if (requestCode == SELECAO_GALERIA) { //se foi aceita a permissao ira abrir a opcao da camera ou galeria
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, SELECAO_GALERIA)
                }

            } else if (requestCode == SELECAO_CAMERA) {
                if (baseContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, SELECAO_CAMERA)
                    }
                } else {
                    //notFound
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            try {
                when (requestCode) {
                    SELECAO_CAMERA -> {
                        imagem = data?.extras?.get("data") as Bitmap
                    }
                    SELECAO_GALERIA -> {
                        val localImagemSelecionada = data?.data
                        imagem = MediaStore.Images.Media.getBitmap(
                            contentResolver,
                            localImagemSelecionada
                        )
                    }
                }
                if(imagem != null){
                    //recuperar dados da imagem para o firebase
                    imagemPerfil.setImageBitmap(imagem)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun alertaPermissao(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permissão Negada")
        builder.setMessage("Para poder acessar este recurso é neceessário aceitar a permissão")
        builder.setCancelable(false)
        builder.setPositiveButton("OK"){ dialogInterface, i ->  }


        val dialog = builder.create()
        dialog.show()
    }
    fun salvar(servico: Servico){
        val pd = ProgressDialog(this)
        pd.setMessage("Salvando...")
        pd.show()

        val produtoRef = database.child("servicos")
        var id = produtoRef.push().key
       produtoRef.child(id!!).setValue(servico).addOnCompleteListener {
           if(it.isSuccessful){
               salvarFoto(produtoRef.child(id!!), id)
           }
       }
    }
    fun salvarFoto(ref: DatabaseReference, id: String){
        //recuperar dados da imagem para o firebase
        val baos =  ByteArrayOutputStream()

        imagem?.compress(Bitmap.CompressFormat.JPEG, 70, baos)

        val dadosImagem = baos.toByteArray()

        //Salvar no Firebase
        val imagemRef = storageReference
            .child("imagens")
            .child("servicos")
            .child(id)
            .child("servico.jpeg")

        val uploadTask = imagemRef.putBytes(dadosImagem)
        uploadTask.addOnFailureListener{
            //Se houve erro no upload da imageFile
            Toast.makeText(this, "Erro ao salvar  a foto", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener {
            imagemRef.downloadUrl.addOnSuccessListener {
                ref.child("foto").setValue(it.toString())
                ref.child("id").setValue(id)
            }
            //Se o upload da imageFile foi realizado com sucesso
            Toast.makeText(this, "Serviço cadastrado com sucesso!", Toast.LENGTH_SHORT)
                .show()
            val intent = Intent(this, HomeAdmActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent)
        }
    }
}
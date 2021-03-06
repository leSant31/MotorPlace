package com.example.motorplace.fragments.adm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.motorplace.R
import com.example.motorplace.adapter.ServicosAdapter
import com.example.motorplace.adapter.ServicosAdmAdapter
import com.example.motorplace.model.Servico
import com.google.firebase.database.*
import java.util.*

class PinturaFragment : Fragment() {
    private lateinit var recyclerViewServicos: RecyclerView
    private var servicos = arrayListOf<Servico>()
    private lateinit var adapterServico: ServicosAdmAdapter
    private lateinit var servicosRecuperados : DatabaseReference
    private lateinit var servicosExcluidos : DatabaseReference
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_pintura, container, false)


        servicosRecuperados =  FirebaseDatabase.getInstance().reference.child("servicos")
        servicosExcluidos =  FirebaseDatabase.getInstance().reference.child("servicosSolicitados")

        recyclerViewServicos = view.findViewById(R.id.recycler_pintura_cliente)
        recyclerViewServicos.layoutManager =  StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerViewServicos.hasFixedSize()

        adapterServico = ServicosAdmAdapter(view.context!!,servicos,servicosRecuperados,servicosExcluidos)

        recyclerViewServicos.adapter = adapterServico

        //recupera dados
        recuperarServico()

        return view
    }


    private fun recuperarServico(){
        servicosRecuperados.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                servicos.clear()
                for (d in dataSnapshot.children){
                    var u = d.getValue(Servico::class.java)
                    if(u!!.categoria.equals("Pintura e Funilária") && !u!!.custo.isEmpty()){
                        servicos.add(u!!)
                    }

                }

                Collections.reverse(servicos)
                adapterServico.notifyDataSetChanged()
            }

        })

    }
}
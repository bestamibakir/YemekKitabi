package com.bestamibakir.yemekkitabi.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.bestamibakir.yemekkitabi.databinding.FragmentTarifBinding
import com.bestamibakir.yemekkitabi.model.Tarif
import com.bestamibakir.yemekkitabi.roomdb.TarifDAO
import com.bestamibakir.yemekkitabi.roomdb.TarifDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream

class TarifFragment : Fragment() {

    private var _binding: FragmentTarifBinding? = null
    private val binding get() = _binding!!

    //bu izin istemek için
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    //bu da galeriye gitmek için
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    //seçilen görselin nerede olduğunu gösteren path
    private var secilenGorsel : Uri? = null
    //görseli bulunduğu konumdan alıp png,jpeg gibi görsellere çevirir
    private var secilenBitmap : Bitmap? = null

    private var secilenTarif : Tarif? = null


    private lateinit var db : TarifDatabase
    private lateinit var tarifDAO: TarifDAO

    private val mDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db = Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler").build()
        tarifDAO = db.tarifDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTarifBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kaydetButton.setOnClickListener { kaydet(it) }
        binding.silButton.setOnClickListener { sil(it) }
        binding.imageView.setOnClickListener{ gorselSec(it) }

        arguments?.let {
            val bilgi = TarifFragmentArgs.fromBundle(it).bilgi

            if(bilgi == "yeni"){
                //Yeni tarif eklenecek
                secilenTarif = null
                binding.silButton.isEnabled = false
                binding.kaydetButton.isEnabled = true
                binding.isimText.setText("")
                binding.malzemeText.setText("")
            }else {
                //Eskiden eklenmiş tarif gösteriliyor
                binding.silButton.isEnabled = true
                binding.kaydetButton.isEnabled = false
                val id = TarifFragmentArgs.fromBundle(it).id

                mDisposable.add(
                    tarifDAO.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponse)
                )
            }
        }
    }

    private fun handleResponse(tarif : Tarif){
        //gorselin byte array'den bitmap'e çeviriyoruz.
        val bitmap = BitmapFactory.decodeByteArray(tarif.gorsel,0,tarif.gorsel.size)
        binding.imageView.setImageBitmap(bitmap)

        binding.isimText.setText(tarif.isim)
        binding.malzemeText.setText(tarif.malzeme)

        secilenTarif = tarif
    }


    fun kaydet(view : View){
        val isim = binding.isimText.text.toString()
        val malzeme = binding.malzemeText.text.toString()

        if(secilenBitmap != null){
            val kucukBitmap = kucukBitmapOlustur(secilenBitmap!!,300)
            val outpuStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outpuStream)
            val byteDizisi = outpuStream.toByteArray()

            val tarif = Tarif(isim,malzeme,byteDizisi)

            //RxJava
            mDisposable.add(
                tarifDAO.insert(tarif)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )

        }
    }

    private fun handleResponseForInsert(){
        //bir önceki fragmente dön
        val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    fun sil(view : View){
        if(secilenTarif != null){
            mDisposable.add(
                tarifDAO.delete(tarif = secilenTarif!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )
        }
    }

    fun gorselSec(view : View){

        //versiyon kontrolü yapmak için
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                //İZİN VERİLMEMİŞ, İZİN İSTEMEMİZ GEREKİYOR!!
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_MEDIA_IMAGES)){
                    //SnackBar göstermemiz lazım, kullanıcıdan neden istediğimizi söyleyerek İZİN İSTEMEMİZ lazım.
                    Snackbar.make(view,"Galeriden görsel seçmemiz lazım.",Snackbar.LENGTH_INDEFINITE).setAction(
                        "İzin ver",
                        View.OnClickListener{
                            //İZİN İSTEYECEĞİZ
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    ).show()
                }else {
                    //İZİN İSTEYECEĞİZ
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else {
                //İZİN VERİLMİŞ,Galeriye gidebilirim
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }else {
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //İZİN VERİLMEMİŞ, İZİN İSTEMEMİZ GEREKİYOR!!
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //SnackBar göstermemiz lazım, kullanıcıdan neden istediğimizi söyleyerek İZİN İSTEMEMİZ lazım.
                    Snackbar.make(view,"Galeriden görsel seçmemiz lazım.",Snackbar.LENGTH_INDEFINITE).setAction(
                        "İzin ver",
                        View.OnClickListener{
                            //İZİN İSTEYECEĞİZ
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    ).show()
                }else {
                    //İZİN İSTEYECEĞİZ
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else {
                //İZİN VERİLMİŞ,Galeriye gidebilirim
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }

    }

    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == AppCompatActivity.RESULT_OK){
                val intentFromResult = result.data
                if(intentFromResult != null){
                    secilenGorsel = intentFromResult.data

                    try {
                        if(Build.VERSION.SDK_INT >= 29){
                            val source = ImageDecoder.createSource(requireActivity().contentResolver,secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,secilenGorsel)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }
                    }catch (e : Exception){
                        println(e.localizedMessage)
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if(result){
                //İZİN VERİLDİ
                //Galeriye gidebiliriz
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else {
                //İZİN VERİLMEDİ
                Toast.makeText(requireContext(),"İzin verilmedi!",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun kucukBitmapOlustur (kullanicininBitmapi : Bitmap, maximumBoyut : Int) : Bitmap {
        var width = kullanicininBitmapi.width
        var height = kullanicininBitmapi.height

        val bitmapOrani : Double = width.toDouble() / height.toDouble()

        if(bitmapOrani > 1){
            //gorsel yatay
            width = maximumBoyut
            val kisaltilmisYukseklik = width / bitmapOrani
            height = kisaltilmisYukseklik.toInt()
        }else {
            //gorsel dikey
            height = maximumBoyut
            val kisaltilmisGenislik = height * bitmapOrani
            width = kisaltilmisGenislik.toInt()
        }


        return Bitmap.createScaledBitmap(kullanicininBitmapi,width,height,true)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}
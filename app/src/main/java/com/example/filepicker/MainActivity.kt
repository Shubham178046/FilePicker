package com.example.filepicker

import `in`.gauriinfotech.commons.Commons
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class MainActivity : AppCompatActivity(), PickiTCallbacks {
    val FILE = 11
    var pickiT: PickiT? = null
    private var hasPermission = false
    var picturePathNew: String? = null
    var uri: Uri? = null
    private lateinit var currentDirectory: File
    private lateinit var filesList: List<File>
    private lateinit var adapter: ArrayAdapter<String>

    private var configs: Configurations? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configs = Configurations.Builder().build()
        selectMediaFile()
        setupUi()

        //Initialize PickiT
        pickiT = PickiT(this, this, this)
        /*findViewById<Button>(R.id.btn_picker).setOnClickListener {
            chooseFile(FILE)
        }*/
        findViewById<Button>(R.id.permissionButton).setOnClickListener {
            requestStoragePermission(this)
        }
        findViewById<Button>(R.id.btn_open_pdf).setOnClickListener {
            val pdfOpenintent = Intent(Intent.ACTION_VIEW)
            pdfOpenintent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            pdfOpenintent.setDataAndType(uri, "application/pdf")
            try {
                startActivity(pdfOpenintent)
            } catch (e: ActivityNotFoundException) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasPermission = checkStoragePermission(this)
        if (hasPermission) {
            findViewById<Button>(R.id.btn_open_pdf).visibility = View.VISIBLE
            findViewById<Button>(R.id.btn_picker).visibility = View.VISIBLE
            open(Environment.getExternalStorageDirectory())
        } else {
            findViewById<Button>(R.id.btn_open_pdf).visibility = View.GONE
            findViewById<Button>(R.id.btn_picker).visibility = View.GONE
        }
    }

    private fun setupUi() {

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        findViewById<ListView>(R.id.filesTreeView).adapter = adapter
        findViewById<ListView>(R.id.filesTreeView).setOnItemClickListener { _, _, position, _ ->
            val selectedItem = filesList[position]
            open(selectedItem)
        }
    }

    private fun open(selectedItem: File) {
        if (selectedItem.isFile) {
            Log.d("PATH", "open: " + selectedItem.path)
            return openFile(this, selectedItem)
        }

        currentDirectory = selectedItem
        filesList = getFilesList(currentDirectory)
        adapter.clear()
        adapter.addAll(filesList.map {
            if (it.path == selectedItem.parentFile.path) {
                renderParentLink(this)
            } else {
                renderItem(this, it)
            }
        })

        adapter.notifyDataSetChanged()
    }

    private fun selectMediaFile() {
        findViewById<Button>(R.id.btn_picker)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            startActivityForResult(intent, 0)
        }
    }

    private fun chooseFile(flag: Int, vararg types: String) {
        val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.setType("*/*")
        startActivityForResult(fileIntent, flag)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE) {
            val mediaFiles: ArrayList<MediaFile> = ArrayList()
            val contentResolver = contentResolver
            data!!.data?.also { documentUri ->

                contentResolver.takePersistableUriPermission(
                    documentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                MediaFileLoader.asMediaFile(contentResolver, documentUri, configs)?.let {
                    mediaFiles.add(
                        it
                    )
                };
                var size = mediaFiles.size
                var uri: Uri? = null
                Log.d("Size", "onActivityResult: " + size)
                uri = documentUri
                picturePathNew =
                    documentUri.path.toString()
                println("<><><><>$picturePathNew" + mediaFiles.get(0).path)
                try {
                    for (i in 0 until mediaFiles.size) {
                        println(
                            "Name<><><><>" + mediaFiles.get(i).name + " " + mediaFiles.get(i).path + " " + mediaFiles.get(
                                i
                            ).uri
                        )
                        uri = mediaFiles.get(i).uri
                        val input = getPath(uri, this)
                        Log.d("Path", "onActivityResult: " + input)
                    }
                    val path = Commons.getPath(uri, this)
                    Toast.makeText(this, path, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    println(e.localizedMessage)
                }
            }
        } else if (requestCode == 0 && resultCode == RESULT_OK) {
            try {
                data?.data?.let { uri ->
                    val mediaFiles: ArrayList<MediaFile> = ArrayList()


                    //  Get path from PickiT (The path will be returned in PickiTonCompleteListener)
                    //
                    //  If the selected file is from Dropbox/Google Drive or OnDrive:
                    //  Then it will be "copied" to your app directory (see path example below) and when done the path will be returned in PickiTonCompleteListener
                    //  /storage/emulated/0/Android/data/your.package.name/files/Temp/tempDriveFile.mp4
                    //
                    //  else the path will directly be returned in PickiTonCompleteListener
                    pickiT!!.getPath(data.data, Build.VERSION.SDK_INT)
                    val contentResolver = contentResolver
                    MediaFileLoader.asMediaFile(contentResolver, uri, configs)?.let {
                        mediaFiles.add(
                            it
                        )
                    };
                    var uris: Uri? = null
                    for (i in 0 until mediaFiles.size) {
                        println(
                            "Name<><><><>" + mediaFiles.get(i).name + " " + mediaFiles.get(i).path + " " + mediaFiles.get(
                                i
                            ).uri
                        )
                        uris = mediaFiles.get(i).uri
                    }
                    // val inputStream = getContentResolver().openInputStream(uris!!)
                    //  val file = Utils.getFile(this, DocumentFile.fromSingleUri(this, uris)!!)
                    Log.d("TAG", "onActivityResult: " + uri + " " + uris)
                    val id = DocumentsContract.getDocumentId(uris)
                    val path = FileUtils.getPath(
                        this,
                        Uri.parse("content://com.android.providers.downloads.documents/document/171")
                    )
                    Toast.makeText(this, path, Toast.LENGTH_LONG).show()
                    println("<><><><><>Path" + uri.path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getPath(uri: Uri): String? {
        val column = "_data"
        val cursor = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(column))
            } else {
                return ""
            }
        } catch (e: Exception) {
            println(e.localizedMessage)
        }
        return ""
    }

    private fun getPath(uri: Uri, context: Context): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0)
            } else {
                return null
            }
        } catch (e: Exception) {
            println(e.localizedMessage)
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
        return ""
    }

    fun storefile(uri: Uri, requireActivity: Context): String {
        var file: File? = null
        val returnCursor =
            requireActivity.contentResolver.query(uri, null, null, null, null)
        /* * Get the column indexes of the data in the Cursor, * * move to the first row in the Cursor, get the data, * * and display it. * */
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor!!.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
        file = File(requireActivity.filesDir, name)
        try {
            val inputStream: InputStream? =
                requireActivity.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 25 * 1024 * 1024
            val bytesAvailable: Int = inputStream!!.available()
            //int bufferSize = 1024;
            // val bufferSize = Math.min(bytesAvailable, size.toDouble())
            val buffers = ByteArray(size.toInt())
            while (inputStream.read(buffers).also({ read = it }) != -1) {
                outputStream.write(buffers, 0, read)
            }
            Log.e("File Size", "Size " + file!!.length())
            inputStream.close()
            outputStream.close()
            Log.e("File Path", "Path " + file!!.path)
            Log.e("File Size", "Size " + file!!.length())
        } catch (e: Exception) {
            Log.e("Exception", e.message!!)
        }
        var path = file.path
        return path
    }

    override fun PickiTonUriReturned() {
    }

    override fun PickiTonStartListener() {
    }

    override fun PickiTonProgressUpdate(progress: Int) {
    }

    override fun PickiTonCompleteListener(
        path: String?,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
        if (wasSuccessful) {
            println("<><><><><>Path" + path)
            Toast.makeText(this,path,Toast.LENGTH_LONG).show()
        }
    }
}
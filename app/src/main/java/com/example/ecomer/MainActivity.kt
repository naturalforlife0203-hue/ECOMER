package com.example.ecomer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EcomerScreen()
                }
            }
        }
    }
}

@Composable
fun EcomerScreen() {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }

    var tenSp by remember { mutableStateOf(TextFieldValue("")) }
    var loaiSp by remember { mutableStateOf(TextFieldValue("")) }
    var gia by remember { mutableStateOf(TextFieldValue("")) }

    var selectedImageName by remember { mutableStateOf("") }
    var currentImageUrl by remember { mutableStateOf("") }

    var editingProductId by remember { mutableStateOf<String?>(null) }
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }

    // Launcher chọn ảnh từ máy và chuyển sang Base64
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor != null && nameIndex != null && cursor.moveToFirst()) {
                selectedImageName = cursor.getString(nameIndex)
                cursor.close()
            } else {
                selectedImageName = "image_selected.jpg"
            }

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 160, 160, true)
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                    val byteArray = outputStream.toByteArray()
                    currentImageUrl = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    Toast.makeText(context, "Đã chọn ảnh thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Không đọc được ảnh!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi đọc ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }
                    productList = items
                }
            }
        onDispose { listener.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dữ liệu sản phẩm",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = tenSp,
            onValueChange = { tenSp = it },
            label = { Text("Tên sản phẩm") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = loaiSp,
            onValueChange = { loaiSp = it },
            label = { Text("Loại sản phẩm") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = gia,
            onValueChange = { gia = it },
            label = { Text("Giá sản phẩm") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .clickable { launcher.launch("image/*") }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📁", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedImageName.isNotBlank()) selectedImageName
                    else if (currentImageUrl.isNotBlank()) "Đã có ảnh (Bấm để đổi)"
                    else "Chọn ảnh từ thiết bị...",
                    color = if (selectedImageName.isNotBlank() || currentImageUrl.isNotBlank()) Color.Black else Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val giaDouble = gia.text.toDoubleOrNull() ?: 0.0
                if (tenSp.text.isBlank() || loaiSp.text.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val productData = hashMapOf(
                    "tenSp" to tenSp.text,
                    "loaiSp" to loaiSp.text,
                    "gia" to giaDouble,
                    "imageUrl" to currentImageUrl
                )

                if (editingProductId == null) {
                    db.collection("products").add(productData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Thêm thành công!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Lỗi thêm: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    db.collection("products").document(editingProductId!!).update(productData as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                            editingProductId = null
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Lỗi sửa: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                tenSp = TextFieldValue("")
                loaiSp = TextFieldValue("")
                gia = TextFieldValue("")
                selectedImageName = ""
                currentImageUrl = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text(if (editingProductId == null) "THÊM SẢN PHẨM" else "LƯU CẬP NHẬT")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Danh sách sản phẩm:",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(productList) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            val imageModel = remember(product.imageUrl) {
                                if (product.imageUrl.startsWith("data:image")) {
                                    decodeBase64ToBitmap(product.imageUrl)
                                } else {
                                    product.imageUrl
                                }
                            }

                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Ảnh sp",
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(Color.LightGray, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No img", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Tên sp: ${product.tenSp}", fontWeight = FontWeight.Medium)
                            Text(text = "Giá sp: ${product.gia.toLong()}")
                            Text(text = "Loại sp: ${product.loaiSp}")
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = {
                                editingProductId = product.id
                                tenSp = TextFieldValue(product.tenSp)
                                loaiSp = TextFieldValue(product.loaiSp)
                                gia = TextFieldValue(if (product.gia == 0.0) "" else product.gia.toLong().toString())
                                currentImageUrl = product.imageUrl
                                selectedImageName = ""
                            }) {
                                Text("✏️", fontSize = 18.sp)
                            }

                            IconButton(onClick = {
                                db.collection("products").document(product.id).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Đã xóa!", Toast.LENGTH_SHORT).show()
                                    }
                            }) {
                                Text("🗑️", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val pureBase64 = if (base64Str.contains(",")) {
            base64Str.substringAfter(",")
        } else {
            base64Str
        }
        val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}
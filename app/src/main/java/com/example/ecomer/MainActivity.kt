package com.example.ecomer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

    // Dùng TextFieldValue để gõ tiếng Việt Telex/VNI không bị mất chữ
    var tenSp by remember { mutableStateOf(TextFieldValue("")) }
    var loaiSp by remember { mutableStateOf(TextFieldValue("")) }
    var gia by remember { mutableStateOf(TextFieldValue("")) }
    var imageUrl by remember { mutableStateOf(TextFieldValue("")) }

    var editingProductId by remember { mutableStateOf<String?>(null) }
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }

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

        OutlinedTextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            label = { Text("Link ảnh (URL)") },
            modifier = Modifier.fillMaxWidth()
        )
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
                    "imageUrl" to imageUrl.text
                )

                if (editingProductId == null) {
                    db.collection("products").add(productData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Thêm thành công!", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    db.collection("products").document(editingProductId!!).update(productData as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                            editingProductId = null
                        }
                }

                // Reset form
                tenSp = TextFieldValue("")
                loaiSp = TextFieldValue("")
                gia = TextFieldValue("")
                imageUrl = TextFieldValue("")
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
                            AsyncImage(
                                model = product.imageUrl,
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
                                imageUrl = TextFieldValue(product.imageUrl)
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
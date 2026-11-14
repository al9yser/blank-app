package com.daftary.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val Ink = Color(0xFF172C26)
private val Green = Color(0xFF2F6B55)
private val Mint = Color(0xFFEAF3EE)
private val Canvas = Color(0xFFF6F7F5)
private val Muted = Color(0xFF7C8982)

data class Sale(val id: Long, val name: String, val car: String, val price: Double, val down: Double, val count: Int, val paid: Double) {
    val remaining: Double get() = (price - paid).coerceAtLeast(0.0)
}
class DaftaryDb(context: Context) : SQLiteOpenHelper(context, "daftary.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, car TEXT NOT NULL, price REAL NOT NULL DEFAULT 0, down_payment REAL NOT NULL DEFAULT 0, installments INTEGER NOT NULL DEFAULT 0, paid REAL NOT NULL DEFAULT 0)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun all(): List<Sale> {
        val out = mutableListOf<Sale>()
        readableDatabase.rawQuery("SELECT * FROM sales ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out.add(Sale(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4), c.getInt(5), c.getDouble(6)))
        }
        return out
    }
    fun add(name: String, car: String, price: Double, down: Double, count: Int) {
        val v = ContentValues().apply { put("name", name); put("car", car); put("price", price); put("down_payment", down); put("installments", count); put("paid", down) }
        writableDatabase.insert("sales", null, v)
    }
    fun receive(id: Long, amount: Double) { writableDatabase.execSQL("UPDATE sales SET paid = paid + ? WHERE id = ?", arrayOf(amount, id)) }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DaftaryApp(DaftaryDb(this)) }
    }
}
@Composable
fun DaftaryApp(db: DaftaryDb) {
    var sales by remember { mutableStateOf(db.all()) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Sale?>(null) }
    var search by remember { mutableStateOf("") }
    val total = sales.sumOf { it.price }
    val received = sales.sumOf { it.paid }
    val debt = sales.sumOf { it.remaining }
    val visible = sales.filter { (it.name + it.car).contains(search, true) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Canvas,
                bottomBar = {
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                        NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("العقود") })
                        NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.AttachMoney, null) }, label = { Text("التحصيل") })
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAdd = true }, containerColor = Ink, contentColor = Color.White, shape = CircleShape) { Icon(Icons.Default.Add, "إضافة عملية بيع") }
                }
            ) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text("مرحباً بك 👋", color = Muted, fontSize = 13.sp); Text("دفتري", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold) }
                            Box(Modifier.size(46.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = Green) }
                        }
                    }
                    if (tab == 0) {
                        item {
                            Column(Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(26.dp)).padding(20.dp)) {
                                Text("إجمالي المبالغ المتبقية", color = Color(0xFFC9D9D1), fontSize = 13.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(money(debt), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(18.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    MiniMetric("إجمالي المبيعات", money(total), Color.White)
                                    MiniMetric("المبالغ المحصلة", money(received), Color(0xFFB8D8C8))
                                }
                            }
                        }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("عدد العقود", sales.size.toString(), Icons.Default.ReceiptLong, Modifier.weight(1f))
                            StatCard("عقود قيد السداد", sales.count { it.remaining > 0 }.toString(), Icons.Default.TrendingUp, Modifier.weight(1f))
                        } }
                    } else {
                        item { Column { Text(if (tab == 1) "عقود السيارات" else "سجل التحصيل", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink); Text(if (tab == 1) "جميع عمليات البيع المسجلة" else "اختر عقداً لتسجيل دفعة جديدة", color = Muted, fontSize = 13.sp) } }
                    }
                    item {
                        OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("ابحث باسم المشتري أو السيارة…", fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE8ECE9), focusedBorderColor = Green))
                    }
                    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (tab == 2) "العملاء والأقساط" else "أحدث العقود", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink); Text(visible.size.toString() + " عملية", color = Muted, fontSize = 12.sp) } }
                    if (visible.isEmpty()) {
                        item { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(22.dp)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.DirectionsCar, null, tint = Green, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(10.dp)); Text("لا توجد عقود حتى الآن", fontWeight = FontWeight.SemiBold, color = Ink); Text("اضغط + لإضافة أول عملية بيع", color = Muted, fontSize = 12.sp) } }
                    } else items(visible, key = { it.id }) { sale -> SaleCard(sale) { selected = sale } }
                }
            }
        }
        if (showAdd) AddSaleDialog({ showAdd = false }) { name, car, price, down, count -> db.add(name, car, price, down, count); sales = db.all(); showAdd = false }
        selected?.let { sale -> PaymentDialog(sale, { selected = null }) { amount -> db.receive(sale.id, amount); sales = db.all(); selected = null } }
    }
}
@Composable
fun MiniMetric(label: String, value: String, color: Color) {
    Column { Text(label, color = color.copy(alpha = .75f), fontSize = 11.sp); Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}
@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(20.dp)).padding(15.dp)) { Icon(icon, null, tint = Green); Spacer(Modifier.height(12.dp)); Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(label, color = Muted, fontSize = 11.sp) }
}
@Composable
fun SaleCard(sale: Sale, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(21.dp)).clickable { onClick() }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Mint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = Green) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(sale.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(sale.car, color = Muted, fontSize = 12.sp) }
            Text(if (sale.remaining <= 0) "مكتمل" else "قيد السداد", color = if (sale.remaining <= 0) Green else Color(0xFFB7791F), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(15.dp)); HorizontalDivider(color = Color(0xFFF0F2F0)); Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("قيمة السيارة", color = Muted, fontSize = 11.sp); Text(money(sale.price), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            Column(horizontalAlignment = Alignment.End) { Text("المبلغ المتبقي", color = Muted, fontSize = 11.sp); Text(money(sale.remaining), color = Green, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
        if (sale.price > 0) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { (sale.paid / sale.price).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Green, trackColor = Mint) }
        Spacer(Modifier.height(8.dp)); Text("اضغط لتسجيل دفعة أو عرض التفاصيل", color = Muted, fontSize = 10.sp)
    }
}
@Composable
fun AddSaleDialog(onDismiss: () -> Unit, onSave: (String, String, Double, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }; var car by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }; var down by remember { mutableStateOf("") }; var count by remember { mutableStateOf("12") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("عملية بيع جديدة", fontWeight = FontWeight.Bold, color = Ink) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("بيانات المشتري الأساسية فقط مطلوبة", color = Muted, fontSize = 12.sp)
            FormField("اسم المشتري *", name, { name = it }); FormField("نوع السيارة *", car, { car = it })
            FormField("سعر السيارة (اختياري)", price, { price = it }, true); FormField("الدفعة المقدمة (اختياري)", down, { down = it }, true); FormField("عدد الأقساط (اختياري)", count, { count = it }, true)
        } },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank() && car.isNotBlank()) onSave(name.trim(), car.trim(), price.toDoubleOrNull() ?: 0.0, down.toDoubleOrNull() ?: 0.0, count.toIntOrNull() ?: 0) }) { Text("حفظ العملية") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}
@Composable
fun FormField(label: String, value: String, onValue: (String) -> Unit, numeric: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onValue, label = { Text(label, fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(13.dp), keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE7ECE8), focusedBorderColor = Green))
}
@Composable
fun PaymentDialog(sale: Sale, onDismiss: () -> Unit, onPay: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تحصيل دفعة", color = Ink, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(sale.name + " · " + sale.car, color = Muted, fontSize = 13.sp); Text("الرصيد المتبقي: " + money(sale.remaining), color = Green, fontWeight = FontWeight.Bold); FormField("مبلغ الدفعة", amount, { amount = it }, true) } },
        confirmButton = { TextButton(onClick = { val v = amount.toDoubleOrNull(); if (v != null && v > 0 && v <= sale.remaining) onPay(v) }) { Text("تأكيد التحصيل") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}
fun money(value: Double): String = NumberFormat.getNumberInstance(Locale.US).format(value) + " د.إ"

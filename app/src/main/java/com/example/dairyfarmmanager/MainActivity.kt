package com.example.dairyfarmmanager

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DairyFarmApp()
        }
        val currentData = FarmStore(this).read()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        notifyFarmAlerts(this, currentData)
    }
}

private const val ALARM_ACTION = "com.example.dairyfarmmanager.DAILY_ALARM"
private const val ALARM_REQUEST_CODE = 2001

class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val message = intent?.getStringExtra("message") ?: "یادآوری مدیریت گاوداری"
        showAlarmNotification(context, message)
    }
}

private enum class Screen(val title: String) {
    Dashboard("داشبورد"), Animals("دام‌ها"), Milk("تولید شیر"), Finance("مالی"),
    Employees("کارکنان"), Reports("گزارش‌ها"), Calendar("برنامه‌ریزی"), Health("سلامت"),
    Inventory("انبارداری"), Sales("فروش محصولات"), Alerts("تنظیم هشدار")
}

private data class FarmData(
    val cows: Int,
    val milk: Int,
    val revenue: Int,
    val orders: Int,
    val employees: Int,
    val healthChecks: Int,
    val nextTask: String,
    val inventory: Int,
    val invoices: Int,
    val lastInvoiceCode: String,
    val employeeList: List<EmployeeRecord>,
    val animalList: List<AnimalRecord>,
    val invoiceList: List<InvoiceRecord>,
    val taskList: List<TaskRecord>,
    val incomeList: List<IncomeRecord>,
    val milkList: List<MilkRecord>,
    val inventoryList: List<InventoryRecord>,
    val alarmList: List<AlarmRecord>
)

private data class MilkRecord(
    val id: Long,
    val date: String,
    val amount: Int,
    val unit: String,
    val note: String
)

private data class InventoryRecord(
    val id: Long,
    val date: String,
    val product: String,
    val movement: String,
    val bags: Int,
    val totalWeight: Int,
    val note: String
)

private data class AlarmRecord(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val message: String,
    val enabled: Boolean
)

private data class EmployeeRecord(
    val id: Long,
    val name: String,
    val role: String,
    val phone: String,
    val hireDate: String
)

private data class AnimalRecord(
    val id: Long,
    val tag: String,
    val breed: String,
    val gender: String,
    val birthDate: String,
    val health: String
)

private data class InvoiceRecord(
    val id: Long,
    val code: String,
    val customer: String,
    val product: String,
    val quantity: Int,
    val unitPrice: Int,
    val total: Int,
    val date: String,
    val note: String
)

private data class TaskRecord(
    val id: Long,
    val title: String,
    val dueDate: String,
    val note: String
)

private data class IncomeRecord(
    val id: Long,
    val date: String,
    val category: String,
    val source: String,
    val amount: Int,
    val method: String,
    val note: String
)

private class FarmStore(context: Context) {
    private val preferences = context.getSharedPreferences("farm_data", Context.MODE_PRIVATE)

    fun read(): FarmData {
        if (!preferences.getBoolean("zero_data_v2", false)) {
            preferences.edit().clear().putBoolean("zero_data_v2", true).apply()
        }
        return FarmData(
            preferences.getInt("cows", 0), preferences.getInt("milk", 0),
            preferences.getInt("revenue", 0), preferences.getInt("orders", 0),
            preferences.getInt("employees", 0), preferences.getInt("health", 0),
            preferences.getString("task", "") ?: "", preferences.getInt("inventory", 0),
            preferences.getInt("invoices", 0), preferences.getString("invoice", "") ?: "",
            decodeEmployees(preferences.getString("employee_list", "") ?: ""),
            decodeAnimals(preferences.getString("animal_list", "") ?: ""),
            decodeInvoices(preferences.getString("invoice_list", "") ?: ""),
            decodeTasks(preferences.getString("task_list", "") ?: ""),
            decodeIncome(preferences.getString("income_list", "") ?: ""),
            decodeMilk(preferences.getString("milk_list", "") ?: ""),
            decodeInventory(preferences.getString("inventory_list", "") ?: ""),
            decodeAlarms(preferences.getString("alarm_list", "") ?: "")
        )
    }

    fun save(data: FarmData) {
        preferences.edit()
            .putInt("cows", data.cows)
            .putInt("milk", data.milk)
            .putInt("revenue", data.revenue)
            .putInt("orders", data.orders)
            .putInt("employees", data.employees)
            .putInt("health", data.healthChecks)
            .putString("task", data.nextTask)
            .putInt("inventory", data.inventory)
            .putInt("invoices", data.invoices)
            .putString("invoice", data.lastInvoiceCode)
            .putString("employee_list", encodeEmployees(data.employeeList))
            .putString("animal_list", encodeAnimals(data.animalList))
            .putString("invoice_list", encodeInvoices(data.invoiceList))
            .putString("task_list", encodeTasks(data.taskList))
            .putString("income_list", encodeIncome(data.incomeList))
            .putString("milk_list", encodeMilk(data.milkList))
            .putString("inventory_list", encodeInventory(data.inventoryList))
            .putString("alarm_list", encodeAlarms(data.alarmList))
            .apply()
    }

    private fun encodeEmployees(items: List<EmployeeRecord>) = items.joinToString(";;") {
        listOf(it.id, it.name, it.role, it.phone, it.hireDate).joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeEmployees(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 5) EmployeeRecord(parts[0].toLongOrNull() ?: 0L, parts[1], parts[2], parts[3], parts[4]) else null
    }

    private fun encodeAnimals(items: List<AnimalRecord>) = items.joinToString(";;") {
        listOf(it.id, it.tag, it.breed, it.gender, it.birthDate, it.health).joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeAnimals(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 6) AnimalRecord(parts[0].toLongOrNull() ?: 0L, parts[1], parts[2], parts[3], parts[4], parts[5]) else null
    }

    private fun encodeInvoices(items: List<InvoiceRecord>) = items.joinToString(";;") {
        listOf(it.id, it.code, it.customer, it.product, it.quantity, it.unitPrice, it.total, it.date, it.note)
            .joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeInvoices(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 9) InvoiceRecord(
            parts[0].toLongOrNull() ?: 0L, parts[1], parts[2], parts[3],
            parts[4].toIntOrNull() ?: 0, parts[5].toIntOrNull() ?: 0,
            parts[6].toIntOrNull() ?: 0, parts[7], parts[8]
        ) else null
    }

    private fun encodeTasks(items: List<TaskRecord>) = items.joinToString(";;") {
        listOf(it.id, it.title, it.dueDate, it.note).joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeTasks(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 4) TaskRecord(parts[0].toLongOrNull() ?: 0L, parts[1], parts[2], parts[3]) else null
    }

    private fun encodeIncome(items: List<IncomeRecord>) = items.joinToString(";;") {
        listOf(it.id, it.date, it.category, it.source, it.amount, it.method, it.note)
            .joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeIncome(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 7) IncomeRecord(
            parts[0].toLongOrNull() ?: 0L, parts[1], parts[2], parts[3],
            parts[4].toIntOrNull() ?: 0, parts[5], parts[6]
        ) else null
    }

    private fun encodeMilk(items: List<MilkRecord>) = items.joinToString(";;") {
        listOf(it.id, it.date, it.amount, it.unit, it.note).joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeMilk(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        when {
            parts.size == 5 -> MilkRecord(parts[0].toLongOrNull() ?: 0L, parts[1], parts[2].toIntOrNull() ?: 0, parts[3], parts[4])
            parts.size == 4 -> MilkRecord(parts[0].toLongOrNull() ?: 0L, parts[1], parts[2].toIntOrNull() ?: 0, "لیتر", parts[3])
            else -> null
        }
    }

    private fun encodeInventory(items: List<InventoryRecord>) = items.joinToString(";;") {
        listOf(it.id, it.date, it.product, it.movement, it.bags, it.totalWeight, it.note)
            .joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeInventory(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 7) InventoryRecord(
            parts[0].toLongOrNull() ?: 0L, parts[1], parts[2], parts[3],
            parts[4].toIntOrNull() ?: 0, parts[5].toIntOrNull() ?: 0, parts[6]
        ) else null
    }

    private fun encodeAlarms(items: List<AlarmRecord>) = items.joinToString(";;") {
        listOf(it.id, it.hour, it.minute, it.message, it.enabled).joinToString("|") { value -> value.toString().replace("|", " ").replace(";", " ") }
    }

    private fun decodeAlarms(value: String) = value.split(";;").filter { it.isNotBlank() }.mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 5) AlarmRecord(
            parts[0].toLongOrNull() ?: 0L,
            parts[1].toIntOrNull()?.coerceIn(0, 23) ?: 8,
            parts[2].toIntOrNull()?.coerceIn(0, 59) ?: 0,
            parts[3],
            parts[4].toBooleanStrictOrNull() ?: false
        ) else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DairyFarmApp() {
    val context = LocalContext.current
    val store = remember { FarmStore(context) }
    var data by remember { mutableStateOf(store.read()) }
    var screen by remember { mutableStateOf(Screen.Dashboard) }
    var dialog by remember { mutableStateOf<Screen?>(null) }
    var editing by remember { mutableStateOf(false) }
    var employeeEditor by remember { mutableStateOf<EmployeeRecord?>(null) }
    var animalEditor by remember { mutableStateOf<AnimalRecord?>(null) }
    var showEmployeeEditor by remember { mutableStateOf(false) }
    var showAnimalEditor by remember { mutableStateOf(false) }
    var invoiceEditor by remember { mutableStateOf<InvoiceRecord?>(null) }
    var showInvoiceEditor by remember { mutableStateOf(false) }
    var taskEditor by remember { mutableStateOf<TaskRecord?>(null) }
    var showTaskEditor by remember { mutableStateOf(false) }
    var incomeEditor by remember { mutableStateOf<IncomeRecord?>(null) }
    var showIncomeEditor by remember { mutableStateOf(false) }
    var inventoryEditor by remember { mutableStateOf<InventoryRecord?>(null) }
    var showInventoryEditor by remember { mutableStateOf(false) }
    var milkEditor by remember { mutableStateOf<MilkRecord?>(null) }
    var deleteRequest by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var darkMode by remember { mutableStateOf(context.getSharedPreferences("farm_data", Context.MODE_PRIVATE).getBoolean("dark", false)) }

    BackHandler(enabled = dialog != null || showEmployeeEditor || showAnimalEditor || showInvoiceEditor || showTaskEditor || showIncomeEditor || showInventoryEditor || screen != Screen.Dashboard) {
        when {
            deleteRequest != null -> deleteRequest = null
            dialog != null -> { dialog = null; editing = false; milkEditor = null }
            showEmployeeEditor -> { employeeEditor = null; showEmployeeEditor = false }
            showAnimalEditor -> { animalEditor = null; showAnimalEditor = false }
            showInvoiceEditor -> { invoiceEditor = null; showInvoiceEditor = false }
            showTaskEditor -> { taskEditor = null; showTaskEditor = false }
            showIncomeEditor -> { incomeEditor = null; showIncomeEditor = false }
            showInventoryEditor -> { inventoryEditor = null; showInventoryEditor = false }
            screen != Screen.Dashboard -> screen = Screen.Dashboard
        }
    }

    fun update(transform: (FarmData) -> FarmData) {
        data = transform(data)
        store.save(data)
    }

    fun requestDelete(label: String, action: () -> Unit) {
        deleteRequest = label to action
    }

    MaterialTheme(
        colorScheme = if (darkMode) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF397D54), secondary = Color(0xFF8A9A5B), tertiary = Color(0xFFD4A373)
        )
    ) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("مدیریت گاوداری | ${screen.title}") },
                    actions = {
                        Text("تیره")
                        Switch(checked = darkMode, onCheckedChange = {
                            darkMode = it
                            context.getSharedPreferences("farm_data", Context.MODE_PRIVATE).edit().putBoolean("dark", it).apply()
                        })
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                when (screen) {
                    Screen.Dashboard -> Dashboard(data, onOpen = { screen = it })
                    Screen.Animals -> Animals(data, onAdd = { animalEditor = null; showAnimalEditor = true }, onEdit = { animalEditor = it; showAnimalEditor = true }, onDelete = { id -> requestDelete("این دام") { update { value -> value.copy(animalList = value.animalList.filterNot { item -> item.id == id }, cows = (value.animalList.size - 1).coerceAtLeast(0)) } } })
                    Screen.Milk -> Milk(data, onAdd = { editing = false; milkEditor = null; dialog = Screen.Milk }, onEdit = { record -> editing = true; milkEditor = record; dialog = Screen.Milk }, onDelete = { requestDelete("اطلاعات تولید شیر") { update { it.copy(milk = 0, milkList = emptyList()) } } })
                    Screen.Finance -> Finance(data, onAdd = { incomeEditor = null; showIncomeEditor = true }, onEdit = { incomeEditor = it; showIncomeEditor = true }, onDelete = { id -> requestDelete("این درآمد") { update { value -> val list = value.incomeList.filterNot { item -> item.id == id }; value.copy(incomeList = list, revenue = list.sumOf { item -> item.amount }) } } })
                    Screen.Employees -> Employees(data, onAdd = { employeeEditor = null; showEmployeeEditor = true }, onEdit = { employeeEditor = it; showEmployeeEditor = true }, onDelete = { id -> requestDelete("این کارمند") { update { value -> value.copy(employeeList = value.employeeList.filterNot { item -> item.id == id }, employees = (value.employeeList.size - 1).coerceAtLeast(0)) } } })
                    Screen.Reports -> Reports(data)
                    Screen.Calendar -> Calendar(data, onAdd = { taskEditor = null; showTaskEditor = true }, onEdit = { taskEditor = it; showTaskEditor = true }, onDelete = { id -> requestDelete("این کار") { update { value -> value.copy(taskList = value.taskList.filterNot { item -> item.id == id }, nextTask = value.taskList.filterNot { item -> item.id == id }.firstOrNull()?.title ?: "") } } })
                    Screen.Health -> Health(data, onAdd = { editing = false; dialog = Screen.Health }, onEdit = { editing = true; dialog = Screen.Health }, onDelete = { requestDelete("اطلاعات سلامت") { update { it.copy(healthChecks = 0) } } })
                    Screen.Inventory -> Inventory(
                        data,
                        onAdd = { inventoryEditor = null; showInventoryEditor = true },
                        onEdit = { inventoryEditor = it; showInventoryEditor = true },
                        onDelete = { id -> requestDelete("این تراکنش انبار") { update { value -> value.copy(inventoryList = value.inventoryList.filterNot { item -> item.id == id }) } } }
                    )
                    Screen.Sales -> Sales(data, onAdd = { invoiceEditor = null; showInvoiceEditor = true }, onEdit = { invoiceEditor = it; showInvoiceEditor = true }, onDelete = { id -> requestDelete("این فاکتور") { update { value -> val list = value.invoiceList.filterNot { item -> item.id == id }; value.copy(invoiceList = list, invoices = list.size, lastInvoiceCode = list.lastOrNull()?.code ?: "") } } })
                    Screen.Alerts -> AlertSettings(data, onUpdate = ::update)
                }
                if (screen != Screen.Dashboard) {
                    OutlinedButton(onClick = { screen = Screen.Dashboard }, modifier = Modifier.fillMaxWidth()) {
                        Text("بازگشت به داشبورد")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    }

    dialog?.let { activeScreen ->
        EntryDialog(activeScreen, editing, milkEditor, onDismiss = { dialog = null; editing = false; milkEditor = null }) { amount, note, unit ->
            when (activeScreen) {
            Screen.Animals -> update { it.copy(cows = if (editing) amount else it.cows + amount.coerceAtLeast(1)) }
            Screen.Milk -> update { value ->
                val entry = MilkRecord(milkEditor?.id ?: System.currentTimeMillis(), milkEditor?.date ?: persianDate(), amount.coerceAtLeast(1), unit, note)
                val records = if (editing) value.milkList.map { if (it.id == entry.id) entry else it } else value.milkList + entry
                value.copy(milk = if (records.isEmpty()) 0 else records.sumOf { item -> item.amount }, milkList = records)
            }
            Screen.Finance -> update { it.copy(revenue = if (editing) amount else it.revenue + amount.coerceAtLeast(0)) }
            Screen.Employees -> update { it.copy(employees = if (editing) amount else it.employees + amount.coerceAtLeast(1)) }
            Screen.Health -> update { it.copy(healthChecks = if (editing) amount else it.healthChecks + 1) }
            Screen.Calendar -> update { it.copy(nextTask = note) }
            Screen.Inventory -> Unit
                Screen.Sales -> {
                    val code = note.ifBlank { "INV-${data.invoices + 1}" }
                    update { it.copy(invoices = it.invoices + 1, lastInvoiceCode = code) }
                    saveInvoicePdf(context, code, amount)
                }
                else -> Unit
            }
            editing = false
            dialog = null
            milkEditor = null
        }
    }

    if (showEmployeeEditor) {
        EmployeeDialog(employeeEditor, onDismiss = { employeeEditor = null; showEmployeeEditor = false }) { record ->
            update { value ->
                val list = value.employeeList.filterNot { it.id == record.id } + record
                value.copy(employeeList = list, employees = list.size)
            }
            employeeEditor = null
            showEmployeeEditor = false
        }
    }

    if (showAnimalEditor) {
        AnimalDialog(animalEditor, onDismiss = { animalEditor = null; showAnimalEditor = false }) { record ->
            update { value ->
                val list = value.animalList.filterNot { it.id == record.id } + record
                value.copy(animalList = list, cows = list.size)
            }
            animalEditor = null
            showAnimalEditor = false
        }
    }

    if (showInvoiceEditor) {
        InvoiceDialog(invoiceEditor, onDismiss = { invoiceEditor = null; showInvoiceEditor = false }) { invoice ->
            update { value ->
                val list = value.invoiceList.filterNot { it.id == invoice.id } + invoice
                value.copy(invoiceList = list, invoices = list.size, lastInvoiceCode = invoice.code, revenue = list.sumOf { it.total })
            }
            saveInvoicePdf(context, invoice.code, invoice.total)
            invoiceEditor = null
            showInvoiceEditor = false
        }
    }

    if (showTaskEditor) {
        TaskDialog(taskEditor, onDismiss = { taskEditor = null; showTaskEditor = false }) { task ->
            update { value ->
                val list = value.taskList.filterNot { it.id == task.id } + task
                value.copy(taskList = list, nextTask = list.firstOrNull()?.title ?: "")
            }
            taskEditor = null
            showTaskEditor = false
        }
    }

    if (showIncomeEditor) {
        IncomeDialog(incomeEditor, onDismiss = { incomeEditor = null; showIncomeEditor = false }) { income ->
            update { value ->
                val list = value.incomeList.filterNot { it.id == income.id } + income
                value.copy(incomeList = list, revenue = list.sumOf { item -> item.amount })
            }
            incomeEditor = null
            showIncomeEditor = false
        }
    }

    if (showInventoryEditor) {
        InventoryDialog(inventoryEditor, onDismiss = { inventoryEditor = null; showInventoryEditor = false }) { record ->
            update { value ->
                val list = value.inventoryList.filterNot { it.id == record.id } + record
                value.copy(
                    inventoryList = list,
                    inventory = list.sumOf { item -> if (item.movement == "ورود") item.totalWeight else -item.totalWeight }
                        .coerceAtLeast(0)
                )
            }
            inventoryEditor = null
            showInventoryEditor = false
        }
    }

    deleteRequest?.let { (label, action) ->
        AlertDialog(
            onDismissRequest = { deleteRequest = null },
            title = { Text("تأیید حذف") },
            text = { Text("آیا از حذف $label مطمئن هستید؟ این عملیات قابل بازگشت نیست.") },
            confirmButton = { TextButton(onClick = { action(); deleteRequest = null }) { Text("حذف قطعی") } },
            dismissButton = { TextButton(onClick = { deleteRequest = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun Dashboard(data: FarmData, onOpen: (Screen) -> Unit) {
    Text("خلاصه وضعیت امروز", fontSize = 25.sp, fontWeight = FontWeight.Bold)
    Text("امروز: ${persianDate()}", color = Color(0xFF64748B))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Metric("شیر امروز", "${data.milk} لیتر", Color(0xFF168AAD), Modifier.weight(1f))
        Metric("تعداد دام", data.cows.toString(), Color(0xFF2A9D8F), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Metric("درآمد", "${data.revenue} تومان", Color(0xFFE76F51), Modifier.weight(1f))
        Metric("سفارش‌ها", data.orders.toString(), Color(0xFF6C63FF), Modifier.weight(1f))
    }
    SectionCard("هشدارها", Icons.Default.Warning) {
        Text("• ${data.healthChecks} مورد نیازمند بررسی سلامت", color = Color(0xFF7C2D12))
        Text("• موجودی خوراک را امروز کنترل کنید", color = Color(0xFF7C2D12))
        Text("• کار بعدی: ${data.nextTask}", color = Color(0xFF334155))
    }
    Text("بخش‌های برنامه", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Button(onClick = { onOpen(Screen.Reports) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.BarChart, contentDescription = null)
        Text("  مشاهده گزارش ماهانه شیر")
    }
    Button(onClick = { onOpen(Screen.Alerts) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Alarm, contentDescription = null)
        Text("  تنظیم هشدار")
    }
    Button(onClick = { onOpen(Screen.Sales) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.LocalDrink, contentDescription = null)
        Text("  فروش محصولات")
    }
    val screens = listOf(
        Screen.Animals to ("دام‌ها و گوساله‌ها" to Icons.Default.Pets),
        Screen.Milk to ("ثبت تولید شیر" to Icons.Default.LocalDrink),
        Screen.Finance to ("درآمد و هزینه" to Icons.Default.Money),
        Screen.Employees to ("کارکنان" to Icons.Default.AccountCircle),
        Screen.Reports to ("گزارش‌ها" to Icons.Default.BarChart),
        Screen.Calendar to ("تقویم کارها" to Icons.Default.CalendarToday),
        Screen.Health to ("سلامت و واکسن" to Icons.Default.CheckCircle),
        Screen.Inventory to ("انبارداری" to Icons.Default.Money)
    )
    screens.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { (target, info) ->
                Button(onClick = { onOpen(target) }, modifier = Modifier.weight(1f)) {
                    Icon(info.second, contentDescription = null)
                    Text("  ${info.first}")
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Animals(data: FarmData, onAdd: () -> Unit, onEdit: (AnimalRecord) -> Unit, onDelete: (Long) -> Unit) {
    DetailHeader("مدیریت دام‌ها", "پلاک، نژاد، جنسیت، تاریخ تولد و سلامت هر دام")
    InfoCard("تعداد کل دام", "${data.animalList.size} رأس", "دام‌های ثبت‌شده")
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت دام جدید") }
    if (data.animalList.isEmpty()) Text("هنوز دام ثبت نشده است.", color = Color(0xFF64748B))
    data.animalList.forEach { animal ->
        RecordCard(
            title = "پلاک ${animal.tag}",
            lines = listOf("نژاد: ${animal.breed}", "جنسیت: ${animal.gender}", "تولد: ${animal.birthDate}", "سلامت: ${animal.health}"),
            onEdit = { onEdit(animal) }, onDelete = { onDelete(animal.id) }
        )
    }
}

@Composable
private fun Milk(data: FarmData, onAdd: () -> Unit, onEdit: (MilkRecord) -> Unit, onDelete: () -> Unit) {
    DetailHeader("تولید شیر", "ثبت شیر صبح و عصر و پیگیری تولید روزانه")
    val today = persianDate()
    val todayMilk = data.milkList.filter { it.date == today }.sumOf { it.amount }
    InfoCard("تولید امروز", "${data.milkList.filter { it.date == today }.sumOf { it.amount }} واحد", "ترکیبی از لیتر و کیلو")
    InfoCard("میانگین هر دام", "${(todayMilk / data.cows.coerceAtLeast(1))} واحد", "بر اساس دام‌های ثبت‌شده")
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت نوبت شیردوشی") }
    if (data.milkList.isEmpty()) Text("هنوز رکورد شیری ثبت نشده است.", color = Color(0xFF64748B))
    data.milkList.sortedByDescending { it.id }.take(8).forEach { record ->
        RecordCard(
            title = "${record.amount} ${record.unit} | ${record.date}",
            lines = listOf("توضیحات: ${record.note.ifBlank { "بدون توضیح" }}"),
            onEdit = { onEdit(record) },
            onDelete = onDelete
        )
    }
    if (data.milkList.isNotEmpty()) {
        OutlinedButton(onClick = { onEdit(data.milkList.maxByOrNull { it.id }!!) }, modifier = Modifier.fillMaxWidth()) { Text("ویرایش آخرین ثبت") }
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("حذف همه ثبت‌های شیر") }
    }
}

@Composable
private fun Finance(data: FarmData, onAdd: () -> Unit, onEdit: (IncomeRecord) -> Unit, onDelete: (Long) -> Unit) {
    DetailHeader("درآمد و حسابداری", "ثبت دقیق درآمدها و جمع واقعی از اطلاعات ثبت‌شده")
    InfoCard("مجموع درآمد", "${data.incomeList.sumOf { it.amount }} تومان", "بر اساس ${data.incomeList.size} رکورد")
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت درآمد جدید") }
    if (data.incomeList.isEmpty()) Text("هنوز درآمدی ثبت نشده است.", color = Color(0xFF64748B))
    data.incomeList.forEach { income ->
        RecordCard(
            title = "${income.category} | ${income.amount} تومان",
            lines = listOf("تاریخ: ${income.date}", "منبع: ${income.source}", "روش دریافت: ${income.method}", "توضیحات: ${income.note}"),
            onEdit = { onEdit(income) }, onDelete = { onDelete(income.id) }
        )
    }
}

@Composable
private fun Employees(data: FarmData, onAdd: () -> Unit, onEdit: (EmployeeRecord) -> Unit, onDelete: (Long) -> Unit) {
    DetailHeader("کارکنان", "نام، سمت، تلفن و تاریخ استخدام هر نیرو")
    InfoCard("کارکنان فعال", "${data.employeeList.size} نفر", "فهرست نیروهای ثبت‌شده")
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت کارمند") }
    if (data.employeeList.isEmpty()) Text("هنوز کارمندی ثبت نشده است.", color = Color(0xFF64748B))
    data.employeeList.forEach { employee ->
        RecordCard(
            title = employee.name,
            lines = listOf("سمت: ${employee.role}", "تلفن: ${employee.phone}", "تاریخ استخدام: ${employee.hireDate}"),
            onEdit = { onEdit(employee) }, onDelete = { onDelete(employee.id) }
        )
    }
}

@Composable
private fun Reports(data: FarmData) {
    val incomeTotal = data.incomeList.sumOf { it.amount }
    val salesTotal = data.invoiceList.sumOf { it.total }
    val estimatedExpenses = 0
    DetailHeader("گزارش‌های دقیق", "خلاصه واقعی دام، تولید، فروش و حسابداری")
    InfoCard("گزارش دام", "${data.animalList.size} رأس", "کارکنان: ${data.employeeList.size} نفر | موارد سلامت: ${data.healthChecks}")
    InfoCard("گزارش تولید", "${data.milk} لیتر", "تولید ثبت‌شده فعلی | موجودی خوراک: ${data.inventory} واحد")
    MonthlyMilkReport(data)
    InfoCard("گزارش درآمد", "${incomeTotal} تومان", "${data.incomeList.size} رکورد درآمد")
    InfoCard("گزارش فروش", "${salesTotal} تومان", "${data.invoiceList.size} فاکتور | آخرین فاکتور: ${data.lastInvoiceCode.ifBlank { "ندارد" }}")
    InfoCard("سود و زیان", "${incomeTotal - estimatedExpenses} تومان", "درآمد: $incomeTotal | هزینه ثبت‌شده فعلی: $estimatedExpenses")
    SectionCard("جزئیات عملیاتی", Icons.Default.BarChart) {
        Text("فاکتورهای فروش: ${data.invoiceList.size}")
        Text("کارهای برنامه‌ریزی‌شده: ${data.taskList.size}")
        Text("دام‌های ثبت‌شده: ${data.animalList.size}")
        Text("کارکنان ثبت‌شده: ${data.employeeList.size}")
    }
}


@Composable
private fun AlertSettings(data: FarmData, onUpdate: ((FarmData) -> FarmData) -> Unit) {
    val context = LocalContext.current
    var editor by remember { mutableStateOf<AlarmRecord?>(null) }
    DetailHeader("تنظیم هشدار", "هشدارهای روزانه را اضافه، ویرایش یا حذف کنید")
    Button(onClick = { editor = AlarmRecord(System.currentTimeMillis(), 8, 0, "یادآوری مدیریت گاوداری", true) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Alarm, contentDescription = null)
        Text("  افزودن هشدار جدید")
    }
    if (data.alarmList.isEmpty()) Text("هنوز هشداری تنظیم نشده است.", color = Color(0xFF64748B))
    data.alarmList.sortedBy { it.id }.forEach { alarm ->
        RecordCard(
            title = "${alarm.hour.toString().padStart(2, '0')}:${alarm.minute.toString().padStart(2, '0')} | ${if (alarm.enabled) "فعال" else "غیرفعال"}",
            lines = listOf(alarm.message),
            onEdit = { editor = alarm },
            onDelete = { onUpdate { value -> value.copy(alarmList = value.alarmList.filterNot { it.id == alarm.id }) }; cancelDailyAlarm(context, alarm.id) }
        )
    }
    editor?.let { alarm ->
        AlarmEditor(alarm, onDismiss = { editor = null }) { updated ->
            onUpdate { value -> value.copy(alarmList = value.alarmList.filterNot { it.id == updated.id } + updated) }
            if (updated.enabled) scheduleDailyAlarm(context, updated) else cancelDailyAlarm(context, updated.id)
            editor = null
        }
    }
}

@Composable
private fun AlarmEditor(existing: AlarmRecord, onDismiss: () -> Unit, onSave: (AlarmRecord) -> Unit) {
    var hour by remember(existing.id) { mutableStateOf(existing.hour.toString()) }
    var minute by remember(existing.id) { mutableStateOf(existing.minute.toString().padStart(2, '0')) }
    var message by remember(existing.id) { mutableStateOf(existing.message) }
    var enabled by remember(existing.id) { mutableStateOf(existing.enabled) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (existing.id == 0L) "افزودن" else "ویرایش"} هشدار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("ساعت") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, label = { Text("دقیقه") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(message, { message = it }, label = { Text("توضیحات هشدار") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("فعال باشد")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(AlarmRecord(existing.id, hour.toIntOrNull()?.coerceIn(0, 23) ?: 8, minute.toIntOrNull()?.coerceIn(0, 59) ?: 0, message.ifBlank { "یادآوری مدیریت گاوداری" }, enabled))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyMilkReport(data: FarmData) {
    val context = LocalContext.current
    var month by remember { mutableStateOf(persianDate().substringBeforeLast("/")) }
    var monthExpanded by remember { mutableStateOf(false) }
    val availableMonths = (data.milkList.map { it.date.substringBeforeLast("/") } + persianDate().substringBeforeLast("/")).distinct().sortedDescending()
    val records = data.milkList.filter { it.date.startsWith("$month/") }
    val units = records.map { it.unit }.distinct().sorted()
    val dailyTotals = units.associateWith { unit ->
        records.filter { it.unit == unit }.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.amount } }
    }
    val weeklyTotals = units.associateWith { unit ->
        records.filter { it.unit == unit }.groupBy { record ->
            val day = record.date.substringAfterLast("/").toIntOrNull() ?: 1
            "هفته ${((day - 1) / 7) + 1}"
        }.mapValues { (_, items) -> items.sumOf { it.amount } }
    }
    val monthTotal = records.groupBy { it.unit }.mapValues { (_, items) -> items.sumOf { it.amount } }
    val recordedDays = records.map { it.date }.distinct().size
    SectionCard("گزارش ماهانه شیر", Icons.Default.BarChart) {
        ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = !monthExpanded }) {
            OutlinedTextField(
                value = month,
                onValueChange = {},
                readOnly = true,
                label = { Text("انتخاب ماه گزارش") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                availableMonths.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        month = option
                        monthExpanded = false
                    })
                }
            }
        }
        InfoCard("جمع نهایی ماه $month", monthTotal.entries.joinToString(" | ") { "${it.value} ${it.key}" }.ifBlank { "بدون داده" }, "بر اساس $recordedDays روز ثبت‌شده")
        Text("میانگین روزهای ثبت‌شده")
        monthTotal.forEach { (unit, total) -> Text("${if (recordedDays == 0) 0 else total / recordedDays} $unit در روز") }
        Text("گزارش هفتگی", fontWeight = FontWeight.Bold)
        if (records.isEmpty()) Text("برای این ماه هنوز داده‌ای ثبت نشده است.", color = Color(0xFF64748B))
        weeklyTotals.forEach { (unit, weeks) ->
            Text(unit, fontWeight = FontWeight.Bold)
            weeks.toSortedMap().forEach { (week, total) -> Text("$week: $total $unit") }
        }
        Text("جزئیات روزانه", fontWeight = FontWeight.Bold)
        dailyTotals.forEach { (unit, days) ->
            days.toSortedMap().forEach { (date, total) -> Text("$date: $total $unit") }
        }
        Button(
            onClick = { saveMonthlyMilkPdf(context, month, monthTotal, recordedDays, weeklyTotals, dailyTotals) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ذخیره گزارش ماهانه به‌صورت PDF") }
    }
}

@Composable
private fun Calendar(data: FarmData, onAdd: () -> Unit, onEdit: (TaskRecord) -> Unit, onDelete: (Long) -> Unit) {
    DetailHeader("تقویم و کارها", "عنوان کار و تاریخ انجام را جداگانه ثبت کنید")
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت کار جدید") }
    if (data.taskList.isEmpty()) Text("هنوز کاری ثبت نشده است.", color = Color(0xFF64748B))
    data.taskList.forEach { task ->
        RecordCard(
            title = task.title,
            lines = listOf("تاریخ انجام: ${task.dueDate}", "توضیحات: ${task.note}"),
            onEdit = { onEdit(task) }, onDelete = { onDelete(task.id) }
        )
    }
}

@Composable
private fun Health(data: FarmData, onAdd: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    DetailHeader("سلامت دام‌ها", "واکسیناسیون، معاینه و پیگیری بیماری‌ها")
    InfoCard("موارد نیازمند بررسی", data.healthChecks.toString(), "دامپزشک را مطلع کنید")
    InfoCard("واکسیناسیون بعدی", "۱۵ مهر", "گوساله‌های جوان")
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت معاینه یا واکسن") }
    EditDelete(onEdit, onDelete)
}

@Composable
private fun Inventory(data: FarmData, onAdd: () -> Unit, onEdit: (InventoryRecord) -> Unit, onDelete: (Long) -> Unit) {
    val context = LocalContext.current
    val currentMonth = persianDate().substringBeforeLast("/")
    val monthRecords = data.inventoryList.filter { it.date.startsWith("$currentMonth/") }
    val monthIn = monthRecords.filter { it.movement == "ورود" }.sumOf { it.totalWeight }
    val monthOut = monthRecords.filter { it.movement == "خروج" }.sumOf { it.totalWeight }
    DetailHeader("انبارداری", "ورود و خروج محصولات و خوراک با گزارش روزانه و ماهانه")
    InfoCard("موجودی فعلی", "${data.inventory} کیلو", "محاسبه‌شده از ورود و خروج ثبت‌شده")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        InfoCard("ورود این ماه", "$monthIn کیلو", "${monthRecords.count { it.movement == "ورود" }} تراکنش")
        InfoCard("خروج این ماه", "$monthOut کیلو", "${monthRecords.count { it.movement == "خروج" }} تراکنش")
    }
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت ورود یا خروج") }
    if (monthRecords.isEmpty()) {
        Text("برای این ماه هنوز گزارشی ثبت نشده است.", color = Color(0xFF64748B))
    } else {
        SectionCard("گزارش ماه $currentMonth", Icons.Default.BarChart) {
            Button(onClick = { saveInventoryPdf(context, currentMonth, monthRecords) }, modifier = Modifier.fillMaxWidth()) {
                Text("ذخیره آمار انبار این ماه به‌صورت PDF")
            }
            Text("آمار روزانه", fontWeight = FontWeight.Bold)
            monthRecords.groupBy { it.date }.toSortedMap().forEach { (date, records) ->
                val incoming = records.filter { it.movement == "ورود" }.sumOf { it.totalWeight }
                val outgoing = records.filter { it.movement == "خروج" }.sumOf { it.totalWeight }
                Text("$date | ورود: $incoming کیلو | خروج: $outgoing کیلو")
            }
            Text("آمار کلی ماه: ورود $monthIn کیلو | خروج $monthOut کیلو | خالص ${monthIn - monthOut} کیلو", fontWeight = FontWeight.Bold)
        }
    }
    if (data.inventoryList.isEmpty()) Text("هنوز تراکنش انبار ثبت نشده است.", color = Color(0xFF64748B))
    data.inventoryList.sortedByDescending { it.id }.forEach { record ->
        RecordCard(
            title = "${record.movement} | ${record.product} | ${record.date}",
            lines = listOf("تعداد کیسه: ${record.bags}", "وزن کلی: ${record.totalWeight} کیلو", "توضیحات: ${record.note.ifBlank { "بدون توضیح" }}"),
            onEdit = { onEdit(record) },
            onDelete = { onDelete(record.id) }
        )
    }
}

@Composable
private fun Sales(data: FarmData, onAdd: () -> Unit, onEdit: (InvoiceRecord) -> Unit, onDelete: (Long) -> Unit) {
    DetailHeader("فروش محصولات", "ثبت فروش کامل و مدیریت فهرست فاکتورها")
    InfoCard("تعداد فاکتورها", data.invoiceList.size.toString(), "فاکتورهای ذخیره‌شده")
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ ثبت فاکتور جدید") }
    if (data.invoiceList.isEmpty()) Text("هنوز فاکتوری ثبت نشده است.", color = Color(0xFF64748B))
    data.invoiceList.forEach { invoice ->
        RecordCard(
            title = "${invoice.code} | ${invoice.customer}",
            lines = listOf("محصول: ${invoice.product}", "تعداد: ${invoice.quantity}", "قیمت واحد: ${invoice.unitPrice} تومان", "مبلغ کل: ${invoice.total} تومان", "تاریخ: ${invoice.date}", "یادداشت: ${invoice.note}"),
            onEdit = { onEdit(invoice) }, onDelete = { onDelete(invoice.id) }
        )
    }
    Text("فاکتور پس از ذخیره به‌صورت PDF در پوشه Documents برنامه ذخیره می‌شود.", color = Color(0xFF64748B), fontSize = 13.sp)
}

@Composable
private fun EditDelete(onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("ویرایش") }
        TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("حذف") }
    }
}

@Composable
private fun RecordCard(title: String, lines: List<String>, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            lines.forEach { Text(it, color = Color(0xFF475569), fontSize = 13.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("ویرایش") }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("حذف") }
            }
        }
    }
}

@Composable
private fun InvoiceDialog(existing: InvoiceRecord?, onDismiss: () -> Unit, onSave: (InvoiceRecord) -> Unit) {
    var customer by remember { mutableStateOf(existing?.customer ?: "") }
    var product by remember { mutableStateOf(existing?.product ?: "") }
    var quantity by remember { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var unitPrice by remember { mutableStateOf(existing?.unitPrice?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    val code = existing?.code ?: "INV-${persianDate().replace("/", "")}-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"
    val total = (quantity.toIntOrNull() ?: 0) * (unitPrice.toIntOrNull() ?: 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت فاکتور فروش" else "ویرایش فاکتور ${existing.code}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard("کد فاکتور", code, "توسط برنامه ساخته شده و تکراری نیست")
                OutlinedTextField(customer, { customer = it }, label = { Text("نام مشتری") }, singleLine = true)
                OutlinedTextField(product, { product = it }, label = { Text("نام محصول") }, singleLine = true)
                OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("تعداد") }, singleLine = true)
                OutlinedTextField(unitPrice, { unitPrice = it.filter(Char::isDigit) }, label = { Text("قیمت واحد (تومان)") }, singleLine = true)
                Text("مبلغ کل: $total تومان", fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                OutlinedTextField(note, { note = it }, label = { Text("توضیحات") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (customer.isNotBlank() && product.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0) {
                    onSave(InvoiceRecord(existing?.id ?: System.currentTimeMillis(), code, customer, product, quantity.toInt(), unitPrice.toIntOrNull() ?: 0, total, persianDate(), note))
                }
            }) { Text("ذخیره و ساخت PDF") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryDialog(existing: InventoryRecord?, onDismiss: () -> Unit, onSave: (InventoryRecord) -> Unit) {
    val products = listOf("ذرت", "جو", "کنسانتره", "یونجه", "کاه", "دارو", "واکسن", "سایر")
    var product by remember { mutableStateOf(existing?.product ?: products.first()) }
    var movement by remember { mutableStateOf(existing?.movement ?: "ورود") }
    var bags by remember { mutableStateOf(existing?.bags?.toString() ?: "") }
    var totalWeight by remember { mutableStateOf(existing?.totalWeight?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var productExpanded by remember { mutableStateOf(false) }
    var movementExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت ورود یا خروج انبار" else "ویرایش تراکنش انبار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = productExpanded, onExpandedChange = { productExpanded = !productExpanded }) {
                    OutlinedTextField(
                        value = product,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("انتخاب محصول") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                        products.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                product = option
                                productExpanded = false
                            })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = movementExpanded, onExpandedChange = { movementExpanded = !movementExpanded }) {
                    OutlinedTextField(
                        value = movement,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع تراکنش") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = movementExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = movementExpanded, onDismissRequest = { movementExpanded = false }) {
                        listOf("ورود", "خروج").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                movement = option
                                movementExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = bags,
                    onValueChange = { bags = it.filter(Char::isDigit) },
                    label = { Text("تعداد کیسه‌ها") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalWeight,
                    onValueChange = { totalWeight = it.filter(Char::isDigit) },
                    label = { Text("وزن کلی تمام کیسه‌ها (کیلو)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(note, { note = it }, label = { Text("توضیحات") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedBags = bags.toIntOrNull() ?: 0
                val parsedWeight = totalWeight.toIntOrNull() ?: 0
                if (parsedBags > 0 && parsedWeight > 0) {
                    onSave(InventoryRecord(existing?.id ?: System.currentTimeMillis(), existing?.date ?: persianDate(), product, movement, parsedBags, parsedWeight, note))
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun TaskDialog(existing: TaskRecord?, onDismiss: () -> Unit, onSave: (TaskRecord) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var dueDate by remember { mutableStateOf(existing?.dueDate ?: persianDate()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت کار جدید" else "ویرایش کار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان کار") }, singleLine = true)
                OutlinedTextField(dueDate, { dueDate = it }, label = { Text("تاریخ انجام شمسی") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("توضیحات") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && dueDate.isNotBlank()) {
                    onSave(TaskRecord(existing?.id ?: System.currentTimeMillis(), title, dueDate, note))
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun IncomeDialog(existing: IncomeRecord?, onDismiss: () -> Unit, onSave: (IncomeRecord) -> Unit) {
    var date by remember { mutableStateOf(existing?.date ?: persianDate()) }
    var category by remember { mutableStateOf(existing?.category ?: "فروش شیر") }
    var source by remember { mutableStateOf(existing?.source ?: "") }
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var method by remember { mutableStateOf(existing?.method ?: "کارت بانکی") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت درآمد" else "ویرایش درآمد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it }, label = { Text("تاریخ شمسی") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("دسته‌بندی درآمد") }, singleLine = true)
                OutlinedTextField(source, { source = it }, label = { Text("منبع درآمد") }, singleLine = true)
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("مبلغ به تومان") }, singleLine = true)
                OutlinedTextField(method, { method = it }, label = { Text("روش دریافت") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("توضیحات و شماره پیگیری") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = amount.toIntOrNull() ?: 0
                if (category.isNotBlank() && source.isNotBlank() && parsedAmount > 0) {
                    onSave(IncomeRecord(existing?.id ?: System.currentTimeMillis(), date, category, source, parsedAmount, method, note))
                }
            }) { Text("ذخیره درآمد") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun EmployeeDialog(existing: EmployeeRecord?, onDismiss: () -> Unit, onSave: (EmployeeRecord) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var hireDate by remember { mutableStateOf(existing?.hireDate ?: persianDate()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت کارمند" else "ویرایش کارمند") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام و نام خانوادگی") }, singleLine = true)
                OutlinedTextField(role, { role = it }, label = { Text("سمت یا وظیفه") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس") }, singleLine = true)
                OutlinedTextField(hireDate, { hireDate = it }, label = { Text("تاریخ استخدام شمسی") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(EmployeeRecord(existing?.id ?: System.currentTimeMillis(), name, role, phone, hireDate))
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimalDialog(existing: AnimalRecord?, onDismiss: () -> Unit, onSave: (AnimalRecord) -> Unit) {
    var tag by remember { mutableStateOf(existing?.tag ?: "") }
    var breed by remember { mutableStateOf(existing?.breed ?: "") }
    var gender by remember { mutableStateOf(existing?.gender ?: "ماده") }
    var birthDate by remember { mutableStateOf(existing?.birthDate ?: persianDate()) }
    var health by remember { mutableStateOf(existing?.health ?: "سالم") }
    var genderExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت دام" else "ویرایش دام") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(tag, { tag = it }, label = { Text("پلاک دام") }, singleLine = true)
                OutlinedTextField(breed, { breed = it }, label = { Text("نژاد") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("جنسیت") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        listOf("ماده", "نر").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                gender = option
                                genderExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(birthDate, { birthDate = it }, label = { Text("تاریخ تولد شمسی") }, singleLine = true)
                OutlinedTextField(health, { health = it }, label = { Text("وضعیت سلامت") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (tag.isNotBlank()) {
                    onSave(AnimalRecord(existing?.id ?: System.currentTimeMillis(), tag, breed, gender, birthDate, health))
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

private fun persianDate(): String {
    val calendar = Calendar.getInstance()
    val (year, month, day) = gregorianToJalali(
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
    )
    return String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
}

private fun gregorianToJalali(gyInput: Int, gmInput: Int, gdInput: Int): Triple<Int, Int, Int> {
    val monthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var gy = gyInput - 1600
    val gm = gmInput - 1
    val gd = gdInput - 1
    var dayNumber = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
    dayNumber += monthDays[gm] + gd
    if (gm > 1 && (gyInput % 4 == 0 && gyInput % 100 != 0 || gyInput % 400 == 0)) dayNumber++
    var jalaliDay = dayNumber - 79
    val cycle = jalaliDay / 12053
    jalaliDay %= 12053
    var jy = 979 + 33 * cycle + 4 * (jalaliDay / 1461)
    jalaliDay %= 1461
    if (jalaliDay >= 366) {
        jy += (jalaliDay - 1) / 365
        jalaliDay = (jalaliDay - 1) % 365
    }
    val jm = if (jalaliDay < 186) 1 + jalaliDay / 31 else 7 + (jalaliDay - 186) / 30
    val jd = 1 + if (jalaliDay < 186) jalaliDay % 31 else (jalaliDay - 186) % 30
    return Triple(jy, jm, jd)
}

private fun notifyFarmAlerts(context: Context, data: FarmData) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "farm_alerts"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(NotificationChannel(channelId, "هشدارهای گاوداری", NotificationManager.IMPORTANCE_HIGH))
    }
    val messages = buildList {
        if (data.healthChecks > 0) add("${data.healthChecks} مورد سلامت دام نیازمند بررسی است")
        if (data.inventory == 0) add("موجودی انبار ثبت نشده یا تمام شده است")
    }
    if (messages.isEmpty()) return
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("هشدار مدیریت گاوداری")
        .setContentText(messages.first())
        .setStyle(NotificationCompat.BigTextStyle().bigText(messages.joinToString("\n")))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    try {
        NotificationManagerCompat.from(context).notify(1001, notification)
    } catch (_: SecurityException) {
        // Permission request is handled by MainActivity on Android 13+.
    }
}

private fun alarmPendingIntent(context: Context, alarmId: Long, message: String = "یادآوری مدیریت گاوداری"): PendingIntent {
    val intent = Intent(context, DailyAlarmReceiver::class.java)
        .setAction(ALARM_ACTION)
        .putExtra("alarm_id", alarmId)
        .putExtra("message", message)
    return PendingIntent.getBroadcast(
        context,
        alarmId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun scheduleDailyAlarm(context: Context, alarm: AlarmRecord) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val nextAlarm = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }
    cancelDailyAlarm(context, alarm.id)
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        nextAlarm.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        alarmPendingIntent(context, alarm.id, alarm.message)
    )
    Toast.makeText(context, "هشدار روزانه تنظیم شد", Toast.LENGTH_SHORT).show()
}

private fun cancelDailyAlarm(context: Context, alarmId: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(alarmPendingIntent(context, alarmId))
}

private fun showAlarmNotification(context: Context, message: String) {
    val channelId = "daily_alarm"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(NotificationChannel(channelId, "هشدارهای روزانه", NotificationManager.IMPORTANCE_HIGH))
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("یادآوری گاوداری")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    try {
        NotificationManagerCompat.from(context).notify(2001, notification)
    } catch (_: SecurityException) {
        // Notification permission is requested by MainActivity on Android 13+.
    }
}

private fun saveInventoryPdf(context: Context, month: String, records: List<InventoryRecord>) {
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    directory.mkdirs()
    val file = File(directory, "inventory-report-$month.pdf")
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val paint = android.graphics.Paint().apply { textSize = 15f; color = android.graphics.Color.DKGRAY }
    var y = 60f
    val incoming = records.filter { it.movement == "ورود" }.sumOf { it.totalWeight }
    val outgoing = records.filter { it.movement == "خروج" }.sumOf { it.totalWeight }
    page.canvas.drawText("Dairy Farm Manager - Inventory Report", 48f, y, paint)
    y += 30f
    page.canvas.drawText("Month: $month", 48f, y, paint)
    y += 26f
    page.canvas.drawText("Monthly total: in $incoming kg | out $outgoing kg | net ${incoming - outgoing} kg", 48f, y, paint)
    y += 38f
    page.canvas.drawText("Daily details", 48f, y, paint)
    y += 26f
    records.groupBy { it.date }.toSortedMap().forEach { (date, dayRecords) ->
        if (y > 790f) return@forEach
        val dayIn = dayRecords.filter { it.movement == "ورود" }.sumOf { it.totalWeight }
        val dayOut = dayRecords.filter { it.movement == "خروج" }.sumOf { it.totalWeight }
        page.canvas.drawText("$date: in $dayIn kg | out $dayOut kg", 64f, y, paint)
        y += 22f
    }
    document.finishPage(page)
    file.outputStream().use { document.writeTo(it) }
    document.close()
    Toast.makeText(context, "گزارش انبار در PDF ذخیره شد", Toast.LENGTH_LONG).show()
}

private fun saveMonthlyMilkPdf(
    context: Context,
    month: String,
    monthTotal: Map<String, Int>,
    recordedDays: Int,
    weeklyTotals: Map<String, Map<String, Int>>,
    dailyTotals: Map<String, Map<String, Int>>
) {
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    directory.mkdirs()
    val file = File(directory, "milk-report-$month.pdf")
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val paint = android.graphics.Paint().apply { textSize = 16f; color = android.graphics.Color.DKGRAY }
    var y = 60f
    page.canvas.drawText("Dairy Farm Manager - Monthly Milk Report", 48f, y, paint)
    y += 36f
    page.canvas.drawText("Month: $month", 48f, y, paint)
    y += 28f
    page.canvas.drawText("Total milk: ${monthTotal.entries.joinToString(" | ") { "${it.value} ${it.key}" }}", 48f, y, paint)
    y += 28f
    page.canvas.drawText("Recorded days: $recordedDays", 48f, y, paint)
    y += 42f
    page.canvas.drawText("Weekly summary", 48f, y, paint)
    y += 28f
    weeklyTotals.forEach { (unit, weeks) ->
        weeks.toSortedMap().forEach { (week, total) ->
            page.canvas.drawText("$week: $total $unit", 64f, y, paint)
            y += 24f
        }
    }
    y += 18f
    page.canvas.drawText("Daily details", 48f, y, paint)
    y += 28f
    dailyTotals.forEach { (unit, days) ->
        days.toSortedMap().forEach { (date, total) ->
            if (y > 800f) return@forEach
            page.canvas.drawText("$date: $total $unit", 64f, y, paint)
            y += 22f
        }
    }
    document.finishPage(page)
    file.outputStream().use { document.writeTo(it) }
    document.close()
    Toast.makeText(context, "گزارش ماهانه در PDF ذخیره شد", Toast.LENGTH_LONG).show()
}

private fun saveInvoicePdf(context: Context, code: String, amount: Int) {
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    directory.mkdirs()
    val file = File(directory, "invoice-$code.pdf")
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val paint = android.graphics.Paint().apply { textSize = 20f; color = android.graphics.Color.DKGRAY }
    page.canvas.drawText("Dairy Farm Manager", 380f, 70f, paint)
    page.canvas.drawText("Invoice: $code", 380f, 120f, paint)
    page.canvas.drawText("Date: ${persianDate()}", 380f, 170f, paint)
    page.canvas.drawText("Product sale total: $amount toman", 380f, 240f, paint)
    page.canvas.drawText("Saved on device", 380f, 300f, paint)
    document.finishPage(page)
    file.outputStream().use { document.writeTo(it) }
    document.close()
    Toast.makeText(context, "فاکتور در فایل PDF ذخیره شد", Toast.LENGTH_LONG).show()
}

@Composable
private fun DetailHeader(title: String, subtitle: String) {
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = Color(0xFF64748B))
}

@Composable
private fun Metric(title: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f))) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 13.sp, color = Color(0xFF475569))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String, detail: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 21.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = Color(0xFFD97706))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDialog(screen: Screen, editing: Boolean, existingMilk: MilkRecord?, onDismiss: () -> Unit, onSave: (Int, String, String) -> Unit) {
    var amount by remember(existingMilk?.id, screen) { mutableStateOf(existingMilk?.amount?.toString() ?: "") }
    var note by remember(existingMilk?.id, screen) { mutableStateOf(existingMilk?.note ?: "") }
    var unit by remember(existingMilk?.id, screen) { mutableStateOf(existingMilk?.unit ?: "لیتر") }
    var unitExpanded by remember { mutableStateOf(false) }
    val needsAmount = screen != Screen.Calendar && screen != Screen.Health
    val amountLabel = when (screen) {
        Screen.Milk -> "مقدار تولید شیر ($unit)"
        Screen.Finance -> "مبلغ (تومان)"
        Screen.Inventory -> "مقدار موجودی"
        Screen.Sales -> "مبلغ فاکتور (تومان)"
        Screen.Animals -> "تعداد دام"
        Screen.Employees -> "تعداد نیرو"
        else -> "مقدار"
    }
    val noteLabel = when (screen) {
        Screen.Milk -> "شیفت یا توضیحات تولید"
        Screen.Finance -> "نوع تراکنش و توضیحات"
        Screen.Inventory -> "نام قلم انبار"
        Screen.Sales -> "کد فاکتور"
        Screen.Calendar -> "عنوان کار و تاریخ انجام"
        Screen.Health -> "شرح معاینه یا واکسن"
        else -> "توضیحات"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت اطلاعات: ${screen.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (needsAmount) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { value ->
                            amount = if (screen == Screen.Milk) value.filter { character -> character in '0'..'9' } else value.filter(Char::isDigit)
                        },
                        label = { Text(amountLabel) },
                        singleLine = true,
                        keyboardOptions = if (screen == Screen.Milk) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default
                    )
                }
                if (screen == Screen.Milk) {
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = !unitExpanded }) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("واحد") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            listOf("لیتر", "کیلو").forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    unit = option
                                    unitExpanded = false
                                })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(noteLabel) },
                    singleLine = true
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(amount.toIntOrNull() ?: 0, note, unit) }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}


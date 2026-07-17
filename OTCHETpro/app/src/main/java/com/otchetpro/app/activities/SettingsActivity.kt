package com.otchetpro.app.activities

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.otchetpro.app.R
import com.otchetpro.app.adapters.*
import com.otchetpro.app.data.*
import com.otchetpro.app.utils.SharedPrefs
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvDeptInfo: TextView
    private lateinit var llSubDepts: LinearLayout
    private lateinit var llTemplates: LinearLayout
    private lateinit var llVars: LinearLayout
    private lateinit var llRecipients: LinearLayout
    private lateinit var etSubDeptNew: EditText
    private lateinit var etRecipientName: EditText
    private lateinit var etRecipientEmail: EditText

    private var dept = ""
    private var subDepts = mutableListOf<String>()
    private var templates = mutableListOf<Template>()
    private var variables = mutableListOf<Variable>()
    private var recipients = mutableListOf<Recipient>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvDeptInfo = findViewById(R.id.tv_dept_info_settings)
        llSubDepts = findViewById(R.id.ll_subdepts_list)
        llTemplates = findViewById(R.id.ll_templates_list)
        llVars = findViewById(R.id.ll_vars_list)
        llRecipients = findViewById(R.id.ll_recipients_list)
        etSubDeptNew = findViewById(R.id.et_subdept_new)
        etRecipientName = findViewById(R.id.et_recipient_name)
        etRecipientEmail = findViewById(R.id.et_recipient_email)

        findViewById<Button>(R.id.btn_close_settings).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_subdept_add).setOnClickListener { addSubDept() }
        findViewById<Button>(R.id.btn_recipient_add).setOnClickListener { addRecipient() }
        findViewById<Button>(R.id.btn_template_add).setOnClickListener { addTemplate() }
        findViewById<Button>(R.id.btn_var_add).setOnClickListener { addVariable() }
        findViewById<Button>(R.id.btn_export).setOnClickListener { exportSettings() }
        findViewById<Button>(R.id.btn_import).setOnClickListener { importSettings() }
        findViewById<Button>(R.id.btn_template_download).setOnClickListener { downloadTemplate() }

        // Аккордеоны
        setupAccordion(R.id.accordion_dept_header, R.id.accordion_dept_body)
        setupAccordion(R.id.accordion_subdepts_header, R.id.accordion_subdepts_body)
        setupAccordion(R.id.accordion_templates_header, R.id.accordion_templates_body)
        setupAccordion(R.id.accordion_vars_header, R.id.accordion_vars_body)
        setupAccordion(R.id.accordion_recipients_header, R.id.accordion_recipients_body)
        setupAccordion(R.id.accordion_export_header, R.id.accordion_export_body)

        // Кнопки смены отдела
        findViewById<Button>(R.id.dept_bpla).setOnClickListener { changeDept("БпЛА") }
        findViewById<Button>(R.id.dept_minomet).setOnClickListener { changeDept("Миномет") }
        findViewById<Button>(R.id.dept_artillery).setOnClickListener { changeDept("Артиллерия") }
        findViewById<Button>(R.id.dept_tanks).setOnClickListener { changeDept("Танки") }

        loadData()
    }

    private fun setupAccordion(headerId: Int, bodyId: Int) {
        val header = findViewById<TextView>(headerId)
        val body = findViewById<LinearLayout>(bodyId)
        header.setOnClickListener {
            if (body.visibility == View.VISIBLE) {
                body.visibility = View.GONE
                header.text = "▶ " + header.text.toString().substring(2)
            } else {
                body.visibility = View.VISIBLE
                header.text = "▼ " + header.text.toString().substring(2)
            }
        }
    }

    private fun changeDept(newDept: String) {
        SharedPrefs.saveDept(this, newDept)
        dept = newDept
        loadData()
    }

    private fun loadData() {
        dept = SharedPrefs.getDept(this)
        tvDeptInfo.text = "Настройки для отдела: $dept"
        
        subDepts = SharedPrefs.getSubDepts(this).toMutableList()
        templates = SharedPrefs.getTemplates(this).filter { it.dept == dept || it.type == "common" }.toMutableList()
        variables = SharedPrefs.getVariables(this).filter { it.dept == dept || it.typeGlobal == "common" }.toMutableList()
        recipients = SharedPrefs.getRecipients(this).toMutableList()

        // Подподразделения
        llSubDepts.removeAllViews()
        val subAdapter = SubDeptAdapter(subDepts) { pos ->
            subDepts.removeAt(pos)
            SharedPrefs.saveSubDepts(this, subDepts)
            loadData()
        }
        for (i in 0 until subDepts.size) {
            val v = LayoutInflater.from(this).inflate(R.layout.item_subdept, null)
            v.findViewById<TextView>(R.id.tv_subdept_name).text = subDepts[i]
            val pos = i
            v.findViewById<Button>(R.id.btn_subdept_delete).setOnClickListener {
                subDepts.removeAt(pos)
                SharedPrefs.saveSubDepts(this, subDepts)
                loadData()
            }
            llSubDepts.addView(v)
        }

        // Шаблоны
        llTemplates.removeAllViews()
        templates.forEachIndexed { i, t ->
            val v = LayoutInflater.from(this).inflate(R.layout.item_template, null)
            v.findViewById<TextView>(R.id.tv_template_name).text = t.name
            v.findViewById<TextView>(R.id.tv_template_preview).text = t.text.take(60) + if (t.text.length > 60) "..." else ""
            v.findViewById<Button>(R.id.btn_template_edit).setOnClickListener { editTemplate(i) }
            v.findViewById<Button>(R.id.btn_template_delete).setOnClickListener {
                templates.removeAt(i)
                SharedPrefs.saveTemplates(this, templates)
                loadData()
            }
            llTemplates.addView(v)
        }

        // Переменные
        llVars.removeAllViews()
        variables.forEachIndexed { i, v ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_variable, null)
            view.findViewById<TextView>(R.id.tv_var_name).text = v.name + if (v.required) " *" else ""
            view.findViewById<TextView>(R.id.tv_var_type).text = VariableTypes.displayNames[v.type] ?: v.type
            view.findViewById<Button>(R.id.btn_var_delete).setOnClickListener {
                variables.removeAt(i)
                SharedPrefs.saveVariables(this, variables)
                loadData()
            }
            llVars.addView(view)
        }

        // Адресная книга
        llRecipients.removeAllViews()
        recipients.forEachIndexed { i, r ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_recipient, null)
            view.findViewById<TextView>(R.id.tv_recipient_name).text = r.name
            view.findViewById<TextView>(R.id.tv_recipient_email).text = r.email
            view.findViewById<Button>(R.id.btn_recipient_delete).setOnClickListener {
                recipients.removeAt(i)
                SharedPrefs.saveRecipients(this, recipients)
                loadData()
            }
            llRecipients.addView(view)
        }

        findViewById<TextView>(R.id.tv_settings_info).text = 
            "${subDepts.size} подподразделений · ${templates.size} шаблонов · ${variables.size} переменных"
        findViewById<TextView>(R.id.tv_settings_tag).text = dept
    }

    private fun addSubDept() {
        val name = etSubDeptNew.text.toString().trim()
        if (name.isEmpty()) { Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show(); return }
        subDepts.add(name)
        SharedPrefs.saveSubDepts(this, subDepts)
        etSubDeptNew.text.clear()
        loadData()
        Toast.makeText(this, "✅ Добавлено", Toast.LENGTH_SHORT).show()
    }

    private fun addRecipient() {
        val name = etRecipientName.text.toString().trim()
        val email = etRecipientEmail.text.toString().trim()
        if (name.isEmpty() || email.isEmpty()) { Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show(); return }
        if (!email.contains("@")) { Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show(); return }
        recipients.add(Recipient(UUID.randomUUID().toString(), name, email, dept))
        SharedPrefs.saveRecipients(this, recipients)
        etRecipientName.text.clear()
        etRecipientEmail.text.clear()
        loadData()
        Toast.makeText(this, "✅ Добавлен", Toast.LENGTH_SHORT).show()
    }

    private fun addTemplate() {
        val nm = EditText(this).apply { hint = "Название" }
        val tx = EditText(this).apply { hint = "Текст с {{Переменная}}"; minLines = 4 }
        val ct = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(nm); addView(tx) }
        AlertDialog.Builder(this).setTitle("Добавить шаблон").setView(ct)
            .setPositiveButton("OK") { _, _ ->
                val name = nm.text.toString().trim()
                val text = tx.text.toString().trim()
                if (name.isEmpty() || text.isEmpty()) { Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                templates.add(Template(UUID.randomUUID().toString(), name, text, "own", dept))
                SharedPrefs.saveTemplates(this, templates)
                loadData()
                Toast.makeText(this, "✅ Шаблон добавлен", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun editTemplate(index: Int) {
        val t = templates[index]
        val nm = EditText(this).apply { setText(t.name) }
        val tx = EditText(this).apply { setText(t.text); minLines = 4 }
        val ct = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(nm); addView(tx) }
        AlertDialog.Builder(this).setTitle("Редактировать шаблон").setView(ct)
            .setPositiveButton("OK") { _, _ ->
                val name = nm.text.toString().trim()
                val text = tx.text.toString().trim()
                if (name.isEmpty() || text.isEmpty()) { Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                templates[index] = t.copy(name = name, text = text)
                SharedPrefs.saveTemplates(this, templates)
                loadData()
                Toast.makeText(this, "✅ Шаблон обновлен", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addVariable() {
        val nm = EditText(this).apply { hint = "Название" }
        val tp = Spinner(this).apply { 
            adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, 
                VariableTypes.all.map { VariableTypes.displayNames[it] ?: it })
        }
        val ck = CheckBox(this).apply { text = "Обязательное" }
        val cm = CheckBox(this).apply { text = "Общая" }
        val ct = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(nm); addView(tp); addView(ck); addView(cm) }
        AlertDialog.Builder(this).setTitle("Добавить переменную").setView(ct)
            .setPositiveButton("OK") { _, _ ->
                val name = nm.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val type = VariableTypes.all[tp.selectedItemPosition]
                variables.add(Variable(
                    UUID.randomUUID().toString(), name, type, ck.isChecked,
                    if (cm.isChecked) "common" else "own",
                    if (cm.isChecked) "" else dept
                ))
                SharedPrefs.saveVariables(this, variables)
                loadData()
                Toast.makeText(this, "✅ Переменная добавлена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun exportSettings() {
        val data = mapOf(
            "dept" to dept,
            "subDepts" to subDepts,
            "templates" to templates,
            "variables" to variables,
            "recipients" to recipients
        )
        try {
            val json = com.google.gson.Gson().toJson(data)
            val file = java.io.File(getExternalFilesDir(null), "настройки_${dept}.json")
            java.io.FileOutputStream(file).use { it.write(json.toByteArray()) }
            Toast.makeText(this, "✅ Выгружено: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importSettings() {
        Toast.makeText(this, "Выберите JSON-файл с настройками", Toast.LENGTH_SHORT).show()
    }

    private fun downloadTemplate() {
        val template = mapOf(
            "_comment" to "Шаблон настроек OTCHETpro",
            "dept" to "НАЗВАНИЕ_ОТДЕЛА",
            "subDepts" to listOf("Подразделение 1", "Подразделение 2"),
            "templates" to listOf(mapOf("name" to "Название", "text" to "Текст с {{Переменная}}")),
            "variables" to listOf(mapOf("name" to "Переменная", "type" to "text", "required" to true)),
            "recipients" to listOf(mapOf("name" to "ФИО", "email" to "email@domain.ru"))
        )
        try {
            val json = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(template)
            val file = java.io.File(getExternalFilesDir(null), "шаблон_настроек.json")
            java.io.FileOutputStream(file).use { it.write(json.toByteArray()) }
            Toast.makeText(this, "✅ Шаблон скачан", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

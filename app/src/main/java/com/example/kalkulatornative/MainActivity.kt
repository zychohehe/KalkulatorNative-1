package com.example.kalkulatornative

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.MathContext

class MainActivity : Activity() {

    private var expr = ""
    private lateinit var tvExpr: TextView
    private lateinit var tvRes: TextView
    private val ops = "+−×÷"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0F1115")
        window.navigationBarColor = Color.parseColor("#0F1115")
        setContentView(R.layout.activity_main)
        tvExpr = findViewById(R.id.tvExpr)
        tvRes = findViewById(R.id.tvRes)
        buildKeys(findViewById(R.id.keys))
        savedInstanceState?.getString("expr")?.let { expr = it; refresh() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("expr", expr)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildKeys(container: LinearLayout) {
        val rows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )
        for (row in rows) {
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
            for (label in row) {
                val weight = if (label == "0") 2f else 1f
                line.addView(makeButton(label, weight))
            }
            container.addView(line)
        }
    }

    private fun makeButton(label: String, weight: Float): Button {
        val (bg, fg) = when {
            label == "=" -> "#4F7CFF" to "#FFFFFF"
            label in listOf("÷", "×", "−", "+") -> "#2B3A67" to "#9DB4FF"
            label in listOf("C", "⌫", "%") -> "#262B36" to "#C7CDD9"
            else -> "#1C2029" to "#FFFFFF"
        }
        return Button(this).apply {
            text = label
            textSize = 26f
            isAllCaps = false
            stateListAnimator = null
            setTextColor(Color.parseColor(fg))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bg))
                cornerRadius = dp(22).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
                setMargins(dp(5), dp(5), dp(5), dp(5))
            }
            setOnClickListener { onKey(label) }
        }
    }

    private fun onKey(k: String) {
        when {
            k == "C" -> expr = ""
            k == "⌫" -> expr = expr.dropLast(1)
            k == "%" -> if (expr.lastOrNull()?.isDigit() == true) expr += "%"
            k == "=" -> {
                val v = evaluate(expr)
                if (v == null) { tvRes.text = "Error"; return }
                expr = format(v).takeIf { it != "Error" } ?: ""
                tvRes.text = format(v)
                tvExpr.text = ""
                fitText()
                return
            }
            k in ops.map { it.toString() } -> {
                if (expr.isEmpty()) { if (k == "−") expr = "−" }
                else if (ops.contains(expr.last())) expr = expr.dropLast(1) + k
                else expr += k
            }
            k == "." -> {
                val last = expr.split('+', '−', '×', '÷').last()
                if (!last.contains('.')) expr += if (last.isEmpty()) "0." else "."
            }
            else -> expr += k
        }
        refresh()
    }

    private fun refresh() {
        tvExpr.text = expr
        if (expr.isEmpty()) { tvRes.text = "0"; fitText(); return }
        val t = if (ops.contains(expr.last())) expr.dropLast(1) else expr
        evaluate(t)?.let { tvRes.text = format(it) }
        fitText()
    }

    private fun fitText() {
        val n = tvRes.text.length
        tvRes.textSize = when {
            n > 13 -> 30f
            n > 10 -> 40f
            n > 7 -> 52f
            else -> 64f
        }
    }

    private fun format(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        return BigDecimal(v).round(MathContext(12)).stripTrailingZeros().toPlainString()
    }

    private fun evaluate(s: String): Double? =
        try { Parser(s).parse() } catch (e: Exception) { null }

    private class Parser(val s: String) {
        var i = 0
        fun parse(): Double {
            val v = expression()
            if (i != s.length) throw IllegalArgumentException()
            return v
        }
        fun expression(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '−')) {
                val o = s[i++]; val t = term()
                v = if (o == '+') v + t else v - t
            }
            return v
        }
        fun term(): Double {
            var v = unary()
            while (i < s.length && (s[i] == '×' || s[i] == '÷')) {
                val o = s[i++]; val t = unary()
                v = if (o == '×') v * t else v / t
            }
            return v
        }
        fun unary(): Double {
            if (i < s.length && s[i] == '−') { i++; return -unary() }
            return postfix()
        }
        fun postfix(): Double {
            var v = number()
            while (i < s.length && s[i] == '%') { i++; v /= 100 }
            return v
        }
        fun number(): Double {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (st == i) throw IllegalArgumentException()
            return s.substring(st, i).toDouble()
        }
    }
}

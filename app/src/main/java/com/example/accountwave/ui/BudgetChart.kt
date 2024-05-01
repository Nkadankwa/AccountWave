package com.example.accountwave.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetChart(modifier: Modifier = Modifier, category: String, limit: Double, spent: Double) {
    val budgetColor = Color.Blue.toArgb()
    val spentColor = Color(0xFFADD8E6).toArgb()

    val entries = entryModelOf(
        listOf(FloatEntry(0f, limit.toFloat())),
        listOf(FloatEntry(0f, spent.toFloat()))
    )

    Row(modifier = modifier.padding(vertical = 8.dp)) {
        Text(text = "$category:", modifier = Modifier.weight(1f))
        Chart(
            modifier = Modifier.weight(2f).fillMaxWidth(),
            chart = columnChart(
                columns = listOf(
                    LineComponent(
                        color = budgetColor,
                        thicknessDp = 12f,
                        shape = Shapes.roundedCornerShape(4)
                    ),
                    LineComponent(
                        color = spentColor,
                        thicknessDp = 12f,
                        shape = Shapes.roundedCornerShape(4)
                    )
                ),
                spacing = 8.dp
            ),
            model = entries,
                 )
        Text(
            text = "${NumberFormat.getCurrencyInstance(Locale("en", "GH")).format(spent)} / ${NumberFormat.getCurrencyInstance(Locale("en", "GH")).format(limit)}",
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetChartPreview() {
    BudgetChart(category = "Food", limit = 1000.0, spent = 750.0)
}
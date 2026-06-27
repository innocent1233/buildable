package com.libraryx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.libraryx.data.model.MonthPaymentStatus
import com.libraryx.data.model.PaymentStatus
import com.libraryx.ui.theme.StudyLabColors

/** Replaces the repeated `<Badge variant=...>{status}</Badge>` pattern across pages. */
@Composable
fun StatusBadge(status: PaymentStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        PaymentStatus.Paid -> StudyLabColors.StatusPaid
        PaymentStatus.Unpaid -> StudyLabColors.StatusUnpaid
        PaymentStatus.Overdue -> StudyLabColors.StatusOverdue
    }
    BadgeChip(text = status.name.uppercase(), color = color, modifier = modifier)
}

@Composable
fun MonthStatusBadge(status: MonthPaymentStatus, modifier: Modifier = Modifier) {
    val (text, color) = when (status) {
        MonthPaymentStatus.Paid -> "PAID" to StudyLabColors.StatusPaid
        MonthPaymentStatus.Unpaid -> "UNPAID" to StudyLabColors.StatusUnpaid
        MonthPaymentStatus.Overdue -> "OVERDUE" to StudyLabColors.StatusOverdue
        MonthPaymentStatus.NotApplicable -> "\u2014" to StudyLabColors.TextMuted
    }
    BadgeChip(text = text, color = color, modifier = modifier)
}

@Composable
fun BadgeChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/** Replaces the recurring stat-summary `<Card>` blocks in Dashboard.tsx/Reports.tsx. */
@Composable
fun StatCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
        }
    }
}

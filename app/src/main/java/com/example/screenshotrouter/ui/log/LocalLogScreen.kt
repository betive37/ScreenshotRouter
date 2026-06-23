package com.example.screenshotrouter.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.LogEntry
import java.text.DateFormat
import java.util.Date

@Composable
fun LocalLogScreen(
    modifier: Modifier = Modifier,
    entries: List<LogEntry>,
    onClear: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.log_privacy_description))
        OutlinedButton(onClick = onClear, enabled = entries.isNotEmpty()) { Text(stringResource(R.string.action_clear_log)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entries.isEmpty()) {
                item { Text(stringResource(R.string.log_empty)) }
            } else {
                items(entries) { entry -> LogEntryCard(entry) }
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(DateFormat.getDateTimeInstance().format(Date(entry.timestampMillis)), fontWeight = FontWeight.Bold)
            Text(entry.message)
        }
    }
}

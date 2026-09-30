package de.konstellarum.synesis.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.konstellarum.synesis.AppModules
import de.konstellarum.synesis.ModuleHost
import de.konstellarum.synesis.core.platform.ModuleCategory

/**
 * Screen for a module category (e.g. IoT): a tab per contained module. Selecting a tab
 * swaps the content; the back arrow returns to the home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: ModuleCategory,
    host: ModuleHost,
    onBack: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val activeModule = category.modules.getOrNull(selectedTab.coerceAtLeast(0))
    val content = activeModule?.let { AppModules.byId(it.id) }?.content

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (category.modules.size > 1) {
                TabRow(selectedTabIndex = selectedTab.coerceAtLeast(0)) {
                    category.modules.forEachIndexed { index, module ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(module.title) },
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                content?.invoke(host)
            }
        }
    }
}

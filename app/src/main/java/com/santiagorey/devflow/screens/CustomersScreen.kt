package com.santiagorey.devflow.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.ui.theme.DevFlowTheme

@Composable
fun CustomersScreen(
    currentUser: FirebaseUser? = null,
    clients: List<Client> = emptyList(),
    projects: List<Project> = emptyList(),
    onTabSelected: (Int) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onClientClick: (clientId: String) -> Unit = {},
    onAddClientClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredClients = clients.filter { client ->
        searchQuery.isBlank() ||
            client.name.contains(searchQuery, ignoreCase = true) ||
            client.company.contains(searchQuery, ignoreCase = true) ||
            client.phone.contains(searchQuery) ||
            client.email.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DevFlowColors.Background,
        bottomBar = {
            DevFlowBottomBar(
                selectedTab = 3, // Pestaña Clientes
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(5.dp)) }

            item {
                TopHeaderBar(
                    currentUser = currentUser,
                    onLogoutClick = onLogoutClick,
                    section = "CLIENTES"
                )
            }

            item {
                HeaderSection(
                    clientCount = clients.size,
                    projectCount = projects.count { it.clientId != null },
                    onAddClientClick = onAddClientClick
                )
            }

            if (clients.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No tienes clientes por el momento",
                        subtitle = "Agrega un cliente con su nombre y número para llamarlo, escribirle y asignarlo a tus proyectos.",
                        actionLabel = "+ Agregar cliente",
                        onAction = onAddClientClick
                    )
                }
            } else {
                item {
                    SearchSection(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it }
                    )
                }

                if (filteredClients.isEmpty()) {
                    item { EmptyClientsCard(searchActive = true) }
                } else {
                    items(
                        items = filteredClients,
                        key = { it.id }
                    ) { client ->
                        ClientCard(
                            client = client,
                            projectCount = projects.count { it.clientId == client.id },
                            onClick = { onClientClick(client.id) },
                            onCall = { ContactActions.call(context, client.phone) },
                            onMessage = { ContactActions.sms(context, client.phone) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomersScreenPreview() {
    DevFlowTheme {
        CustomersScreen()
    }
}

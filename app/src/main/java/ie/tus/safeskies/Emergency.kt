package ie.tus.safeskies

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ie.tus.finalyearproject.Bars.MyBottomBar
import ie.tus.finalyearproject.Bars.MyTopBar

data class EmergencyContact(
    val userID: String,
    val fullName: String,
    val email: String,
    val relationship: String,
    val phoneNumber: String,
    val secondaryPhoneNumber: String = "",
    val address: String = "",
    val notes: String = "",
    val documentID: String
)

@Composable
fun emergencyPage(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val emergencyContacts = remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    val expandedCards = remember { mutableStateOf<Set<String>>(emptySet()) }
    val showAddContactDialog = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            firestore.collection("emergencyContacts")
                .whereEqualTo("userID", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    emergencyContacts.value = documents.map { document ->
                        EmergencyContact(
                            userID = document.getString("userID") ?: "Unknown ID",
                            fullName = document.getString("fullName") ?: "Unknown Name",
                            email = document.getString("email") ?: "Unknown Email",
                            relationship = document.getString("relationship") ?: "Unknown Relationship",
                            phoneNumber = document.getString("phoneNumber") ?: "Unknown Number",
                            secondaryPhoneNumber = document.getString("secondaryPhoneNumber") ?: "",
                            address = document.getString("address") ?: "",
                            notes = document.getString("notes") ?: "",
                            documentID = document.id
                        )
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("Firestore", "Error fetching contacts", error)
                    emergencyContacts.value = emptyList()
                }
        }
    }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                modifier = Modifier.fillMaxWidth()
            )
        },
        bottomBar = {
            MyBottomBar(
                navController = navController,
                modifier = Modifier.fillMaxWidth()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog.value = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", color = Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (emergencyContacts.value.isEmpty()) {
                emptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(emergencyContacts.value) { contact ->
                        val isExpanded = expandedCards.value.contains(contact.documentID)
                        contactCard(
                            contact = contact,
                            isExpanded = isExpanded,
                            onCardClick = {
                                expandedCards.value = if (isExpanded) {
                                    expandedCards.value - contact.documentID
                                } else {
                                    expandedCards.value + contact.documentID
                                }
                            },
                            onDeleteClick = {
                                deleteContact(contact, firestore, emergencyContacts)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddContactDialog.value) {
        addContactDialog(
            onDismiss = { showAddContactDialog.value = false },
            onAddContact = { newContact ->
                currentUser?.let { user ->
                    val userID = user.uid
                    val contactData = mapOf(
                        "userID" to userID,
                        "fullName" to newContact.fullName,
                        "email" to newContact.email,
                        "relationship" to newContact.relationship,
                        "phoneNumber" to newContact.phoneNumber,
                        "secondaryPhoneNumber" to newContact.secondaryPhoneNumber,
                        "address" to newContact.address,
                        "notes" to newContact.notes
                    )
                    firestore.collection("emergencyContacts")
                        .add(contactData)
                        .addOnSuccessListener { documentRef ->
                            val newContactObj = EmergencyContact(
                                userID = userID,
                                fullName = newContact.fullName,
                                email = newContact.email,
                                relationship = newContact.relationship,
                                phoneNumber = newContact.phoneNumber,
                                secondaryPhoneNumber = newContact.secondaryPhoneNumber,
                                address = newContact.address,
                                notes = newContact.notes,
                                documentID = documentRef.id
                            )
                            emergencyContacts.value = emergencyContacts.value + newContactObj
                            showAddContactDialog.value = false
                        }
                        .addOnFailureListener {
                        }
                }
            }
        )
    }
}

@Composable
fun emptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ContactPhone,
            contentDescription = "No Contacts",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No emergency contacts found.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Add a new contact using the + button below.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
fun contactCard(
    contact: EmergencyContact,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initials = contact.fullName
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .take(2)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = contact.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.weight(1f))
                val dropIcon = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown
                Icon(
                    imageVector = dropIcon,
                    contentDescription = "Expand or Collapse"
                )
            }

            if (isExpanded) {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (contact.email.isNotEmpty()) {
                        detailRow(icon = Icons.Default.Email, text = contact.email)
                    }
                    if (contact.relationship.isNotEmpty()) {
                        detailRow(icon = Icons.Default.Favorite, text = contact.relationship)
                    }
                    if (contact.phoneNumber.isNotEmpty()) {
                        detailRow(icon = Icons.Default.Call, text = contact.phoneNumber)
                    }
                    if (contact.secondaryPhoneNumber.isNotEmpty()) {
                        detailRow(icon = Icons.Default.Call, text = contact.secondaryPhoneNumber)
                    }
                    if (contact.address.isNotEmpty()) {
                        detailRow(icon = Icons.Default.Home, text = contact.address)
                    }
                    if (contact.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Notes: ${contact.notes}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "Delete", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun detailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun addContactDialog(
    onDismiss: () -> Unit,
    onAddContact: (EmergencyContact) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var secondaryPhoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var documentID by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Add Contact Icon",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Add Emergency Contact",
                style = MaterialTheme.typography.titleLarge
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Primary Phone Number") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secondaryPhoneNumber,
                    onValueChange = { secondaryPhoneNumber = it },
                    label = { Text("Secondary Phone Number (optional)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (optional)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddContact(
                    EmergencyContact(
                        userID = "",
                        fullName = fullName,
                        email = email,
                        relationship = relationship,
                        phoneNumber = phoneNumber,
                        secondaryPhoneNumber = secondaryPhoneNumber,
                        address = address,
                        notes = notes,
                        documentID = documentID
                    )
                )
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun deleteContact(
    contact: EmergencyContact,
    firestore: FirebaseFirestore,
    emergencyContacts: MutableState<List<EmergencyContact>>
) {
    Log.d("Firestore", "Attempting to delete contact: ${contact.fullName} (ID: ${contact.documentID})")
    firestore.collection("emergencyContacts")
        .document(contact.documentID)
        .delete()
        .addOnSuccessListener {
            Log.d("Firestore", "Successfully deleted contact: ${contact.documentID}")
            emergencyContacts.value = emergencyContacts.value.filter { it.documentID != contact.documentID }
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error deleting contact", exception)
        }
}

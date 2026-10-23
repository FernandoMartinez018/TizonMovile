package com.lfmm.tizonmovile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfmm.tizonmovile.data.model.UiState
import com.lfmm.tizonmovile.ui.theme.TizonColors
import com.lfmm.tizonmovile.viewmodel.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) { onLoginSuccess(); viewModel.resetState() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(TizonColors.BgDark, Color(0xFF0A1510)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = TizonColors.BgCard, modifier = Modifier.size(80.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("🌿", fontSize = 40.sp) }
            }
            Spacer(Modifier.height(20.dp))
            Text("Deteccion de Tizon", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TizonColors.TextPrimary, textAlign = TextAlign.Center)
            Text("Cultivos de Papa", fontSize = 14.sp, color = TizonColors.GreenPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = TizonColors.BgCard) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Iniciar sesion", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TizonColors.TextPrimary)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Usuario", fontSize = 13.sp, color = TizonColors.TextMuted, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = username, onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("admin", color = TizonColors.TextMuted.copy(alpha = 0.5f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TizonColors.GreenPrimary, unfocusedBorderColor = TizonColors.BorderColor,
                                focusedTextColor = TizonColors.TextPrimary, unfocusedTextColor = TizonColors.TextPrimary,
                                cursorColor = TizonColors.GreenPrimary, focusedContainerColor = TizonColors.BgCardDark,
                                unfocusedContainerColor = TizonColors.BgCardDark
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Contrasena", fontSize = 13.sp, color = TizonColors.TextMuted, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("••••••••", color = TizonColors.TextMuted.copy(alpha = 0.5f)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); viewModel.login(username, password) }),
                            trailingIcon = {
                                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(if (passwordVisible) "Ocultar" else "Ver", fontSize = 12.sp, color = TizonColors.GreenPrimary)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TizonColors.GreenPrimary, unfocusedBorderColor = TizonColors.BorderColor,
                                focusedTextColor = TizonColors.TextPrimary, unfocusedTextColor = TizonColors.TextPrimary,
                                cursorColor = TizonColors.GreenPrimary, focusedContainerColor = TizonColors.BgCardDark,
                                unfocusedContainerColor = TizonColors.BgCardDark
                            )
                        )
                    }

                    AnimatedVisibility(visible = uiState is UiState.Error) {
                        Surface(shape = RoundedCornerShape(8.dp), color = TizonColors.RedDanger.copy(alpha = 0.15f)) {
                            Text((uiState as? UiState.Error)?.message ?: "",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = TizonColors.RedDanger, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = { keyboard?.hide(); viewModel.login(username, password) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState !is UiState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TizonColors.GreenPrimary, disabledContainerColor = TizonColors.GreenPrimary.copy(alpha = 0.5f))
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        } else {
                            Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A1510))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("v1.0.0 · Deteccion IA en campo", fontSize = 12.sp, color = TizonColors.TextMuted.copy(alpha = 0.6f))
        }
    }
}
package com.vitor.listatarefas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


// Tela responsável pelo cadastro de uma nova tarefa
@Composable
fun NovaTarefaScreen(
    onSalvar: (String, String) -> Unit,
    onVoltar: () -> Unit
) {

    // Guarda o texto digitado no campo de título
    var titulo by remember {
        mutableStateOf("")
    }

    // Guarda o texto digitado no campo de descrição
    var descricao by remember {
        mutableStateOf("")
    }

    // Organiza os elementos um abaixo do outro
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        // Define o espaço entre os elementos
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Título da tela
        Text(
            text = "Nova tarefa"
        )

        // Campo onde o usuário digita o título
        OutlinedTextField(
            value = titulo,

            // Atualiza a variável quando o usuário digitar
            onValueChange = {
                titulo = it
            },

            // Texto exibido dentro do campo
            label = {
                Text("Título")
            }
        )

        // Campo onde o usuário digita a descrição
        OutlinedTextField(
            value = descricao,

            // Atualiza a variável quando o usuário digitar
            onValueChange = {
                descricao = it
            },

            // Texto exibido dentro do campo
            label = {
                Text("Descrição")
            },

            // Permite escrever várias linhas
            minLines = 3
        )

        // Botão para salvar a tarefa
        Button(
            onClick = {

                // Envia o título e a descrição para a tela principal
                onSalvar(titulo, descricao)
            }
        ) {
            Text("SALVAR")
        }

        // Botão para sair sem salvar
        Button(
            onClick = {

                // Volta para a tela principal
                // e descarta o que foi digitado
                onVoltar()
            }
        ) {
            Text("VOLTAR")
        }
    }
}
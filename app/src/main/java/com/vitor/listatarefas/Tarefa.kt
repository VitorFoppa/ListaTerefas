package com.vitor.listatarefas
import androidx.room.Entity
import androidx.room.PrimaryKey

// Representa uma tarefa dentro do aplicativo
@Entity(tableName = "tarefas")
data class Tarefa(

    // Identificador único da tarefa
    @PrimaryKey val id: Long,

    // Título da tarefa
    val titulo: String,

    // Descrição da tarefa
    val descricao: String,

    // Indica se a tarefa foi concluída
    val concluida: Boolean = false,

    // Define a posição da tarefa dentro do seu grupo
    val ordem: Int
)
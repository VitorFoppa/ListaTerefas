package com.vitor.listatarefas

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow


// ============================================================
// DAO - ACESSO ÀS TAREFAS
// ============================================================

@Dao
interface TarefaDao {

    // ========================================================
    // OBSERVAR TAREFAS
    // ========================================================

    // Flow:
    // sempre que a tabela mudar, a lista é atualizada
    // automaticamente na tela.
    @Query("SELECT * FROM tarefas")
    fun observarTodas(): Flow<List<Tarefa>>


    // ========================================================
    // SALVAR UMA TAREFA
    // ========================================================

    // Insere uma tarefa nova.
    // Se o ID já existir, atualiza a tarefa.
    @Upsert
    suspend fun salvar(tarefa: Tarefa)


    // ========================================================
    // SALVAR VÁRIAS TAREFAS
    // ========================================================

    // Usado principalmente quando várias tarefas
    // têm sua ordem alterada pelo arrastar e soltar.
    @Upsert
    suspend fun salvarVarias(tarefas: List<Tarefa>)


    // ========================================================
    // EXCLUIR UMA TAREFA
    // ========================================================

    // Remove a tarefa do banco de dados.
    @Delete
    suspend fun excluir(tarefa: Tarefa)
}


// ============================================================
// BANCO DE DADOS
// ============================================================

@Database(
    entities = [Tarefa::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Disponibiliza o DAO para o aplicativo
    abstract fun tarefaDao(): TarefaDao


    companion object {

        // Mantém uma única instância do banco
        @Volatile
        private var instancia: AppDatabase? = null


        // Retorna a instância existente ou cria uma nova
        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {

                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tarefas.db"
                )
                    .build()
                    .also {
                        instancia = it
                    }
            }
    }
}
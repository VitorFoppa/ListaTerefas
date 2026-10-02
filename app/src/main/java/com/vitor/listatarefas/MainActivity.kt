package com.vitor.listatarefas

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.vitor.listatarefas.ui.theme.ListaTarefasTheme

import kotlinx.coroutines.launch

import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


// ============================================================
// ACTIVITY PRINCIPAL
// ============================================================

// Activity principal do aplicativo
class MainActivity : ComponentActivity() {

    // Função executada quando o aplicativo é iniciado
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite que o aplicativo utilize toda a área da tela
        enableEdgeToEdge()

        // Define a interface do aplicativo
        setContent {

            // Aplica o tema visual do aplicativo
            ListaTarefasTheme {

                // Inicia a navegação entre as telas
                Navegacao()
            }
        }
    }
}


// ============================================================
// NAVEGAÇÃO
// ============================================================

// Controla a navegação entre as telas do aplicativo
@Composable
fun Navegacao() {

    // Controlador responsável por trocar de uma tela para outra
    val navController = rememberNavController()

    // Acesso ao banco de dados Room
    val context = LocalContext.current

    // Obtém o DAO do banco
    val dao = remember {
        AppDatabase.get(context).tarefaDao()
    }

    // Escopo usado para executar operações no banco
    val scope = rememberCoroutineScope()

    // A lista vem diretamente do Room.
    //
    // Sempre que uma tarefa for criada, alterada,
    // reorganizada ou excluída, o Flow emitirá
    // uma nova lista.
    val tarefas by remember {
        dao.observarTodas()
    }.collectAsState(initial = emptyList())


    // ========================================================
    // ALTERAR CONCLUSÃO
    // ========================================================

    // Altera o estado de conclusão de uma tarefa
    fun alterarConclusao(tarefa: Tarefa) {

        // Verifica para qual grupo a tarefa está indo
        val indoParaConcluidas = !tarefa.concluida

        // A tarefa será colocada no final do grupo de destino
        val novaOrdem = tarefas
            .filter {
                it.concluida == indoParaConcluidas
            }
            .maxOfOrNull {
                it.ordem
            }
            ?.plus(1)
            ?: 0

        // Grava a alteração no banco
        scope.launch {

            dao.salvar(
                tarefa.copy(
                    concluida = indoParaConcluidas,
                    ordem = novaOrdem
                )
            )
        }
    }


    // ========================================================
    // EXCLUIR TAREFA
    // ========================================================

    // Exclui uma tarefa do banco de dados
    fun excluirTarefa(tarefa: Tarefa) {

        // Operações do Room precisam acontecer
        // dentro de uma coroutine.
        scope.launch {

            // Remove a tarefa do banco
            dao.excluir(tarefa)
        }
    }


    // ========================================================
    // NAVEGAÇÃO ENTRE TELAS
    // ========================================================

    NavHost(
        navController = navController,
        startDestination = "principal"
    ) {

        // ----------------------------------------------------
        // TELA PRINCIPAL
        // ----------------------------------------------------

        composable("principal") {

            TelaPrincipal(

                // Lista atual de tarefas
                tarefas = tarefas,

                // Abre a tela de nova tarefa
                onAdicionarTarefa = {
                    navController.navigate("nova_tarefa")
                },

                // Marca ou desmarca uma tarefa
                onAlterarConclusao = { tarefa ->
                    alterarConclusao(tarefa)
                },

                // Reorganiza as tarefas pendentes
                onAlterarOrdem = { tarefasReordenadas ->

                    scope.launch {

                        // Salva no banco as novas posições
                        dao.salvarVarias(tarefasReordenadas)
                    }
                },

                // Exclui uma tarefa
                onExcluirTarefa = { tarefa ->

                    excluirTarefa(tarefa)
                }
            )
        }


        // ----------------------------------------------------
        // TELA DE NOVA TAREFA
        // ----------------------------------------------------

        composable("nova_tarefa") {

            NovaTarefaScreen(

                // Volta sem salvar
                onVoltar = {
                    navController.popBackStack()
                },

                // Recebe os dados da nova tarefa
                onSalvar = { titulo, descricao ->

                    // Descobre a próxima posição
                    // entre as tarefas pendentes
                    val proximaOrdem = tarefas
                        .filter {
                            !it.concluida
                        }
                        .maxOfOrNull {
                            it.ordem
                        }
                        ?.plus(1)
                        ?: 0

                    // Cria a nova tarefa
                    val novaTarefa = Tarefa(

                        // Identificador único
                        id = System.currentTimeMillis(),

                        // Título informado
                        titulo = titulo,

                        // Descrição informada
                        descricao = descricao,

                        // Toda tarefa nova começa pendente
                        concluida = false,

                        // Coloca no final das pendentes
                        ordem = proximaOrdem
                    )

                    // Salva no banco
                    scope.launch {

                        dao.salvar(novaTarefa)
                    }

                    // Volta para a tela principal
                    navController.popBackStack()
                }
            )
        }
    }
}


// ============================================================
// TELA PRINCIPAL
// ============================================================

@Composable
fun TelaPrincipal(
    tarefas: List<Tarefa>,

    // Abre a tela de nova tarefa
    onAdicionarTarefa: () -> Unit,

    // Altera o estado concluída/pendente
    onAlterarConclusao: (Tarefa) -> Unit,

    // Atualiza a ordem das tarefas
    onAlterarOrdem: (List<Tarefa>) -> Unit,

    // Exclui uma tarefa
    onExcluirTarefa: (Tarefa) -> Unit
) {

    // ========================================================
    // CONTROLE DO DIÁLOGO DE EXCLUSÃO
    // ========================================================

    // Guarda qual tarefa o usuário deseja excluir.
    //
    // null = nenhuma exclusão sendo confirmada.
    var tarefaParaExcluir by remember {
        mutableStateOf<Tarefa?>(null)
    }


    // ========================================================
    // SEPARAÇÃO DAS TAREFAS
    // ========================================================

    // Tarefas pendentes
    val tarefasPendentes = tarefas
        .filter {
            !it.concluida
        }
        .sortedBy {
            it.ordem
        }

    // Tarefas concluídas
    val tarefasConcluidas = tarefas
        .filter {
            it.concluida
        }
        .sortedBy {
            it.ordem
        }


    // ========================================================
    // ESTADO DA LISTA
    // ========================================================

    // Controla a posição da LazyColumn
    val lazyListState = rememberLazyListState()


    // ========================================================
    // ARRASTAR E SOLTAR
    // ========================================================

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->

        // Só permite arrastar tarefas pendentes
        if (
            from.index < tarefasPendentes.size &&
            to.index < tarefasPendentes.size
        ) {

            // Cria uma cópia mutável
            val novaOrdem = tarefasPendentes.toMutableList()

            // Remove a tarefa da posição original
            val tarefaMovida = novaOrdem.removeAt(from.index)

            // Coloca na nova posição
            novaOrdem.add(
                to.index,
                tarefaMovida
            )

            // Recalcula a ordem
            val tarefasAtualizadas =
                novaOrdem.mapIndexed { index, tarefa ->

                    tarefa.copy(
                        ordem = index
                    )
                }

            // Envia para o Room
            onAlterarOrdem(tarefasAtualizadas)
        }
    }


    // ========================================================
    // ESTRUTURA DA TELA
    // ========================================================

    Scaffold(

        modifier = Modifier.fillMaxSize(),

        // Botão flutuante "+"
        floatingActionButton = {

            FloatingActionButton(

                onClick = {
                    onAdicionarTarefa()
                }

            ) {

                Text("+")
            }
        }

    ) { innerPadding ->


        // ====================================================
        // CONTEÚDO
        // ====================================================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Verifica se não existem tarefas
            if (tarefas.isEmpty()) {

                // Mensagem central
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Nenhuma tarefa cadastrada"
                    )
                }

            } else {

                // =================================================
                // LISTA
                // =================================================

                LazyColumn(
                    state = lazyListState,

                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {


                    // =============================================
                    // TAREFAS PENDENTES
                    // =============================================

                    items(
                        items = tarefasPendentes,

                        key = {
                                tarefa -> tarefa.id
                        }
                    ) { tarefa ->

                        ReorderableItem(
                            state = reorderableState,
                            key = tarefa.id
                        ) {

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .draggableHandle()
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {


                                    // =================================
                                    // CHECKBOX
                                    // =================================

                                    Checkbox(
                                        checked = tarefa.concluida,

                                        onCheckedChange = {
                                            onAlterarConclusao(tarefa)
                                        }
                                    )


                                    Spacer(
                                        modifier = Modifier.width(12.dp)
                                    )


                                    // =================================
                                    // INFORMAÇÕES
                                    // =================================

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = tarefa.titulo,
                                            style = MaterialTheme
                                                .typography
                                                .titleMedium
                                        )

                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )

                                        Text(
                                            text = tarefa.descricao,
                                            style = MaterialTheme
                                                .typography
                                                .bodyMedium
                                        )
                                    }


                                    // =================================
                                    // BOTÃO EXCLUIR
                                    // =================================

                                    TextButton(

                                        // Não exclui imediatamente.
                                        // Primeiro abre a confirmação.
                                        onClick = {

                                            tarefaParaExcluir = tarefa
                                        }

                                    ) {

                                        Text("🗑️")
                                    }
                                }
                            }
                        }
                    }


                    // =============================================
                    // TAREFAS CONCLUÍDAS
                    // =============================================

                    items(
                        items = tarefasConcluidas,

                        key = {
                                tarefa -> tarefa.id
                        }
                    ) { tarefa ->

                        // Tarefas concluídas não podem
                        // ser arrastadas.
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {


                                // =================================
                                // CHECKBOX
                                // =================================

                                Checkbox(
                                    checked = true,

                                    onCheckedChange = {
                                        onAlterarConclusao(tarefa)
                                    }
                                )


                                Spacer(
                                    modifier = Modifier.width(12.dp)
                                )


                                // =================================
                                // INFORMAÇÕES
                                // =================================

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = tarefa.titulo,

                                        style = MaterialTheme
                                            .typography
                                            .titleMedium,

                                        textDecoration =
                                            TextDecoration.LineThrough
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(
                                        text = tarefa.descricao,

                                        style = MaterialTheme
                                            .typography
                                            .bodyMedium
                                    )
                                }


                                // =================================
                                // BOTÃO EXCLUIR
                                // =================================

                                TextButton(

                                    onClick = {

                                        // Guarda a tarefa que
                                        // o usuário deseja apagar
                                        tarefaParaExcluir = tarefa
                                    }

                                ) {

                                    Text("🗑️")
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // ========================================================
    // DIÁLOGO DE CONFIRMAÇÃO
    // ========================================================

    // Só aparece quando existe uma tarefa
    // aguardando confirmação.
    tarefaParaExcluir?.let { tarefa ->

        AlertDialog(

            // Fecha o diálogo
            onDismissRequest = {

                tarefaParaExcluir = null
            },

            // Título
            title = {

                Text("Excluir tarefa?")
            },

            // Mensagem
            text = {

                Column {

                    Text(
                        text = "Deseja realmente excluir esta tarefa?"
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = tarefa.titulo,
                        style = MaterialTheme
                            .typography
                            .titleMedium
                    )
                }
            },

            // Botão cancelar
            dismissButton = {

                TextButton(

                    onClick = {

                        // Fecha o diálogo
                        // sem excluir nada
                        tarefaParaExcluir = null
                    }

                ) {

                    Text("CANCELAR")
                }
            },

            // Botão excluir
            confirmButton = {

                Button(

                    onClick = {

                        // Exclui a tarefa
                        onExcluirTarefa(tarefa)

                        // Fecha o diálogo
                        tarefaParaExcluir = null
                    }

                ) {

                    Text("EXCLUIR")
                }
            }
        )
    }
}
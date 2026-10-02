package com.vitor.listatarefas

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.animation.core.animateDpAsState

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.TaskAlt

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

@OptIn(ExperimentalMaterial3Api::class)
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

    // Guarda qual tarefa o usuário deseja excluir.
    // null = nenhuma exclusão sendo confirmada.
    var tarefaParaExcluir by remember {
        mutableStateOf<Tarefa?>(null)
    }

    // Tarefas pendentes
    val tarefasPendentes = tarefas
        .filter { !it.concluida }
        .sortedBy { it.ordem }

    // Tarefas concluídas
    val tarefasConcluidas = tarefas
        .filter { it.concluida }
        .sortedBy { it.ordem }

    // Controla a posição da LazyColumn
    val lazyListState = rememberLazyListState()

    // Comportamento da barra superior grande (encolhe ao rolar)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()


    // ========================================================
    // ARRASTAR E SOLTAR
    // ========================================================

    // Como agora existem cabeçalhos dentro da lista, os índices
    // não batem mais com a lista de pendentes. Por isso a busca
    // é feita pela key (id da tarefa).
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->

        val de = tarefasPendentes.indexOfFirst { it.id == from.key }
        val para = tarefasPendentes.indexOfFirst { it.id == to.key }

        // Só permite mover entre tarefas pendentes
        if (de != -1 && para != -1) {

            val nova = tarefasPendentes.toMutableList().apply {
                add(para, removeAt(de))
            }

            // Recalcula a ordem e envia para o Room
            onAlterarOrdem(
                nova.mapIndexed { index, tarefa ->
                    tarefa.copy(ordem = index)
                }
            )
        }
    }


    // ========================================================
    // ESTRUTURA DA TELA
    // ========================================================

    Scaffold(

        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),

        // Barra superior
        topBar = {

            LargeTopAppBar(
                title = {
                    Text(
                        text = "Minhas tarefas",
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },

        // Botão flutuante
        floatingActionButton = {

            ExtendedFloatingActionButton(
                onClick = onAdicionarTarefa,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text("Nova tarefa")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }

    ) { innerPadding ->

        if (tarefas.isEmpty()) {

            // Nenhuma tarefa cadastrada
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                EstadoVazio()
            }

        } else {

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),

                // O espaço inferior evita que o botão flutuante
                // cubra o último item da lista
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp
                ),

                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // =============================================
                // TAREFAS PENDENTES
                // =============================================

                if (tarefasPendentes.isNotEmpty()) {

                    item(key = "header_pendentes") {
                        SecaoTitulo("Pendentes · ${tarefasPendentes.size}")
                    }

                    items(
                        items = tarefasPendentes,
                        key = { tarefa -> tarefa.id }
                    ) { tarefa ->

                        ReorderableItem(
                            state = reorderableState,
                            key = tarefa.id
                        ) { isDragging ->

                            // Sombra maior enquanto o item é arrastado
                            val elevacao by animateDpAsState(
                                targetValue = if (isDragging) 8.dp else 1.dp,
                                label = "elevacao"
                            )

                            TarefaItem(
                                tarefa = tarefa,
                                elevation = elevacao,

                                // Alça dedicada para arrastar
                                dragHandle = {
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.draggableHandle()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Reordenar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },

                                onAlterarConclusao = {
                                    onAlterarConclusao(tarefa)
                                },

                                // Não exclui imediatamente.
                                // Primeiro abre a confirmação.
                                onExcluir = {
                                    tarefaParaExcluir = tarefa
                                }
                            )
                        }
                    }
                }


                // =============================================
                // TAREFAS CONCLUÍDAS
                // =============================================

                if (tarefasConcluidas.isNotEmpty()) {

                    item(key = "header_concluidas") {
                        SecaoTitulo("Concluídas · ${tarefasConcluidas.size}")
                    }

                    items(
                        items = tarefasConcluidas,
                        key = { tarefa -> tarefa.id }
                    ) { tarefa ->

                        // Tarefas concluídas não podem ser arrastadas
                        TarefaItem(
                            tarefa = tarefa,
                            onAlterarConclusao = {
                                onAlterarConclusao(tarefa)
                            },
                            onExcluir = {
                                tarefaParaExcluir = tarefa
                            }
                        )
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

            title = {
                Text("Excluir tarefa?")
            },

            text = {
                Text("\"${tarefa.titulo}\" será removida permanentemente.")
            },

            // Botão cancelar
            dismissButton = {
                TextButton(
                    onClick = {
                        tarefaParaExcluir = null
                    }
                ) {
                    Text("Cancelar")
                }
            },

            // Botão excluir
            confirmButton = {
                Button(
                    onClick = {
                        onExcluirTarefa(tarefa)
                        tarefaParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
                }
            }
        )
    }
}


// ============================================================
// COMPONENTES REUTILIZÁVEIS
// ============================================================

// Card de uma tarefa (usado nas pendentes e nas concluídas)
@Composable
fun TarefaItem(
    tarefa: Tarefa,
    elevation: Dp = 1.dp,
    dragHandle: @Composable () -> Unit = {},
    onAlterarConclusao: () -> Unit,
    onExcluir: () -> Unit
) {

    val colors = MaterialTheme.colorScheme
    val concluida = tarefa.concluida

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Checkbox
            Checkbox(
                checked = concluida,
                onCheckedChange = {
                    onAlterarConclusao()
                }
            )

            // Título e descrição
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {

                Text(
                    text = tarefa.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (concluida) TextDecoration.LineThrough else null,
                    color = if (concluida) colors.onSurfaceVariant else colors.onSurface
                )

                if (tarefa.descricao.isNotBlank()) {
                    Text(
                        text = tarefa.descricao,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Botão excluir
            IconButton(onClick = onExcluir) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Excluir tarefa",
                    tint = colors.onSurfaceVariant
                )
            }

            // Alça de arrastar (vazia nas concluídas)
            dragHandle()
        }
    }
}


// Título de seção da lista ("Pendentes", "Concluídas")
@Composable
private fun SecaoTitulo(texto: String) {

    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = 4.dp,
            top = 12.dp,
            bottom = 2.dp
        )
    )
}


// Tela exibida quando não há nenhuma tarefa
@Composable
private fun EstadoVazio() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Outlined.TaskAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nada por aqui ainda",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Toque em \"Nova tarefa\" para começar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
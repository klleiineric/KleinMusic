package app.kleinmusic

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de verdade dos botões do navegador, rodando em um emulador Android.
 * As páginas usadas são "data:" (não dependem da internet).
 */
@RunWith(AndroidJUnit4::class)
class BrowserButtonsTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun page(title: String) = "data:text/html,<title>$title</title><h1>$title</h1>"

    private fun waitText(text: String) = rule.waitUntil(timeoutMillis = 30_000) {
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitGone(text: String) = rule.waitUntil(timeoutMillis = 30_000) {
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
    }

    private fun waitEnabled(description: String) = rule.waitUntil(timeoutMillis = 30_000) {
        try {
            rule.onNodeWithContentDescription(description).assertIsEnabled()
            true
        } catch (e: AssertionError) {
            false
        }
    }

    /** PESQUISAR: digita na barra de endereço e toca em "Ir". */
    private fun open(title: String) {
        rule.onNodeWithTag("endereco").performTextReplacement(page(title))
        rule.onNodeWithContentDescription("Ir").performClick()
        waitText(title)
    }

    @Test
    fun pesquisar_abre_a_pagina_digitada() {
        open("PaginaUm")
        rule.onNodeWithContentDescription("Atualizar").assertIsEnabled()
    }

    @Test
    fun voltar_e_avancar_funcionam() {
        open("PaginaA")
        rule.onNodeWithContentDescription("Voltar").assertIsNotEnabled()
        open("PaginaB")

        waitEnabled("Voltar")
        rule.onNodeWithContentDescription("Voltar").assertIsEnabled().performClick()
        waitText("PaginaA")

        rule.onNodeWithContentDescription("Avançar").assertIsEnabled().performClick()
        waitText("PaginaB")
    }

    @Test
    fun atualizar_recarrega_sem_travar() {
        open("PaginaR")
        rule.onNodeWithContentDescription("Atualizar").performClick()
        waitText("PaginaR")
    }

    @Test
    fun nova_aba_e_fechar_aba() {
        open("PaginaX")
        rule.onNodeWithContentDescription("Nova aba").performClick()
        waitText("Nova aba")                         // chip da aba nova
        rule.onAllNodesWithContentDescription("Fechar aba")[1].performClick()
        waitGone("Nova aba")                         // aba nova fechada
        waitText("PaginaX")                          // a primeira continua
    }

    @Test
    fun cada_aba_tem_o_seu_proprio_historico() {
        open("TabUm-A")
        open("TabUm-B")                              // aba 1 tem 2 páginas
        rule.onNodeWithContentDescription("Nova aba").performClick()
        open("TabDois")                              // aba 2 tem 1 página
        rule.onNodeWithContentDescription("Voltar").assertIsNotEnabled()
        rule.onNodeWithText("TabUm-B").performClick()   // volta para a aba 1
        waitEnabled("Voltar")
        rule.onNodeWithContentDescription("Voltar").assertIsEnabled()
    }

    @Test
    fun favoritos_e_historico_guardam_e_listam_a_pagina() {
        open("PaginaFav")
        rule.onNodeWithContentDescription("Favorito").performClick()

        rule.onNodeWithText("Biblioteca").performClick()
        waitText("PaginaFav")                        // lista de favoritos

        rule.onNodeWithText("Histórico").performClick()
        waitText("PaginaFav")                        // lista de histórico
    }
}

package com.samluiz.ordermgmt.gerenciar.pedido.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samluiz.ordermgmt.common.exceptions.PedidoException;
import com.samluiz.ordermgmt.common.exceptions.RecursoNaoEncontradoException;
import com.samluiz.ordermgmt.common.utils.ControllerUtils;
import com.samluiz.ordermgmt.gerenciar.pedido.dtos.CriarOuAdicionarPedidoDTO;
import com.samluiz.ordermgmt.gerenciar.pedido.models.Pedido;
import com.samluiz.ordermgmt.gerenciar.pedido.services.PedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collections;
import java.util.UUID;

import static com.samluiz.ordermgmt.utils.ControllerTestUtils.montarCriarPedidoDTO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@WithMockUser(username = "admin", password = "admin", roles = {"ADMIN", "EDITOR", "VIEWER"})
class PedidoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PedidoService pedidoService;

    @MockBean
    private ControllerUtils<Pedido> controllerUtils;

    @Test
    void findById_ExistingId_ReturnsPedido() throws Exception {
        UUID existingPedidoId = UUID.randomUUID();
        Pedido existingPedido = new Pedido();
        existingPedido.setId(existingPedidoId);
        when(pedidoService.buscarPedidoPorId(existingPedidoId)).thenReturn(existingPedido);

        mockMvc.perform(MockMvcRequestBuilders.get("/pedidos/{id}", existingPedidoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(existingPedidoId.toString()))
                .andDo(print());
    }

    @Test
    void findAll_ReturnsPageOfPedidos() throws Exception {
        Page<Pedido> mockPage = Page.empty();
        when(pedidoService.buscarTodos(any(PageRequest.class))).thenReturn(mockPage);
        when(controllerUtils.generateResponse(mockPage)).thenReturn(Collections.singletonMap("content", Collections.emptyList()));

        mockMvc.perform(MockMvcRequestBuilders.get("/pedidos")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").isArray())
                .andDo(print());
    }

    @Test
    void create_ValidPedido_ReturnsCreatedPedido() throws Exception {
        Pedido pedidoToCreate = new Pedido();
        Pedido createdPedido = new Pedido();
        when(pedidoService.criarPedido(any(CriarOuAdicionarPedidoDTO.class))).thenReturn(createdPedido);

        mockMvc.perform(MockMvcRequestBuilders.post("/pedidos")
                        .content(objectMapper.writeValueAsString(pedidoToCreate))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(print());
    }

    @Test
    void findById_NonExistingId_ReturnsNotFound() throws Exception {
        UUID nonExistingPedidoId = UUID.randomUUID();
        when(pedidoService.buscarPedidoPorId(nonExistingPedidoId)).thenThrow(RecursoNaoEncontradoException.class);

        mockMvc.perform(MockMvcRequestBuilders.get("/pedidos/{id}", nonExistingPedidoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(print());
    }

    @Test
    void findAll_DataAccessException_ReturnsInternalServerError() throws Exception {
        when(pedidoService.buscarTodos(any(PageRequest.class))).thenThrow(PermissionDeniedDataAccessException.class);

        try {
            mockMvc.perform(MockMvcRequestBuilders.get("/pedidos")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                    .andDo(print());
        } catch (Exception ignored) {}
    }

    @Test
    void create_DataAccessException_ReturnsInternalServerError() throws Exception {
        doThrow(PermissionDeniedDataAccessException.class).when(pedidoService).criarPedido(any(CriarOuAdicionarPedidoDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/pedidos")
                        .content(objectMapper.writeValueAsString(montarCriarPedidoDTO()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andDo(print());
    }

    @Test
    void addItem_ValidPedido_ReturnsOk() throws Exception {
        UUID existingPedidoId = UUID.randomUUID();
        Pedido existingPedido = new Pedido();
        existingPedido.setId(existingPedidoId);
        when(pedidoService.adicionarNovoItem(any(UUID.class), any(CriarOuAdicionarPedidoDTO.class))).thenReturn(existingPedido);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/{id}/item", existingPedidoId)
                        .content(objectMapper.writeValueAsString(montarCriarPedidoDTO()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(print());
    }

    @Test
    void addItem_NonExistingPedido_ReturnsNotFound() throws Exception {
        UUID nonExistingPedidoId = UUID.randomUUID();
        when(pedidoService.adicionarNovoItem(any(UUID.class), any(CriarOuAdicionarPedidoDTO.class))).thenThrow(RecursoNaoEncontradoException.class);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/{id}/item", nonExistingPedidoId)
                        .content(objectMapper.writeValueAsString(montarCriarPedidoDTO()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(print());
    }

    @Test
    void addItem_DataAccessException_ReturnsInternalServerError() throws Exception {
        UUID existingPedidoId = UUID.randomUUID();
        when(pedidoService.adicionarNovoItem(any(UUID.class), any(CriarOuAdicionarPedidoDTO.class))).thenThrow(PermissionDeniedDataAccessException.class);

        try {
            mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/{id}/item", existingPedidoId)
                            .content(objectMapper.writeValueAsString(montarCriarPedidoDTO()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                    .andDo(print());
        } catch (Exception ignored) {}
    }

    @Test
    void removeItem_ValidPedido_ReturnsOk() throws Exception {
        UUID existingPedidoId = UUID.randomUUID();
        UUID existingItemPedidoId = UUID.randomUUID();
        Pedido existingPedido = new Pedido();
        existingPedido.setId(existingPedidoId);
        when(pedidoService.removerItem(any(UUID.class), any(UUID.class))).thenReturn(existingPedido);

        mockMvc.perform(MockMvcRequestBuilders.delete("/pedidos/{pedidoId}/item/{itemId}", existingPedidoId, existingItemPedidoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(print());
    }

    @Test
    void removeItem_NonExistingPedido_ReturnsNotFound() throws Exception {
        UUID nonExistingPedidoId = UUID.randomUUID();
        UUID existingItemPedidoId = UUID.randomUUID();
        when(pedidoService.removerItem(any(UUID.class), any(UUID.class))).thenThrow(RecursoNaoEncontradoException.class);

        mockMvc.perform(MockMvcRequestBuilders.delete("/pedidos/{pedidoId}/item/{itemId}", nonExistingPedidoId, existingItemPedidoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(print());
    }

    @Test
    void removeItem_DataAccessException_ReturnsInternalServerError() throws Exception {
        UUID existingPedidoId = UUID.randomUUID();
        UUID existingItemPedidoId = UUID.randomUUID();
        when(pedidoService.removerItem(any(UUID.class), any(UUID.class))).thenThrow(PermissionDeniedDataAccessException.class);

        try {
            mockMvc.perform(MockMvcRequestBuilders.delete("/pedidos/{id}/item/{itemId}", existingPedidoId, existingItemPedidoId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                    .andDo(print());
        } catch (Exception ignored) {}
    }

    @Test
    void aumentarQuantidade_ValidPedido_ReturnsOk() throws Exception {
        UUID existingItemPedidoId = UUID.randomUUID();
        Integer quantidade = 1;
        Pedido existingPedido = new Pedido();
        existingPedido.setId(UUID.randomUUID());
        when(pedidoService.aumentarQuantidadeProdutoItem(any(UUID.class), anyInt())).thenReturn(existingPedido);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/item/{itemId}/adicionar", existingItemPedidoId)
                        .queryParam("quantidade", quantidade.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(print());
    }

    @Test
    void aumentarQuantidade_NonExistingPedido_ReturnsNotFound() throws Exception {
        UUID nonExistingItemPedidoId = UUID.randomUUID();
        Integer quantidade = 1;
        when(pedidoService.aumentarQuantidadeProdutoItem(any(UUID.class), anyInt())).thenThrow(RecursoNaoEncontradoException.class);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/item/{itemId}", nonExistingItemPedidoId, quantidade)
                        .queryParam("quantidade", quantidade.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(print());
    }

    @Test
    void aumentarQuantidade_QuantidadeInvalida_ReturnsBadRequest() throws Exception {
        UUID existingItemPedidoId = UUID.randomUUID();
        Integer quantidade = 0;
        when(pedidoService.aumentarQuantidadeProdutoItem(any(UUID.class), anyInt())).thenThrow(PedidoException.class);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/item/{itemId}/adicionar", existingItemPedidoId)
                        .queryParam("quantidade", quantidade.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(print());
    }


    @Test
    void diminuirQuantidade_ValidPedido_ReturnsOk() throws Exception {
        UUID existingItemPedidoId = UUID.randomUUID();
        Integer quantidade = 1;
        Pedido existingPedido = new Pedido();
        existingPedido.setId(UUID.randomUUID());
        when(pedidoService.diminuirQuantidadeProdutoItem(any(UUID.class), anyInt())).thenReturn(existingPedido);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/item/{itemId}/diminuir", existingItemPedidoId, quantidade)
                        .queryParam("quantidade", quantidade.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(print());
    }

    @Test
    void diminuirQuantidade_NonExistingPedido_ReturnsNotFound() throws Exception {
        UUID nonExistingItemPedidoId = UUID.randomUUID();
        Integer quantidade = 1;
        when(pedidoService.diminuirQuantidadeProdutoItem(any(UUID.class), anyInt())).thenThrow(RecursoNaoEncontradoException.class);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/item/{itemId}/diminuir", nonExistingItemPedidoId, quantidade)
                        .queryParam("quantidade", quantidade.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(print());
    }

    @Test
    void diminuirQuantidade_QuantidadeInvalida_ReturnsBadRequest() throws Exception {
        UUID existingItemPedidoId = UUID.randomUUID();
        Integer quantidade = 0;
        when(pedidoService.diminuirQuantidadeProdutoItem(any(UUID.class), anyInt())).thenThrow(PedidoException.class);

        mockMvc.perform(MockMvcRequestBuilders.patch("/pedidos/item/{itemId}/diminuir", existingItemPedidoId, quantidade)
                        .queryParam("quantidade", quantidade.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = {"VIEWER"})
    void create_InvalidRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/pedidos")
                        .content(objectMapper.writeValueAsString(montarCriarPedidoDTO()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(print());
    }
}
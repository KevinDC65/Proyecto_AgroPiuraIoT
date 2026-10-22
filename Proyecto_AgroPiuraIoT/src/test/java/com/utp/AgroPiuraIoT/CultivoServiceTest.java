package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.service.CultivoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CultivoServiceTest {

    @Autowired
    private CultivoService cultivos;

    private CultivoForm formulario() {
        return new CultivoForm("Mango - Parcela A", "mango", "San Lorenzo",
                new java.math.BigDecimal("3.20"), null, null);
    }

    @Test
    void guardaYLista() {
        Cultivo creado = cultivos.guardar(formulario());
        assertTrue(creado.getIdCultivo() != null);
        assertFalse(cultivos.listar().isEmpty());
    }

    @Test
    void actualizaYElimina() {
        Cultivo creado = cultivos.guardar(formulario());
        cultivos.actualizar(creado.getIdCultivo(),
                new CultivoForm("Mango - Parcela A2", "mango", "Tambogrande",
                        new java.math.BigDecimal("4.50"), null, null));
        assertEquals("Tambogrande", cultivos.buscar(creado.getIdCultivo()).getUbicacion());
        cultivos.eliminar(creado.getIdCultivo());
        assertTrue(cultivos.listar().stream()
                .noneMatch(c -> c.getIdCultivo().equals(creado.getIdCultivo())));
    }

    @Test
    void registraYAsignaDispositivo() {
        Cultivo creado = cultivos.guardar(formulario());
        DispositivoIoT nodo = cultivos.registrarDispositivo(creado.getIdCultivo());
        assertTrue(nodo.getIdDispositivo() != null);
        assertEquals("activo", nodo.getEstado());
        assertEquals(1, cultivos.buscar(creado.getIdCultivo()).getDispositivos().size());
    }
}

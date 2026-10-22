package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CultivoService {

    private final CultivoRepository cultivos;
    private final DispositivoIoTRepository dispositivos;

    public CultivoService(CultivoRepository cultivos, DispositivoIoTRepository dispositivos) {
        this.cultivos = cultivos;
        this.dispositivos = dispositivos;
    }

    @Transactional(readOnly = true)
    public List<Cultivo> listar() {
        return cultivos.findAll();
    }

    @Transactional(readOnly = true)
    public Cultivo buscar(Integer id) {
        return cultivos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cultivo no encontrado: " + id));
    }

    @Transactional
    public Cultivo guardar(CultivoForm form) {
        Cultivo cultivo = new Cultivo();
        aplicar(form, cultivo);
        return cultivos.save(cultivo);
    }

    @Transactional
    public Cultivo actualizar(Integer id, CultivoForm form) {
        Cultivo cultivo = buscar(id);
        aplicar(form, cultivo);
        return cultivos.save(cultivo);
    }

    @Transactional
    public void eliminar(Integer id) {
        cultivos.delete(buscar(id));
    }

    @Transactional
    public void asignarDispositivo(Integer idCultivo, Integer idDispositivo) {
        Cultivo cultivo = buscar(idCultivo);
        DispositivoIoT dispositivo = dispositivos.findById(idDispositivo)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + idDispositivo));
        cultivo.getDispositivos().add(dispositivo);
    }

    @Transactional
    public DispositivoIoT registrarDispositivo(Integer idCultivo) {
        Cultivo cultivo = buscar(idCultivo);
        DispositivoIoT dispositivo = new DispositivoIoT();
        dispositivo = dispositivos.save(dispositivo);
        cultivo.getDispositivos().add(dispositivo);
        return dispositivo;
    }

    private void aplicar(CultivoForm form, Cultivo cultivo) {
        cultivo.setNombre(form.nombre().trim());
        cultivo.setTipoCultivo(form.tipoCultivo().trim());
        cultivo.setUbicacion(form.ubicacion() == null || form.ubicacion().isBlank()
                ? null : form.ubicacion().trim());
        cultivo.setAreaHa(form.areaHa());
        cultivo.setLatitud(form.latitud());
        cultivo.setLongitud(form.longitud());
    }
}

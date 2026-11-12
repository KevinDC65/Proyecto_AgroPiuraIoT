package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CultivoService {

    private final CultivoRepository cultivos;
    private final DispositivoIoTRepository dispositivos;
    private final UmbralRepository umbrales;
    private final UsuarioCultivoRepository usuarioCultivos;
    private final AlertaRepository alertas;
    private final NotificacionUsuarioRepository notificaciones;
    private final AlertasService alertasService;

    public CultivoService(CultivoRepository cultivos,
                          DispositivoIoTRepository dispositivos,
                          UmbralRepository umbrales,
                          UsuarioCultivoRepository usuarioCultivos,
                          AlertaRepository alertas,
                          NotificacionUsuarioRepository notificaciones,
                          AlertasService alertasService) {
        this.cultivos = cultivos;
        this.dispositivos = dispositivos;
        this.umbrales = umbrales;
        this.usuarioCultivos = usuarioCultivos;
        this.alertas = alertas;
        this.notificaciones = notificaciones;
        this.alertasService = alertasService;
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
        validar(form);
        Cultivo cultivo = new Cultivo();
        aplicar(form, cultivo);
        return cultivos.save(cultivo);
    }

    @Transactional
    public Cultivo actualizar(Integer id, CultivoForm form) {
        validar(form);
        Cultivo cultivo = buscar(id);
        aplicar(form, cultivo);
        return cultivos.save(cultivo);
    }

    @Transactional
    public void eliminar(Integer id) {
        Cultivo cultivo = buscar(id);

        // 1. Invalidar cache de alertas y desvincular dispositivos asociados
        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            alertasService.invalidarCacheUmbrales(dispositivo.getIdDispositivo());
        }
        cultivo.getDispositivos().clear();

        // 2. Eliminar permisos de usuarios al cultivo
        List<UsuarioCultivo> asignaciones = usuarioCultivos.findByCultivoIdCultivo(id);
        if (!asignaciones.isEmpty()) {
            usuarioCultivos.deleteAll(asignaciones);
        }

        // 3. Eliminar alertas y notificaciones asociadas a los umbrales del cultivo
        List<Alerta> alertasCultivo = alertas.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(id);
        for (Alerta a : alertasCultivo) {
            List<NotificacionUsuario> notifs = notificaciones.findByAlertaIdAlerta(a.getIdAlerta());
            if (!notifs.isEmpty()) {
                notificaciones.deleteAll(notifs);
            }
        }
        if (!alertasCultivo.isEmpty()) {
            alertas.deleteAll(alertasCultivo);
        }

        // 4. Eliminar umbrales del cultivo
        List<Umbral> umbralesCultivo = umbrales.findByCultivoIdCultivo(id);
        if (!umbralesCultivo.isEmpty()) {
            umbrales.deleteAll(umbralesCultivo);
        }

        // 5. Eliminar la entidad cultivo
        cultivos.delete(cultivo);
    }

    @Transactional
    public void asignarDispositivo(Integer idCultivo, Integer idDispositivo) {
        Cultivo cultivo = buscar(idCultivo);
        DispositivoIoT dispositivo = dispositivos.findById(idDispositivo)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + idDispositivo));
        cultivo.getDispositivos().add(dispositivo);
        alertasService.invalidarCacheUmbrales(idDispositivo);
    }

    @Transactional
    public void desasignarDispositivo(Integer idCultivo, Integer idDispositivo) {
        Cultivo cultivo = buscar(idCultivo);
        DispositivoIoT dispositivo = dispositivos.findById(idDispositivo)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + idDispositivo));
        cultivo.getDispositivos().remove(dispositivo);
        alertasService.invalidarCacheUmbrales(idDispositivo);
    }

    @Transactional
    public DispositivoIoT registrarDispositivo(Integer idCultivo) {
        Cultivo cultivo = buscar(idCultivo);
        DispositivoIoT dispositivo = new DispositivoIoT();
        dispositivo = dispositivos.save(dispositivo);
        cultivo.getDispositivos().add(dispositivo);
        alertasService.invalidarCacheUmbrales(dispositivo.getIdDispositivo());
        return dispositivo;
    }

    private void validar(CultivoForm form) {
        if (form == null) {
            throw new IllegalArgumentException("El formulario de cultivo no puede ser nulo");
        }
        if (form.nombre() == null || form.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cultivo es obligatorio");
        }
        if (form.tipoCultivo() == null || form.tipoCultivo().isBlank()) {
            throw new IllegalArgumentException("El tipo de cultivo es obligatorio");
        }
        if (form.areaHa() != null && form.areaHa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El área debe ser un valor positivo");
        }
        if (form.latitud() != null && (form.latitud().compareTo(new BigDecimal("-90")) < 0 || form.latitud().compareTo(new BigDecimal("90")) > 0)) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90");
        }
        if (form.longitud() != null && (form.longitud().compareTo(new BigDecimal("-180")) < 0 || form.longitud().compareTo(new BigDecimal("180")) > 0)) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180");
        }
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

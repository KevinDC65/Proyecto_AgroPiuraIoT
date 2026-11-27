package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import com.utp.AgroPiuraIoT.security.TokenNodoService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de cultivos y sus dispositivos (RF-02). La autorización por cultivo se
 * aplica en los controladores ({@code @permisos}); este servicio garantiza que
 * todo cultivo creado desde la web tenga un administrador.
 */
@Service
public class CultivoService {

    private static final Logger log = LoggerFactory.getLogger(CultivoService.class);

    private final CultivoRepository cultivos;
    private final DispositivoIoTRepository dispositivos;
    private final UsuarioRepository usuarios;
    private final UsuarioCultivoRepository accesos;
    private final RolRepository roles;
    private final SensorRepository sensores;
    private final TipoSensorRepository tipos;
    private final AlertasService alertas;
    private final TokenNodoService tokenNodoService;

    public CultivoService(CultivoRepository cultivos, DispositivoIoTRepository dispositivos,
                          UsuarioRepository usuarios, UsuarioCultivoRepository accesos,
                          RolRepository roles, SensorRepository sensores,
                          TipoSensorRepository tipos, AlertasService alertas,
                          TokenNodoService tokenNodoService) {
        this.cultivos = cultivos;
        this.dispositivos = dispositivos;
        this.usuarios = usuarios;
        this.accesos = accesos;
        this.roles = roles;
        this.sensores = sensores;
        this.tipos = tipos;
        this.alertas = alertas;
        this.tokenNodoService = tokenNodoService;
    }

    /** Todos los cultivos (uso interno y pruebas; la web usa {@link #listarPara}). */
    @Transactional(readOnly = true)
    public List<Cultivo> listar() {
        return cultivos.findAll();
    }

    /** Cultivos a los que el usuario tiene acceso, con cualquier rol. */
    @Transactional(readOnly = true)
    public List<Cultivo> listarPara(String correo) {
        return cultivos.buscarPorUsuario(correo);
    }

    /** Ids de los cultivos que el usuario administra (para mostrar acciones en la vista). */
    @Transactional(readOnly = true)
    public Set<Integer> idsAdministradosPor(String correo) {
        return accesos.findByUsuarioCorreo(correo).stream()
                .filter(a -> a.getRol() != null && a.getRol().esAdministrador())
                .map(a -> a.getCultivo().getIdCultivo())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Cultivo buscar(Integer id) {
        return cultivos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cultivo", id));
    }

    /** Crea el cultivo sin asignar accesos (seeder y pruebas). */
    @Transactional
    public Cultivo guardar(CultivoForm form) {
        Cultivo cultivo = new Cultivo();
        aplicar(form, cultivo);
        return cultivos.save(cultivo);
    }

    /**
     * Crea el cultivo y deja a su creador como administrador: sin ello el cultivo
     * quedaría huérfano, invisible para todos y sin nadie que gestione accesos.
     */
    @Transactional
    public Cultivo crear(CultivoForm form, String correoCreador) {
        Usuario creador = usuarios.findByCorreo(correoCreador)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correoCreador));
        Rol administrador = roles.findByNombreRolIgnoreCase(AccesoService.ROL_ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("El rol administrador no está sembrado"));
        Cultivo cultivo = guardar(form);

        UsuarioCultivo acceso = new UsuarioCultivo();
        acceso.setUsuario(creador);
        acceso.setCultivo(cultivo);
        acceso.setRol(administrador);
        accesos.save(acceso);
        log.info("Cultivo {} creado por {}", cultivo.getIdCultivo(), correoCreador);
        return cultivo;
    }

    @Transactional
    public Cultivo actualizar(Integer id, CultivoForm form) {
        Cultivo cultivo = buscar(id);
        aplicar(form, cultivo);
        return cultivos.save(cultivo);
    }

    /**
     * Elimina el cultivo. Umbrales, accesos y la relación con dispositivos se
     * borran en cascada en la BD (ON DELETE CASCADE). Un nodo asignado también a
     * otro cultivo se conserva; el que se queda sin ningún cultivo se elimina con
     * sus sensores, lecturas y alertas para no dejar datos huérfanos.
     */
    @Transactional
    public void eliminar(Integer id) {
        Cultivo cultivo = buscar(id);
        invalidarCacheUmbrales(cultivo);
        int nodosEliminados = dispositivos.eliminarExclusivosDeCultivo(id);
        cultivos.delete(cultivo);
        log.info("Cultivo {} eliminado junto con {} nodo(s) sin otro cultivo", id, nodosEliminados);
    }

    @Transactional
    public void asignarDispositivo(Integer idCultivo, Integer idDispositivo) {
        if (idDispositivo == null) {
            throw new ReglaNegocioException("Indica el ID del nodo a asignar");
        }
        Cultivo cultivo = buscar(idCultivo);
        DispositivoIoT dispositivo = dispositivos.findById(idDispositivo)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No existe un nodo con ID " + idDispositivo));
        if (!cultivo.getDispositivos().add(dispositivo)) {
            throw new ReglaNegocioException(
                    "El nodo #" + idDispositivo + " ya está asignado a este cultivo");
        }
        // Cambian los umbrales que aplican al nodo: el motor de alertas no debe usar los cacheados.
        alertas.invalidarCacheUmbrales(idDispositivo);
    }

    /**
     * Registra un nodo nuevo en el cultivo con un sensor por cada tipo del
     * catálogo (humedad de suelo y ambiente, temperatura, pH), como el nodo
     * ESP32 de referencia. Sin sensores el nodo no podría producir lecturas.
     */
    @Transactional
    public DispositivoIoT registrarDispositivo(Integer idCultivo) {
        Cultivo cultivo = buscar(idCultivo);
        DispositivoIoT dispositivo = dispositivos.save(new DispositivoIoT());
        dispositivo.setTokenHash(tokenNodoService.generarPara(dispositivo.getIdDispositivo()).tokenHash());
        dispositivo = dispositivos.save(dispositivo);
        for (TipoSensor tipo : tipos.findAll()) {
            Sensor sensor = new Sensor();
            sensor.setDispositivo(dispositivo);
            sensor.setTipoSensor(tipo);
            sensores.save(sensor);
        }
        cultivo.getDispositivos().add(dispositivo);
        log.info("Nodo {} registrado en el cultivo {}", dispositivo.getIdDispositivo(), idCultivo);
        return dispositivo;
    }

    private void invalidarCacheUmbrales(Cultivo cultivo) {
        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            alertas.invalidarCacheUmbrales(dispositivo.getIdDispositivo());
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

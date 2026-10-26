package com.utp.AgroPiuraIoT.config;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Usuario administrador de demostración (RF-07); documentado en el README. */
    public static final String ADMIN_DEMO_CORREO = "admin@agropiura.com";
    public static final String ADMIN_DEMO_CLAVE = "AgroAdmin2026";

    @Bean
    ApplicationRunner sembrarCatalogos(RolRepository roles, TipoSensorRepository tipos,
                                       UsuarioRepository usuarios, CultivoRepository cultivos,
                                       UsuarioCultivoRepository accesos,
                                       PasswordEncoder claves) {
        return args -> {
            sembrarRoles(roles);
            sembrarTiposSensor(tipos);
            sembrarAdminDemo(roles, usuarios, cultivos, accesos, claves);
        };
    }

    private void sembrarRoles(RolRepository roles) {
        if (roles.count() == 0) {
            for (String nombre : new String[] {"administrador", "colaborador"}) {
                Rol rol = new Rol();
                rol.setNombreRol(nombre);
                roles.save(rol);
            }
            log.info("Catálogo roles sembrado: administrador, colaborador");
        }
    }

    private void sembrarTiposSensor(TipoSensorRepository tipos) {
        if (tipos.count() == 0) {
            String[][] catalogo = {
                {"humedad_suelo", "%"},
                {"temperatura_ambiente", "°C"},
                {"humedad_ambiente", "%"},
                {"ph_suelo", "pH"}
            };
            for (String[] fila : catalogo) {
                TipoSensor tipo = new TipoSensor();
                tipo.setNombreTipo(fila[0]);
                tipo.setUnidadMedida(fila[1]);
                tipos.save(tipo);
            }
            log.info("Catálogo tipo_sensor sembrado con {} tipos", catalogo.length);
        }
    }

    /**
     * Resuelve el "primer administrador": sin esto, nadie podría asignar accesos
     * (RF-07) porque el ROLE_ADMIN solo se otorga vía usuario_cultivo. Siembra
     * admin@agropiura.com, el cultivo demo y su acceso como administrador.
     */
    private void sembrarAdminDemo(RolRepository roles, UsuarioRepository usuarios,
                                  CultivoRepository cultivos, UsuarioCultivoRepository accesos,
                                  PasswordEncoder claves) {
        if (usuarios.existsByCorreo(ADMIN_DEMO_CORREO)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administrador Demo");
        admin.setCorreo(ADMIN_DEMO_CORREO);
        admin.setContrasena(claves.encode(ADMIN_DEMO_CLAVE));
        usuarios.save(admin);

        Cultivo demo = cultivos.findByNombre("Parcela Demo - Piura")
                .orElseGet(() -> {
                    Cultivo c = new Cultivo();
                    c.setNombre("Parcela Demo - Piura");
                    c.setTipoCultivo("limon");
                    c.setUbicacion("Piura");
                    c.setAreaHa(new BigDecimal("2.50"));
                    return cultivos.save(c);
                });

        Rol administrador = roles.findByNombreRolIgnoreCase("administrador")
                .orElseThrow(() -> new IllegalStateException(
                        "El rol administrador no está sembrado"));
        UsuarioCultivo acceso = new UsuarioCultivo();
        acceso.setUsuario(admin);
        acceso.setCultivo(demo);
        acceso.setRol(administrador);
        accesos.save(acceso);

        log.info("Usuario administrador de demostración sembrado: {} "
                + "(clave: ADMIN_DEMO_CLAVE, definida en DataSeeder)", ADMIN_DEMO_CORREO);
    }
}

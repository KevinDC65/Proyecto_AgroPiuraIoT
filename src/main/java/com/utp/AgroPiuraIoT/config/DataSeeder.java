package com.utp.AgroPiuraIoT.config;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.PlantillaUmbral;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.PlantillaUmbralRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import com.utp.AgroPiuraIoT.util.NormalizadorCultivo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Usuario administrador de demostración (RF-07); documentado en docs/05-seguridad-y-permisos.md. */
    public static final String ADMIN_DEMO_CORREO = "admin@agropiura.com";
    /** Clave demo por defecto; en cualquier entorno compartido se sobrescribe con ADMIN_DEMO_CLAVE. */
    static final String ADMIN_DEMO_CLAVE_POR_DEFECTO = "AgroAdmin2026";

    private final boolean adminDemoHabilitado;
    private final String adminDemoClave;

    public DataSeeder(@Value("${agro.demo.admin.habilitado:true}") boolean adminDemoHabilitado,
                      @Value("${agro.demo.admin.clave:${ADMIN_DEMO_CLAVE:" + ADMIN_DEMO_CLAVE_POR_DEFECTO + "}}")
                      String adminDemoClave) {
        this.adminDemoHabilitado = adminDemoHabilitado;
        this.adminDemoClave = adminDemoClave;
    }

    /** Colaborador de demostración de "Parcela Demo - Piura" (RF-07); solo en modo demo. */
    public static final String COLAB_DEMO_CORREO = "colaborador@agropiura.com";
    public static final String COLAB_DEMO_CLAVE = "AgroColab2026";

    /** Usuario de demostración sin cultivos (prueba de aislamiento); solo en modo demo. */
    public static final String USER_DEMO_CORREO = "usuario@agropiura.com";
    public static final String USER_DEMO_CLAVE = "AgroUser2026";

    @Bean
    ApplicationRunner sembrarCatalogos(RolRepository roles, TipoSensorRepository tipos,
                                       UsuarioRepository usuarios, CultivoRepository cultivos,
                                       UsuarioCultivoRepository accesos,
                                       PlantillaUmbralRepository plantillas,
                                       PasswordEncoder claves) {
        return args -> {
            sembrarRoles(roles);
            sembrarTiposSensor(tipos);
            sembrarPlantillas(plantillas, tipos);
            if (adminDemoHabilitado) {
                // Cuentas de demostración (docs/10-usuarios-de-prueba.md). Solo en modo demo:
                // con agro.demo.admin.habilitado=false no se crea ninguna.
                sembrarAdminDemo(roles, usuarios, cultivos, accesos, claves);
                sembrarColabDemo(roles, usuarios, cultivos, accesos, claves);
                sembrarUsuarioDemo(usuarios, claves);
            }
            avisarCultivosSinAdministrador(cultivos);
        };
    }

    /** Un cultivo sin administrador no lo ve nadie: se avisa en el log con la solución. */
    private void avisarCultivosSinAdministrador(CultivoRepository cultivos) {
        List<Cultivo> sinAdmin = cultivos.findSinAdministrador();
        if (!sinAdmin.isEmpty()) {
            log.warn("{} cultivo(s) sin administrador, invisibles para todos los usuarios: {}. "
                    + "Asígnalos con database/migracion-datos-anteriores.sql (ver docs/04-base-de-datos.md)",
                    sinAdmin.size(),
                    sinAdmin.stream().map(c -> c.getIdCultivo() + " " + c.getNombre()).toList());
        }
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
     * Siembra las plantillas de umbrales desde {@code plantillas-umbral.csv}.
     * Es idempotente y no sobrescribe filas existentes: si el equipo actualiza
     * una fuente, se añade una fila nueva o se limpia la tabla para resembrar.
     */
    private void sembrarPlantillas(PlantillaUmbralRepository plantillas,
                                   TipoSensorRepository tipos) {
        Map<String, TipoSensor> sensoresPorNombre = tipos.findAll().stream()
                .collect(Collectors.toMap(TipoSensor::getNombreTipo, Function.identity()));
        Map<String, PlantillaUmbral> existentes = plantillas.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getTipoCultivo() + "|" + p.getTipoSensor().getIdTipoSensor(),
                        Function.identity()));

        int creadas = 0;
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new ClassPathResource("plantillas-umbral.csv").getInputStream(),
                StandardCharsets.UTF_8))) {
            String linea;
            int numero = 0;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (numero == 1 || linea.isBlank()) {
                    continue; // cabecera o línea vacía
                }
                String[] campos = linea.split(";", -1);
                if (campos.length < 6) {
                    log.warn("Plantilla de umbral ignorada (línea {}): se esperaban 6 columnas",
                            numero);
                    continue;
                }
                String tipoCultivo = NormalizadorCultivo.normalizar(campos[0]);
                TipoSensor tipoSensor = sensoresPorNombre.get(campos[1].trim());
                if (tipoCultivo.isBlank() || tipoSensor == null) {
                    log.warn("Plantilla de umbral ignorada (línea {}): cultivo '{}' o sensor '{}' desconocido",
                            numero, campos[0], campos[1]);
                    continue;
                }
                String clave = tipoCultivo + "|" + tipoSensor.getIdTipoSensor();
                if (existentes.containsKey(clave)) {
                    continue;
                }
                try {
                    PlantillaUmbral plantilla = new PlantillaUmbral();
                    plantilla.setTipoCultivo(tipoCultivo);
                    plantilla.setTipoSensor(tipoSensor);
                    plantilla.setValorMinimo(new BigDecimal(campos[2].trim()));
                    plantilla.setValorMaximo(new BigDecimal(campos[3].trim()));
                    plantilla.setFuente(campos[4].trim());
                    plantilla.setUrlFuente(campos[5].trim());
                    plantilla.setNotas(campos.length > 6 ? campos[6].trim() : null);
                    plantillas.save(plantilla);
                    existentes.put(clave, plantilla);
                    creadas++;
                } catch (NumberFormatException e) {
                    log.warn("Plantilla de umbral ignorada (línea {}): mínimo/máximo inválido",
                            numero);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer plantillas-umbral.csv", e);
        }
        if (creadas > 0) {
            log.info("Catálogo plantilla_umbral sembrado con {} fila(s)", creadas);
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
        admin.setContrasena(claves.encode(adminDemoClave));
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

        log.info("Usuario administrador de demostración sembrado: {}", ADMIN_DEMO_CORREO);
        if (ADMIN_DEMO_CLAVE_POR_DEFECTO.equals(adminDemoClave)) {
            log.warn("El administrador demo usa la clave pública por defecto. Defínela con la "
                    + "variable ADMIN_DEMO_CLAVE o desactívalo con agro.demo.admin.habilitado=false "
                    + "fuera del entorno local.");
        }
    }

    private void sembrarColabDemo(RolRepository roles, UsuarioRepository usuarios,
                                  CultivoRepository cultivos, UsuarioCultivoRepository accesos,
                                  PasswordEncoder claves) {
        if (usuarios.existsByCorreo(COLAB_DEMO_CORREO)) {
            return;
        }
        Usuario colab = new Usuario();
        colab.setNombre("Colaborador Demo");
        colab.setCorreo(COLAB_DEMO_CORREO);
        colab.setContrasena(claves.encode(COLAB_DEMO_CLAVE));
        usuarios.save(colab);

        cultivos.findByNombre("Parcela Demo - Piura").ifPresent(demo -> {
            roles.findByNombreRolIgnoreCase("colaborador").ifPresent(rolColab -> {
                UsuarioCultivo acceso = new UsuarioCultivo();
                acceso.setUsuario(colab);
                acceso.setCultivo(demo);
                acceso.setRol(rolColab);
                accesos.save(acceso);
            });
        });

        log.info("Usuario colaborador de demostración sembrado: {} "
                + "(ver docs/10-usuarios-de-prueba.md)", COLAB_DEMO_CORREO);
    }

    private void sembrarUsuarioDemo(UsuarioRepository usuarios, PasswordEncoder claves) {
        if (usuarios.existsByCorreo(USER_DEMO_CORREO)) {
            return;
        }
        Usuario user = new Usuario();
        user.setNombre("Usuario Estándar Demo");
        user.setCorreo(USER_DEMO_CORREO);
        user.setContrasena(claves.encode(USER_DEMO_CLAVE));
        usuarios.save(user);

        log.info("Usuario estándar de demostración sembrado: {} "
                + "(ver docs/10-usuarios-de-prueba.md)", USER_DEMO_CORREO);
    }
}

package com.utp.AgroPiuraIoT.config;

import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    ApplicationRunner sembrarCatalogos(RolRepository roles, TipoSensorRepository tipos) {
        return args -> {
            if (roles.count() == 0) {
                for (String nombre : new String[] {"administrador", "colaborador"}) {
                    Rol rol = new Rol();
                    rol.setNombreRol(nombre);
                    roles.save(rol);
                }
                log.info("Catálogo roles sembrado: administrador, colaborador");
            }
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
        };
    }
}

package com.utp.AgroPiuraIoT.util.excel;

import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class HistoricoExcelExporter {

    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    public static final String[] CABECERAS =
            {"Fecha y hora", "Variable", "Valor", "Unidad", "Umbral", "Alerta"};

    private HistoricoExcelExporter() {
    }

    public static byte[] exportar(List<Lectura> lecturas, List<Umbral> umbralesCultivo)
            throws IOException {
        try (Workbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Histórico");
            CellStyle negrita = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            negrita.setFont(fuente);

            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < CABECERAS.length; i++) {
                Cell celda = cabecera.createCell(i);
                celda.setCellValue(CABECERAS[i]);
                celda.setCellStyle(negrita);
            }

            int fila = 1;
            for (Lectura lectura : lecturas) {
                Umbral umbral = umbralesDe(lectura, umbralesCultivo);
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(FORMATO.format(lectura.getFechaHora()));
                r.createCell(1).setCellValue(lectura.getSensor().getTipoSensor().getNombreTipo());
                r.createCell(2).setCellValue(lectura.getValor().doubleValue());
                r.createCell(3).setCellValue(lectura.getSensor().getTipoSensor().getUnidadMedida());
                r.createCell(4).setCellValue(umbral == null ? "" : rangoTexto(umbral));
                r.createCell(5).setCellValue(
                        lectura.estaFueraDeRango(umbral) ? "FUERA DE RANGO" : "dentro de rango");
            }

            for (int i = 0; i < CABECERAS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.createFreezePane(0, 1);
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private static String rangoTexto(Umbral umbral) {
        return umbral.getValorMinimo() + " - " + umbral.getValorMaximo();
    }

    private static Umbral umbralesDe(Lectura lectura, List<Umbral> umbralesCultivo) {
        Integer idTipo = lectura.getSensor().getTipoSensor().getIdTipoSensor();
        return umbralesCultivo.stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor().equals(idTipo))
                .findFirst()
                .orElse(null);
    }
}

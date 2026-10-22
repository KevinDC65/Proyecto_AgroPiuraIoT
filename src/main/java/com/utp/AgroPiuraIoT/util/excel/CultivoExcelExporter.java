package com.utp.AgroPiuraIoT.util.excel;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class CultivoExcelExporter {

    public static final String[] CABECERAS =
            {"ID", "Nombre", "Tipo", "Zona", "Área (ha)", "N° dispositivos", "Fecha creación"};

    private CultivoExcelExporter() {
    }

    public static byte[] exportar(List<Cultivo> cultivos) throws IOException {
        try (Workbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Cultivos");
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
            for (Cultivo cultivo : cultivos) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(cultivo.getIdCultivo() == null ? 0 : cultivo.getIdCultivo());
                r.createCell(1).setCellValue(texto(cultivo.getNombre()));
                r.createCell(2).setCellValue(texto(cultivo.getTipoCultivo()));
                r.createCell(3).setCellValue(texto(cultivo.getUbicacion()));
                if (cultivo.getAreaHa() == null) {
                    r.createCell(4).setCellValue("");
                } else {
                    r.createCell(4).setCellValue(cultivo.getAreaHa().doubleValue());
                }
                r.createCell(5).setCellValue(cultivo.getDispositivos() == null
                        ? 0 : cultivo.getDispositivos().size());
                r.createCell(6).setCellValue(cultivo.getFechaCreacion() == null
                        ? "" : cultivo.getFechaCreacion().toString());
            }

            for (int i = 0; i < CABECERAS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.createFreezePane(0, 1);
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}

package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.util.excel.CultivoExcelExporter;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CultivoExcelExporterTest {

    @Test
    void generaHojaConCabeceraYFilas() throws Exception {
        Cultivo mango = new Cultivo();
        mango.setIdCultivo(1);
        mango.setNombre("Mango - Parcela A");
        mango.setTipoCultivo("mango");
        mango.setUbicacion("San Lorenzo");
        mango.setFechaCreacion(LocalDate.of(2026, 10, 1));

        Cultivo limon = new Cultivo();
        limon.setIdCultivo(2);
        limon.setNombre("Limón - Parcela B");
        limon.setTipoCultivo("limón");
        limon.setAreaHa(new java.math.BigDecimal("2.00"));

        byte[] archivo = CultivoExcelExporter.exportar(List.of(mango, limon));
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheet("Cultivos");
            Row cabecera = hoja.getRow(0);
            for (int i = 0; i < CultivoExcelExporter.CABECERAS.length; i++) {
                assertEquals(CultivoExcelExporter.CABECERAS[i], cabecera.getCell(i).getStringCellValue());
            }
            assertEquals(2, hoja.getLastRowNum());
            assertEquals("Mango - Parcela A", hoja.getRow(1).getCell(1).getStringCellValue());
            assertEquals("", hoja.getRow(1).getCell(4).getStringCellValue());
            assertEquals("2026-10-01", hoja.getRow(1).getCell(6).getStringCellValue());
            assertEquals("Limón - Parcela B", hoja.getRow(2).getCell(1).getStringCellValue());
            assertEquals(2.0, hoja.getRow(2).getCell(4).getNumericCellValue());
        }
    }

    @Test
    void exportaListaVaciaSoloConCabecera() throws Exception {
        byte[] archivo = CultivoExcelExporter.exportar(List.of());
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheet("Cultivos");
            assertEquals(0, hoja.getLastRowNum());
            assertArrayEquals(CultivoExcelExporter.CABECERAS,
                    fila(hoja.getRow(0), CultivoExcelExporter.CABECERAS.length));
        }
    }

    private static String[] fila(Row fila, int columnas) {
        String[] valores = new String[columnas];
        for (int i = 0; i < columnas; i++) {
            valores[i] = fila.getCell(i).getStringCellValue();
        }
        return valores;
    }
}

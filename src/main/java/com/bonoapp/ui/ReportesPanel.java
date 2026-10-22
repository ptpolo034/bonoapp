package com.bonoapp.ui;

import com.bonoapp.db.ReporteDAO;
import com.bonoapp.model.ResumenVendedora;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Reporte por vendedora en un rango de fechas: boletas, ahorro y prestamos, con totales y exportacion a CSV. */
public class ReportesPanel extends JPanel implements Actualizable {

    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));
    private static final String[] COLUMNAS = {"Vendedora", "Entregadas", "Vendidas", "Devueltas",
            "Sin liquidar", "Recaudado", "Ahorro neto", "Préstamos", "Abonos"};

    private final JDateChooser campoDesde = new JDateChooser(
            Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    private final JDateChooser campoHasta = new JDateChooser(new Date());
    private final ModeloReporte modelo = new ModeloReporte();
    private final JTable tabla = new JTable(modelo);

    private LocalDate desdeGenerado;
    private LocalDate hastaGenerado;

    public ReportesPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Reportes");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));

        campoDesde.setDateFormatString("dd/MM/yyyy");
        campoHasta.setDateFormatString("dd/MM/yyyy");
        Dimension tamanoFecha = new Dimension(150, new JButton("x").getPreferredSize().height);
        campoDesde.setPreferredSize(tamanoFecha);
        campoHasta.setPreferredSize(tamanoFecha);

        JButton botonGenerar = Botones.principal("Generar", "report");
        botonGenerar.addActionListener(e -> generar(true));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Desde"));
        filtros.add(campoDesde);
        filtros.add(new JLabel("hasta"));
        filtros.add(campoHasta);
        filtros.add(botonGenerar);

        JPanel norte = new JPanel(new BorderLayout(0, 12));
        norte.setOpaque(false);
        norte.add(titulo, BorderLayout.NORTH);
        norte.add(filtros, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        tabla.setDefaultRenderer(Object.class, new RenderizadorReporte());
        Tablas.estilizar(tabla, 1, 2, 3, 4, 5, 6, 7, 8);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(240);
        add(Tablas.enDesplazamiento(tabla), BorderLayout.CENTER);

        JButton botonExportar = Botones.secundario("Exportar a Excel (CSV)", "report");
        botonExportar.addActionListener(e -> exportar());
        JLabel nota = new JLabel("<html>Entregadas y sin liquidar: por fecha de entrega. Vendidas, devueltas y recaudado: "
                + "por fecha de liquidación (sin las revertidas).<br>Ahorro (ya descontados los retiros), préstamos "
                + "y abonos: por la fecha en que se registraron.</html>");
        nota.setForeground(Theme.TEXTO_SUAVE);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pie.setOpaque(false);
        pie.add(botonExportar);
        pie.add(nota);
        add(pie, BorderLayout.SOUTH);

        generar(false);
    }

    @Override
    public void actualizar() {
        generar(false);
    }

    private void generar(boolean avisar) {
        LocalDate desde = Formularios.aLocalDate(campoDesde.getDate());
        LocalDate hasta = Formularios.aLocalDate(campoHasta.getDate());
        if (desde == null || hasta == null) {
            if (avisar) {
                JOptionPane.showMessageDialog(this, "Elige las dos fechas del reporte",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        if (desde.isAfter(hasta)) {
            if (avisar) {
                JOptionPane.showMessageDialog(this, "La fecha \"desde\" no puede ser posterior a \"hasta\"",
                        "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        try {
            modelo.setFilas(ReporteDAO.resumenPorVendedora(desde, hasta));
            desdeGenerado = desde;
            hastaGenerado = hasta;
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el reporte: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportar() {
        if (desdeGenerado == null || modelo.filas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos en el reporte para exportar",
                    "Nada que exportar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar reporte");
        selector.setFileFilter(new FileNameExtensionFilter("CSV para Excel (*.csv)", "csv"));
        selector.setSelectedFile(new File("reporte_" + desdeGenerado + "_a_" + hastaGenerado + ".csv"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            destino = new File(destino.getPath() + ".csv");
        }
        try {
            Files.writeString(destino.toPath(), construirCsv(), StandardCharsets.UTF_8);
            Avisos.exito(this, "Reporte guardado", "El reporte quedÃ³ en:\n" + destino);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el reporte: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** CSV con ";" como separador y BOM, para que Excel en español lo abra bien con tildes y columnas. */
    private String construirCsv() {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Reporte del ").append(desdeGenerado).append(" al ").append(hastaGenerado).append('\n');
        csv.append(String.join(";", COLUMNAS)).append('\n');
        for (ResumenVendedora f : modelo.filasConTotal()) {
            csv.append('"').append(f.nombre().replace("\"", "\"\"")).append('"').append(';')
                    .append(f.entregadas()).append(';')
                    .append(f.vendidas()).append(';')
                    .append(f.devueltas()).append(';')
                    .append(f.sinLiquidar()).append(';')
                    .append(f.recaudado()).append(';')
                    .append(f.ahorro()).append(';')
                    .append(f.prestado()).append(';')
                    .append(f.abonos()).append('\n');
        }
        return csv.toString();
    }

    private static class ModeloReporte extends AbstractTableModel {
        private List<ResumenVendedora> filas = new ArrayList<>();
        private ResumenVendedora total = new ResumenVendedora("TOTAL", null, 0, 0, 0, 0, 0, 0, 0, 0);

        void setFilas(List<ResumenVendedora> filas) {
            this.filas = filas;
            int e = 0, v = 0, d = 0, s = 0, r = 0, a = 0, p = 0, ab = 0;
            for (ResumenVendedora f : filas) {
                e += f.entregadas();
                v += f.vendidas();
                d += f.devueltas();
                s += f.sinLiquidar();
                r += f.recaudado();
                a += f.ahorro();
                p += f.prestado();
                ab += f.abonos();
            }
            total = new ResumenVendedora("TOTAL", null, e, v, d, s, r, a, p, ab);
            fireTableDataChanged();
        }

        List<ResumenVendedora> filasConTotal() {
            List<ResumenVendedora> todas = new ArrayList<>(filas);
            todas.add(total);
            return todas;
        }

        boolean esFilaTotal(int fila) {
            return !filas.isEmpty() && fila == filas.size();
        }

        @Override
        public int getRowCount() {
            return filas.isEmpty() ? 0 : filas.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            ResumenVendedora f = fila == filas.size() ? total : filas.get(fila);
            return switch (columna) {
                case 0 -> f.nombre();
                case 1 -> f.entregadas();
                case 2 -> f.vendidas();
                case 3 -> f.devueltas();
                case 4 -> f.sinLiquidar();
                case 5 -> "$ " + FORMATO_PESOS.format(f.recaudado());
                case 6 -> "$ " + FORMATO_PESOS.format(f.ahorro());
                case 7 -> "$ " + FORMATO_PESOS.format(f.prestado());
                case 8 -> "$ " + FORMATO_PESOS.format(f.abonos());
                default -> "";
            };
        }
    }

    /** Numeros alineados a la derecha y fila de totales en negrita. */
    private class RenderizadorReporte extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            setHorizontalAlignment(columna == 0 ? LEFT : RIGHT);
            c.setFont(t.getFont().deriveFont(modelo.esFilaTotal(fila) ? Font.BOLD : Font.PLAIN));
            return c;
        }
    }
}

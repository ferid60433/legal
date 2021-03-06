package com.ecloga.legalmaster;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Tok {
    private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private int width = (int) (screenSize.getWidth() * 0.5);
    private int height = (int) (screenSize.getHeight() * 0.25);
    private JPanel panel, tablePanel, menuPanel;
    private DefaultTableModel model;
    private JTable table;
    private JScrollPane scrollPane;
    private JButton bDodaj, bUkloni, bIzmeni;
    private ArrayList<String> tok = new ArrayList<String>();
    private String predmetSifra;
    private int maxID = 0;
    public boolean infoShown = false;
    private int predmetId;
    private JFrame frame = new JFrame();
    private Predmeti predmeti;

    public Tok(Predmeti predmeti, String predmetSifra) {
        this.predmetSifra = predmetSifra;
        this.predmeti = predmeti;

        panel = new JPanel();
        tablePanel = new JPanel();
        menuPanel = new JPanel();

        model = new DefaultTableModel();

        table = new JTable(model) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String[] columns = {"ID", "Faza", "Datum", "Vreme"};

        for(String value : columns) {
            model.addColumn(value);
        }

        table.setPreferredScrollableViewportSize(new Dimension(width, height));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if(!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.decode("#ecf0f1") : Color.WHITE);
                }

                if(column == 0) {
                    this.setHorizontalAlignment(JLabel.CENTER);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }else {
                    this.setHorizontalAlignment(JLabel.LEFT);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }

                table.getColumnModel().getColumn(0).setCellRenderer(this);

                return c;
            }
        });

        refresh();

        scrollPane = new JScrollPane(table);

        bDodaj = new JButton("Dodaj");
        bDodaj.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!infoShown){
                    infoShown = true;

                    TokInfo dodaj = new TokInfo(Tok.this);
                    dodaj.show();
                }else {
                    JOptionPane.showMessageDialog(null, "Prozor je aktivan", "Poruka", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        bUkloni = new JButton("Ukloni");
        bUkloni.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = table.getSelectedRow();

                if(selectedIndex != -1) {
                    String id = String.valueOf(table.getValueAt(selectedIndex, 0));

                    Main.executeDB("DELETE FROM tok WHERE id=" + id);

                    model.removeRow(selectedIndex);
                    tok.remove(id);
                }else {
                    JOptionPane.showMessageDialog(null, "Faza nije selektovana", "Poruka", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        bIzmeni = new JButton("Izmeni");
        bIzmeni.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!infoShown) {
                    int selectedIndex = table.getSelectedRow();

                    if(selectedIndex != -1) {
                        HashMap<Integer, String> info = new HashMap<Integer, String>();

                        for(int i = 0; i < table.getColumnCount(); i++) {
                            info.put(i, String.valueOf(table.getValueAt(selectedIndex, i)));
                        }

                        infoShown = true;

                        TokInfo izmeni = new TokInfo(Tok.this, info);
                        izmeni.show();
                    }else {
                        JOptionPane.showMessageDialog(null, "Faza nije selektovana", "Poruka", JOptionPane.INFORMATION_MESSAGE);
                    }
                }else {
                    JOptionPane.showMessageDialog(null, "Prozor je aktivan", "Poruka", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                predmeti.tokShown.remove(predmetSifra);
            }
        });
    }

    public void add(Object[] row) {
        maxID++;
        String id = String.valueOf(maxID);
        row[0] = id;
        Main.executeDB("INSERT INTO tok VALUES (" + row[0] + ", '" + row[1] + "', '" + row[2] + "', '" + row[3] + "', " + getId() + ")");
        addRow(row);
        predmeti.refresh();
    }

    private void addRow(Object[] row) {
        model.addRow(row);
        table.setModel(model);
        tok.add(String.valueOf(row[0]));
    }

    public void update(Object[] row) {
        String id = String.valueOf(row[0]);
        Main.executeDB("UPDATE tok SET ime='" + row[1] + "', datum='" + row[2] + "', vreme='" + row[3] + "' WHERE id=" + id);
        refresh();
        predmeti.refresh();
    }

    public void refresh() {
        model.setRowCount(0);
        table.setModel(model);

        ResultSet rs = null;

        String cmd = "SELECT * FROM tok";

        predmetId = getId();

        try {
            rs = Main.s.executeQuery(cmd);

            while(rs.next()){
                if(rs.getInt("predmet") == predmetId) {
                    addRow(new Object[] {rs.getInt("id"), rs.getString("ime"), rs.getString("datum"), rs.getString("vreme")});
                }

                if(rs.getInt("id") > maxID) {
                    maxID = rs.getInt("id");
                }
            }

            rs.close();
        }catch(SQLException e) {
            e.printStackTrace();
        }

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setMaxWidth(75);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(75);
        columnModel.getColumn(3).setMinWidth(75);
    }

    private int getId() {
        ResultSet rs = null;

        String cmd = "SELECT * FROM predmeti WHERE sifra='" + predmetSifra + "'";

        int predmetId = 0;

        try {
            rs = Main.s.executeQuery(cmd);

            while(rs.next()){
                predmetId = rs.getInt("id");
            }

            rs.close();
        }catch(SQLException e) {
            e.printStackTrace();
        }

        return predmetId;
    }

    public void show() {
        menuPanel.add(bDodaj);
        menuPanel.add(bUkloni);
        menuPanel.add(bIzmeni);

        tablePanel.add(scrollPane);

        panel.add(menuPanel);
        panel.add(tablePanel);

        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();
        frame.setAlwaysOnTop(false);

        frame.setTitle(predmetSifra + " - Tok predmeta");
        frame.setSize(new Dimension(width, height));
        frame.setLocation(screenSize.width / 2 - width / 2,screenSize.height / 2 - height / 2);
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}

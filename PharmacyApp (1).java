import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * Pharmacy Management System - Java Swing version of pharmacy.html
 * Single file, no external libraries. Run:  javac PharmacyApp.java && java PharmacyApp
 * Login: admin / admin123.  Data is saved to pharmacy-data.ser in the folder you run from.
 */
public class PharmacyApp extends JFrame {

    // ======================================================================
    //  THEME + CONSTANTS
    // ======================================================================
    static final Color BLUE = new Color(0x1a73e8), GREEN = new Color(0x12a36b), RED = new Color(0xe5484d),
            ORANGE = new Color(0xe08a00), PURPLE = new Color(0x7a4fd1), DARKRED = new Color(0x8b1e22),
            BG = new Color(0xf3f8fb), TEXT = new Color(0x12263a), MUTED = new Color(0x5f7385),
            LINE = new Color(0xe2eaf1), FIELD = new Color(0xc9d6e2);
    static final String RS = "\u20B9"; // rupee sign
    static final String[] CATS = {"Analgesic", "Antibiotic", "Antihistamine", "Antidiabetic", "Cardiac",
            "Antacid", "Supplement", "Cough & Cold"};
    static final String[][] NAV = {{"dash", "Dashboard"}, {"meds", "Medicine Management"}, {"inv", "Inventory"},
            {"cust", "Customers"}, {"bill", "Billing"}, {"search", "Search"}, {"rep", "Reports"},
            {"prof", "Admin Profile"}, {"out", "Logout"}};
    static final Preferences PREFS = Preferences.userRoot().node("pharmacyms");

    // ======================================================================
    //  DATA MODEL  (saved with Java serialization, like localStorage in the HTML)
    // ======================================================================
    static class Medicine implements Serializable {
        private static final long serialVersionUID = 1L;
        String id, name, cat, mfr, batch, sup;
        int qty, min;
        double price;
        LocalDate exp;
    }

    static class Customer implements Serializable {
        private static final long serialVersionUID = 1L;
        String id, name, phone, email, addr;
    }

    static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        String id, name;
        int qty;
        double price;

        Item(String id, String name, int qty, double price) {
            this.id = id; this.name = name; this.qty = qty; this.price = price;
        }
    }

    static class Bill implements Serializable {
        private static final long serialVersionUID = 1L;
        String id, cust = "";
        LocalDate date;
        List<Item> items = new ArrayList<>();
        double sub, dis, tax, total;
    }

    static class Data implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Medicine> meds = new ArrayList<>();
        List<Customer> custs = new ArrayList<>();
        List<Bill> bills = new ArrayList<>();
    }

    static final File DATA_FILE = new File("pharmacy-data.ser");
    static Data db;

    static Data load() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            return (Data) in.readObject();
        } catch (Exception e) {
            return seed();
        }
    }

    static void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(db);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Data seed() {
        Data d = new Data();
        LocalDate today = LocalDate.now();
        Object[][] m = {
                {"Paracetamol 500mg", 0, "Cipla", 240, 50, 18, 400, "MedSupply Co"},
                {"Amoxicillin 250mg", 1, "Sun Pharma", 35, 40, 72, 200, "HealthLine Distributors"},
                {"Cetirizine 10mg", 2, "Dr. Reddy's", 120, 30, 25, 300, "MedSupply Co"},
                {"Metformin 500mg", 3, "Lupin", 180, 60, 42, 500, "Apex Pharma Traders"},
                {"Atorvastatin 10mg", 4, "Zydus", 12, 30, 95, 45, "HealthLine Distributors"},
                {"Omeprazole 20mg", 5, "Cipla", 0, 25, 58, 150, "Apex Pharma Traders"},
                {"Azithromycin 500mg", 1, "Alkem", 60, 25, 118, 25, "MedSupply Co"},
                {"Ibuprofen 400mg", 0, "Abbott", 90, 40, 32, -20, "Apex Pharma Traders"},
                {"Vitamin C 500mg", 6, "Himalaya", 200, 50, 45, 600, "HealthLine Distributors"},
                {"ORS Sachet", 6, "FDC", 150, 40, 20, 365, "MedSupply Co"},
                {"Amlodipine 5mg", 4, "Torrent", 75, 30, 36, 420, "Apex Pharma Traders"},
                {"Cough Syrup 100ml", 7, "Dabur", 28, 30, 85, 270, "HealthLine Distributors"}};
        for (int i = 0; i < m.length; i++) {
            Object[] x = m[i];
            Medicine md = new Medicine();
            md.id = String.format("MED%03d", i + 1);
            md.name = (String) x[0];
            md.cat = CATS[(Integer) x[1]];
            md.mfr = (String) x[2];
            md.batch = "B" + (2401 + i * 7);
            md.qty = (Integer) x[3];
            md.min = (Integer) x[4];
            md.price = (Integer) x[5];
            md.exp = today.plusDays((Integer) x[6]);
            md.sup = (String) x[7];
            d.meds.add(md);
        }
        String[][] c = {
                {"Arun Kumar", "9876543210", "arun.k@mail.com", "12 Gandhi Nagar, Chennai"},
                {"Priya Sharma", "9845012345", "priya.s@mail.com", "45 MG Road, Bengaluru"},
                {"Karthik Raj", "9790011223", "karthik.r@mail.com", "7 Lake View, Coimbatore"},
                {"Meena Devi", "9444455555", "meena.d@mail.com", "88 Temple St, Madurai"},
                {"Suresh Babu", "9600123456", "suresh.b@mail.com", "23 Park Ave, Salem"}};
        for (int i = 0; i < c.length; i++) {
            Customer cu = new Customer();
            cu.id = String.format("CUS%03d", i + 1);
            cu.name = c[i][0]; cu.phone = c[i][1]; cu.email = c[i][2]; cu.addr = c[i][3];
            d.custs.add(cu);
        }
        for (int i = 0; i < 18; i++) {
            Bill b = new Bill();
            b.id = "BILL" + (1001 + i);
            b.date = today.minusDays(i % 7);
            b.cust = d.custs.get(i % 5).id;
            Medicine[] pick = {d.meds.get((i * 3) % 12), d.meds.get((i * 5 + 1) % 12)};
            int j = 0;
            for (Medicine x : pick) {
                if (x.qty > 0) {
                    b.items.add(new Item(x.id, x.name, 1 + (i + j) % 3, x.price));
                    j++;
                }
            }
            double[] t = calc(b.items, 0, 5);
            b.sub = t[0]; b.dis = t[1]; b.tax = t[2]; b.total = t[3];
            d.bills.add(b);
        }
        return d;
    }

    /** returns {subtotal, discount, tax, total} */
    static double[] calc(List<Item> items, double discPct, double taxPct) {
        double sub = 0;
        for (Item i : items) sub += i.qty * i.price;
        double dis = sub * discPct / 100, tax = (sub - dis) * taxPct / 100;
        return new double[]{sub, dis, tax, sub - dis + tax};
    }

    // ======================================================================
    //  SMALL HELPERS
    // ======================================================================
    static String money(double n) { return RS + String.format(Locale.US, "%.2f", n); }

    static String f(String fmt, Object... a) { return String.format(Locale.US, fmt, a); }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String hex(Color c) { return String.format("#%06x", c.getRGB() & 0xffffff); }

    static int num(String s) {
        String d = s.replaceAll("\\D", "");
        return d.isEmpty() ? 0 : Integer.parseInt(d);
    }

    static String status(Medicine m) {
        LocalDate t = LocalDate.now();
        if (m.exp.isBefore(t)) return "Expired";
        if (m.qty == 0) return "Out of Stock";
        if (m.qty <= m.min) return "Low Stock";
        if (!m.exp.isAfter(t.plusDays(60))) return "Expiring Soon";
        return "In Stock";
    }

    static Color statusColor(String s) {
        switch (s) {
            case "Expired": return DARKRED;
            case "Out of Stock": return RED;
            case "Low Stock": return ORANGE;
            case "Expiring Soon": return PURPLE;
            default: return GREEN;
        }
    }

    static String cn(String id) {
        for (Customer c : db.custs) if (c.id.equals(id)) return c.name;
        return "Walk-in Customer";
    }

    static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    static JPanel panel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    static JLabel lbl(String t, int style, int size, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(c);
        return l;
    }

    static void styleField(JComponent c) {
        c.setBorder(new CompoundBorder(new LineBorder(FIELD, 1, true), new EmptyBorder(7, 9, 7, 9)));
        c.setBackground(Color.WHITE);
        c.setForeground(TEXT);
    }

    static JTextField tf() {
        JTextField t = new JTextField();
        styleField(t);
        return t;
    }

    static JComboBox<String> combo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setBackground(Color.WHITE);
        c.setForeground(TEXT);
        c.setPreferredSize(new Dimension(c.getPreferredSize().width + 10, 34));
        return c;
    }

    static String[] prepend(String first, String[] rest) {
        String[] a = new String[rest.length + 1];
        a[0] = first;
        System.arraycopy(rest, 0, a, 1, rest.length);
        return a;
    }

    static void onChange(JTextField tf, Runnable r) {
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    /** vertical stack: each child gets the full width and its natural height */
    static JPanel vstack(int gap, Component... cs) {
        JPanel p = panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < cs.length; i++) {
            c.gridy = i;
            c.insets = new Insets(0, 0, i < cs.length - 1 ? gap : 0, 0);
            p.add(cs[i], c);
        }
        return p;
    }

    static JPanel grid(int rows, int cols, Component... cs) {
        JPanel p = panel(new GridLayout(rows, cols, 14, 14));
        for (Component c : cs) p.add(c);
        return p;
    }

    static JPanel bar(JComponent grow, Component... rest) {
        JPanel p = panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(grow, c);
        c.weightx = 0;
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(0, 8, 0, 0);
        for (Component x : rest) p.add(x, c);
        return p;
    }

    static Card titled(String title, JComponent body) {
        Card c = new Card(new BorderLayout(0, 10));
        c.add(lbl(title, Font.BOLD, 16, TEXT), BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    static Card wrap(JComponent body) {
        Card c = new Card(new BorderLayout());
        c.setBorder(new EmptyBorder(8, 8, 8, 8));
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    static Card stat(String label, String value, Color c) {
        Card k = new Card(new BorderLayout(0, 2));
        k.add(lbl(value, Font.BOLD, 24, c), BorderLayout.NORTH);
        k.add(lbl(label, Font.PLAIN, 12, MUTED), BorderLayout.CENTER);
        return k;
    }

    static Btn btn(String t, Color c, ActionListener a) { return new Btn(t, c, false, a); }

    static Btn obtn(String t, Color c, ActionListener a) { return new Btn(t, c, true, a); }

    static JPanel row(Component... cs) {
        JPanel p = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (Component c : cs) p.add(c);
        return p;
    }

    // ======================================================================
    //  CUSTOM WIDGETS
    // ======================================================================
    static class Card extends JPanel {
        Card() { this(new BorderLayout()); }

        Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(16, 16, 16, 16));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            Shape r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(Color.WHITE);
            g2.fill(r);
            g2.setColor(LINE);
            g2.draw(r);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Btn extends JButton {
        final Color base;
        final boolean outline;

        Btn(String text, Color base, boolean outline, ActionListener a) {
            super(text);
            this.base = base;
            this.outline = outline;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setForeground(outline ? base : Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (a != null) addActionListener(a);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            boolean hot = getModel().isRollover() || getModel().isArmed();
            Shape r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 12, 12);
            if (outline) {
                g2.setColor(hot ? new Color(base.getRed(), base.getGreen(), base.getBlue(), 30) : Color.WHITE);
                g2.fill(r);
                g2.setColor(base);
                g2.draw(r);
            } else {
                g2.setColor(hot ? base.darker() : base);
                g2.fill(r);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Nav extends JButton {
        boolean on;

        Nav(String t) {
            super(t);
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(MUTED);
            setBorder(new EmptyBorder(11, 14, 11, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setOn(boolean v) {
            on = v;
            setForeground(v ? Color.WHITE : MUTED);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            Shape r = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12);
            if (on) {
                g2.setPaint(new GradientPaint(0, 0, BLUE, getWidth(), 0, GREEN));
                g2.fill(r);
            } else if (getModel().isRollover()) {
                g2.setColor(BG);
                g2.fill(r);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class SearchField extends JTextField {
        final String hint;

        SearchField(String hint) {
            this.hint = hint;
            styleField(this);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = aa(g);
                g2.setColor(new Color(0x9aabbb));
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    static class Chip extends JLabel {
        final Color c;

        Chip(String t, Color c) {
            super(t, SwingConstants.CENTER);
            this.c = c;
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(new EmptyBorder(5, 10, 5, 10));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class LogoBox extends JComponent {
        LogoBox() { setPreferredSize(new Dimension(36, 36)); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setPaint(new GradientPaint(0, 0, BLUE, 36, 36, GREEN));
            g2.fill(new RoundRectangle2D.Double(0, 0, 36, 36, 12, 12));
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("Rx", (36 - fm.stringWidth("Rx")) / 2, (36 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    static class Hero extends JComponent {
        Hero() { setPreferredSize(new Dimension(220, 220)); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fill(new Ellipse2D.Double(cx - 100, cy - 100, 200, 200));
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(cx - 25, cy - 60, 50, 120, 24, 24));
            g2.fill(new RoundRectangle2D.Double(cx - 60, cy - 25, 120, 50, 24, 24));
            g2.rotate(Math.toRadians(-25), cx, cy + 53);
            g2.setColor(GREEN);
            g2.fill(new RoundRectangle2D.Double(cx - 50, cy + 40, 100, 26, 26, 26));
            g2.setColor(BLUE);
            g2.fill(new RoundRectangle2D.Double(cx, cy + 40, 50, 26, 26, 26));
            g2.dispose();
        }
    }

    static class BarChart extends JPanel {
        final String[] labels;
        final double[] vals;

        BarChart(String[] labels, double[] vals) {
            this.labels = labels;
            this.vals = vals;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 200));
        }

        static String compact(double v) { return v >= 1000 ? f("%.1fk", v / 1000) : f("%.0f", v); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight(), n = Math.max(vals.length, 1), pad = 12, bottom = 26, top = 20;
            double max = 1;
            for (double v : vals) max = Math.max(max, v);
            int slot = (w - 2 * pad) / n, bw = Math.min(46, slot * 2 / 3);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(LINE);
            g2.drawLine(pad, h - bottom, w - pad, h - bottom);
            for (int i = 0; i < vals.length; i++) {
                int bh = (int) ((h - top - bottom) * vals[i] / max);
                int x = pad + i * slot + (slot - bw) / 2, y = h - bottom - bh;
                g2.setColor(i % 2 == 0 ? BLUE : GREEN);
                g2.fill(new RoundRectangle2D.Double(x, y, bw, Math.max(bh, 3), 8, 8));
                g2.setColor(MUTED);
                g2.drawString(labels[i], x + (bw - fm.stringWidth(labels[i])) / 2, h - 9);
                String vs = compact(vals[i]);
                g2.drawString(vs, x + (bw - fm.stringWidth(vs)) / 2, y - 4);
            }
            g2.dispose();
        }
    }

    static class Donut extends JPanel {
        final Map<String, Integer> data;

        Donut(Map<String, Integer> data) {
            this.data = data;
            setOpaque(false);
            setPreferredSize(new Dimension(170, 170));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = aa(g);
            int total = 0;
            for (int v : data.values()) total += v;
            int size = Math.min(getWidth(), getHeight()) - 10, x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            double start = 90;
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                if (e.getValue() == 0 || total == 0) continue;
                double ext = -360.0 * e.getValue() / total;
                g2.setColor(statusColor(e.getKey()));
                g2.fill(new Arc2D.Double(x, y, size, size, start, ext, Arc2D.PIE));
                start += ext;
            }
            int hole = size * 6 / 10;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x + (size - hole) / 2.0, y + (size - hole) / 2.0, hole, hole));
            g2.dispose();
        }
    }

    /** Table with a styled header and no scrollbar of its own (the page scrolls). */
    static class DataTable extends JPanel {
        final DefaultTableModel model;
        final JTable table = new JTable();
        final JLabel empty = lbl("No records found", Font.PLAIN, 13, MUTED);
        boolean[] tint = new boolean[0];
        int statusCol = -1;

        DataTable(String... cols) {
            super(new BorderLayout());
            setOpaque(false);
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table.setModel(model);
            table.setRowHeight(32);
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 0));
            table.setSelectionBackground(new Color(0xdbe9fc));
            table.setSelectionForeground(TEXT);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            for (int i = 0; i < cols.length; i++) if (cols[i].equals("Status")) statusCol = i;

            JTableHeader h = table.getTableHeader();
            h.setReorderingAllowed(false);
            h.setPreferredSize(new Dimension(100, 34));
            h.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean fo, int r, int c) {
                    super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(), false, false, r, c);
                    setBackground(Color.WHITE);
                    setForeground(MUTED);
                    setFont(new Font("SansSerif", Font.BOLD, 11));
                    setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 8, 0, 8)));
                    setHorizontalAlignment(SwingConstants.LEFT);
                    return this;
                }
            });
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean fo, int r, int c) {
                    super.getTableCellRendererComponent(t, v, false, false, r, c);
                    setBackground(sel ? t.getSelectionBackground() : (r < tint.length && tint[r]) ? new Color(0xfdeaea) : Color.WHITE);
                    setForeground(TEXT);
                    setFont(new Font("SansSerif", Font.PLAIN, 13));
                    if (c == statusCol && v != null) {
                        setForeground(statusColor(v.toString()));
                        setFont(new Font("SansSerif", Font.BOLD, 12));
                    }
                    setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 8, 0, 8)));
                    return this;
                }
            });
            empty.setBorder(new EmptyBorder(10, 8, 4, 8));
            add(h, BorderLayout.NORTH);
            add(table, BorderLayout.CENTER);
            add(empty, BorderLayout.SOUTH);
            empty.setVisible(false);
        }

        void setRows(List<Object[]> rows, boolean[] t) {
            tint = t == null ? new boolean[0] : t;
            model.setRowCount(0);
            for (Object[] r : rows) model.addRow(r);
            empty.setVisible(rows.isEmpty());
            revalidate();
            repaint();
        }

        void widths(int... w) {
            for (int i = 0; i < w.length && i < table.getColumnCount(); i++)
                table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }

        void onDouble(Runnable r) {
            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) r.run(); }
            });
        }
    }

    /** Scroll content that always matches the viewport width. */
    static class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel(LayoutManager lm) { super(lm); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static class Field {
        final String key, label, type;
        final String[] opts;

        Field(String key, String label, String type, String... opts) {
            this.key = key; this.label = label; this.type = type; this.opts = opts;
        }
    }

    static final Field[] MF = {
            new Field("name", "Medicine Name", "text"), new Field("cat", "Category", "select", CATS),
            new Field("mfr", "Manufacturer", "text"), new Field("batch", "Batch Number", "text"),
            new Field("qty", "Quantity", "int"), new Field("min", "Minimum Stock", "int"),
            new Field("price", "Price (" + RS + ")", "num"), new Field("exp", "Expiry Date (YYYY-MM-DD)", "date"),
            new Field("sup", "Supplier", "text")};
    static final Field[] CF = {
            new Field("name", "Customer Name", "text"), new Field("phone", "Phone (10 digits)", "tel"),
            new Field("email", "Email", "email"), new Field("addr", "Address", "text")};

    // ======================================================================
    //  MAIN WINDOW
    // ======================================================================
    final CardLayout rootLayout = new CardLayout();
    final JPanel rootPanel = new JPanel(rootLayout);
    JTextField userField;
    JPasswordField passField;
    JCheckBox rememberChk;
    JLabel loginErr, title, toastLbl;
    JButton loginBtn;
    JPanel holder;
    JScrollPane scroll;
    Nav[] navs;
    javax.swing.Timer toastTimer;
    String cur = "dash", repType = "daily";
    final List<Item> cart = new ArrayList<>();
    Bill lastBill;

    public PharmacyApp() {
        super("Pharmacy Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        rootPanel.add(buildLogin(), "login");
        rootPanel.add(buildApp(), "app");
        setContentPane(rootPanel);
        getRootPane().setDefaultButton(loginBtn);
    }

    // ---------------------------------------------------------------- login
    JComponent buildLogin() {
        JPanel left = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setPaint(new GradientPaint(0, 0, BLUE, getWidth(), getHeight(), GREEN));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        JLabel h1 = lbl("Pharmacy Management System", Font.BOLD, 26, Color.WHITE);
        h1.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel sub = lbl("Medicines \u2022 Customers \u2022 Billing \u2022 Inventory", Font.PLAIN, 14, Color.WHITE);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        left.add(vstack(12, new Hero(), h1, sub));

        userField = tf();
        passField = new JPasswordField();
        styleField(passField);
        String saved = PREFS.get("user", "");
        userField.setText(saved);
        rememberChk = new JCheckBox("Remember me", !saved.isEmpty());
        rememberChk.setOpaque(false);
        rememberChk.setForeground(MUTED);
        JCheckBox showPw = new JCheckBox("Show password");
        showPw.setOpaque(false);
        showPw.setForeground(MUTED);
        char dot = passField.getEchoChar();
        showPw.addActionListener(e -> passField.setEchoChar(showPw.isSelected() ? (char) 0 : dot));
        JLabel forgot = lbl("Forgot password?", Font.PLAIN, 13, BLUE);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(PharmacyApp.this, "Reset link sent to registered email (demo)");
            }
        });
        JPanel opts = panel(new BorderLayout());
        opts.add(rememberChk, BorderLayout.WEST);
        opts.add(forgot, BorderLayout.EAST);
        loginErr = lbl(" ", Font.BOLD, 12, RED);
        loginBtn = btn("Login", BLUE, e -> doLogin());
        passField.addActionListener(e -> doLogin());
        userField.addActionListener(e -> passField.requestFocusInWindow());

        JPanel form = vstack(6,
                lbl("PHARMACY MS", Font.BOLD, 18, BLUE),
                lbl("Admin Login", Font.BOLD, 24, TEXT),
                lbl("Demo: admin / admin123", Font.PLAIN, 13, MUTED),
                lbl("Username", Font.BOLD, 12, MUTED), userField,
                lbl("Password", Font.BOLD, 12, MUTED), passField,
                showPw, opts, loginErr, loginBtn);
        form.setPreferredSize(new Dimension(380, form.getPreferredSize().height));
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(BG);
        right.add(form);

        JPanel p = new JPanel(new GridLayout(1, 2));
        p.add(left);
        p.add(right);
        return p;
    }

    void doLogin() {
        if (userField.getText().trim().equals("admin") && new String(passField.getPassword()).equals("admin123")) {
            if (rememberChk.isSelected()) PREFS.put("user", userField.getText().trim());
            else PREFS.remove("user");
            loginErr.setText(" ");
            rootLayout.show(rootPanel, "app");
            getRootPane().setDefaultButton(null);
            go("dash");
            toast("Welcome back, Admin!", false);
        } else {
            loginErr.setText("Invalid username or password");
        }
    }

    // ---------------------------------------------------------------- app shell
    JComponent buildApp() {
        JPanel app = new JPanel(new BorderLayout());

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Color.WHITE);
        side.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, LINE), new EmptyBorder(16, 14, 16, 14)));
        side.setPreferredSize(new Dimension(250, 0));
        JPanel brand = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.add(new LogoBox());
        brand.add(lbl("PHARMACY MS", Font.BOLD, 17, TEXT));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        side.add(brand);
        side.add(Box.createVerticalStrut(14));
        navs = new Nav[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            final String key = NAV[i][0];
            Nav n = new Nav(NAV[i][1]);
            n.setAlignmentX(LEFT_ALIGNMENT);
            n.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            n.addActionListener(e -> go(key));
            navs[i] = n;
            side.add(n);
            side.add(Box.createVerticalStrut(2));
        }
        app.add(side, BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBackground(Color.WHITE);
        top.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(12, 22, 12, 22)));
        title = lbl("", Font.BOLD, 18, TEXT);
        top.add(title, BorderLayout.WEST);
        toastLbl = new JLabel();
        toastLbl.setOpaque(true);
        toastLbl.setForeground(Color.WHITE);
        toastLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        toastLbl.setBorder(new EmptyBorder(6, 14, 6, 14));
        toastLbl.setVisible(false);
        JPanel mid = panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        mid.add(toastLbl);
        top.add(mid, BorderLayout.CENTER);
        top.add(lbl("Admin", Font.BOLD, 13, MUTED), BorderLayout.EAST);
        main.add(top, BorderLayout.NORTH);

        holder = new ScrollPanel(new BorderLayout());
        holder.setBackground(BG);
        holder.setBorder(new EmptyBorder(20, 22, 24, 22));
        scroll = new JScrollPane(holder, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        main.add(scroll, BorderLayout.CENTER);
        app.add(main, BorderLayout.CENTER);
        return app;
    }

    void toast(String msg, boolean err) {
        toastLbl.setText(msg);
        toastLbl.setBackground(err ? RED : GREEN);
        toastLbl.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new javax.swing.Timer(3200, e -> toastLbl.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    void go(String p) {
        if (p.equals("out")) {
            rootLayout.show(rootPanel, "login");
            passField.setText("");
            loginErr.setText(" ");
            getRootPane().setDefaultButton(loginBtn);
            return;
        }
        cur = p;
        for (int i = 0; i < NAV.length; i++) {
            navs[i].setOn(NAV[i][0].equals(p));
            if (NAV[i][0].equals(p)) title.setText(NAV[i][1]);
        }
        JComponent pg;
        switch (p) {
            case "meds": pg = pgMeds(); break;
            case "inv": pg = pgInv(); break;
            case "cust": pg = pgCust(); break;
            case "bill": pg = new BillingPanel(); break;
            case "search": pg = pgSearch(); break;
            case "rep": pg = pgRep(); break;
            case "prof": pg = pgProf(); break;
            default: pg = pgDash();
        }
        holder.removeAll();
        holder.add(pg, BorderLayout.NORTH);
        holder.revalidate();
        holder.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    // ======================================================================
    //  DIALOG HELPERS
    // ======================================================================
    JDialog dlg(String t) { return new JDialog(this, t, true); }

    void showDialog(JDialog d, JComponent body) {
        body.setOpaque(true);
        body.setBackground(Color.WHITE);
        d.setContentPane(body);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    JPanel closeRow(JDialog d) {
        JPanel p = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.add(obtn("Close", BLUE, e -> d.dispose()));
        return p;
    }

    void info(String heading, String[][] rows) {
        JDialog d = dlg("Details");
        JPanel g = panel(new GridLayout(0, 2, 16, 8));
        for (String[] r : rows) {
            g.add(lbl(r[0], Font.BOLD, 13, MUTED));
            g.add(lbl(r[1], Font.PLAIN, 13, TEXT));
        }
        JPanel body = panel(new BorderLayout(0, 14));
        body.setBorder(new EmptyBorder(18, 20, 16, 20));
        body.add(lbl(heading, Font.BOLD, 16, TEXT), BorderLayout.NORTH);
        body.add(g, BorderLayout.CENTER);
        body.add(closeRow(d), BorderLayout.SOUTH);
        Dimension pd = body.getPreferredSize();
        body.setPreferredSize(new Dimension(Math.max(480, pd.width), pd.height));
        showDialog(d, body);
    }

    static String validate(Field f, String x) {
        if (x.isEmpty()) return f.label + " is required";
        switch (f.type) {
            case "int":
                if (!x.matches("\\d{1,7}")) return f.label + " must be a whole number (0 or more)";
                break;
            case "num":
                if (!x.matches("\\d+(\\.\\d+)?")) return f.label + " must be a valid positive number";
                break;
            case "date":
                try { LocalDate.parse(x); } catch (Exception e) { return f.label + " must be a date like 2027-03-31"; }
                break;
            case "tel":
                if (!x.matches("\\d{10}")) return "Phone must be 10 digits";
                break;
            case "email":
                if (!x.matches("\\S+@\\S+\\.\\S+")) return "Enter a valid email";
                break;
            default:
        }
        return null;
    }

    void form(String heading, Field[] fs, Map<String, String> v, Consumer<Map<String, String>> ok) {
        JDialog d = dlg(heading);
        Map<String, JComponent> comps = new LinkedHashMap<>();
        JPanel g = panel(new GridLayout(0, 2, 14, 6));
        for (Field fd : fs) {
            JComponent c;
            if (fd.type.equals("select")) {
                JComboBox<String> cb = combo(fd.opts);
                if (v.get(fd.key) != null) cb.setSelectedItem(v.get(fd.key));
                c = cb;
            } else {
                JTextField t = tf();
                t.setText(v.getOrDefault(fd.key, ""));
                c = t;
            }
            comps.put(fd.key, c);
            JPanel cell = panel(new BorderLayout(0, 3));
            cell.setBorder(new EmptyBorder(0, 0, 8, 0));
            cell.add(lbl(fd.label, Font.BOLD, 12, MUTED), BorderLayout.NORTH);
            cell.add(c, BorderLayout.CENTER);
            g.add(cell);
        }
        Btn saveBtn = btn("Save", BLUE, e -> {
            Map<String, String> out = new LinkedHashMap<>();
            for (Field fd : fs) {
                JComponent c = comps.get(fd.key);
                String x = c instanceof JComboBox ? String.valueOf(((JComboBox<?>) c).getSelectedItem())
                        : ((JTextField) c).getText().trim();
                String err = validate(fd, x);
                if (err != null) {
                    JOptionPane.showMessageDialog(d, err, "Please check your input", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                out.put(fd.key, x);
            }
            d.dispose();
            ok.accept(out);
        });
        JPanel btns = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(saveBtn);
        btns.add(obtn("Cancel", BLUE, e -> d.dispose()));
        JPanel body = panel(new BorderLayout(0, 12));
        body.setBorder(new EmptyBorder(18, 20, 16, 20));
        body.add(lbl(heading, Font.BOLD, 17, TEXT), BorderLayout.NORTH);
        body.add(g, BorderLayout.CENTER);
        body.add(btns, BorderLayout.SOUTH);
        Dimension pd = body.getPreferredSize();
        body.setPreferredSize(new Dimension(Math.max(580, pd.width), pd.height));
        d.getRootPane().setDefaultButton(saveBtn);
        showDialog(d, body);
    }

    // ======================================================================
    //  DASHBOARD
    // ======================================================================
    JComponent pgDash() {
        LocalDate today = LocalDate.now();
        Map<String, Integer> cnt = new LinkedHashMap<>();
        for (String s : new String[]{"In Stock", "Low Stock", "Out of Stock", "Expiring Soon", "Expired"}) cnt.put(s, 0);
        int low = 0, totalQty = 0;
        List<Medicine> lowList = new ArrayList<>();
        for (Medicine m : db.meds) {
            String s = status(m);
            cnt.put(s, cnt.get(s) + 1);
            totalQty += m.qty;
            if (s.equals("Low Stock") || s.equals("Out of Stock")) { low++; lowList.add(m); }
        }
        int todayBills = 0;
        double todaySales = 0;
        for (Bill b : db.bills) if (b.date.equals(today)) { todayBills++; todaySales += b.total; }

        JPanel stats = grid(2, 3,
                stat("Total Medicines", "" + db.meds.size(), BLUE), stat("Available Stock", "" + totalQty, GREEN),
                stat("Low Stock Medicines", "" + low, ORANGE), stat("Total Customers", "" + db.custs.size(), PURPLE),
                stat("Today's Sales", money(todaySales), GREEN), stat("Today's Bills", "" + todayBills, BLUE));

        JPanel qa = row(btn("New Bill", BLUE, e -> go("bill")),
                btn("Add Medicine", GREEN, e -> { go("meds"); addMed(); }),
                obtn("Add Customer", BLUE, e -> { go("cust"); addCust(); }),
                obtn("Reports", BLUE, e -> go("rep")));
        Card quick = titled("Quick Actions", qa);

        String[] l = new String[7];
        double[] v = new double[7];
        salesLast7(l, v);
        Card sales = titled("Sales Overview (7 days)", new BarChart(l, v));

        StringBuilder lg = new StringBuilder("<html>");
        for (Map.Entry<String, Integer> e : cnt.entrySet())
            lg.append("<span style='color:").append(hex(statusColor(e.getKey()))).append("'>&#9632;</span> ")
                    .append(e.getKey()).append(" (").append(e.getValue()).append(")<br>");
        JLabel legend = lbl(lg.append("</html>").toString(), Font.PLAIN, 12, MUTED);
        JPanel dn = panel(new BorderLayout(0, 8));
        dn.add(new Donut(cnt), BorderLayout.CENTER);
        dn.add(legend, BorderLayout.SOUTH);
        Card inv = titled("Inventory Status", dn);

        List<Bill> recent = new ArrayList<>(db.bills);
        recent.sort((a, b) -> b.date.equals(a.date) ? b.id.compareTo(a.id) : b.date.compareTo(a.date));
        DataTable rt = new DataTable("Bill", "Customer", "Date", "Total");
        List<Object[]> rows = new ArrayList<>();
        for (Bill b : recent.subList(0, Math.min(6, recent.size())))
            rows.add(new Object[]{b.id, cn(b.cust), b.date.toString(), money(b.total)});
        rt.setRows(rows, null);
        Card recentCard = titled("Recent Transactions", rt);

        JPanel alerts = new JPanel(new GridLayout(0, 1, 0, 0));
        alerts.setOpaque(false);
        for (Medicine m : lowList) {
            JPanel r = panel(new BorderLayout());
            r.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(7, 0, 7, 0)));
            r.add(lbl(m.name, Font.PLAIN, 13, TEXT), BorderLayout.WEST);
            r.add(lbl("<html><span style='color:#5f7385'>" + m.qty + "/" + m.min + "</span> &nbsp;<b style='color:"
                    + hex(statusColor(status(m))) + "'>" + status(m) + "</b></html>", Font.PLAIN, 13, TEXT), BorderLayout.EAST);
            alerts.add(r);
        }
        if (lowList.isEmpty()) alerts.add(lbl("All stock levels are healthy", Font.PLAIN, 13, MUTED));
        JPanel alertWrap = panel(new BorderLayout());
        alertWrap.add(alerts, BorderLayout.NORTH);
        Card alertCard = titled("Low-Stock Alerts", alertWrap);

        String[] future = {"MySQL database integration", "Barcode scanning", "Expiry-date notifications",
                "Online medicine ordering", "Mobile application", "Multi-user access", "Automated reports", "Cloud storage"};
        JPanel chips = panel(new GridLayout(3, 3, 8, 8));
        for (String s : future) chips.add(new Chip(s, GREEN));
        JLabel about = lbl("<html><div style='width:620px'>The Pharmacy Management System is designed to automate pharmacy "
                + "operations such as medicine management, customer record maintenance, billing and inventory tracking. "
                + "It reduces manual effort, improves accuracy and provides an efficient solution for pharmacy management.</div></html>",
                Font.PLAIN, 13, TEXT);
        Card aboutCard = titled("About Project", vstack(10, about, lbl("Future Scope", Font.BOLD, 16, TEXT), chips));

        return vstack(14, stats, quick, grid(1, 2, sales, inv), grid(1, 2, recentCard, alertCard), aboutCard);
    }

    void salesLast7(String[] labels, double[] vals) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.minusDays(6 - i);
            labels[i] = d.toString().substring(5);
            double s = 0;
            for (Bill b : db.bills) if (b.date.equals(d)) s += b.total;
            vals[i] = s;
        }
    }

    // ======================================================================
    //  MEDICINE MANAGEMENT
    // ======================================================================
    JComponent pgMeds() {
        SearchField search = new SearchField("Search name, ID, manufacturer");
        JComboBox<String> catCb = combo(prepend("All Categories", CATS));
        JComboBox<String> sortCb = combo(new String[]{"Sort: ID", "Sort: Name", "Sort: Price", "Sort: Quantity", "Sort: Expiry"});
        DataTable dt = new DataTable("ID", "Name", "Category", "Manufacturer", "Batch", "Qty", "Price", "Expiry", "Status");
        dt.widths(60, 150, 100, 110, 65, 50, 70, 90, 100);
        List<Medicine> shown = new ArrayList<>();

        Runnable refresh = () -> {
            String q = search.getText().toLowerCase().trim();
            int ci = catCb.getSelectedIndex();
            shown.clear();
            for (Medicine m : db.meds)
                if ((ci == 0 || m.cat.equals(CATS[ci - 1])) && (m.name + m.id + m.mfr).toLowerCase().contains(q)) shown.add(m);
            Comparator<Medicine> cmp;
            switch (sortCb.getSelectedIndex()) {
                case 1: cmp = Comparator.comparing(m -> m.name); break;
                case 2: cmp = Comparator.comparingDouble(m -> m.price); break;
                case 3: cmp = Comparator.comparingInt(m -> m.qty); break;
                case 4: cmp = Comparator.comparing(m -> m.exp); break;
                default: cmp = Comparator.comparing(m -> m.id);
            }
            shown.sort(cmp);
            List<Object[]> rows = new ArrayList<>();
            boolean[] tint = new boolean[shown.size()];
            for (int i = 0; i < shown.size(); i++) {
                Medicine m = shown.get(i);
                rows.add(new Object[]{m.id, m.name, m.cat, m.mfr, m.batch, m.qty, money(m.price), m.exp.toString(), status(m)});
                tint[i] = m.exp.isBefore(LocalDate.now());
            }
            dt.setRows(rows, tint);
        };
        onChange(search, refresh);
        catCb.addActionListener(e -> refresh.run());
        sortCb.addActionListener(e -> refresh.run());
        refresh.run();

        Supplier<Medicine> sel = () -> {
            int r = dt.table.getSelectedRow();
            if (r < 0 || r >= shown.size()) { toast("Select a medicine row first", true); return null; }
            return shown.get(r);
        };
        dt.onDouble(() -> { Medicine m = sel.get(); if (m != null) viewMed(m); });
        JPanel acts = row(
                obtn("View", BLUE, e -> { Medicine m = sel.get(); if (m != null) viewMed(m); }),
                btn("Edit", GREEN, e -> { Medicine m = sel.get(); if (m != null) editMed(m); }),
                btn("Delete", RED, e -> { Medicine m = sel.get(); if (m != null) delMed(m); }),
                lbl("  Select a row first (double-click a row to view)", Font.PLAIN, 12, MUTED));
        return vstack(12, bar(search, catCb, sortCb, btn("Add Medicine", GREEN, e -> addMed())), acts, wrap(dt));
    }

    static void applyMed(Medicine m, Map<String, String> o) {
        m.name = o.get("name");
        m.cat = o.get("cat");
        m.mfr = o.get("mfr");
        m.batch = o.get("batch");
        m.qty = Integer.parseInt(o.get("qty"));
        m.min = Integer.parseInt(o.get("min"));
        m.price = Double.parseDouble(o.get("price"));
        m.exp = LocalDate.parse(o.get("exp"));
        m.sup = o.get("sup");
    }

    void addMed() {
        Map<String, String> init = new HashMap<>();
        init.put("exp", LocalDate.now().plusDays(365).toString());
        form("Add Medicine", MF, init, o -> {
            int mx = 0;
            for (Medicine x : db.meds) mx = Math.max(mx, num(x.id));
            Medicine m = new Medicine();
            m.id = String.format("MED%03d", mx + 1);
            applyMed(m, o);
            db.meds.add(m);
            save();
            go(cur);
            toast("Medicine added successfully", false);
        });
    }

    void editMed(Medicine m) {
        Map<String, String> v = new HashMap<>();
        v.put("name", m.name); v.put("cat", m.cat); v.put("mfr", m.mfr); v.put("batch", m.batch);
        v.put("qty", "" + m.qty); v.put("min", "" + m.min); v.put("price", "" + m.price);
        v.put("exp", m.exp.toString()); v.put("sup", m.sup);
        form("Edit " + m.id, MF, v, o -> {
            applyMed(m, o);
            save();
            go(cur);
            toast("Medicine updated", false);
        });
    }

    void delMed(Medicine m) {
        if (JOptionPane.showConfirmDialog(this, "Delete " + m.id + " (" + m.name + ")?", "Confirm delete",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            db.meds.remove(m);
            save();
            go(cur);
            toast("Medicine deleted", false);
        }
    }

    void viewMed(Medicine m) {
        info(m.name + "  [" + status(m) + "]", new String[][]{{"ID", m.id}, {"Category", m.cat},
                {"Manufacturer", m.mfr}, {"Batch", m.batch}, {"Quantity", "" + m.qty}, {"Price", money(m.price)},
                {"Expiry", m.exp.toString()}, {"Supplier", m.sup}});
    }

    // ======================================================================
    //  INVENTORY
    // ======================================================================
    JComponent pgInv() {
        DataTable dt = new DataTable("Medicine", "Current Stock", "Min Level", "Status", "Expiry", "Supplier");
        List<Object[]> rows = new ArrayList<>();
        boolean[] tint = new boolean[db.meds.size()];
        for (int i = 0; i < db.meds.size(); i++) {
            Medicine m = db.meds.get(i);
            rows.add(new Object[]{m.name, m.qty, m.min, status(m), m.exp.toString(), m.sup});
            tint[i] = m.exp.isBefore(LocalDate.now());
        }
        dt.setRows(rows, tint);
        Runnable upd = () -> {
            int r = dt.table.getSelectedRow();
            if (r < 0) { toast("Select a medicine row first", true); return; }
            upStock(db.meds.get(r));
        };
        dt.onDouble(upd);
        JPanel acts = row(btn("Update Stock", BLUE, e -> upd.run()),
                lbl("  Select a row first (double-click a row to update)", Font.PLAIN, 12, MUTED));
        return vstack(12, acts, wrap(dt));
    }

    void upStock(Medicine m) {
        Map<String, String> v = new HashMap<>();
        v.put("qty", "" + m.qty);
        v.put("min", "" + m.min);
        form("Update Stock: " + m.name, new Field[]{new Field("qty", "New Quantity", "int"),
                new Field("min", "Minimum Level", "int")}, v, o -> {
            m.qty = Integer.parseInt(o.get("qty"));
            m.min = Integer.parseInt(o.get("min"));
            save();
            go(cur);
            toast("Stock updated", false);
        });
    }

    // ======================================================================
    //  CUSTOMERS
    // ======================================================================
    JComponent pgCust() {
        SearchField search = new SearchField("Search customer");
        DataTable dt = new DataTable("ID", "Name", "Phone", "Email", "Address");
        List<Customer> shown = new ArrayList<>();
        Runnable refresh = () -> {
            String q = search.getText().toLowerCase().trim();
            shown.clear();
            List<Object[]> rows = new ArrayList<>();
            for (Customer c : db.custs)
                if ((c.name + c.id + c.phone).toLowerCase().contains(q)) {
                    shown.add(c);
                    rows.add(new Object[]{c.id, c.name, c.phone, c.email, c.addr});
                }
            dt.setRows(rows, null);
        };
        onChange(search, refresh);
        refresh.run();
        Supplier<Customer> sel = () -> {
            int r = dt.table.getSelectedRow();
            if (r < 0 || r >= shown.size()) { toast("Select a customer row first", true); return null; }
            return shown.get(r);
        };
        dt.onDouble(() -> { Customer c = sel.get(); if (c != null) hist(c); });
        JPanel acts = row(
                obtn("History", BLUE, e -> { Customer c = sel.get(); if (c != null) hist(c); }),
                btn("Edit", GREEN, e -> { Customer c = sel.get(); if (c != null) editCust(c); }),
                btn("Delete", RED, e -> { Customer c = sel.get(); if (c != null) delCust(c); }),
                lbl("  Select a row first (double-click a row for purchase history)", Font.PLAIN, 12, MUTED));
        return vstack(12, bar(search, btn("Add Customer", GREEN, e -> addCust())), acts, wrap(dt));
    }

    void addCust() {
        form("Add Customer", CF, new HashMap<>(), o -> {
            int mx = 0;
            for (Customer x : db.custs) mx = Math.max(mx, num(x.id));
            Customer c = new Customer();
            c.id = String.format("CUS%03d", mx + 1);
            c.name = o.get("name"); c.phone = o.get("phone"); c.email = o.get("email"); c.addr = o.get("addr");
            db.custs.add(c);
            save();
            go(cur);
            toast("Customer added", false);
        });
    }

    void editCust(Customer c) {
        Map<String, String> v = new HashMap<>();
        v.put("name", c.name); v.put("phone", c.phone); v.put("email", c.email); v.put("addr", c.addr);
        form("Edit Customer", CF, v, o -> {
            c.name = o.get("name"); c.phone = o.get("phone"); c.email = o.get("email"); c.addr = o.get("addr");
            save();
            go(cur);
            toast("Customer updated", false);
        });
    }

    void delCust(Customer c) {
        if (JOptionPane.showConfirmDialog(this, "Delete customer " + c.name + "?", "Confirm delete",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            db.custs.remove(c);
            save();
            go(cur);
            toast("Customer deleted", false);
        }
    }

    void hist(Customer c) {
        JDialog d = dlg("Purchase History");
        DataTable t = new DataTable("Bill", "Date", "Items", "Total");
        List<Object[]> rows = new ArrayList<>();
        double spent = 0;
        for (Bill b : db.bills) {
            if (!b.cust.equals(c.id)) continue;
            StringBuilder its = new StringBuilder();
            for (Item i : b.items) its.append(its.length() > 0 ? ", " : "").append(i.name).append(" x").append(i.qty);
            rows.add(new Object[]{b.id, b.date.toString(), its.toString(), money(b.total)});
            spent += b.total;
        }
        t.setRows(rows, null);
        t.widths(70, 90, 330, 80);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(LINE));
        sp.setPreferredSize(new Dimension(700, 300));
        JPanel south = panel(new BorderLayout());
        south.add(lbl("Total spent: " + money(spent), Font.BOLD, 14, TEXT), BorderLayout.WEST);
        south.add(closeRow(d), BorderLayout.EAST);
        JPanel body = panel(new BorderLayout(0, 12));
        body.setBorder(new EmptyBorder(18, 20, 16, 20));
        body.add(lbl("Purchase History: " + c.name, Font.BOLD, 17, TEXT), BorderLayout.NORTH);
        body.add(sp, BorderLayout.CENTER);
        body.add(south, BorderLayout.SOUTH);
        showDialog(d, body);
    }

    // ======================================================================
    //  BILLING
    // ======================================================================
    class BillingPanel extends JPanel {
        final JComboBox<String> custCb;
        final List<String> custIds = new ArrayList<>();
        final SearchField search = new SearchField("Type to filter medicines");
        final DefaultListModel<String> listModel = new DefaultListModel<>();
        final JList<String> medList = new JList<>(listModel);
        final List<Medicine> avail = new ArrayList<>();
        final JSpinner qtySp = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        final DataTable cartTable = new DataTable("Item", "Price", "Qty", "Total");
        { cartTable.widths(170, 70, 45, 80); }
        final JSpinner discSp = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 1.0));
        final JSpinner taxSp = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 100.0, 1.0));
        final JLabel subL = lbl("", Font.PLAIN, 14, TEXT), disL = lbl("", Font.PLAIN, 14, TEXT),
                taxL = lbl("", Font.PLAIN, 14, TEXT), totL = lbl("", Font.BOLD, 18, TEXT);

        BillingPanel() {
            super(new GridLayout(1, 2, 14, 14));
            setOpaque(false);
            List<String> names = new ArrayList<>();
            names.add("Walk-in Customer");
            for (Customer c : db.custs) { names.add(c.name); custIds.add(c.id); }
            custCb = combo(names.toArray(new String[0]));

            medList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            medList.setVisibleRowCount(7);
            medList.setSelectionBackground(new Color(0xdbe9fc));
            medList.setSelectionForeground(TEXT);
            JScrollPane lp = new JScrollPane(medList);
            lp.setBorder(new LineBorder(FIELD, 1, true));
            styleField(qtySp.getEditor() instanceof JSpinner.DefaultEditor
                    ? ((JSpinner.DefaultEditor) qtySp.getEditor()).getTextField() : new JTextField());
            onChange(search, this::fillMed);

            Card left = titled("Add Items", vstack(6,
                    lbl("Customer", Font.BOLD, 12, MUTED), custCb,
                    lbl("Search Medicine", Font.BOLD, 12, MUTED), search,
                    lbl("Medicine", Font.BOLD, 12, MUTED), lp,
                    lbl("Quantity", Font.BOLD, 12, MUTED), qtySp,
                    btn("Add to Cart", GREEN, e -> addCart())));
            JPanel leftTop = panel(new BorderLayout());
            leftTop.add(left, BorderLayout.NORTH);

            JPanel qtyBtns = row(obtn("+", BLUE, e -> changeQty(1)), obtn("\u2212", BLUE, e -> changeQty(-1)),
                    btn("Remove", RED, e -> changeQty(-99999)),
                    lbl("  Select a cart row first", Font.PLAIN, 12, MUTED));
            JPanel dt = panel(new GridLayout(1, 2, 12, 0));
            dt.add(vstack(3, lbl("Discount %", Font.BOLD, 12, MUTED), discSp));
            dt.add(vstack(3, lbl("Tax %", Font.BOLD, 12, MUTED), taxSp));
            discSp.addChangeListener(e -> drawCart());
            taxSp.addChangeListener(e -> drawCart());
            JPanel totals = new JPanel(new GridLayout(0, 1, 0, 2));
            totals.setOpaque(false);
            totals.add(pair("Subtotal", subL));
            totals.add(pair("Discount", disL));
            totals.add(pair("Tax", taxL));
            JPanel grand = pair("Grand Total", totL);
            grand.setBorder(new MatteBorder(2, 0, 0, 0, LINE));
            totals.add(grand);
            JPanel acts = row(btn("Generate Bill", BLUE, e -> genBill()),
                    obtn("Print Receipt", BLUE, e -> {
                        if (lastBill == null) toast("Generate a bill first", true);
                        else showReceipt(lastBill);
                    }),
                    btn("Clear Cart", RED, e -> { cart.clear(); drawCart(); }));
            Card right = titled("Cart", vstack(10, cartTable, qtyBtns, dt, totals, acts));
            JPanel rightTop = panel(new BorderLayout());
            rightTop.add(right, BorderLayout.NORTH);

            add(leftTop);
            add(rightTop);
            fillMed();
            drawCart();
        }

        JPanel pair(String name, JLabel value) {
            JPanel p = panel(new BorderLayout());
            p.setBorder(new EmptyBorder(4, 0, 2, 0));
            p.add(lbl(name, Font.PLAIN, 13, MUTED), BorderLayout.WEST);
            p.add(value, BorderLayout.EAST);
            return p;
        }

        void fillMed() {
            String q = search.getText().toLowerCase().trim();
            avail.clear();
            listModel.clear();
            for (Medicine m : db.meds)
                if (m.qty > 0 && !m.exp.isBefore(LocalDate.now()) && m.name.toLowerCase().contains(q)) {
                    avail.add(m);
                    listModel.addElement(m.name + " \u2014 " + money(m.price) + " (" + m.qty + " left)");
                }
            if (!avail.isEmpty()) medList.setSelectedIndex(0);
        }

        void addCart() {
            int i = medList.getSelectedIndex();
            if (i < 0) { toast("Select a medicine", true); return; }
            Medicine m = avail.get(i);
            int n = (Integer) qtySp.getValue();
            Item existing = null;
            for (Item c : cart) if (c.id.equals(m.id)) existing = c;
            int total = (existing == null ? 0 : existing.qty) + n;
            if (total > m.qty) { toast("Only " + m.qty + " in stock", true); return; }
            if (existing != null) existing.qty = total;
            else cart.add(new Item(m.id, m.name, n, m.price));
            drawCart();
            toast(m.name + " added", false);
        }

        void changeQty(int delta) {
            int r = cartTable.table.getSelectedRow();
            if (r < 0 || r >= cart.size()) { toast("Select a cart row first", true); return; }
            Item it = cart.get(r);
            Medicine m = null;
            for (Medicine x : db.meds) if (x.id.equals(it.id)) m = x;
            int n = it.qty + delta;
            if (n < 1) {
                cart.remove(r);
                drawCart();
                return;
            }
            if (m != null && n > m.qty) { toast("Stock limit reached", true); return; }
            it.qty = n;
            drawCart();
            cartTable.table.setRowSelectionInterval(r, r);
        }

        void drawCart() {
            List<Object[]> rows = new ArrayList<>();
            for (Item i : cart) rows.add(new Object[]{i.name, money(i.price), i.qty, money(i.qty * i.price)});
            cartTable.setRows(rows, null);
            double[] t = calc(cart, ((Number) discSp.getValue()).doubleValue(), ((Number) taxSp.getValue()).doubleValue());
            subL.setText(money(t[0]));
            disL.setText("-" + money(t[1]));
            taxL.setText(money(t[2]));
            totL.setText(money(t[3]));
        }

        void genBill() {
            if (cart.isEmpty()) { toast("Cart is empty", true); return; }
            double d = ((Number) discSp.getValue()).doubleValue(), t = ((Number) taxSp.getValue()).doubleValue();
            for (Item i : cart) for (Medicine m : db.meds) if (m.id.equals(i.id)) m.qty -= i.qty;
            Bill b = new Bill();
            b.id = "BILL" + (1001 + db.bills.size());
            b.date = LocalDate.now();
            int ci = custCb.getSelectedIndex();
            b.cust = ci <= 0 ? "" : custIds.get(ci - 1);
            for (Item i : cart) b.items.add(new Item(i.id, i.name, i.qty, i.price));
            double[] tt = calc(b.items, d, t);
            b.sub = tt[0]; b.dis = tt[1]; b.tax = tt[2]; b.total = tt[3];
            db.bills.add(b);
            lastBill = b;
            save();
            cart.clear();
            go("bill");
            toast("Bill " + b.id + " generated", false);
            showReceipt(b);
        }
    }

    void showReceipt(Bill b) {
        JDialog d = dlg("Receipt " + b.id);
        StringBuilder sb = new StringBuilder();
        sb.append("        PHARMACY MS\n");
        sb.append("     Tax Invoice / Receipt\n");
        sb.append("------------------------------------\n");
        sb.append("Bill     : ").append(b.id).append('\n');
        sb.append("Date     : ").append(b.date).append('\n');
        sb.append("Customer : ").append(b.cust.isEmpty() ? "Walk-in Customer" : cn(b.cust)).append('\n');
        sb.append("------------------------------------\n");
        for (Item i : b.items) {
            sb.append(i.name).append('\n');
            sb.append(String.format(Locale.US, "   %d x %s", i.qty, money(i.price)));
            String tot = money(i.qty * i.price);
            sb.append(String.format(Locale.US, "%" + Math.max(1, 36 - 3 - (i.qty + " x " + money(i.price)).length()) + "s", tot)).append('\n');
        }
        sb.append("------------------------------------\n");
        sb.append(String.format(Locale.US, "%-18s%18s%n", "Subtotal", money(b.sub)));
        sb.append(String.format(Locale.US, "%-18s%18s%n", "Discount", "-" + money(b.dis)));
        sb.append(String.format(Locale.US, "%-18s%18s%n", "Tax", money(b.tax)));
        sb.append(String.format(Locale.US, "%-18s%18s%n", "GRAND TOTAL", money(b.total)));
        sb.append("------------------------------------\n");
        sb.append("      Thank you. Get well soon!\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ta.setEditable(false);
        ta.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(new LineBorder(LINE));
        sp.setPreferredSize(new Dimension(420, 380));
        JPanel btns = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.add(btn("Print", BLUE, e -> {
            try { ta.print(); } catch (Exception ex) { toast("Printing failed", true); }
        }));
        btns.add(obtn("Close", BLUE, e -> d.dispose()));
        JPanel body = panel(new BorderLayout(0, 12));
        body.setBorder(new EmptyBorder(18, 20, 16, 20));
        body.add(lbl("Receipt " + b.id, Font.BOLD, 17, TEXT), BorderLayout.NORTH);
        body.add(sp, BorderLayout.CENTER);
        body.add(btns, BorderLayout.SOUTH);
        showDialog(d, body);
    }

    // ======================================================================
    //  SEARCH
    // ======================================================================
    JComponent pgSearch() {
        SearchField s = new SearchField("Search medicines, customers, bills");
        DataTable mt = new DataTable("ID", "Medicine", "Category", "Qty", "Status");
        DataTable ct = new DataTable("ID", "Name", "Phone", "Email");
        DataTable bt = new DataTable("Bill", "Customer", "Date", "Total");
        Runnable r = () -> {
            String q = s.getText().toLowerCase().trim();
            List<Object[]> mr = new ArrayList<>(), cr = new ArrayList<>(), br = new ArrayList<>();
            for (Medicine m : db.meds)
                if (mr.size() < 10 && (m.name + m.id + m.mfr + m.cat + m.batch).toLowerCase().contains(q))
                    mr.add(new Object[]{m.id, m.name, m.cat, m.qty, status(m)});
            for (Customer c : db.custs)
                if (cr.size() < 10 && (c.name + c.id + c.phone + c.email).toLowerCase().contains(q))
                    cr.add(new Object[]{c.id, c.name, c.phone, c.email});
            for (Bill b : db.bills)
                if (br.size() < 10 && (b.id + cn(b.cust) + b.date).toLowerCase().contains(q))
                    br.add(new Object[]{b.id, cn(b.cust), b.date.toString(), money(b.total)});
            mt.setRows(mr, null);
            ct.setRows(cr, null);
            bt.setRows(br, null);
        };
        onChange(s, r);
        r.run();
        return vstack(12, s, titled("Medicines", mt), titled("Customers", ct), titled("Bills", bt));
    }

    // ======================================================================
    //  REPORTS
    // ======================================================================
    JComponent pgRep() {
        String[][] types = {{"daily", "Daily Sales"}, {"weekly", "Weekly Sales"}, {"monthly", "Monthly Sales"},
                {"stock", "Stock Report"}};
        JPanel tabs = row();
        for (String[] t : types) {
            final String k = t[0];
            if (k.equals(repType)) tabs.add(btn(t[1], BLUE, null));
            else tabs.add(obtn(t[1], BLUE, e -> { repType = k; go("rep"); }));
        }

        if (repType.equals("stock")) {
            DataTable dt = new DataTable("ID", "Medicine", "Qty", "Min", "Price", "Stock Value", "Status");
            List<Object[]> rows = new ArrayList<>();
            double value = 0;
            int attention = 0;
            for (Medicine m : db.meds) {
                double v = m.qty * m.price;
                value += v;
                String st = status(m);
                if (!st.equals("In Stock")) attention++;
                rows.add(new Object[]{m.id, m.name, m.qty, m.min, money(m.price), money(v), st});
            }
            dt.setRows(rows, null);
            JPanel stats = grid(1, 3, stat("Medicines", "" + db.meds.size(), BLUE),
                    stat("Total Stock Value", money(value), GREEN), stat("Need Attention", "" + attention, ORANGE));
            return vstack(12, tabs, stats, wrap(dt));
        }

        int days = repType.equals("daily") ? 1 : repType.equals("weekly") ? 7 : 30;
        LocalDate from = LocalDate.now().minusDays(days - 1);
        List<Bill> inRange = new ArrayList<>();
        double sales = 0;
        int units = 0;
        Map<String, double[]> top = new LinkedHashMap<>(); // name -> {units, revenue}
        for (Bill b : db.bills) {
            if (b.date.isBefore(from)) continue;
            inRange.add(b);
            sales += b.total;
            for (Item i : b.items) {
                units += i.qty;
                double[] a = top.computeIfAbsent(i.name, k -> new double[2]);
                a[0] += i.qty;
                a[1] += i.qty * i.price;
            }
        }
        inRange.sort((a, b) -> b.date.equals(a.date) ? b.id.compareTo(a.id) : b.date.compareTo(a.date));

        DataTable bt = new DataTable("Bill", "Date", "Customer", "Items", "Total");
        List<Object[]> br = new ArrayList<>();
        for (Bill b : inRange) br.add(new Object[]{b.id, b.date.toString(), cn(b.cust), b.items.size(), money(b.total)});
        bt.setRows(br, null);

        List<Map.Entry<String, double[]>> sorted = new ArrayList<>(top.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));
        DataTable tt = new DataTable("Medicine", "Units Sold", "Revenue");
        List<Object[]> tr = new ArrayList<>();
        for (Map.Entry<String, double[]> e : sorted.subList(0, Math.min(8, sorted.size())))
            tr.add(new Object[]{e.getKey(), (int) e.getValue()[0], money(e.getValue()[1])});
        tt.setRows(tr, null);

        JPanel stats = grid(1, 3, stat("Total Sales", money(sales), GREEN), stat("Bills", "" + inRange.size(), BLUE),
                stat("Units Sold", "" + units, PURPLE));
        JPanel lists = inRange.isEmpty() ? vstack(0, wrap(bt)) : grid(1, 2, titled("Bills", bt), titled("Top Medicines", tt));
        return vstack(12, tabs, stats, lists);
    }

    // ======================================================================
    //  ADMIN PROFILE
    // ======================================================================
    JComponent pgProf() {
        JPanel g = panel(new GridLayout(0, 2, 16, 10));
        String[][] rows = {{"Name", "Administrator"}, {"Username", "admin"}, {"Role", "Pharmacy Admin"},
                {"Medicines", "" + db.meds.size()}, {"Customers", "" + db.custs.size()}, {"Bills", "" + db.bills.size()},
                {"Data file", DATA_FILE.getAbsolutePath()}};
        for (String[] r : rows) {
            g.add(lbl(r[0], Font.BOLD, 13, MUTED));
            g.add(lbl(r[1], Font.PLAIN, 13, TEXT));
        }
        JPanel acts = row(btn("Reset Demo Data", RED, e -> {
            if (JOptionPane.showConfirmDialog(this, "Replace all data with the demo data? This cannot be undone.",
                    "Confirm reset", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                db = seed();
                cart.clear();
                lastBill = null;
                save();
                go("prof");
                toast("Demo data restored", false);
            }
        }), obtn("Logout", BLUE, e -> go("out")));
        return vstack(12, titled("Admin Profile", g), acts);
    }

    // ======================================================================
    //  ENTRY POINT
    // ======================================================================
    public static void main(String[] args) {
        db = load();
        SwingUtilities.invokeLater(() -> new PharmacyApp().setVisible(true));
    }
}

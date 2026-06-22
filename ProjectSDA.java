import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.Queue;

// Visualisasi graph peta kampus UNS
public class ProjectSDA extends JPanel {

    // Tipe edge (warna & gaya garis)
    enum EdgeType { JALAN, HALTE }

    // Representasi vertex
    static class Vertex {
        String id;
        String label;
        int x, y;
        Vertex(String id, String label, int x, int y) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
        }
    }

    // Representasi edge dengan Bobot Waktu dalam Satuan Menit
    static class Edge {
        String from, to;
        EdgeType type;
        int weight; //menit
        Edge(String from, String to, EdgeType type, int weight) {
            this.from = from;
            this.to = to;
            this.type = type;
            this.weight = weight;
        }
    }

    private final Map<String, Vertex> vertices = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    // URUTAN HALTE BUS 
    private final List<String> RuteBus = Arrays.asList(
        "asrama", "halteStudcen", "halteFmipa", "halteFp", "halteInn", "halteFeb", "haltePasca", "asrama"
    );
    
    // Durasi antar halte (menit)
    private final int[] DurasiAntarGedung = {
        9, 5, 7, 5, 6, 4, 9
    };

    // Daftar semua halte transit untuk dicari jalur jalan kakinya
    private final List<String> semuaHalte = Arrays.asList(
        "halteStudcen", "haltePasca", "halteFeb", "halteFmipa", "halteFp", "halteInn"
    );

    // Jadwal keberangkatan bus dari Asrama
    private final String[][] JadwalBus = {
        {"06.30"}, {"08.00"}, {"09.30"}, {"11.00"}, {"12.30"}, {"14.00"}, {"15.30"}
    };

    // Tracking visualisasi rute aktif
    private List<String> ruteBusAktif = new ArrayList<>();
    private List<String> ruteKakiAktif = new ArrayList<>();
    private int totalWaktuJalanAktif = 0;                           // Menyimpan hasil hitungan menit jalan kaki terpilih
    private String HasilRekomendasi = "Silakan input gedung dan waktu tujuan kamu (>'-'<)";

    public ProjectSDA() {
        setPreferredSize(new Dimension(850, 800));
        setBackground(Color.WHITE);
        initData();
    }

    private void addVertex(String id, String label, int x, int y) {
        vertices.put(id, new Vertex(id, label, x, y));
    }

    private void addEdge(String from, String to, EdgeType type, int weight) {
        edges.add(new Edge(from, to, type, weight));
    }

    private void initData() {
        // Koordinat posisi gedung kampus
        addVertex("asrama",   "Asrama",            700, 70);
        addVertex("gerbel",   "Gerbang Belakang",  335, 140);
        addVertex("gersam",   "Gerbang Samping",   165, 250);
        addVertex("gerdep",   "Gerbang Depan",     294, 735);
        addVertex("tower",    "Tower UNS",         195, 725);
        addVertex("danau",    "Danau",             395, 560);
        addVertex("nh",       "Masjid Nurul Huda", 320, 190);
        addVertex("studcen",  "Student Center",    355, 210);
        addVertex("medcen",   "Medical Center",    355, 255);
        addVertex("ukm",      "Graha UKM",         425, 195);
        addVertex("fkip",     "FKIP",              320, 285);
        addVertex("fisip",    "FISIP",             115, 300);
        addVertex("fh",       "FH",                190, 355);
        addVertex("pasca",    "Pascasarjana",      320, 350);
        addVertex("stadion",  "Stadion UNS",       395, 310);
        addVertex("gor",      "GOR UNS",           450, 275);
        addVertex("javano",   "Javanologi",        530, 205);
        addVertex("fk",       "FK",                485, 360);
        addVertex("feb",      "FEB",               130, 410);
        addVertex("bahasa",   "UPT Bahasa",        170, 485);
        addVertex("fsrd",     "FSRD",              240, 475);
        addVertex("perpus",   "Perpustakaan",      290, 425);
        addVertex("tik",      "UPT TIK",           325, 395);
        addVertex("fmipa",    "FMIPA",             350, 450);
        addVertex("audit",    "Auditorium",        300, 515);
        addVertex("fib",      "FIB",               225, 520);
        addVertex("rektorat", "Rektorat",          300, 565);
        addVertex("fp",       "FP",                395, 540);
        addVertex("fapet",    "Fapet",             440, 450);
        addVertex("ft",       "FT",                170, 575);
        addVertex("lppm",     "LPPM",              285, 625);
        addVertex("spmb",     "SPMB",              310, 625);
        addVertex("akademik", "Akademik",          380, 625);
        addVertex("pplh",     "PPLH",              495, 625);
        addVertex("menwa",    "Menwa",             360, 675);
        addVertex("inn",      "UNS Inn",           289, 695);

        // Koordinat posisi titik halte bus
        addVertex("halteStudcen", "Halte Student Center", 343, 230);
        addVertex("haltePasca",   "Halte Pascasarjana",   325, 325);
        addVertex("halteFeb",     "Halte FEB",            130, 430);
        addVertex("halteFmipa",   "Halte FMIPA",          345, 430);
        addVertex("halteFp",      "Halte FP",             395, 510);
        addVertex("halteInn",     "Halte UNS Inn",        289, 712);

        // DATA ESTIMASI WAKTU JALAN KAKI
        Object[][] jalurJalan = {
            {"gerbel",  "halteStudcen", 2},     {"fkip",        "halteStudcen", 2},     {"medcen",   "halteStudcen", 1},
            {"studcen", "halteStudcen", 1},     {"javano",      "halteStudcen", 7},     {"nh",       "halteStudcen", 2},
            {"gor",     "halteStudcen", 5},     {"ukm",         "halteStudcen", 2},     {"pasca",    "haltePasca",   1},
            {"stadion", "haltePasca",   3},     {"fk",          "haltePasca",   6},     {"tik",      "halteFmipa",   3},
            {"fmipa",   "halteFmipa",   1},     {"fapet",       "halteFmipa",   5},     {"fh",       "halteFeb",     3},
            {"perpus",  "halteFeb",     6},     {"fsrd",        "halteFeb",     3},     {"bahasa",   "halteFeb",     4},
            {"fisip",   "halteFeb",     4},     {"gersam",      "halteFeb",     5},     {"feb",      "halteFeb",     1},
            {"ft",      "halteFeb",     8},     {"fib",         "halteFeb",     8},     {"audit",    "halteFp",      9},
            {"danau",   "halteFp",      3},     {"rektorat",    "halteFp",      5},     {"fp",       "halteFp",      1},
            {"pplh",    "halteInn",     4},     {"akademik",    "halteInn",     3},     {"spmb",     "halteInn",     3},
            {"menwa",   "halteInn",     6},     {"tower",       "halteInn",     2},     {"gerdep",   "halteInn",     1},
            {"inn",     "halteInn",     1},     {"lppm",        "halteInn",     2}
        };

        for (Object[] j : jalurJalan) {
            String u = (String) j[0];
            String v = (String) j[1];
            int menit = (Integer) j[2];
            addEdge(u, v, EdgeType.JALAN, menit);
            addEdge(v, u, EdgeType.JALAN, menit); // Dua arah 
        }

        // Jalur Utama Shuttle Bus Kampus
        for (int i = 0; i < RuteBus.size() - 1; i++) {
            addEdge(RuteBus.get(i), RuteBus.get(i + 1), EdgeType.HALTE, DurasiAntarGedung[i]);
        }
    }

    // Algoritma Dijkstra untuk mencari lintasan terpendek berdasarkan bobot (menit)
    private List<String> cariRuteJalanKakiDijkstra(String start, String targetGedung) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        
        // PENGOPTIMALAN 1: Menggunakan komparator eksplisit
        PriorityQueue<String> pq = new PriorityQueue<>((a, b) -> Integer.compare(distances.get(a), distances.get(b)));

        //----------------------------------------------------------------------
        // BLOK 1 : Looping semua Vertex -> O (v)
        for (String vertexId : vertices.keySet()) {                              // Berjalan sebanyak V kali O(V)
            distances.put(vertexId, Integer.MAX_VALUE);                          // Operasi put pada HashMap O(1)
        }
        //---------------------------------------------------------------------- Total Blok 1 = O(V)
        
        distances.put(start, 0);                                           // Operasi put HashMap O(1)
        pq.add(start);                                                           // Operasi insert pada Binary Heap (PriorityQueue) O(log V)

        // BLOK 2 : PROSES UTAMA DIJKSTRA -> O(V * E)
        while (!pq.isEmpty()) {                                                   // Berjalan maksimal sebanyak V kali O(V)
            String current = pq.poll();                                           // Mengambil nilai minimum dari heap O(log V)

            // 2A. RUTING KAKI (LINKEDLIST) -> O(V) 
            if (current.equals(targetGedung)) {                                   // Evaluasi kondisi O(1)
                LinkedList<String> path = new LinkedList<>();                     // Inisialisasi LinkedList O(1)
                String curr = targetGedung;                                       // Assignment variabel O(1)
                
                while (curr != null) {                                            // Loop berjalan sepanjang rute terpendek N (N <= V) O(V)
                    path.addFirst(curr);                                          // Insert di depan pada LinkedList (Konstan!) O(1)
                    curr = parentMap.get(curr);                                   // Akses HashMap parent O(1)
                } 
                return path; 
            }
            //Total BLOK 2A = O(V)

            // 2B. RELAKSASI EDGE -> O(E) ---
            for (Edge e : edges) {                                                  // Looping mencari tetangga dari list global E O(E)
                if (e.type == EdgeType.JALAN && e.from.equals(current)) {           // Cek kecocokan O(1)
                    int newDist = distances.get(current) + e.weight;                // Akses Map & hitung O(1)
                    if (newDist < distances.get(e.to)) {                            // Perbandingan bobot O(1)
                        distances.put(e.to, newDist);                               // Update Map jarak O(1)
                        parentMap.put(e.to, current);                               // Update Map parent O(1)
                        pq.add(e.to);                                               // Insert node kembali ke Priority Queue O(log V)
                    }
                }
            } //Total Sub-Blok 2B = O(E)
        } // ---------------------------------------------------------------------- Total Blok 2 = O(V * E)
        
        return new ArrayList<>(); // Kondisi fallback rute kosong O(1)
    }

    // Menghitung akumulasi bobot menit dari list rute yang terbentuk
    private int hitungTotalMenitRuteKaki(List<String> rute) {
        int total = 0;
        for (int i = 0; i < rute.size() - 1; i++) {
            String from = rute.get(i);
            String to = rute.get(i + 1);
            for (Edge e : edges) {
                if (e.type == EdgeType.JALAN && e.from.equals(from) && e.to.equals(to)) {
                    total += e.weight;
                    break;
                }
            }
        }
        return total;
    }

    // Perhitungan rute cerdas berbasis akumulasi bobot 
    public void hitungRekomendasiJadwal(String kodeGedung, String jamKuliahStr) {
        ruteBusAktif.clear();
        ruteKakiAktif.clear();
        totalWaktuJalanAktif = 0;
        
        int jamKuliahMenit = convertTimeToMinutes(jamKuliahStr);
        int batasAwalOperasional = convertTimeToMinutes("07.30");
        int batasAkhirOperasional = convertTimeToMinutes("16.20");

        if (jamKuliahMenit < batasAwalOperasional || jamKuliahMenit > batasAkhirOperasional) {
            HasilRekomendasi = "<html><font color='red'><b>JAM DI LUAR OPERASIONAL:</b> Kelas kamu berada di luar jam operasional shuttle bus (07.30 - 16.20) ╯︿╰ </font></html>";
            repaint();
            return;
        }

        String halteTujuanTerbaik = "";
        List<String> ruteKakiTerbaik = new ArrayList<>();
        int minWaktuJalan = Integer.MAX_VALUE;

        // Cari halte dengan waktu tempuh terkecil (Dijkstra)
        for (String halte : semuaHalte) {
            List<String> ruteKaki = cariRuteJalanKakiDijkstra(halte, kodeGedung);
            if (!ruteKaki.isEmpty()) {
                int totalMenitJalan = hitungTotalMenitRuteKaki(ruteKaki);
                if (totalMenitJalan < minWaktuJalan) {
                    minWaktuJalan = totalMenitJalan;
                    halteTujuanTerbaik = halte;
                    ruteKakiTerbaik = ruteKaki;
                }
            }
        }

        if (halteTujuanTerbaik.isEmpty()) {
            HasilRekomendasi = "<html><font color='red'>Rute menuju gedung tersebut tidak ditemukan di dalam peta jaringan.</font></html>";
            repaint();
            return;
        }

        totalWaktuJalanAktif = minWaktuJalan;

        // Mengalkulasi jalur Bus dari Asrama ke Halte Transit
        String halteAwal = "asrama";
        int indeksSekarang = RuteBus.indexOf(halteAwal);
        int indeksTujuan = RuteBus.indexOf(halteTujuanTerbaik);
        int durasiDiDalamBus = 0;
        
        ruteBusAktif.add(halteAwal);
        
        while (indeksSekarang != indeksTujuan) {
            durasiDiDalamBus += DurasiAntarGedung[indeksSekarang];
            indeksSekarang = (indeksSekarang + 1) % (RuteBus.size() - 1);
            ruteBusAktif.add(RuteBus.get(indeksSekarang));
        }

        ruteKakiAktif.addAll(ruteKakiTerbaik);

        int batasTiba = jamKuliahMenit - totalWaktuJalanAktif;
        StringBuilder listJadwalAmanHTML = new StringBuilder();
        int jumlahJadwalDitemukan = 0;

        String namaGedungTujuan = vertices.get(kodeGedung).label;

        for (int i = 0; i < JadwalBus.length; i++) {
            int jamBerangkatAsrama = convertTimeToMinutes(JadwalBus[i][0]);
            int waktuTibaDiTujuanBus = jamBerangkatAsrama + durasiDiDalamBus;
            
            int waktuTibaDiGedungAkhir = waktuTibaDiTujuanBus + totalWaktuJalanAktif;

            if (waktuTibaDiTujuanBus <= batasTiba) {
                jumlahJadwalDitemukan++;
                int sisaWaktuMenit = jamKuliahMenit - waktuTibaDiGedungAkhir;
                
                listJadwalAmanHTML.append(String.format(
                    "&nbsp;&nbsp;&nbsp;&nbsp;%d. <b>Bus Kampus</b> → Berangkat Asrama: <b>%s</b> | Tiba di %s: <b>%s</b> | " +
                    "<font color='#006600'><b>Tiba di %s: %s</b></font> (Sisa %d menit sebelum kelas)<br>",
                    jumlahJadwalDitemukan, JadwalBus[i][0], vertices.get(halteTujuanTerbaik).label, 
                    convertMinutesToTime(waktuTibaDiTujuanBus), namaGedungTujuan, convertMinutesToTime(waktuTibaDiGedungAkhir), sisaWaktuMenit
                ));
            }
        }

        if (jumlahJadwalDitemukan == 0) {
            HasilRekomendasi = "<html><font color='red'><b>WAKTU TERLALU MEPET!</b> Tidak ada jadwal bus yang sempat mengejar jam masuk kelas kamu pukul " + jamKuliahStr + ".</font></html>";
        } else {
            int totalEstMenit = durasiDiDalamBus + totalWaktuJalanAktif;
            HasilRekomendasi = "<html>" +
                    "<b>ANALISIS NAVIGASI RUTING KAMPUS (DIJKSTRA WEIGHTED GRAPH):</b><br>" +
                    "• Gedung tujuan: <b>" + namaGedungTujuan + "</b> (Jam Masuk Kelas: " + jamKuliahStr + ")<br>" +
                    "• Jarak Halte Terpilih: Turun di <font color='blue'><b>" + vertices.get(halteTujuanTerbaik).label + "</b></font> lalu jalan kaki (<b>" + totalWaktuJalanAktif + " menit</b>).<br>" +
                    "• Total akumulasi perjalanan (shuttle + jalan kaki): <b>" + totalEstMenit + " menit</b>.<br><br>" +
                    "<font color='green'><b>OPSI JADWAL BUS YANG DIREKOMENDASIKAN (TIDAK TELAT):</b></font><br>" +
                    listJadwalAmanHTML.toString() +
                    "</html>";
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Gambar rute default bus (Garis orange putus-putus)
        float[] dash = {6f, 6f};
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0));
        g2.setColor(new Color(255, 165, 0));
        for (Edge e : edges) {
            if (e.type == EdgeType.HALTE) {
                Vertex a = vertices.get(e.from);
                Vertex b = vertices.get(e.to);
                if (a != null && b != null) g2.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        // 2. Gambar rute jalan kaki (Garis abu-abu tipis)
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(200, 200, 200));
        for (Edge e : edges) {
            if (e.type == EdgeType.JALAN) {
                Vertex a = vertices.get(e.from);
                Vertex b = vertices.get(e.to);
                if (a != null && b != null) g2.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        // 3. Highlight Rute Perjalanan Bus Aktif (orange tebal)
        if (ruteBusAktif.size() > 1) {
            g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 69, 0)); 
            for (int i = 0; i < ruteBusAktif.size() - 1; i++) {
                Vertex a = vertices.get(ruteBusAktif.get(i));
                Vertex b = vertices.get(ruteBusAktif.get(i + 1));
                if (a != null && b != null) g2.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        // 4. Highlight Jalur Jalan Kaki Aktif (Bbiru cerah)
        if (ruteKakiAktif.size() > 1) {
            g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(30, 144, 255)); 
            for (int i = 0; i < ruteKakiAktif.size() - 1; i++) {
                Vertex a = vertices.get(ruteKakiAktif.get(i));
                Vertex b = vertices.get(ruteKakiAktif.get(i + 1));
                if (a != null && b != null) g2.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        // 5. Gambar Titik Node (Gedung & Halte)
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        for (Vertex v : vertices.values()) {
            boolean isHalte = v.id.startsWith("halte");
            boolean diRuteBus = ruteBusAktif.contains(v.id);
            boolean diRuteKaki = ruteKakiAktif.contains(v.id);

            if (diRuteBus && isHalte && v.id.equals(ruteBusAktif.get(ruteBusAktif.size() - 1))) {
                g2.setColor(Color.BLUE); 
            } else if (diRuteKaki && v.id.equals(ruteKakiAktif.get(ruteKakiAktif.size() - 1))) {
                g2.setColor(Color.RED);  
            } else if (v.id.equals("asrama")) {
                g2.setColor(Color.PINK); 
            } else if (diRuteBus || diRuteKaki) {
                g2.setColor(new Color(0, 191, 255));
            } else {
                g2.setColor(isHalte ? new Color(244, 164, 96) : Color.LIGHT_GRAY);
            }

            g2.fillOval(v.x - 7, v.y - 7, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawOval(v.x - 7, v.y - 7, 14, 14);
            g2.drawString(v.label, v.x + 12, v.y + 5);
        }
    }

    private static int convertTimeToMinutes(String timeStr) {
        String[] parts = timeStr.replace(".", ":").split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private static String convertMinutesToTime(int totalMinutes) {
        int hours = (totalMinutes / 60) % 24;
        int mins = totalMinutes % 60;
        return String.format("%02d.%02d", hours, mins);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Aplikasi Rekomendasi Jadwal Shuttle Bus Kampus UNS (◠‿◠)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            ProjectSDA mapPanel = new ProjectSDA();
            JScrollPane scrollMap = new JScrollPane(mapPanel);

            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            inputPanel.setBackground(new Color(240, 240, 240));

            inputPanel.add(new JLabel("Gedung / Fakultas Tujuan:"));
            String[] daftarPilihanGedung = {
                "fkip", "nh", "studcen", "medcen", "ukm", "fisip", "fh", "pasca", "stadion", "gor", "tower",
                "javano", "fk", "feb", "bahasa", "fsrd", "perpus", "tik", "fmipa", "audit", "fib", 
                "rektorat", "fp", "fapet", "ft", "lppm", "spmb", "akademik", "pplh", "menwa", "inn"
            };
            JComboBox<String> comboGedung = new JComboBox<>(daftarPilihanGedung);
            inputPanel.add(comboGedung);

            inputPanel.add(new JLabel("Jam Kuliah (hh.mm):"));
            JTextField txtJam = new JTextField("07.30", 5);
            inputPanel.add(txtJam);

            JButton btnHitung = new JButton("Cek Rute Navigasi");
            inputPanel.add(btnHitung);

            JLabel lblInfo = new JLabel(mapPanel.HasilRekomendasi);
            lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 13));

            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setBorder(BorderFactory.createTitledBorder("Informasi Sistem Operational & Jalur Efisien"));
            infoPanel.setPreferredSize(new Dimension(1100, 220));
            
            JPanel textPadding = new JPanel(new BorderLayout());
            textPadding.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            textPadding.add(lblInfo, BorderLayout.CENTER);
            
            JScrollPane scrollInfo = new JScrollPane(textPadding);
            infoPanel.add(scrollInfo, BorderLayout.CENTER);

            btnHitung.addActionListener(e -> {
                String gedung = (String) comboGedung.getSelectedItem();
                String jamStr = txtJam.getText().trim();
                mapPanel.hitungRekomendasiJadwal(gedung, jamStr);
                lblInfo.setText(mapPanel.HasilRekomendasi);
            });

            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(scrollMap, BorderLayout.CENTER);
            frame.add(infoPanel, BorderLayout.SOUTH);

            frame.setSize(1150, 850);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
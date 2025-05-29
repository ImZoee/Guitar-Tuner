import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.FileReader;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.CompoundBorder;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class tuner extends JFrame {
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 4096;
    // Modificat pentru a permite schimbarea frecvențelor
    private double[] GUITAR_FREQUENCIES = {82.41, 110.00, 146.83, 196.00, 246.94, 329.63}; // E2, A2, D3, G3, B3, E4
    private String[] STRING_NAMES = {"E (Mi Jos)", "A (La)", "D (Re)", "G (Sol)", "B (Si)", "E (Mi Sus)"};
    
    private TargetDataLine line;
    private boolean isRunning = false;
    private Thread audioThread;
    private JLabel statusLabel;
    private JLabel frequencyLabel;
    private JLabel noteLabel;
    private JProgressBar tuningMeter;
    private int selectedString = 0;
    
    private Map<Integer, JButton> stringButtons = new HashMap<>();
    
    // Adăugare variabile pentru UI îmbunătățit - Design modern
    private Color primaryColor = new Color(76, 175, 80);       // Verde vibrant
    private Color secondaryColor = new Color(33, 150, 243);    // Albastru modern
    private Color accentColor = new Color(255, 87, 34);        // Portocaliu accent
    private Color darkPrimary = new Color(27, 94, 32);         // Verde închis
    private Color surfaceColor = new Color(40, 44, 52);        // Gri închis pentru suprafețe
    private Color errorColor = new Color(244, 67, 54);         // Roșu pentru erori
    private Color successColor = new Color(76, 175, 80);       // Verde pentru succes
    private Color warningColor = new Color(255, 193, 7);       // Galben pentru warning
    
    private Font titleFont = new Font("Segoe UI", Font.BOLD, 32);
    private Font headerFont = new Font("Segoe UI", Font.BOLD, 20);
    private Font regularFont = new Font("Segoe UI", Font.PLAIN, 16);
    private Font smallFont = new Font("Segoe UI", Font.PLAIN, 14);
    
    // Adăugare variabile pentru teme - Tema cu fundal alb și text negru
    private Color backgroundColor = Color.WHITE;                // Fundal principal ALB
    private Color cardColor = Color.WHITE;                      // Culoare ALBĂ pentru carduri/infobox-uri
    private Color buttonHoverColor = new Color(66, 165, 245);  // Albastru pentru hover
    private Color buttonBorderColor = new Color(200, 200, 200); // Bordură gri deschis
    private Color meterBackgroundColor = new Color(248, 248, 248); // Fundal deschis pentru meter
    private Color footerBackgroundColor = new Color(245, 245, 245); // Fundal gri deschis pentru footer
    private Color textPrimaryColor = Color.BLACK;               // Text NEGRU pentru contrast cu fundalul alb
    private Color textSecondaryColor = new Color(80, 80, 80);   // Text gri închis pentru fundal alb
    
    // Variabile pentru animații și efecte avansate
    private Timer animationTimer;
    private float pulseEffect = 0.0f;
    private boolean pulseDirection = true;
    private List<ParticleEffect> particleEffects = new ArrayList<>();
    private BufferedImage backgroundTexture;
    private double waveAnimation = 0.0;
    private int hoverButtonIndex = -1;
    
    // Vizualizator de spectru audio
    private double[] audioSpectrum = new double[32];
    private double[] audioSpectrumTarget = new double[32];
    private double[] lastFrequencies = new double[10];
    private int lastFreqIndex = 0;
    private int animationSpeed = 20; // ms
    
    public tuner() {
        super("🎸 Acordor de Chitară Professional");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        setResizable(true);
        setMinimumSize(new Dimension(900, 650));
        
        // Încercăm să setăm look and feel modern
        try {
            // Setăm FlatLaf sau look and feel întunecat
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Configurăm tema întunecată pentru componentele Swing
            UIManager.put("Panel.background", backgroundColor);
            UIManager.put("Button.background", cardColor);
            UIManager.put("Button.foreground", textPrimaryColor);
            UIManager.put("Button.font", regularFont);
            UIManager.put("Button.arc", 12);
            UIManager.put("Label.font", regularFont);
            UIManager.put("Label.foreground", textPrimaryColor);
            UIManager.put("ProgressBar.background", meterBackgroundColor);
            UIManager.put("ProgressBar.foreground", primaryColor);
            UIManager.put("ProgressBar.selectionBackground", darkPrimary);
            UIManager.put("ProgressBar.selectionForeground", textPrimaryColor);
            UIManager.put("TitledBorder.titleColor", textPrimaryColor);
            UIManager.put("Border.color", buttonBorderColor);
            
        } catch (Exception e) {
            System.err.println("Nu s-a putut seta look and feel-ul: " + e.getMessage());
        }
        
        // Verificăm sistemul de operare
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isMacOS = osName.contains("mac");
        
        // Încearcă să solicite permisiuni pentru microfon automat pe macOS
        if (isMacOS) {
            // Afișăm un dialog de informare
            JOptionPane.showMessageDialog(this, 
                "Acordorul de chitară necesită acces la microfon.\n\n" +
                "Vom încerca să deschidem setările de Securitate & Confidențialitate pentru tine.\n" +
                "Trebuie să permiți aplicației Java sau Terminal să acceseze microfonul.\n\n" +
                "După ce acordați permisiunile, închideți și redeschideți această aplicație.",
                "Permisiuni macOS necesare", JOptionPane.INFORMATION_MESSAGE);
            
            // Încercăm mai multe metode pentru a deschide setările de confidențialitate
            boolean settingsOpened = false;
            
            try {
                // Metodă 1: Folosind URL scheme (pentru macOS mai nou)
                Runtime.getRuntime().exec(new String[]{"open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Microphone"});
                settingsOpened = true;
            } catch (IOException e) {
                System.err.println("Nu s-a putut deschide setările folosind URL scheme: " + e.getMessage());
            }
            
            if (!settingsOpened) {
                try {
                    // Metodă 2: Deschide panoul de Securitate & Confidențialitate
                    Runtime.getRuntime().exec(new String[]{"open", "/System/Library/PreferencePanes/Security.prefPane"});
                    
                    // Afișăm instrucțiuni suplimentare
                    JOptionPane.showMessageDialog(this,
                        "Te rugăm să navighezi la tab-ul 'Confidențialitate' și apoi selectează 'Microfon' din lista din stânga.",
                        "Instrucțiuni suplimentare", JOptionPane.INFORMATION_MESSAGE);
                    
                } catch (IOException e) {
                    System.err.println("Nu s-a putut deschide setările de securitate: " + e.getMessage());
                    
                    // Metodă 3: Oferim instrucțiuni manual
                    JOptionPane.showMessageDialog(this,
                        "Nu am putut deschide automat setările. Te rugăm să urmezi acești pași manual:\n\n" +
                        "1. Deschide Preferințe Sistem din dock sau meniul Apple\n" +
                        "2. Selectează 'Securitate & Confidențialitate'\n" +
                        "3. Navighează la tab-ul 'Confidențialitate'\n" +
                        "4. Selectează 'Microfon' din lista din stânga\n" +
                        "5. Bifează 'Java' sau 'Terminal' pentru a permite accesul\n" +
                        "6. Repornește această aplicație",
                        "Instrucțiuni manuale", JOptionPane.WARNING_MESSAGE);
                }
            }
            
            // Încearcă să creeze un fișier care să indice faptul că permisiunile au fost solicitate
            try {
                File permFile = new File(System.getProperty("user.home") + "/.java_mic_perm_requested");
                permFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Nu s-a putut crea fișierul de permisiuni: " + e.getMessage());
            }
        } else {
            // Pentru alte sisteme de operare
            JOptionPane.showMessageDialog(this, 
                "Acordorul de chitară necesită acces la microfon.\n" +
                "Vă rugăm să acordați permisiunile necesare când vi se solicită.\n" +
                "Dacă nu detectează sunetul, verificați setările microfonului în sistemul de operare.",
                "Informație", JOptionPane.INFORMATION_MESSAGE);
        }
        
        initUI();
        initAudio();
    }
    
    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        
        // Inițializăm timer-ul de animație cu rata foarte mare pentru fluiditate maximă
        animationTimer = new Timer(8, e -> { // 125 FPS pentru animații ultra-fluide
            
            // Intensitatea animației depinde de statusul microfonului
            float animationIntensity = isRunning ? 1.0f : 0.3f;
            
            // Actualizăm efectul de pulsație cu intensitate variabilă
            float pulseSpeed = isRunning ? 0.05f : 0.02f; // Mai rapid când microfonul este activ
            if (pulseDirection) {
                pulseEffect += pulseSpeed * animationIntensity;
                if (pulseEffect >= 1.0f) {
                    pulseEffect = 1.0f;
                    pulseDirection = false;
                }
            } else {
                pulseEffect -= pulseSpeed * animationIntensity;
                if (pulseEffect <= 0.0f) {
                    pulseEffect = 0.0f;
                    pulseDirection = true;
                }
            }
            
            // Actualizăm animația valurilor cu viteză crescută în timpul detecției
            float waveSpeed = isRunning ? 0.12f : 0.04f;
            waveAnimation += waveSpeed;
            if (waveAnimation > Math.PI * 2) {
                waveAnimation = 0;
            }
            
            // Actualizăm spectrul audio cu interpolare ultra-smoothă
            float interpolationFactor = isRunning ? 0.12f : 0.05f; // Mai responsiv când microfonul este activ
            for (int i = 0; i < audioSpectrum.length; i++) {
                audioSpectrum[i] += (audioSpectrumTarget[i] - audioSpectrum[i]) * interpolationFactor;
            }
            
            // Adăugăm efecte continue de particule în timpul detecției sunetului
            if (isRunning && Math.random() > 0.92) {
                // Particule ambientale continue în timpul interceptării
                for (int i = 0; i < 2; i++) {
                    int x = (int)(Math.random() * getWidth());
                    int y = (int)(Math.random() * getHeight());
                    Color particleColor = new Color(
                        100 + (int)(Math.random() * 155),
                        150 + (int)(Math.random() * 105),
                        200 + (int)(Math.random() * 55),
                        120 + (int)(Math.random() * 80)
                    );
                    addParticleEffect(x, y, particleColor);
                }
            }
            
            // Actualizăm și ștergem efectele de particule
            for (int i = particleEffects.size() - 1; i >= 0; i--) {
                ParticleEffect effect = particleEffects.get(i);
                effect.update();
                if (effect.isDead()) {
                    particleEffects.remove(i);
                }
            }
            
            // Forțăm repictarea componentelor pentru animații continue
            repaint();
        });
        animationTimer.start();
        
        // HEADER MODERN CU GRADIENT ȘI EFECTE AVANSATE
        createModernHeader();
        
        // PANOU PRINCIPAL CU ORGANIZARE ÎN CARDURI
        createMainContentPanel();
        
        // FOOTER CU CONTROALE ȘI STATUS
        createModernFooter();
        
        // Select the first string by default cu efect de tranziție
        selectString(0);
    }
    
    private void createModernHeader() {
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int w = getWidth();
                int h = getHeight();
                
                // Gradient complex cu multiple opriri
                float[] fractions = {0.0f, 0.3f, 0.7f, 1.0f};
                Color[] colors = {
                    new Color(13, 71, 161),     // Albastru profund
                    new Color(25, 118, 210),    // Albastru mediu
                    new Color(33, 150, 243),    // Albastru deschis
                    new Color(100, 181, 246)    // Albastru foarte deschis
                };
                
                LinearGradientPaint gradient = new LinearGradientPaint(
                    0, 0, w, h, fractions, colors
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, w, h);
                
                // Efecte de lumină animată
                if (isRunning) {
                    // Desenăm particule luminoase
                    for (ParticleEffect effect : particleEffects) {
                        if (effect.y < h) {
                            effect.draw(g2d);
                        }
                    }
                    
                    // Valuri de energie în fundal
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    for (int i = 0; i < 5; i++) {
                        g2d.setColor(Color.WHITE);
                        for (int x = 0; x < w; x += 8) {
                            double y = h/2 + Math.sin(x * 0.015 + waveAnimation + i * 1.2) * (15 + i * 3);
                            g2d.fillOval(x, (int)y, 4, 4);
                        }
                    }
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }
                
                // Overlay cu transparență pentru text
                GradientPaint overlay = new GradientPaint(
                    0, 0, new Color(0, 0, 0, 30),
                    0, h, new Color(0, 0, 0, 80)
                );
                g2d.setPaint(overlay);
                g2d.fillRect(0, 0, w, h);
            }
        };
        
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(0, 120));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        // Titlu principal cu stil modern
        JPanel titleContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        titleContainer.setOpaque(false);
        
        // Icon modern pentru chitară
        JLabel iconLabel = new JLabel("🎸");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setForeground(Color.WHITE);
        
        // Titlu cu efect de shadow
        JLabel titleLabel = new JLabel("ACORDOR PROFESIONAL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Shadow effect
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.setFont(getFont());
                g2d.drawString(getText(), 3, getHeight() - 5);
                
                // Main text
                g2d.setColor(getForeground());
                g2d.drawString(getText(), 1, getHeight() - 7);
            }
        };
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);
        
        // Subtitlu
        JLabel subtitleLabel = new JLabel("Precizie profesională pentru muzicieni profesioniști");
        subtitleLabel.setFont(regularFont);
        subtitleLabel.setForeground(new Color(200, 230, 255));
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitleLabel);
        
        titleContainer.add(iconLabel);
        titleContainer.add(textPanel);
        
        headerPanel.add(titleContainer, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);
    }
    
    private void createMainContentPanel() {
        JPanel mainContainer = new JPanel(new BorderLayout(20, 20));
        mainContainer.setBackground(backgroundColor);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // PARTEA STÂNGĂ - SELECȚIA CORZILOR ÎN STIL CARD
        JPanel leftPanel = createStringSelectionCard();
        
        // PARTEA CENTRALĂ - VIZUALIZATORUL PRINCIPAL
        JPanel centerPanel = createVisualizationCard();
        
        // PARTEA DREAPTĂ - INFORMAȚII ȘI CONTROALE
        JPanel rightPanel = createControlsCard();
        
        mainContainer.add(leftPanel, BorderLayout.WEST);
        mainContainer.add(centerPanel, BorderLayout.CENTER);
        mainContainer.add(rightPanel, BorderLayout.EAST);
        
        add(mainContainer, BorderLayout.CENTER);
    }
    
    private JPanel createStringSelectionCard() {
        JPanel cardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Card background cu shadow
                g2d.setColor(new Color(0, 0, 0, 25));
                g2d.fillRoundRect(4, 4, getWidth()-4, getHeight()-4, 20, 20);
                
                g2d.setColor(cardColor);
                g2d.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 20, 20);
                
                // Bordură subtilă
                g2d.setColor(buttonBorderColor);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, getWidth()-5, getHeight()-5, 20, 20);
            }
        };
        
        cardPanel.setLayout(new BorderLayout(0, 15));
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(280, 0));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        
        // Header pentru card
        JLabel cardTitle = new JLabel("🎯 SELECTARE CORZI");
        cardTitle.setFont(headerFont);
        cardTitle.setForeground(textPrimaryColor);
        cardTitle.setHorizontalAlignment(JLabel.CENTER);
        
        JLabel cardSubtitle = new JLabel("Alegeți coarda pentru acordaj");
        cardSubtitle.setFont(smallFont);
        cardSubtitle.setForeground(textSecondaryColor);
        cardSubtitle.setHorizontalAlignment(JLabel.CENTER);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(cardTitle);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(cardSubtitle);
        
        // Panel pentru butoanele de corzi - design modern
        JPanel stringsPanel = new JPanel();
        stringsPanel.setLayout(new GridLayout(6, 1, 0, 12));
        stringsPanel.setOpaque(false);
        
        // Culori moderne pentru corzi
        Color[] modernStringColors = {
            new Color(229, 57, 53),   // Roșu vibrant pentru E jos
            new Color(255, 152, 0),   // Portocaliu pentru A
            new Color(76, 175, 80),   // Verde pentru D
            new Color(33, 150, 243),  // Albastru pentru G
            new Color(156, 39, 176),  // Violet pentru B
            new Color(96, 125, 139)   // Gri-albastru pentru E sus
        };
        
        for (int i = 0; i < STRING_NAMES.length; i++) {
            final int stringIndex = i;
            final Color stringColor = modernStringColors[i];
            
            JButton stringButton = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int w = getWidth();
                    int h = getHeight();
                    
                    // Design modern pentru butoane
                    if (selectedString == stringIndex) {
                        // Buton selectat - gradient vibrant
                        GradientPaint gradient = new GradientPaint(
                            0, 0, stringColor,
                            0, h, stringColor.darker()
                        );
                        g2d.setPaint(gradient);
                        g2d.fillRoundRect(0, 0, w, h, 15, 15);
                        
                        // Glow effect pentru butonul selectat
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f * pulseEffect));
                        g2d.setColor(Color.WHITE);
                        g2d.fillRoundRect(2, 2, w-4, h-4, 13, 13);
                        g2d.setComposite(AlphaComposite.SrcOver);
                        
                        setForeground(Color.WHITE);
                    } else {
                        // Buton normal - fundal alb
                        g2d.setColor(Color.WHITE);
                        g2d.fillRoundRect(0, 0, w, h, 15, 15);
                        
                        // Indicator de culoare pe marginea stângă
                        g2d.setColor(stringColor);
                        g2d.fillRoundRect(0, 0, 6, h, 15, 15);
                        
                        // Bordură subtilă
                        g2d.setColor(buttonBorderColor);
                        g2d.setStroke(new BasicStroke(1f));
                        g2d.drawRoundRect(0, 0, w-1, h-1, 15, 15);
                        
                        if (hoverButtonIndex == stringIndex) {
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                            g2d.setColor(stringColor);
                            g2d.fillRoundRect(0, 0, w, h, 15, 15);
                            g2d.setComposite(AlphaComposite.SrcOver);
                        }
                        
                        setForeground(textPrimaryColor);
                    }
                    
                    super.paintComponent(g);
                }
            };
            
            stringButton.setText(STRING_NAMES[i] + " • " + String.format("%.1f Hz", GUITAR_FREQUENCIES[i]));
            stringButton.setFont(regularFont);
            stringButton.setFocusPainted(false);
            stringButton.setBorderPainted(false);
            stringButton.setContentAreaFilled(false);
            stringButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            stringButton.setPreferredSize(new Dimension(0, 45));
            
            stringButton.addActionListener(e -> selectString(stringIndex));
            
            // Mouse effects moderne
            stringButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hoverButtonIndex = stringIndex;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    hoverButtonIndex = -1;
                    repaint();
                }
            });
            
            stringsPanel.add(stringButton);
            stringButtons.put(i, stringButton);
        }
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        cardPanel.add(stringsPanel, BorderLayout.CENTER);
        
        return cardPanel;
    }
    
    private JPanel createVisualizationCard() {
        JPanel cardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Card background cu shadow
                g2d.setColor(new Color(0, 0, 0, 25));
                g2d.fillRoundRect(4, 4, getWidth()-4, getHeight()-4, 20, 20);
                
                g2d.setColor(cardColor);
                g2d.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 20, 20);
                
                // Bordură subtilă
                g2d.setColor(buttonBorderColor);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, getWidth()-5, getHeight()-5, 20, 20);
            }
        };
        
        cardPanel.setLayout(new BorderLayout(0, 15));
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        
        // Header pentru card
        JLabel cardTitle = new JLabel("🎼 VIZUALIZATOR AUDIO");
        cardTitle.setFont(headerFont);
        cardTitle.setForeground(textPrimaryColor);
        cardTitle.setHorizontalAlignment(JLabel.CENTER);
        
        JLabel cardSubtitle = new JLabel("Spectru în timp real");
        cardSubtitle.setFont(smallFont);
        cardSubtitle.setForeground(textSecondaryColor);
        cardSubtitle.setHorizontalAlignment(JLabel.CENTER);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(cardTitle);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(cardSubtitle);
        
        // Spectrum Analyzer moderne
        JPanel spectrumPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Fundal pentru spectrum
                g2d.setColor(meterBackgroundColor);
                g2d.fillRoundRect(0, 0, w, h, 10, 10);
                
                // Grid lines pentru referință
                g2d.setColor(buttonBorderColor);
                for (int i = 1; i < 4; i++) {
                    int y = h * i / 4;
                    g2d.drawLine(10, y, w-10, y);
                }
                
                // Desenăm barele de spectru
                int barWidth = (w - 40) / audioSpectrum.length;
                for (int i = 0; i < audioSpectrum.length; i++) {
                    int barHeight = (int) (audioSpectrum[i] * (h - 20) / 100);
                    int x = 20 + i * barWidth;
                    int y = h - 10 - barHeight;
                    
                    // Culoare gradient pentru bare
                    Color barColor;
                    if (barHeight < h * 0.3) {
                        barColor = new Color(76, 175, 80);
                    } else if (barHeight < h * 0.7) {
                        barColor = new Color(255, 193, 7);
                    } else {
                        barColor = new Color(244, 67, 54);
                    }
                    
                    GradientPaint barGradient = new GradientPaint(
                        x, y, barColor.brighter(),
                        x, y + barHeight, barColor.darker()
                    );
                    g2d.setPaint(barGradient);
                    g2d.fillRoundRect(x, y, barWidth - 2, barHeight, 3, 3);
                    
                    // Glow effect pentru bare active
                    if (barHeight > 5) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                        g2d.setColor(Color.WHITE);
                        g2d.fillRoundRect(x-1, y-1, barWidth, barHeight+2, 4, 4);
                        g2d.setComposite(AlphaComposite.SrcOver);
                    }
                }
                
                // Indicator pentru frecvența țintă
                if (selectedString >= 0) {
                    double targetFreq = GUITAR_FREQUENCIES[selectedString];
                    int targetX = (int)(20 + (targetFreq / 400.0) * (w - 40));
                    g2d.setColor(primaryColor);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawLine(targetX, 10, targetX, h-10);
                    g2d.setFont(smallFont);
                    g2d.drawString("Target", targetX + 5, 25);
                }
            }
        };
        spectrumPanel.setPreferredSize(new Dimension(0, 200));
        spectrumPanel.setOpaque(false);
        
        // Tuning meter modern
        JPanel tuningMeterPanel = createModernTuningMeter();
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        cardPanel.add(spectrumPanel, BorderLayout.CENTER);
        cardPanel.add(tuningMeterPanel, BorderLayout.SOUTH);
        
        return cardPanel;
    }
    
    private JPanel createModernTuningMeter() {
        JPanel meterPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Card background cu shadow
                g2d.setColor(new Color(0, 0, 0, 25));
                g2d.fillRoundRect(4, 4, getWidth()-4, getHeight()-4, 20, 20);
                
                g2d.setColor(cardColor);
                g2d.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 20, 20);
                
                // Bordură subtilă
                g2d.setColor(buttonBorderColor);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, getWidth()-5, getHeight()-5, 20, 20);
            }
        };
        
        meterPanel.setLayout(new BorderLayout(0, 15));
        meterPanel.setOpaque(false);
        meterPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        
        // Header pentru card
        JLabel cardTitle = new JLabel("🎯 TUNING METER");
        cardTitle.setFont(headerFont);
        cardTitle.setForeground(textPrimaryColor);
        cardTitle.setHorizontalAlignment(JLabel.CENTER);
        
        JLabel cardSubtitle = new JLabel("Meter de acordaj profesional");
        cardSubtitle.setFont(smallFont);
        cardSubtitle.setForeground(textSecondaryColor);
        cardSubtitle.setHorizontalAlignment(JLabel.CENTER);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(cardTitle);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(cardSubtitle);
        
        // Progress bar modern pentru tuning meter
        tuningMeter = new JProgressBar(JProgressBar.HORIZONTAL, -50, 50) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Desenăm fundalul principal
                RoundRectangle2D background = new RoundRectangle2D.Float(0, 0, w, h, 15, 15);
                
                // Fundal pentru tuning meter
                g2d.setColor(meterBackgroundColor);
                g2d.fill(background);
                
                // Desenăm marcajele de scală cu mai multe detalii
                g2d.setColor(textSecondaryColor);
                int middle = w / 2;
                
                // Marcajele ultra-fine pentru precizie maximă profesională
                for (int i = -50; i <= 50; i += 1) {  // Mărcuțe la fiecare cent
                    int x = middle + (i * middle / 50);
                    int tickHeight = 2;
                    if (i % 5 == 0) tickHeight = 4;    // La fiecare 5 cenți
                    if (i % 10 == 0) tickHeight = 6;   // La fiecare 10 cenți
                    if (i % 20 == 0) tickHeight = 10;  // La fiecare 20 cenți
                    
                    // Culori diferite pentru diferite nivele de precizie
                    if (Math.abs(i) <= 1) {
                        g2d.setColor(successColor); // Verde pentru zona perfectă
                    } else if (Math.abs(i) <= 5) {
                        g2d.setColor(warningColor); // Galben pentru zona bună
                    } else {
                        g2d.setColor(new Color(100, 104, 112)); // Gri pentru rest
                    }
                    
                    int y = (h - tickHeight) / 2;
                    g2d.fillRect(x, y, 1, tickHeight);
                    
                    // Text pentru marcajele principale cu precizie îmbunătățită
                    if (i % 10 == 0 && i != 0) {
                        String text = String.valueOf(i);
                        FontMetrics fm = g2d.getFontMetrics(smallFont);
                        int textWidth = fm.stringWidth(text);
                        g2d.setFont(smallFont);
                        g2d.setColor(textSecondaryColor);
                        g2d.drawString(text, x - textWidth/2, h - 5);
                    }
                }
                
                // Linia centrală mai proeminentă
                g2d.setColor(textSecondaryColor);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawLine(middle, 5, middle, h-5);
                
                // Afișare "0" la centru
                g2d.setFont(smallFont.deriveFont(Font.BOLD));
                g2d.setColor(Color.WHITE);
                g2d.drawString("0", middle - 3, h - 5);
                
                // Desenăm valoarea curentă
                int value = getValue();
                
                if (value != 0) {
                    // Calculăm culoarea bazată pe cât de aproape este de acordaj
                    Color fillColor;
                    if (Math.abs(value) < 3) {
                        fillColor = successColor; // Verde pentru perfect
                    } else if (Math.abs(value) < 10) {
                        fillColor = warningColor; // Galben pentru aproape
                    } else {
                        fillColor = errorColor; // Roșu pentru departe
                    }
                    
                    // Calculăm dimensiunea indicatorului cu precizie îmbunătățită
                    double preciseWidth = ((double)Math.abs(value) * middle) / 50.0;
                    int progressWidth = (int)Math.round(preciseWidth);
                    
                    // Creăm forma pentru indicator
                    Shape indicatorShape;
                    if (value < 0) {
                        indicatorShape = new RoundRectangle2D.Float(
                            middle - progressWidth, 4, progressWidth, h-8, 8, 8);
                    } else {
                        indicatorShape = new RoundRectangle2D.Float(
                            middle, 4, progressWidth, h-8, 8, 8);
                    }
                    
                    // Clipping pentru forma rotunjită
                    Area indicatorArea = new Area(indicatorShape);
                    indicatorArea.intersect(new Area(background));
                    
                    // Gradient pentru indicator
                    GradientPaint indicatorGradient = new GradientPaint(
                        0, 4, brightenColor(fillColor, 0.3f),
                        0, h-4, fillColor.darker()
                    );
                    g2d.setPaint(indicatorGradient);
                    g2d.fill(indicatorArea);
                    
                    // Efect de strălucire pentru acordaj aproape perfect
                    if (Math.abs(value) < 5) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f * pulseEffect));
                        g2d.setColor(Color.WHITE);
                        g2d.fill(indicatorArea);
                        g2d.setComposite(AlphaComposite.SrcOver);
                    }
                    
                    // Afișează valoarea exactă pe indicator
                    if (progressWidth > 20) {
                        g2d.setColor(Color.WHITE);
                        g2d.setFont(smallFont.deriveFont(Font.BOLD, 10f));
                        String valueText = String.valueOf(Math.abs(value));
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(valueText);
                        int textX = value < 0 ? middle - progressWidth/2 - textWidth/2 : middle + progressWidth/2 - textWidth/2;
                        g2d.drawString(valueText, textX, h/2 + 3);
                    }
                }
                
                // Afișăm textul custom din setString()
                String text = getString();
                if (text != null && !text.isEmpty()) {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(regularFont.deriveFont(Font.BOLD, 12f));
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(text);
                    g2d.drawString(text, (w - textWidth) / 2, 15);
                }
                
                // Bordură exterioară
                g2d.setColor(buttonBorderColor);
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.draw(background);
            }
        };
        
        tuningMeter.setPreferredSize(new Dimension(0, 70));
        tuningMeter.setStringPainted(true);
        tuningMeter.setValue(0);
        tuningMeter.setString("Acordaj");
        
        // Panel pentru informații de status
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        // Status cu stil modern
        JLabel statusInfo = new JLabel("Precizie: ±1 cent");
        statusInfo.setFont(smallFont);
        statusInfo.setForeground(textSecondaryColor);
        statusInfo.setHorizontalAlignment(JLabel.CENTER);
        statusInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel rangeInfo = new JLabel("Interval: -50 / +50 cenți");
        rangeInfo.setFont(smallFont);
        rangeInfo.setForeground(textSecondaryColor);
        rangeInfo.setHorizontalAlignment(JLabel.CENTER);
        rangeInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        infoPanel.add(statusInfo);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(rangeInfo);
        
        meterPanel.add(headerPanel, BorderLayout.NORTH);
        meterPanel.add(tuningMeter, BorderLayout.CENTER);
        meterPanel.add(infoPanel, BorderLayout.SOUTH);
        
        return meterPanel;
    }
    
    private void selectString(int index) {
        // Memorăm string-ul anterior selectat pentru efecte de tranziție
        int previousString = selectedString;
        selectedString = index;
        
        // Actualizare UI pentru toate butoanele de corzi
        for (int i = 0; i < STRING_NAMES.length; i++) {
            JButton button = stringButtons.get(i);
            button.repaint(); // Forțăm redesenarea butonului cu noua stare
            
            if (i == selectedString) {
                button.setForeground(Color.WHITE);
                // Adăugăm un efect vizual de feedback pentru utilizator
                if (i != previousString) {
                    // Definim culorile pentru efect
                    Color[] effectColors = {
                        new Color(241, 148, 138), // Roșu deschis pentru E
                        new Color(245, 176, 65),  // Galben pentru A
                        new Color(88, 214, 141),  // Verde pentru D
                        new Color(93, 173, 226),  // Albastru deschis pentru G
                        new Color(165, 105, 189), // Violet pentru B
                        new Color(93, 109, 126)   // Gri albăstrui pentru E sus
                    };
                    
                    for (int j = 0; j < 3; j++) {
                        // Adăugăm particule pentru efectul de selecție
                        addParticleEffect(
                            button.getX() + button.getWidth()/2, 
                            button.getY() + button.getHeight()/2,
                            effectColors[i]
                        );
                    }
                }
            } else {
                button.setForeground(Color.DARK_GRAY);
            }
        }
        
        // Actualizare eticheta cu nota țintă
        noteLabel.setText("Notă țintă: " + STRING_NAMES[selectedString] + " (" + 
                          String.format("%.2f", GUITAR_FREQUENCIES[selectedString]) + " Hz)");
        
        // Reset acordaj cu efect de feedback
        tuningMeter.setValue(0);
        tuningMeter.setString("Acordaj pentru " + STRING_NAMES[selectedString]);
        
        // Afișăm un mesaj de feedback pentru utilizator
        statusLabel.setText("Coarda selectată: " + STRING_NAMES[selectedString]);
        
        // Repaint pentru a actualiza vizualizarea
        repaint();
    }
    
    private void initAudio() {
        try {
            System.out.println("Inițializare audio...");
            
            // Listăm toate dispozitivele de intrare disponibile
            System.out.println("Dispozitive audio disponibile:");
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info info : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info[] lineInfos = mixer.getTargetLineInfo();
                if (lineInfos.length > 0) {
                    System.out.println("Dispozitiv: " + info.getName() + " (" + info.getDescription() + ")");
                }
            }
            
            // Verificăm dacă există un microfon disponibil
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                String errorMsg = "Linia audio nu este suportată. Verificați dacă microfonul este conectat și dacă permisiunile sunt acordate.";
                System.err.println(errorMsg);
                
                // Verificăm dacă suntem pe macOS și oferim instrucțiuni specifice
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    errorMsg += "\n\nPe macOS trebuie să permiteți aplicației Java să acceseze microfonul:\n" +
                                "1. Deschideți Preferințe Sistem > Securitate & Confidențialitate > Confidențialitate\n" +
                                "2. Selectați 'Microfon' din lista din stânga\n" +
                                "3. Asigurați-vă că 'Java' sau 'Terminal' este bifat\n" +
                                "4. Reporniți această aplicație";
                }
                
                JOptionPane.showMessageDialog(this, errorMsg, "Eroare Microfon", JOptionPane.ERROR_MESSAGE);
                
                return;
            }
            
            line = (TargetDataLine) AudioSystem.getLine(info);
            System.out.println("Linie audio obținută: " + line.getLineInfo().toString());
            
        } catch (Exception e) {
            System.err.println("Eroare la inițializarea audio: " + e.getMessage());
            e.printStackTrace();
            
            String errorMsg = "Eroare la inițializarea audio: " + e.getMessage() + 
                "\nVerificați dacă microfonul este conectat și dacă permisiunile sunt acordate.";
            
            // Adăugăm instrucțiuni specifice pentru macOS
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                errorMsg += "\n\nPe macOS:\n" +
                            "1. Deschideți Preferințe Sistem > Securitate & Confidențialitate > Confidențialitate\n" +
                            "2. Selectați 'Microfon' din lista din stânga\n" +
                            "3. Asigurați-vă că 'Java' sau 'Terminal' este bifat\n" +
                            "4. Dacă faceți modificări, reporniți această aplicație";
            }
            
            JOptionPane.showMessageDialog(this, errorMsg, "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void startAudioCapture() {
        try {
            // Adăugare mesaj pentru diagnosticare
            System.out.println("Încercăm să deschidem linia audio...");
            
            // Asigurăm-ne că linia audio este închisă înainte de a o deschide din nou
            if (line.isOpen()) {
                line.close();
            }
            
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            line.open(format);
            line.start();
            isRunning = true;
            
            // Activăm animația intensă pentru toată durata interceptării
            System.out.println("Linia audio s-a deschis cu succes!");
            System.out.println("Animație activată pentru toată durata interceptării...");
            
            // Resetăm valorile pentru o pornire curată a animației
            for (int i = 0; i < audioSpectrumTarget.length; i++) {
                audioSpectrumTarget[i] = 0;
                audioSpectrum[i] = 0;
            }
            
            // Adăugăm un efect vizual de pornire
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < 8; i++) {
                    int x = (int)(Math.random() * getWidth());
                    int y = (int)(Math.random() * getHeight());
                    addParticleEffect(x, y, successColor);
                }
            });
            
            JOptionPane.showMessageDialog(this, "🎤 Microfon activat! Animația va rula continuu pe toată durata interceptării.", 
                                        "Info", JOptionPane.INFORMATION_MESSAGE);
            
            audioThread = new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                
                while (isRunning) {
                    try {
                        int count = line.read(buffer, 0, buffer.length);
                        
                        if (count > 0) {
                            System.out.println("Date audio citite: " + count + " bytes");
                            double frequency = detectPitch(buffer, SAMPLE_RATE);
                            System.out.println("Frecvență detectată: " + frequency + " Hz");
                            updateUI(frequency);
                            
                            // Adăugăm efecte vizuale continue în timpul procesării audio
                            if (frequency > 0 && Math.random() > 0.85) {
                                SwingUtilities.invokeLater(() -> {
                                    int x = (int)(Math.random() * getWidth());
                                    int y = (int)(Math.random() * 200) + 100;
                                    addParticleEffect(x, y, primaryColor);
                                });
                            }
                        } else {
                            // Chiar și când nu detectăm sunet, menținem animația vie
                            updateUI(-1); // Actualizează UI-ul pentru a menține animația
                        }
                    } catch (Exception e) {
                        System.err.println("Eroare la citirea audio: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    // Pauză mică pentru a nu suprasolicita sistemul
                    try {
                        Thread.sleep(10); // 10ms pauză pentru fluiditate
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
                
                System.out.println("Thread-ul audio s-a oprit.");
            });
            
            audioThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("Linia audio este indisponibilă: " + e.getMessage());
            e.printStackTrace();
            
            String errorMessage = "Microfonul nu este disponibil: " + e.getMessage();
            
            // Instrucțiuni specifice pentru macOS
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                errorMessage += "\n\nPe macOS:\n" +
                    "1. Verificați Preferințe Sistem > Securitate & Confidențialitate > Confidențialitate > Microfon\n" +
                    "2. Asigurați-vă că Java sau Terminal are permisiune\n" +
                    "3. Dacă ați acordat permisiunea acum, va trebui să reporniți aplicația";
                
                try {
                    // Încercăm să deschidem direct setările de confidențialitate
                    Runtime.getRuntime().exec(new String[]{"open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Microphone"});
                } catch (IOException ioe) {
                    System.err.println("Nu s-a putut deschide Setările de Confidențialitate: " + ioe.getMessage());
                }
            }
            
            JOptionPane.showMessageDialog(this, errorMessage, "Eroare acces microfon", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            System.err.println("Eroare la pornirea capturii audio: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la pornirea capturii audio: " + e.getMessage() + 
                "\nVerificați permisiunile microfonului în setările sistemului.", "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stopAudioCapture() {
        isRunning = false;
        System.out.println("Oprirea capturii audio și tranziția la animație ambientală...");
        
        // Adăugăm un efect vizual de oprire
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 5; i++) {
                int x = (int)(Math.random() * getWidth());
                int y = (int)(Math.random() * getHeight());
                addParticleEffect(x, y, errorColor);
            }
        });
        
        if (audioThread != null) {
            try {
                audioThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        if (line != null) {
            line.stop();
            line.close();
        }
        
        // Resetăm spectrul audio treptat pentru o tranziție fluidă
        SwingUtilities.invokeLater(() -> {
            // Creăm o tranziție treptată a spectrului către 0
            Timer fadeTimer = new Timer(50, null);
            fadeTimer.addActionListener(e -> {
                boolean stillFading = false;
                for (int i = 0; i < audioSpectrumTarget.length; i++) {
                    if (audioSpectrumTarget[i] > 1) {
                        audioSpectrumTarget[i] *= 0.9; // Reducem treptat
                        stillFading = true;
                    } else {
                        audioSpectrumTarget[i] = 0;
                    }
                }
                
                if (!stillFading) {
                    fadeTimer.stop();
                    System.out.println("Tranziția animației completă - mod ambient activat.");
                }
            });
            fadeTimer.start();
            
            // Resetăm și UI-ul
            frequencyLabel.setText("🎵 Frecvență: - Hz");
            noteLabel.setText("🎯 Notă: -");
            tuningMeter.setValue(0);
            tuningMeter.setString("Așteptare...");
        });
    }
    
    private void updateUI(double frequency) {
        SwingUtilities.invokeLater(() -> {
            // Verificăm dacă frecvența este validă
            if (frequency <= 0) {
                frequencyLabel.setText("🎵 Frecvență: - Hz");
                noteLabel.setText("🎯 Notă: -");
                tuningMeter.setValue(0);
                tuningMeter.setString("Așteptare semnal...");
                
                // Resetăm animația spectrului audio
                for (int i = 0; i < audioSpectrumTarget.length; i++) {
                    audioSpectrumTarget[i] = 0;
                }
                
                return;
            }
            
            // Actualizăm eticheta de frecvență cu precizie maximă
            frequencyLabel.setText(String.format("🎵 Frecvență detectată: %.4f Hz", frequency));
            frequencyLabel.setForeground(primaryColor);
            
            // Actualizăm și stocăm frecvențele recente pentru calcule mai stabile
            lastFrequencies[lastFreqIndex] = frequency;
            lastFreqIndex = (lastFreqIndex + 1) % lastFrequencies.length;
            
            // Calculăm media frecvențelor recente pentru a reduce fluctuațiile
            double avgFrequency = 0;
            int count = 0;
            for (double f : lastFrequencies) {
                if (f > 0) {
                    avgFrequency += f;
                    count++;
                }
            }
            if (count > 0) {
                avgFrequency /= count;
            } else {
                avgFrequency = frequency;
            }
            
            // Actualizăm spectrul audio simulat cu efecte moderne
            for (int i = 0; i < audioSpectrumTarget.length; i++) {
                if (frequency > 0) {
                    // Simulăm un spectru audio bazat pe frecvența detectată
                    double targetFreq = GUITAR_FREQUENCIES[selectedString];
                    double centDiff = 1200 * Math.log(frequency / targetFreq) / Math.log(2);
                    
                    // Poziționăm energia spectrală în jurul frecvenței detectate
                    double binCenter = audioSpectrumTarget.length / 2.0;
                    double binOffset = centDiff / 15.0; // Mapăm centii la poziția în spectru
                    
                    // Calculăm energia pentru fiecare bin, cu un vârf la frecvența detectată
                    double distance = Math.abs(i - (binCenter + binOffset));
                    double factor = Math.exp(-distance * distance / 20.0); // Distribuție gaussiană
                    
                    // Amplitudine maximă în centru, scade spre margini
                    audioSpectrumTarget[i] = Math.min(100, 85 * factor + (Math.random() * 15));
                    
                    // Adăugăm armonice pentru un spectru mai realist
                    if (i < audioSpectrumTarget.length - 2) {
                        double harmonicFactor = Math.exp(-(distance * 2) * (distance * 2) / 40.0);
                        audioSpectrumTarget[i + 1] += Math.min(50, 40 * harmonicFactor);
                    }
                } else {
                    // Când nu este detectată nicio frecvență, reducem spectrul spre 0
                    audioSpectrumTarget[i] *= 0.85;
                }
            }
            
            // Găsim cea mai apropiată notă
            double targetFrequency = GUITAR_FREQUENCIES[selectedString];
            double centDifference = 1200 * Math.log(avgFrequency / targetFrequency) / Math.log(2);
            
            // Calculăm diferența în Hz pentru informații suplimentare cu precizie maximă
            double hzDifference = avgFrequency - targetFrequency;
            String hzStatus = "";
            if (Math.abs(hzDifference) >= 0.01) {  // Afișăm chiar și diferențe de 0.01 Hz
                hzStatus = String.format(" (%.3f Hz %s)", Math.abs(hzDifference), 
                                       hzDifference > 0 ? "sus" : "jos");
            }
            
            // Actualizăm eticheta de notă cu emoji și informații ultra-detaliate
            String noteName = STRING_NAMES[selectedString];
            noteLabel.setText(String.format("🎯 Țintă: %s %.4f Hz%s", noteName, targetFrequency, hzStatus));
            
            // Calculăm valoarea pentru tuningMeter cu precizie ultra-îmbunătățită
            double preciseMeterValue = centDifference;
            int meterValue = (int) Math.round(preciseMeterValue);
            
            // Limităm valoarea la intervalul progress bar-ului
            meterValue = Math.max(-50, Math.min(50, meterValue));
            
            // Actualizăm tuning meter cu informații ultra-precise
            String meterText = "";
            if (Math.abs(preciseMeterValue) < 0.5) {
                meterText = "PERFECT! (±0.5¢)";
            } else if (Math.abs(preciseMeterValue) < 1) {
                meterText = String.format("EXCELENT (%.2f¢)", preciseMeterValue);
            } else if (Math.abs(preciseMeterValue) < 3) {
                meterText = String.format("FOARTE BUN (%.2f¢)", preciseMeterValue);
            } else if (Math.abs(preciseMeterValue) < 8) {
                meterText = String.format("BUN (%.1f¢)", preciseMeterValue);
            } else {
                meterText = String.format("AJUSTARE (%d¢)", meterValue);
            }
            tuningMeter.setString(meterText);
            
            // Setăm culorile pentru feedback vizual modern cu praguri mai precise
            if (Math.abs(preciseMeterValue) < 1) {
                noteLabel.setForeground(successColor);
                statusLabel.setText("🎯 Status: ACORDAT PERFECT!");
                statusLabel.setForeground(successColor);
                
                // Adăugăm efect de particule pentru acordaj perfect
                if (Math.random() > 0.7) {
                    for (int i = 0; i < 3; i++) {
                        int x = (int)(Math.random() * getWidth());
                        int y = (int)(Math.random() * 100) + 150;
                        addParticleEffect(x, y, successColor);
                    }
                }
            } else if (Math.abs(preciseMeterValue) < 5) {
                noteLabel.setForeground(warningColor);
                statusLabel.setText("🔄 Status: Aproape perfect");
                statusLabel.setForeground(warningColor);
            } else {
                noteLabel.setForeground(errorColor);
                statusLabel.setText("🔧 Status: Necesită acordaj");
                statusLabel.setForeground(errorColor);
            }
            
            // Actualizăm bara de acordaj cu animație ultra-fluidă și precision
            if (tuningMeter != null) {
                // Animație super-fluidă pentru maximă precizie
                int currentValue = tuningMeter.getValue();
                int targetValue = meterValue;
                
                if (Math.abs(targetValue - currentValue) > 1) {
                    // Pentru diferențe mari, folosim o tranziție graduală ultra-smoothă
                    double step = (targetValue - currentValue) * 0.3; // Animație mai lentă și mai fluidă
                    int nextValue = currentValue + (int) Math.round(step);
                    if (Math.abs(step) < 1 && Math.abs(targetValue - currentValue) > 0) {
                        nextValue = currentValue + (int) Math.signum(targetValue - currentValue);
                    }
                    tuningMeter.setValue(nextValue);
                } else {
                    // Pentru diferențe mici, setăm direct valoarea pentru precizie maximă
                    tuningMeter.setValue(targetValue);
                }
            }
            
            // Adăugăm efecte vizuale când suntem în acordaj perfect
            if (Math.abs(preciseMeterValue) < 1) {
                // Suntem în acordaj perfect - adăugăm efecte speciale
                if (Math.random() > 0.8) {
                    // Adăugăm particule în jurul tuning meter-ului
                    for (int i = 0; i < 2; i++) {
                        int x = tuningMeter.getWidth()/2 + (int)(Math.random() * 60 - 30);
                        int y = tuningMeter.getHeight()/2;
                        addParticleEffect(x, y, new Color(
                            255, 255, 255, 200
                        ));
                    }
                }
            }
        });
    }
    
    // Improved YIN algorithm for pitch detection
    private double detectPitch(byte[] audioBuffer, float sampleRate) {
        int bufferSize = audioBuffer.length / 2;
        double[] buffer = new double[bufferSize];
        
        // Convert byte array to doubles
        for (int i = 0; i < bufferSize; i++) {
            buffer[i] = ((audioBuffer[2 * i + 1] << 8) | (audioBuffer[2 * i] & 0xFF)) / 32768.0;
        }
        
        // Verificare amplitudine semnal
        double maxAmplitude = 0;
        for (double sample : buffer) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
        }
        
        // Afișare nivel semnal pentru diagnosticare
        System.out.println("Nivel maxim semnal: " + maxAmplitude);
        
        // Dacă semnalul este prea slab, nu încercăm să detectăm pitch-ul
        // Am scăzut pragul pentru a detecta semnale mai slabe
        if (maxAmplitude < 0.005) {  // Valoare mai mică pentru sensibilitate mai mare
            System.out.println("Semnal prea slab pentru detectare");
            return -1;
        }
        
        // YIN algorithm
        double[] yinBuffer = new double[bufferSize / 2];
        
        // Step 1: Autocorrelation
        for (int tau = 0; tau < yinBuffer.length; tau++) {
            yinBuffer[tau] = 0;
            for (int j = 0; j < yinBuffer.length; j++) {
                double delta = buffer[j] - buffer[j + tau];
                yinBuffer[tau] += delta * delta;
            }
        }
        
        // Step 2: Cumulative mean normalized difference
        yinBuffer[0] = 1;
        double runningSum = 0;
        for (int tau = 1; tau < yinBuffer.length; tau++) {
            runningSum += yinBuffer[tau];
            yinBuffer[tau] *= tau / runningSum;
        }
        
        // Step 3: Find the first minimum below the threshold
        // Using a more permissive threshold for better detection
        double threshold = 0.20; // Was 0.15, now more permissive
        int tau = 2;
        while (tau < yinBuffer.length) {
            if (yinBuffer[tau] < threshold && 
                yinBuffer[tau] < yinBuffer[tau - 1] && 
                yinBuffer[tau] < yinBuffer[tau + 1]) {
                break;
            }
            tau++;
            
            if (tau >= yinBuffer.length - 1) {
                System.out.println("Nu s-a găsit un pitch valid");
                return -1; // No pitch found
            }
        }
        
        // Step 4: Interpolate to get a more accurate pitch
        double betterTau = tau;
        if (tau > 0 && tau < yinBuffer.length - 1) {
            double s0 = yinBuffer[tau - 1];
            double s1 = yinBuffer[tau];
            double s2 = yinBuffer[tau + 1];
            double adjustment = (s2 - s0) / (2 * (2 * s1 - s2 - s0));
            betterTau = tau + adjustment;
        }
        
        double detectedFrequency = sampleRate / betterTau;
        System.out.println("Frecvență detectată brut: " + detectedFrequency);
        
        return detectedFrequency;
    }
    
    private void testMicrophone() {
        try {
            // Verificăm dacă avem acces la microfon
            if (line == null) {
                initAudio(); // Reinițializăm audio dacă nu avem linie audio
                if (line == null) {
                    JOptionPane.showMessageDialog(this, 
                        "Nu s-a putut accesa microfonul. Verificați permisiunile și conexiunea.", 
                        "Eroare", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            // Creăm un dialog stilizat pentru testul de microfon
            JDialog testDialog = new JDialog(this, "Test microfon", true);
            testDialog.setSize(500, 350);
            testDialog.setLocationRelativeTo(this);
            testDialog.setLayout(new BorderLayout());
            testDialog.setUndecorated(true); // Eliminăm decorațiunile standard de fereastră
            
            // Cream un panou principal cu margini rotunjite
            JPanel mainPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Fundal principal cu gradient
                    GradientPaint gp = new GradientPaint(
                        0, 0, new Color(250, 250, 255),
                        0, getHeight(), new Color(240, 240, 250)
                    );
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    
                    // Bordura
                    g2d.setColor(new Color(200, 200, 220));
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                    
                    // Adăugăm o linie subtilă pentru bara de titlu
                    g2d.setColor(new Color(230, 230, 240));
                    g2d.drawLine(20, 60, getWidth()-20, 60);
                }
                
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            
            // Panoul de conținut cu gradient
            JPanel contentPanel = new JPanel(new BorderLayout(15, 15)) {
                @Override
                protected void paintComponent(Graphics g) {
                    // Nu desenăm nimic - lăsăm să se vadă panoul principal
                }
                
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
            
            // Titlu cu efect 3D
            JLabel titleLabel = new JLabel("Test Microfon") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    
                    // Desenăm textul cu efect de umbră
                    g2d.setFont(getFont());
                    g2d.setColor(new Color(0, 0, 0, 50));
                    g2d.drawString(getText(), 3, 21);
                    g2d.setColor(primaryColor);
                    g2d.drawString(getText(), 1, 19);
                }
            };
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
            
            // Buton de închidere custom
            JButton closeButton = new JButton("×") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    if (getModel().isPressed()) {
                        g2d.setColor(new Color(220, 80, 80));
                    } else if (getModel().isRollover()) {
                        g2d.setColor(new Color(240, 100, 100));
                    } else {
                        g2d.setColor(new Color(200, 60, 60));
                    }
                    
                    g2d.fillOval(0, 0, getWidth(), getHeight());
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 16));
                    g2d.drawString("×", 7, 16);
                }
                
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            closeButton.setBorderPainted(false);
            closeButton.setFocusPainted(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setPreferredSize(new Dimension(25, 25));
            closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeButton.addActionListener(e -> testDialog.dispose());
            
            // Status cu efect animat
            JLabel statusLabel = new JLabel("Ascultare...") {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getText().equals("Ascultare...")) {
                        // Desenăm un indicator de activitate
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        int centerX = getWidth() - 30;
                        int centerY = getHeight() / 2;
                        int radius = 8;
                        
                        // Desenăm 3 puncte cu opacitate animată
                        for (int i = 0; i < 3; i++) {
                            float alpha = (float) Math.abs(Math.sin((waveAnimation + i * 0.7) % (2 * Math.PI)));
                            g2d.setColor(new Color(0, 120, 210, (int)(alpha * 255)));
                            g2d.fillOval(centerX + i * 12 - radius, centerY - radius, radius * 2, radius * 2);
                        }
                    }
                }
            };
            statusLabel.setHorizontalAlignment(JLabel.CENTER);
            statusLabel.setFont(regularFont.deriveFont(Font.BOLD, 18));
            statusLabel.setForeground(primaryColor);
            
            // Level meter stilizat - vizualizator de spectru
            JPanel levelMeterPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    // Lăsăm transparent
                }
                
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            levelMeterPanel.setPreferredSize(new Dimension(400, 150));
            
            JProgressBar levelMeter = new JProgressBar(0, 100) {
                // Array pentru bara de spectru
                float[] spectrumBars = new float[32];
                float[] spectrumTargets = new float[32];
                
                {
                    // Inițializăm barele de spectru
                    for (int i = 0; i < spectrumBars.length; i++) {
                        spectrumBars[i] = 0;
                        spectrumTargets[i] = 0;
                    }
                    
                    // Timer pentru animație
                    new Timer(30, e -> {
                        for (int i = 0; i < spectrumBars.length; i++) {
                            spectrumBars[i] += (spectrumTargets[i] - spectrumBars[i]) * 0.3;
                        }
                        repaint();
                    }).start();
                }
                
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    
                    int w = getWidth();
                    int h = getHeight();
                    
                    // Desenăm fundalul principal
                    RoundRectangle2D background = new RoundRectangle2D.Float(0, 0, w, h, 15, 15);
                    g2d.setColor(new Color(230, 230, 240));
                    g2d.fill(background);
                    
                    // Efect de adâncime pentru fundal
                    g2d.setColor(new Color(210, 210, 220));
                    g2d.fillRoundRect(5, 5, w-10, h-10, 10, 10);
                    
                    // Desenăm linii de grilă
                    g2d.setColor(new Color(200, 200, 210));
                    for (int y = h-20; y > 20; y -= 20) {
                        g2d.drawLine(10, y, w-10, y);
                    }
                    
                    int value = getValue();
                    
                    // Actualizăm barele de spectru - simulare simplă
                    if (value > 0) {
                        for (int i = 0; i < spectrumTargets.length; i++) {
                            // Simulăm un spectru audio cu frecvențele joase mai puternice
                            double factor = 1.0 - (i / (double)spectrumTargets.length) * 0.7;
                            double randomFactor = 0.3 + 0.7 * Math.random();
                            spectrumTargets[i] = (float)(value * factor * randomFactor);
                        }
                    } else {
                        for (int i = 0; i < spectrumTargets.length; i++) {
                            spectrumTargets[i] *= 0.9f; // Atenuare
                        }
                    }
                    
                    // Desenăm barele de spectru
                    int barWidth = (w - 20) / spectrumBars.length - 2;
                    for (int i = 0; i < spectrumBars.length; i++) {
                        int barHeight = (int)((h - 20) * (spectrumBars[i] / 100.0));
                        int x = 10 + i * (barWidth + 2);
                        int y = h - 10 - barHeight;
                        
                        // Calculăm culoarea în funcție de amplitudine și poziție
                        Color barColor;
                        if (spectrumBars[i] < 30) {
                            barColor = new Color(50, 200, 50);
                        } else if (spectrumBars[i] < 70) {
                            barColor = new Color(200, 200, 50);
                        } else {
                            barColor = new Color(200, 50, 50);
                        }
                        
                        // Gradient pentru bare
                        GradientPaint gp = new GradientPaint(
                            x, y, brightenColor(barColor, 0.2f),
                            x, y + barHeight, barColor.darker()
                        );
                        g2d.setPaint(gp);
                        
                        // Desenăm bara rotunjită
                        RoundRectangle2D bar = new RoundRectangle2D.Float(
                            x, y, barWidth, barHeight, 4, 4
                        );
                        g2d.fill(bar);
                        
                        // Adăugăm reflexie în partea de sus
                        if (barHeight > 5) {
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect(x, y, barWidth, 3);
                            g2d.setComposite(AlphaComposite.SrcOver);
                        }
                    }
                    
                    // Desenăm bordura
                    g2d.setColor(new Color(180, 180, 190));
                    g2d.setStroke(new BasicStroke(1f));
                    g2d.draw(background);
                    
                    // Desenăm textul
                    if (isStringPainted()) {
                        g2d.setFont(regularFont);
                        g2d.setColor(Color.BLACK);
                        String text = getString();
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(text);
                        int textHeight = fm.getHeight();
                        
                        // Fundal semi-transparent pentru text
                        g2d.setColor(new Color(255, 255, 255, 180));
                        g2d.fillRoundRect(
                           
                            (w - textWidth) / 2 - 10, 
                            (h - textHeight) / 2 - 5,
                            textWidth + 20,
                            textHeight + 10,
                            10, 10
                        );
                        
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(text, (w - textWidth) / 2, (h + textHeight / 2) / 2);
                    }
                }
            };
            levelMeter.setValue(0);
            levelMeter.setStringPainted(true);
            levelMeter.setString("Testare microfon...");
            levelMeter.setPreferredSize(new Dimension(400, 100));
            
            // Buton închidere stilizat
            JButton closeDialogButton = new JButton("Închide") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int w = getWidth();
                    int h = getHeight();
                    
                    GradientPaint gp = new GradientPaint(0, 0, primaryColor.brighter(), 0, h, primaryColor);
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, w, h, 10, 10);
                    
                    super.paintComponent(g);
                }
            };
            closeDialogButton.setForeground(Color.WHITE);
            closeDialogButton.setFont(regularFont);
            closeDialogButton.setFocusPainted(false);
            closeDialogButton.setBorderPainted(false);
            closeDialogButton.setContentAreaFilled(false);
            closeDialogButton.addActionListener(event -> {
                testDialog.dispose();
            });
            
            // Panel cu informații
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.add(statusLabel);
            infoPanel.add(Box.createVerticalStrut(15));
            infoPanel.add(levelMeter);
            
            // Adăugăm componentele la dialog
            contentPanel.add(titleLabel, BorderLayout.NORTH);
            contentPanel.add(infoPanel, BorderLayout.CENTER);
            contentPanel.add(closeDialogButton, BorderLayout.SOUTH);
            testDialog.add(contentPanel);
            
            // Pornim un thread pentru a testa microfonul
            Thread testThread = new Thread(() -> {
                try {
                    // Deschidem linia audio
                    if (!line.isOpen()) {
                        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                        line.open(format);
                        line.start();
                    }
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    final boolean[] detectedSound = {false};
                    
                    // Ascultăm pentru 5 secunde
                    for (int i = 0; i < 50 && testDialog.isVisible(); i++) {
                        int count = line.read(buffer, 0, buffer.length);
                        
                        if (count > 0) {
                            // Calculăm nivelul audio
                            double level = calculateAudioLevel(buffer);
                            int meterLevel = (int)(level * 100 * 3);  // Multiplicăm cu 3 pentru mai multă vizibilitate
                            if (meterLevel > 100) meterLevel = 100;   // Limităm la 100
                            
                            // Actualizăm UI
                            final int finalLevel = meterLevel;
                            SwingUtilities.invokeLater(() -> {
                                levelMeter.setValue(finalLevel);
                                levelMeter.setString("Nivel audio: " + finalLevel + "%");
                                
                                if (finalLevel > 5) {
                                    statusLabel.setText("Sunet detectat!");
                                    statusLabel.setForeground(new Color(0, 150, 0));
                                } else {
                                    statusLabel.setText("Ascultare... (vorbiți sau cântați)");
                                    statusLabel.setForeground(Color.BLACK);
                                }
                            });
                            
                            if (level > 0.05) {
                                detectedSound[0] = true;
                            }
                        }
                        
                        try {
                            Thread.sleep(100); // Pauză între citiri
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    
                    // Rezultatul final
                    if (testDialog.isVisible()) {
                        SwingUtilities.invokeLater(() -> {
                            if (detectedSound[0]) {
                                statusLabel.setText("Microfonul funcționează!");
                                statusLabel.setForeground(new Color(0, 150, 0));
                                levelMeter.setString("Test finalizat cu succes");
                            } else {
                                statusLabel.setText("Nu s-a detectat sunet!");
                                statusLabel.setForeground(new Color(200, 0, 0));
                                levelMeter.setString("Verificați microfonul și permisiunile");
                            }
                        });
                    }
                    
                    // Închidem linia audio
                    line.stop();
                    line.close();
                    
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Eroare: " + e.getMessage());
                        statusLabel.setForeground(new Color(200, 0, 0));
                        levelMeter.setString("Eroare la testarea microfonului");
                    });
                    e.printStackTrace();
                }
            });
            
            testThread.start();
            testDialog.setVisible(true); // Acest apel va bloca până când dialogul este închis
            
            // Când dialogul se închide, ne asigurăm că oprim thread-ul
            testThread.interrupt();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Eroare la testarea microfonului: " + e.getMessage(), 
                "Eroare", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Metodă pentru calcularea nivelului audio
    private double calculateAudioLevel(byte[] buffer) {
        int bufferSize = buffer.length / 2;
        double sum = 0;
        
        // Convertim buffer-ul de bytes în valori double și calculăm media pătratică (RMS)
        for (int i = 0; i < bufferSize; i++) {
            double sample = ((buffer[2 * i + 1] << 8) | (buffer[2 * i] & 0xFF)) / 32768.0;
            sum += sample * sample;
        }
        
        double rms = Math.sqrt(sum / bufferSize);
        return rms;
    }
    
    // Metodă pentru adăugarea unui efect de particule
    private void addParticleEffect(int x, int y, Color color) {
        // Creăm între 5 și 10 particule
        int numParticles = 5 + (int)(Math.random() * 6);
        for (int i = 0; i < numParticles; i++) {
            particleEffects.add(new ParticleEffect(x, y, color));
        }
    }
    
    // Metodă pentru a lumina o culoare
    private Color brightenColor(Color color, float factor) {
        int r = Math.min(255, (int)(color.getRed() * (1 + factor)));
        int g = Math.min(255, (int)(color.getGreen() * (1 + factor)));
        int b = Math.min(255, (int)(color.getBlue() * (1 + factor)));
        return new Color(r, g, b);
    }
    
    // Clasă internă pentru efecte de particule
    private class ParticleEffect {
        private float x, y;
        private float speedX, speedY;
        private float size;
        private float alpha;
        private Color color;
        private float gravity = 0.1f;
        
        public ParticleEffect(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            
            // Viteze aleatoare pentru mișcare
            this.speedX = (float)((Math.random() * 2 - 1) * 3);
            this.speedY = (float)((Math.random() * -2) - 1);
            
            // Dimensiune aleatoare
            this.size = (float)(Math.random() * 6 + 2);
            
            // Început cu opacitate completă
            this.alpha = 1.0f;
        }
        
        public void update() {
            // Actualizăm poziția
            x += speedX;
            y += speedY;
            
            // Aplicăm gravitația
            speedY += gravity;
            
            // Reducem opacitatea treptat
            alpha -= 0.02f;
            
            // Reducem ușor dimensiunea
            size *= 0.98f;
        }
        
        public void draw(Graphics2D g2d) {
            if (alpha <= 0) return;
            
            // Salvăm compozitul curent
            Composite originalComposite = g2d.getComposite();
            
            // Setăm opacitatea
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            // Desenăm particula cu gradient radial pentru efect de strălucire
            RadialGradientPaint paint = new RadialGradientPaint(
                x, y, size, 
                new float[] {0.0f, 1.0f}, 
                new Color[] {
                    brightenColor(color, 0.5f),
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
                }
            );
            
            g2d.setPaint(paint);
            g2d.fillOval((int)(x - size), (int)(y - size), (int)(size * 2), (int)(size * 2));
            
            // Restaurăm compozitul original
            g2d.setComposite(originalComposite);
        }
        
        public boolean isDead() {
            return alpha <= 0;
        }
    }
    
    // Metodă pentru salvarea unui profil de acordaj personalizat
    private void saveCustomTuningProfile() {
        try {
            // Obținem informațiile despre acordajul curent
            StringBuilder tuningInfo = new StringBuilder("# Profil de acordaj personalizat\n");
            tuningInfo.append("# Creat la: ").append(new java.util.Date()).append("\n\n");
            
            tuningInfo.append("# Format: Nume_coardă=Frecvență\n");
            
            for (int i = 0; i < GUITAR_FREQUENCIES.length; i++) {
                tuningInfo.append(STRING_NAMES[i]).append("=").append(GUITAR_FREQUENCIES[i]).append("\n");
            }
            
            // Creăm un dialog pentru alegerea numelui profilului
            String profileName = JOptionPane.showInputDialog(
                this,
                "Introduceți un nume pentru profilul de acordaj:",
                "Salvare profil acordaj",
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (profileName != null && !profileName.trim().isEmpty()) {
                // Creăm fișierul de profil
                File profilesDir = new File(System.getProperty("user.home"), ".guitar_tuner_profiles");
                if (!profilesDir.exists()) {
                    profilesDir.mkdir();
                }
                
                File profileFile = new File(profilesDir, profileName.trim() + ".tuning");
                
                // Scriem informațiile în fișier
                try (FileWriter writer = new FileWriter(profileFile)) {
                    writer.write(tuningInfo.toString());
                }
                
                JOptionPane.showMessageDialog(
                    this,
                    "Profilul de acordaj a fost salvat cu succes în:\n" + profileFile.getAbsolutePath(),
                    "Salvare reușită",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                "Eroare la salvarea profilului: " + e.getMessage(),
                "Eroare",
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }
    
    // Metodă pentru încărcarea unui profil de acordaj personalizat
    private void loadCustomTuningProfile() {
        try {
            // Verificăm existența directorului de profile
            File profilesDir = new File(System.getProperty("user.home"), ".guitar_tuner_profiles");
            if (!profilesDir.exists() || !profilesDir.isDirectory()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Nu există profile de acordaj salvate. Creați mai întâi un profil.",
                    "Niciun profil găsit",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            // Listăm fișierele de profil
            File[] profileFiles = profilesDir.listFiles((dir, name) -> name.endsWith(".tuning"));
            
            if (profileFiles == null || profileFiles.length == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Nu există profile de acordaj salvate. Creați mai întâi un profil.",
                    "Niciun profil găsit",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            // Creăm un dialog stilizat pentru selectarea profilului
            JDialog profileDialog = new JDialog(this, "Selectare profil de acordaj", true);
            profileDialog.setSize(450, 400);
            profileDialog.setLocationRelativeTo(this);
            profileDialog.setLayout(new BorderLayout());
            
            // Panou principal cu margini rotunjite
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Fundal principal cu gradient
                    GradientPaint gp = new GradientPaint(
                        0, 0, new Color(250, 250, 255),
                        0, getHeight(), new Color(240, 240, 250)
                    );
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    
                    // Bordura
                    g2d.setColor(new Color(200, 200, 220));
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                }
                
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            
            // Titlu
            JLabel titleLabel = new JLabel("Selectați profilul de acordaj");
            titleLabel.setFont(headerFont);
            titleLabel.setForeground(primaryColor);
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            
            // Creăm un model pentru lista de profile
            DefaultListModel<ProfileItem> listModel = new DefaultListModel<>();
            
            // Populăm modelul cu profilele găsite
            for (File file : profileFiles) {
                String fileName = file.getName();
                String profileName = fileName.substring(0, fileName.length() - 7); // Eliminăm extensia ".tuning"
                listModel.addElement(new ProfileItem(profileName, file));
            }
            
            // Creăm lista de profile
            JList<ProfileItem> profileList = new JList<>(listModel);
            profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            profileList.setCellRenderer(new ProfileListRenderer());
            profileList.setBackground(new Color(250, 250, 255));
            
            // Adăugăm o bordură la lista
            JScrollPane scrollPane = new JScrollPane(profileList);
            scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(new Color(200, 200, 220))
            ));
            
            // Panou pentru previzualizare profil
            JPanel previewPanel = new JPanel();
            previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
            previewPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(buttonBorderColor),
                "Detalii profil",
                0,
                0,
                regularFont,
                primaryColor
            ));
            previewPanel.setOpaque(false);
            
            JLabel previewLabel = new JLabel("Selectați un profil pentru a vedea detaliile");
            previewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            previewPanel.add(previewLabel);
            
            // Panou pentru butoane
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
            buttonPanel.setOpaque(false);
            
            // Buton de încărcare
            JButton loadButton = new JButton("Încarcă") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int w = getWidth();
                    int h = getHeight();
                    
                    GradientPaint gp = new GradientPaint(
                        0, 0, secondaryColor.brighter(), 
                        0, h, secondaryColor
                    );
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, w, h, 10, 10);
                    
                    super.paintComponent(g);
                }
            };
            
            loadButton.setForeground(Color.WHITE);
            loadButton.setFont(regularFont);
            loadButton.setFocusPainted(false);
            loadButton.setBorderPainted(false);
            loadButton.setContentAreaFilled(false);
            loadButton.setEnabled(false);
            loadButton.setPreferredSize(new Dimension(120, 40));
            
            // Buton de anulare
            JButton cancelButton = new JButton("Anulează") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int w = getWidth();
                    int h = getHeight();
                    
                    GradientPaint gp = new GradientPaint(
                        0, 0, new Color(180, 180, 190).brighter(), 
                        0, h, new Color(150, 150, 160)
                    );
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, w, h, 10, 10);
                    
                    super.paintComponent(g);
                }
            };
            cancelButton.setForeground(Color.WHITE);
            cancelButton.setFont(regularFont);
            cancelButton.setFocusPainted(false);
            cancelButton.setBorderPainted(false);
            cancelButton.setContentAreaFilled(false);
            cancelButton.setPreferredSize(new Dimension(120, 40));
            
            // Buton pentru ștergerea profilului
            JButton deleteButton = new JButton("Șterge") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int w = getWidth();
                    int h = getHeight();
                    
                    GradientPaint gp = new GradientPaint(
                        0, 0, accentColor.brighter(), 
                        0, h, accentColor
                    );
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, w, h, 10, 10);
                    
                    super.paintComponent(g);
                }
            };
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setFont(regularFont);
            deleteButton.setFocusPainted(false);
            deleteButton.setBorderPainted(false);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setEnabled(false);
            deleteButton.setPreferredSize(new Dimension(120, 40));
            
            // Adăugăm event listeners
            profileList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    ProfileItem selectedItem = profileList.getSelectedValue();
                    if (selectedItem != null) {
                        loadButton.setEnabled(true);
                        deleteButton.setEnabled(true);
                        
                        // Încărcăm și afișăm detaliile profilului
                        try {
                            java.util.Properties props = new java.util.Properties();
                            try (java.io.FileReader reader = new java.io.FileReader(selectedItem.getFile())) {
                                props.load(reader);
                            }
                            
                            // Construim detaliile profilului
                            StringBuilder details = new StringBuilder("<html><body style='width: 250px'>");
                            details.append("<h3>").append(selectedItem.getDisplayName()).append("</h3>");
                            details.append("<table>");
                            
                            for (String stringName : STRING_NAMES) {
                                String freq = props.getProperty(stringName);
                                if (freq != null) {
                                    details.append("<tr><td style='padding-right:10px'><b>")
                                          .append(stringName)
                                          .append(":</b></td><td>")
                                          .append(freq)
                                          .append(" Hz</td></tr>");
                                }
                            }
                            
                            details.append("</table></body></html>");
                            previewLabel.setText(details.toString());
                            
                        } catch (Exception ex) {
                            previewLabel.setText("<html>Eroare la încărcarea detaliilor profilului:<br>" + 
                                                ex.getMessage() + "</html>");
                        }
                    } else {
                        loadButton.setEnabled(false);
                        deleteButton.setEnabled(false);
                        previewLabel.setText("Selectați un profil pentru a vedea detaliile");
                    }
                }
            });
            
            loadButton.addActionListener(e -> {
                ProfileItem selectedItem = profileList.getSelectedValue();
                if (selectedItem != null) {
                    // Încărcăm profilul selectat
                    try {
                        loadTuningProfile(selectedItem.getFile());
                        profileDialog.dispose();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                            profileDialog,
                            "Eroare la încărcarea profilului: " + ex.getMessage(),
                            "Eroare",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            });
            
            cancelButton.addActionListener(e -> profileDialog.dispose());
            
            deleteButton.addActionListener(e -> {
                ProfileItem selectedItem = profileList.getSelectedValue();
                if (selectedItem != null) {
                    int confirm = JOptionPane.showConfirmDialog(
                        profileDialog,
                        "Sigur doriți să ștergeți profilul '" + selectedItem.getDisplayName() + "'?",
                        "Confirmare ștergere",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        if (selectedItem.getFile().delete()) {
                            listModel.removeElement(selectedItem);
                            previewLabel.setText("Profilul a fost șters");
                            loadButton.setEnabled(false);
                            deleteButton.setEnabled(false);
                            
                            if (listModel.isEmpty()) {
                                profileDialog.dispose();
                                JOptionPane.showMessageDialog(
                                    this,
                                    "Toate profilele au fost șterse.",
                                    "Niciun profil rămas",
                                    JOptionPane.INFORMATION_MESSAGE
                                );
                            }
                        } else {
                            JOptionPane.showMessageDialog(
                                profileDialog,
                                "Nu s-a putut șterge profilul.",
                                "Eroare",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                }
            });
            
            buttonPanel.add(loadButton);
            buttonPanel.add(cancelButton);
            buttonPanel.add(deleteButton);
            
            // Asamblăm toate componentele
            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setOpaque(false);
            contentPanel.add(titleLabel, BorderLayout.NORTH);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            contentPanel.add(previewPanel, BorderLayout.EAST);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            mainPanel.add(contentPanel, BorderLayout.CENTER);
            profileDialog.add(mainPanel);
            
            profileDialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                "Eroare la încărcarea profilurilor: " + e.getMessage(),
                "Eroare",
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }
    
    // Metodă pentru încărcarea efectivă a unui profil de acordaj
    private void loadTuningProfile(File profileFile) throws IOException {
        java.util.Properties props = new java.util.Properties();
        
        try (java.io.FileReader reader = new java.io.FileReader(profileFile)) {
            props.load(reader);
        }
        
        // Verificăm dacă profilul are toate corzile necesare
        boolean isValidProfile = true;
        for (String stringName : STRING_NAMES) {
            if (!props.containsKey(stringName)) {
                isValidProfile = false;
                break;
            }
        }
        
        if (!isValidProfile) {
            throw new IOException("Profilul de acordaj este incomplet sau invalid.");
        }
        
        // Populăm array-ul cu frecvențele din profil
        for (int i = 0; i < STRING_NAMES.length; i++) {
            String freqStr = props.getProperty(STRING_NAMES[i]);
            try {
                double newFreq = Double.parseDouble(freqStr);
                GUITAR_FREQUENCIES[i] = newFreq; // Actualizăm efectiv frecvențele
            } catch (NumberFormatException e) {
                throw new IOException("Valoare de frecvență invalidă pentru coarda " + STRING_NAMES[i]);
            }
        }
        
        // Actualizăm textul butoanelor cu noile frecvențe
        for (int i = 0; i < GUITAR_FREQUENCIES.length; i++) {
            final int index = i;
            SwingUtilities.invokeLater(() -> {
                JButton button = stringButtons.get(index);
                button.setText(STRING_NAMES[index] + " (" + String.format("%.2f", GUITAR_FREQUENCIES[index]) + " Hz)");
            });
        }
        
        // Afișăm un mesaj de succes
        JOptionPane.showMessageDialog(
            this,
            "Profilul de acordaj '" + profileFile.getName().replace(".tuning", "") + "' a fost încărcat cu succes.",
            "Profil încărcat",
            JOptionPane.INFORMATION_MESSAGE
        );
        
        // Resetăm acordajul curent
        tuningMeter.setValue(0);
        tuningMeter.setString("Acordaj");
        
        // Reselect the current string to refresh any displays
        selectString(selectedString);
    }
    
    // Clasă pentru elementele din lista de profile
    private class ProfileItem {
        private String displayName;
        private File file;
        
        public ProfileItem(String displayName, File file) {
            this.displayName = displayName;
            this.file = file;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public File getFile() {
            return file;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Renderer personalizat pentru elementele din listă
    private class ProfileListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                     boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ProfileItem) {
                ProfileItem item = (ProfileItem) value;
                label.setText(item.getDisplayName());
                label.setIcon(UIManager.getIcon("FileView.fileIcon"));
                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                if (isSelected) {
                    label.setBackground(new Color(200, 220, 240));
                    label.setForeground(new Color(0, 0, 100));
                } else {
                    label.setBackground(index % 2 == 0 ? 
                        new Color(245, 245, 250) : new Color(240, 240, 245));
                    label.setForeground(Color.BLACK);
                }
            }
            
            return label;
        }
    }
    
    // Metodă pentru a aplica un acordaj predefinit
    private void applyPredefinedTuning(int tuningIndex) {
        // Definim frecvențele pentru acordajele predefinite
        double[][] predefinedFrequencies = {
            // Standard (E A D G B E)
            {82.41, 110.00, 146.83, 196.00, 246.94, 329.63},
            
            // Drop D (D A D G B E)
            {73.42, 110.00, 146.83, 196.00, 246.94, 329.63},
            
            // Open G (D G D G B D)
            {73.42, 98.00, 146.83, 196.00, 246.94, 293.66},
            
            // Open D (D A D F# A D)
            {73.42, 110.00, 146.83, 185.00, 220.00, 293.66},
            
            // DADGAD (D A D G A D)
            {73.42, 110.00, 146.83, 196.00, 220.00, 293.66}
        };
        
        // Definim numele corzilor pentru acordajele predefinite
        String[][] predefinedStringNames = {
            // Standard
            {"E (Mi Jos)", "A (La)", "D (Re)", "G (Sol)", "B (Si)", "E (Mi Sus)"},
            
            // Drop D
            {"D (Re Jos)", "A (La)", "D (Re)", "G (Sol)", "B (Si)", "E (Mi Sus)"},
            
            // Open G
            {"D (Re Jos)", "G (Sol Jos)", "D (Re)", "G (Sol)", "B (Si)", "D (Re Sus)"},
            
            // Open D
            {"D (Re Jos)", "A (La)", "D (Re)", "F# (Fa#)", "A (La Sus)", "D (Re Sus)"},
            
            // DADGAD
            {"D (Re Jos)", "A (La)", "D (Re)", "G (Sol)", "A (La Sus)", "D (Re Sus)"}
        };
        
        // Verificăm dacă indexul este valid
        if (tuningIndex < 0 || tuningIndex >= predefinedFrequencies.length) {
            JOptionPane.showMessageDialog(
                this,
                "Acordaj nedefinit.",
                "Eroare",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // Aplicăm acordajul selectat
        System.arraycopy(predefinedFrequencies[tuningIndex], 0, GUITAR_FREQUENCIES, 0, 6);
        System.arraycopy(predefinedStringNames[tuningIndex], 0, STRING_NAMES, 0, 6);
        
        // Actualizăm UI-ul
        for (int i = 0; i < GUITAR_FREQUENCIES.length; i++) {
            final int index = i;
            SwingUtilities.invokeLater(() -> {
                JButton button = stringButtons.get(index);
                button.setText(STRING_NAMES[index] + " (" + String.format("%.2f", GUITAR_FREQUENCIES[index]) + " Hz)");
            });
        }
        
        // Afișăm un mesaj de succes
        String[] tuningNames = {
            "Standard (E A D G B E)",
            "Drop D (D A D G B E)",
            "Open G (D G D G B D)",
            "Open D (D A D F# A D)",
            "DADGAD (D A D G A D)"
        };
        
        JOptionPane.showMessageDialog(
            this,
            "Acordajul '" + tuningNames[tuningIndex] + "' a fost aplicat cu succes.",
            "Acordaj aplicat",
            JOptionPane.INFORMATION_MESSAGE
        );
        
        // Resetăm acordajul curent
        tuningMeter.setValue(0);
        tuningMeter.setString("Acordaj");
        
        // Reselect the current string to refresh any displays
        selectString(selectedString);
    }
    
    // Metodă pentru crearea cardului de controale din partea dreaptă
    private JPanel createControlsCard() {
        JPanel cardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Card background cu shadow
                g2d.setColor(new Color(0, 0, 0, 25));
                g2d.fillRoundRect(4, 4, getWidth()-4, getHeight()-4, 20, 20);
                
                g2d.setColor(cardColor);
                g2d.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 20, 20);
                
                // Bordură subtilă
                g2d.setColor(buttonBorderColor);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, getWidth()-5, getHeight()-5, 20, 20);
            }
        };
        
        cardPanel.setLayout(new BorderLayout(0, 15));
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(300, 0));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        
        // Header pentru card
        JLabel cardTitle = new JLabel("CONTROALE");
        cardTitle.setFont(headerFont);
        cardTitle.setForeground(textPrimaryColor);
        cardTitle.setHorizontalAlignment(JLabel.CENTER);
        
        JLabel cardSubtitle = new JLabel("Control și setări acordor");
        cardSubtitle.setFont(smallFont);
        cardSubtitle.setForeground(textSecondaryColor);
        cardSubtitle.setHorizontalAlignment(JLabel.CENTER);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(cardTitle);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(cardSubtitle);
        
        // Panel pentru informații status
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayout(3, 1, 0, 10));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        
        // Inițializăm componentele de status dacă nu există
        if (statusLabel == null) {
            statusLabel = new JLabel("🔴 Status: Oprit");
            statusLabel.setFont(regularFont);
            statusLabel.setForeground(errorColor);
            statusLabel.setHorizontalAlignment(JLabel.CENTER);
        }
        
        if (frequencyLabel == null) {
            frequencyLabel = new JLabel("🎵 Frecvență: - Hz");
            frequencyLabel.setFont(regularFont);
            frequencyLabel.setForeground(textSecondaryColor);
            frequencyLabel.setHorizontalAlignment(JLabel.CENTER);
        }
        
        if (noteLabel == null) {
            noteLabel = new JLabel("🎯 Notă: -");
            noteLabel.setFont(regularFont);
            noteLabel.setForeground(textSecondaryColor);
            noteLabel.setHorizontalAlignment(JLabel.CENTER);
        }
        
        statusPanel.add(statusLabel);
        statusPanel.add(frequencyLabel);
        statusPanel.add(noteLabel);
        
        // Panel pentru butoane de control
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridLayout(6, 1, 0, 10));
        controlsPanel.setOpaque(false);
        
        // Buton Start/Stop cu gradient modern
        JButton startStopButton = new JButton("START") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                Color startColor = isRunning ? errorColor : successColor;
                Color endColor = isRunning ? errorColor.darker() : successColor.darker();
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, startColor,
                    0, h, endColor
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, w, h, 12, 12);
                
                // Bordură
                g2d.setColor(startColor.darker());
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, w-1, h-1, 12, 12);
                
                super.paintComponent(g);
            }
        };
        startStopButton.setForeground(Color.WHITE);
        startStopButton.setFont(regularFont.deriveFont(Font.BOLD));
        startStopButton.setFocusPainted(false);
        startStopButton.setBorderPainted(false);
        startStopButton.setContentAreaFilled(false);
        startStopButton.setPreferredSize(new Dimension(0, 40));
        startStopButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startStopButton.addActionListener(e -> {
            if (isRunning) {
                stopAudioCapture();
                startStopButton.setText("START");
                statusLabel.setText("🔴 Status: Oprit");
                statusLabel.setForeground(errorColor);
            } else {
                startAudioCapture();
                startStopButton.setText("STOP");
                statusLabel.setText("🟢 Status: Ascultare...");
                statusLabel.setForeground(successColor);
            }
        });
        
        // Buton Test Microfon
        JButton testMicButton = new JButton("🎤 TEST MICROFON") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, secondaryColor,
                    0, h, secondaryColor.darker()
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, w, h, 12, 12);
                
                g2d.setColor(secondaryColor.darker());
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, w-1, h-1, 12, 12);
                
                super.paintComponent(g);
            }
        };
        testMicButton.setForeground(Color.WHITE);
        testMicButton.setFont(regularFont);
        testMicButton.setFocusPainted(false);
        testMicButton.setBorderPainted(false);
        testMicButton.setContentAreaFilled(false);
        testMicButton.setPreferredSize(new Dimension(0, 35));
        testMicButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        testMicButton.addActionListener(e -> testMicrophone());
        
        // Dropdown pentru preseturi de acordaj
        String[] tuningPresets = {
            "Standard (E A D G B E)",
            "Drop D (D A D G B E)", 
            "Open G (D G D G B D)",
            "Open D (D A D F# A D)",
            "DADGAD (D A D G A D)"
        };
        
        JComboBox<String> tuningCombo = new JComboBox<>(tuningPresets);
        tuningCombo.setFont(smallFont);
        tuningCombo.setBackground(cardColor);
        tuningCombo.setForeground(textPrimaryColor);
        tuningCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(buttonBorderColor),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        tuningCombo.addActionListener(e -> {
            int selectedIndex = tuningCombo.getSelectedIndex();
            applyPredefinedTuning(selectedIndex);
        });
        
        // Buton Salvare Profil
        JButton saveProfileButton = new JButton("SALVARE PROFIL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, warningColor,
                    0, h, warningColor.darker()
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, w, h, 12, 12);
                
                g2d.setColor(warningColor.darker());
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, w-1, h-1, 12, 12);
                
                super.paintComponent(g);
            }
        };
        saveProfileButton.setForeground(Color.WHITE);
        saveProfileButton.setFont(smallFont);
        saveProfileButton.setFocusPainted(false);
        saveProfileButton.setBorderPainted(false);
        saveProfileButton.setContentAreaFilled(false);
        saveProfileButton.setPreferredSize(new Dimension(0, 35));
        saveProfileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveProfileButton.addActionListener(e -> saveCustomTuningProfile());
        
        // Buton Încărcare Profil
        JButton loadProfileButton = new JButton("📂 ÎNCĂRCARE PROFIL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, accentColor,
                    0, h, accentColor.darker()
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, w, h, 12, 12);
                
                g2d.setColor(accentColor.darker());
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, w-1, h-1, 12, 12);
                
                super.paintComponent(g);
            }
        };
        loadProfileButton.setForeground(Color.WHITE);
        loadProfileButton.setFont(smallFont);
        loadProfileButton.setFocusPainted(false);
        loadProfileButton.setBorderPainted(false);
        loadProfileButton.setContentAreaFilled(false);
        loadProfileButton.setPreferredSize(new Dimension(0, 35));
        loadProfileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loadProfileButton.addActionListener(e -> loadCustomTuningProfile());
        
        controlsPanel.add(startStopButton);
        controlsPanel.add(testMicButton);
        controlsPanel.add(new JLabel(" ")); // Spacer
        controlsPanel.add(tuningCombo);
        controlsPanel.add(saveProfileButton);
        controlsPanel.add(loadProfileButton);
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        cardPanel.add(statusPanel, BorderLayout.CENTER);
        cardPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        return cardPanel;
    }
    
    // Metodă pentru crearea footer-ului modern
    private void createModernFooter() {
        JPanel footerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Gradient pentru footer
                GradientPaint footerGradient = new GradientPaint(
                    0, 0, footerBackgroundColor,
                    0, h, footerBackgroundColor.darker()
                );
                g2d.setPaint(footerGradient);
                g2d.fillRect(0, 0, w, h);
                
                // Linie de separare subtilă în partea de sus
                g2d.setColor(buttonBorderColor);
                g2d.drawLine(0, 0, w, 0);
            }
        };
        
        footerPanel.setLayout(new BorderLayout());
        footerPanel.setPreferredSize(new Dimension(0, 80));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        
        // Panel stânga - informații aplicație
        JPanel leftFooterPanel = new JPanel();
        leftFooterPanel.setLayout(new BoxLayout(leftFooterPanel, BoxLayout.Y_AXIS));
        leftFooterPanel.setOpaque(false);
        
        JLabel appNameLabel = new JLabel("🎸 Guitar Tuner Pro");
        appNameLabel.setFont(regularFont.deriveFont(Font.BOLD));
        appNameLabel.setForeground(textPrimaryColor);
        
        JLabel versionLabel = new JLabel("v2.0");
        versionLabel.setFont(smallFont);
        versionLabel.setForeground(textSecondaryColor);
        
        leftFooterPanel.add(appNameLabel);
        leftFooterPanel.add(versionLabel);
        
        // Panel centru - tuning meter
        JPanel centerFooterPanel = new JPanel();
        centerFooterPanel.setOpaque(false);
        centerFooterPanel.setLayout(new BorderLayout());
        
        JLabel meterLabel = new JLabel("Precizie Acordaj", JLabel.CENTER);
        meterLabel.setFont(smallFont);
        meterLabel.setForeground(textSecondaryColor);
        
        // Inițializăm tuningMeter dacă nu există
        if (tuningMeter == null) {
            // Tuning meter-ul este deja inițializat în createModernTuningMeter()
            // Creăm doar o copie simplă pentru footer
            JProgressBar footerMeter = new JProgressBar(JProgressBar.HORIZONTAL, -50, 50);
            footerMeter.setValue(0);
            footerMeter.setStringPainted(true);
            footerMeter.setString("Acordaj");
            footerMeter.setFont(smallFont);
            footerMeter.setPreferredSize(new Dimension(200, 25));
            
            // Setăm culori personalizate pentru tuning meter
            footerMeter.setBackground(new Color(60, 64, 72));
            footerMeter.setForeground(primaryColor);
            
            centerFooterPanel.add(meterLabel, BorderLayout.NORTH);
            centerFooterPanel.add(footerMeter, BorderLayout.CENTER);
        } else {
            // Folosim tuning meter-ul principal și facem o referință pentru footer
            centerFooterPanel.add(meterLabel, BorderLayout.NORTH);
        }
        
        // Panel dreapta - status și info
        JPanel rightFooterPanel = new JPanel();
        rightFooterPanel.setLayout(new BoxLayout(rightFooterPanel, BoxLayout.Y_AXIS));
        rightFooterPanel.setOpaque(false);
        rightFooterPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        JLabel micStatusLabel = new JLabel("🎤 Microfon: Inactiv");
        micStatusLabel.setFont(smallFont);
        micStatusLabel.setForeground(isRunning ? successColor : errorColor);
        micStatusLabel.setHorizontalAlignment(JLabel.RIGHT);
        
        JButton infoButton = new JButton("Info") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Fundal transparent cu bordură
                g2d.setColor(new Color(60, 64, 72, 100));
                g2d.fillRoundRect(0, 0, w, h, 8, 8);
                
                g2d.setColor(buttonBorderColor);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, w-1, h-1, 8, 8);
                
                super.paintComponent(g);
            }
        };
        infoButton.setForeground(textSecondaryColor);
        infoButton.setFont(smallFont);
        infoButton.setFocusPainted(false);
        infoButton.setBorderPainted(false);
        infoButton.setContentAreaFilled(false);
        infoButton.setPreferredSize(new Dimension(60, 25));
        infoButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        infoButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                this,
                "Acordor de chitară cu interface modernă și\n" +
                "vizualizare avansată a spectrului audio.\n\n" +
                "✨ Caracteristici:\n" +
                "• Precizie ±1 cent\n" +
                "• Spectru audio în timp real\n" +
                "• Efecte vizuale moderne\n" +
                "• Profiluri de acordaj personalizate\n" +
                "• Interface responsive\n\n" +
                "📝 Dezvoltat pentru muzicieni profesioniști",
                "Despre Guitar Tuner Pro",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
        
        rightFooterPanel.add(micStatusLabel);
        rightFooterPanel.add(Box.createVerticalStrut(5));
        rightFooterPanel.add(infoButton);
        
        footerPanel.add(leftFooterPanel, BorderLayout.WEST);
        footerPanel.add(centerFooterPanel, BorderLayout.CENTER);
        footerPanel.add(rightFooterPanel, BorderLayout.EAST);
        
        add(footerPanel, BorderLayout.SOUTH);
        
        // Timer pentru actualizarea status-ului microfonului
        Timer micStatusTimer = new Timer(1000, e -> {
            micStatusLabel.setText("🎤 Microfon: " + (isRunning ? "Activ" : "Inactiv"));
            micStatusLabel.setForeground(isRunning ? successColor : errorColor);
        });
        micStatusTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            tunner tuner = new tunner();
            tuner.setVisible(true);
        });
    }
}
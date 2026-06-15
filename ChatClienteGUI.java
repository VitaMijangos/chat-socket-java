import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClienteGUI extends JFrame {

    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;

    private JTextArea areaChat;
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JComboBox<String> comboEmojis;
    private JButton botonImagen;

    public ChatClienteGUI() {
        setTitle("Cliente - Chat Socket");
        setSize(500, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        areaChat = new JTextArea();
        areaChat.setEditable(false);

        campoMensaje = new JTextField();
        botonEnviar = new JButton("Enviar");
        botonImagen = new JButton("Imagen");

        comboEmojis = new JComboBox<>(new String[]{
                "😀", "😂", "😍", "😎", "😢", "👍", "❤️", "🔥"
        });

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(comboEmojis, BorderLayout.WEST);
        panelInferior.add(campoMensaje, BorderLayout.CENTER);
        panelInferior.add(botonEnviar, BorderLayout.EAST);
        panelInferior.add(botonImagen, BorderLayout.NORTH);

        add(new JScrollPane(areaChat), BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        comboEmojis.addActionListener(e -> {
            campoMensaje.setText(campoMensaje.getText() + comboEmojis.getSelectedItem());
        });

        botonEnviar.addActionListener(e -> enviarMensaje());
        campoMensaje.addActionListener(e -> enviarMensaje());
        botonImagen.addActionListener(e -> enviarImagen());

        conectarServidor();
    }

    private void conectarServidor() {
        try {
            JTextField campoIP = new JTextField();
            JTextField campoPuerto = new JTextField();

            Object[] campos = {
                    "IP del servidor:", campoIP,
                    "Puerto:", campoPuerto
            };

            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    campos,
                    "Configuración de conexión",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (opcion != JOptionPane.OK_OPTION) {
                System.exit(0);
            }

            String ip = campoIP.getText().trim();
            String puertoTexto = campoPuerto.getText().trim();

            if (ip.isEmpty() || puertoTexto.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Debe ingresar la IP y el puerto.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(0);
            }

            int puerto = Integer.parseInt(puertoTexto);

            socket = new Socket(ip, puerto);

            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            areaChat.append("Conectado al servidor " + ip + ":" + puerto + "\n");

            Thread hilo = new Thread(() -> recibirMensajes());
            hilo.start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "El puerto debe ser numérico.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo conectar al servidor.\n" + e.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }
    }

    private void enviarMensaje() {
        try {
            String mensaje = campoMensaje.getText().trim();

            if (!mensaje.isEmpty()) {
                salida.writeUTF("TEXTO");
                salida.writeUTF(mensaje);
                salida.flush();

                areaChat.append("Cliente: " + mensaje + "\n");

                ConexionBD.guardarMensaje("Cliente", mensaje);

                campoMensaje.setText("");
            }

        } catch (IOException e) {
            areaChat.append("Error al enviar mensaje: " + e.getMessage() + "\n");
        }
    }

    private void recibirMensajes() {
        try {
            while (true) {
                String tipo = entrada.readUTF();

                if (tipo.equals("TEXTO")) {
                    String mensaje = entrada.readUTF();

                    areaChat.append("Servidor: " + mensaje + "\n");

                    ConexionBD.guardarMensaje("Servidor", mensaje);
                }

                else if (tipo.equals("IMAGEN")) {
                    String nombreArchivo = entrada.readUTF();
                    int tamanio = entrada.readInt();

                    byte[] bytes = new byte[tamanio];
                    entrada.readFully(bytes);

                    File carpeta = new File("imagenes_recibidas");
                    if (!carpeta.exists()) {
                        carpeta.mkdir();
                    }

                    File archivo = new File(carpeta, nombreArchivo);

                    FileOutputStream fos = new FileOutputStream(archivo);
                    fos.write(bytes);
                    fos.close();

                    ConexionBD.guardarImagen(
                            "Servidor",
                            nombreArchivo,
                            archivo.getAbsolutePath()
                    );

                    areaChat.append("📷 Imagen recibida: " + nombreArchivo + "\n");

                    ImageIcon icono = new ImageIcon(archivo.getAbsolutePath());

                    Image imagenEscalada = icono.getImage().getScaledInstance(
                            250,
                            180,
                            Image.SCALE_SMOOTH
                    );

                    ImageIcon iconoEscalado = new ImageIcon(imagenEscalada);

                    JLabel etiquetaImagen = new JLabel(iconoEscalado);

                    JOptionPane.showMessageDialog(
                            this,
                            etiquetaImagen,
                            "Imagen recibida",
                            JOptionPane.PLAIN_MESSAGE
                    );
                }
            }

        } catch (IOException e) {
            areaChat.append("Conexión finalizada.\n");
        }
    }

    private void enviarImagen() {
        try {
            JFileChooser fileChooser = new JFileChooser();

            int opcion = fileChooser.showOpenDialog(this);

            if (opcion == JFileChooser.APPROVE_OPTION) {
                File archivo = fileChooser.getSelectedFile();

                FileInputStream fis = new FileInputStream(archivo);
                byte[] bytes = fis.readAllBytes();
                fis.close();

                salida.writeUTF("IMAGEN");
                salida.writeUTF(archivo.getName());
                salida.writeInt(bytes.length);
                salida.write(bytes);
                salida.flush();

                String nombreArchivo = archivo.getName();

                areaChat.append("📤 Imagen enviada: " + nombreArchivo + "\n");

                ConexionBD.guardarImagen(
                        "Cliente",
                        nombreArchivo,
                        archivo.getAbsolutePath()
                );
            }

        } catch (IOException e) {
            areaChat.append("Error al enviar imagen: " + e.getMessage() + "\n");
        } catch (Exception e) {
            areaChat.append("No hay conexión con el servidor.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClienteGUI ventana = new ChatClienteGUI();
            ventana.setVisible(true);
        });
    }
}
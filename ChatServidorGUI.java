import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatServidorGUI extends JFrame {

    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;

    private JTextArea areaChat;
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JComboBox<String> comboEmojis;
    private JButton botonImagen;

    public ChatServidorGUI() {
        setTitle("Servidor - Chat Socket");
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

        iniciarServidor();
    }

    private void iniciarServidor() {
        Thread hilo = new Thread(() -> {
            try {
                String puertoTexto = JOptionPane.showInputDialog(
                        this,
                        "Ingrese el puerto del servidor:"
                );

                if (puertoTexto == null || puertoTexto.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Debe ingresar el puerto.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(0);
                }

                int puerto = Integer.parseInt(puertoTexto.trim());

                serverSocket = new ServerSocket(puerto);

                areaChat.append("Servidor iniciado en puerto " + puerto + "\n");
                areaChat.append("Esperando cliente...\n");

                socket = serverSocket.accept();

                areaChat.append("Cliente conectado: "
                        + socket.getInetAddress().getHostName() + "\n");

                entrada = new DataInputStream(socket.getInputStream());
                salida = new DataOutputStream(socket.getOutputStream());

                recibirMensajes();

            } catch (NumberFormatException e) {
                areaChat.append("Puerto inválido.\n");
            } catch (IOException e) {
                areaChat.append("Error en servidor: " + e.getMessage() + "\n");
            }
        });

        hilo.start();
    }

    private void enviarMensaje() {
        try {
            String mensaje = campoMensaje.getText().trim();

            if (!mensaje.isEmpty()) {
                salida.writeUTF("TEXTO");
                salida.writeUTF(mensaje);
                salida.flush();

                areaChat.append("Servidor: " + mensaje + "\n");

                ConexionBD.guardarMensaje("Servidor", mensaje);

                campoMensaje.setText("");
            }

        } catch (Exception e) {
            areaChat.append("No hay cliente conectado o error al enviar.\n");
        }
    }

    private void recibirMensajes() {
        try {
            while (true) {
                String tipo = entrada.readUTF();

                if (tipo.equals("TEXTO")) {
                    String mensaje = entrada.readUTF();

                    areaChat.append("Cliente: " + mensaje + "\n");

                    ConexionBD.guardarMensaje("Cliente", mensaje);
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

                    ConexionBD.guardarImagen(
                            "Cliente",
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

                areaChat.append("📤 Imagen enviada: " + archivo.getName() + "\n");

                ConexionBD.guardarImagen(
                        "Servidor",
                        archivo.getName(),
                        archivo.getAbsolutePath()
                );
            }

        } catch (IOException e) {
            areaChat.append("Error al enviar imagen: " + e.getMessage() + "\n");
        } catch (Exception e) {
            areaChat.append("No hay cliente conectado.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatServidorGUI().setVisible(true);
        });
    }
}
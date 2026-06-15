import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3308/chat_socket";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "";

    public static void guardarMensaje(String usuario, String mensaje) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);

            String sql = "INSERT INTO mensajes(usuario, mensaje) VALUES (?, ?)";
            PreparedStatement ps = conexion.prepareStatement(sql);

            ps.setString(1, usuario);
            ps.setString(2, mensaje);

            ps.executeUpdate();

            System.out.println("Mensaje guardado correctamente");

            ps.close();
            conexion.close();

        } catch (Exception e) {
            System.out.println("Error al guardar mensaje:");
            e.printStackTrace();
        }
    }

    public static void guardarImagen(String usuario, String nombreArchivo, String rutaArchivo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);

            String sql = "INSERT INTO imagenes(usuario, nombre_archivo, ruta_archivo) VALUES (?, ?, ?)";
            PreparedStatement ps = conexion.prepareStatement(sql);

            ps.setString(1, usuario);
            ps.setString(2, nombreArchivo);
            ps.setString(3, rutaArchivo);

            ps.executeUpdate();

            System.out.println("Imagen guardada correctamente");

            ps.close();
            conexion.close();

        } catch (Exception e) {
            System.out.println("Error al guardar imagen:");
            e.printStackTrace();
        }
    }
}
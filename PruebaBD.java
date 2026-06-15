public class PruebaBD {

    public static void main(String[] args) {

        ConexionBD.guardarMensaje(
                "Prueba",
                "Hola desde Java"
        );

        System.out.println("Prueba terminada");
    }
}
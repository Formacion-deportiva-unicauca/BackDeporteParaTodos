package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Asistencia {
    private String idPerfil;
    private Integer clsCodigo;
    private Integer eliminado;
    // Faltas acumuladas (etapa 2 de 5): PRESENTE | AUSENTE. El constructor de
    // 3 argumentos (preexistente, usado en ~25 sitios del proyecto) la
    // inicializa en PRESENTE -- hoy ese es el UNICO significado posible de una
    // fila creada por ese camino (el modelo viejo no genera filas AUSENTE; eso
    // llega en la etapa 4). No se reemplazo @AllArgsConstructor por uno de 4
    // argumentos para no romper esos sitios existentes.
    private String estado;

    public Asistencia(String idPerfil, Integer clsCodigo, Integer eliminado) {
        this(idPerfil, clsCodigo, eliminado, "PRESENTE");
    }

    public Asistencia(String idPerfil, Integer clsCodigo, Integer eliminado, String estado) {
        this.idPerfil = idPerfil;
        this.clsCodigo = clsCodigo;
        this.eliminado = eliminado;
        this.estado = estado;
    }
}

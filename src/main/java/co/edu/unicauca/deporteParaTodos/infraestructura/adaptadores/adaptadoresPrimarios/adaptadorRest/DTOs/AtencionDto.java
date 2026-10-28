package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AtencionDto {

    @NotNull(message = "El id del perfil no puede ser nulo")
    private String idPerfil;

    @NotNull(message = "El id de clase no puede ser nulo")
    private Integer idClase;

    @NotNull(message = "El valor de asistencia no puede ser nulo")
    private Boolean estaAtendido;

    public static AtencionDto fabricarDeModelo(Asistencia modelo) {
        AtencionDto dto = new AtencionDto();
        dto.setIdPerfil(modelo.getIdPerfil());
        dto.setIdClase(modelo.getClsCodigo());
        // Faltas acumuladas (etapa 2 de 5): estaAtendido = (ASI_ESTADO='PRESENTE'
        // AND META_ELIMINADO != 1) -- misma formula que AsistenciaMapper.toDto().
        dto.setEstaAtendido("PRESENTE".equals(modelo.getEstado())
                && (modelo.getEliminado() == null || modelo.getEliminado() != 1));
        return dto;
    }
}

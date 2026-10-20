package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.util.Base64;

import org.springframework.web.multipart.MultipartFile;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Imagen;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ImagenEntidad;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ImagenDto {
    private Integer id;

    
    private String nombre;

    
    private String tipoArchivo;

    
    private Long longitud;
    
    private MultipartFile datosMultipartFile;

    private String datosBase64;

    public static ImagenDto fabricaFromImagenEntidad(ImagenEntidad entidad){
        ImagenDto dto = new ImagenDto();
        dto.setId(entidad.getId());
        dto.setNombre(entidad.getNombre());
        dto.setLongitud(entidad.getLongitud());
        dto.setTipoArchivo(entidad.getTipoArchivo());
        dto.setDatosBase64(Base64.getEncoder().encodeToString(entidad.getDatos()));
        return dto;
    }

    
    public static ImagenDto fabricaFromImagenModelo(Imagen modelo){
        ImagenDto dto = new ImagenDto();
        dto.setId(modelo.getId());
        dto.setLongitud(modelo.getLongitud());
        dto.setNombre(modelo.getNombre());
        dto.setTipoArchivo(modelo.getTipoArchivo());
        dto.setDatosBase64(Base64.getEncoder().encodeToString(modelo.getDatos()));
        return dto;
    }
}

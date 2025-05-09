package br.edu.ufape.sguAuthService.comunicacao.dto.curso;



import br.edu.ufape.sguAuthService.models.Curso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.modelmapper.ModelMapper;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class CursoRequest {

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotNull(message = "O numero de periodos é obrigatório")
    private int numeroPeriodos;

    public Curso convertToEntity(CursoRequest cursoRequest, ModelMapper modelMapper) {
        return modelMapper.map(cursoRequest, Curso.class);
    }
}

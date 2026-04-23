package br.edu.ufape.sguAuthService.comunicacao.dto.aluno;

import br.edu.ufape.sguAuthService.models.Aluno;
import br.edu.ufape.sguAuthService.models.Usuario;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class AlunoPublicResponse {
    private UUID id;
    private CursoPublicDTO curso;

    public AlunoPublicResponse(Usuario usuario) {
        this.id = usuario.getId();
        usuario.getPerfil(Aluno.class).ifPresent(aluno -> {
            if (aluno.getCurso() != null) {
                this.curso = new CursoPublicDTO(aluno.getCurso().getId(), aluno.getCurso().getNome());
            }
        });
    }

    @Data
    @NoArgsConstructor
    public static class CursoPublicDTO {
        private Long id;
        private String nome;

        public CursoPublicDTO(Long id, String nome) {
            this.id = id;
            this.nome = nome;
        }
    }
}
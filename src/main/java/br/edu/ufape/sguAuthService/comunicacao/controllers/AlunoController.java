package br.edu.ufape.sguAuthService.comunicacao.controllers;


import br.edu.ufape.sguAuthService.comunicacao.dto.aluno.AlunoPublicResponse;
import br.edu.ufape.sguAuthService.comunicacao.dto.aluno.AlunoResponse;
import br.edu.ufape.sguAuthService.exceptions.notFoundExceptions.AlunoNotFoundException;
import br.edu.ufape.sguAuthService.exceptions.notFoundExceptions.UsuarioNotFoundException;
import br.edu.ufape.sguAuthService.fachada.Fachada;
import br.edu.ufape.sguAuthService.models.Usuario;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController @RequestMapping("/aluno")  @RequiredArgsConstructor

public class AlunoController {
    private final Fachada fachada;
    private final ModelMapper modelMapper;

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GESTOR')")
    @GetMapping("/{id}") ResponseEntity<AlunoResponse> buscarAluno(@PathVariable UUID id) throws AlunoNotFoundException, UsuarioNotFoundException {
        Usuario response = fachada.buscarAluno(id);
        return new ResponseEntity<>(new AlunoResponse(response, modelMapper), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GESTOR')")
    @GetMapping
    public Page<AlunoResponse> listarAlunos(
            @QuerydslPredicate(root = Usuario.class) Predicate predicate,
            @PageableDefault(value = 2)
            @SortDefault(sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return fachada.listarAlunos(predicate, pageable).map(usuario -> new AlunoResponse(usuario, modelMapper));

    }



    @PostMapping("/batch")
    List<AlunoResponse> listarAlunosEmBatch(@RequestBody List<UUID> ids) {
        return fachada.listarUsuariosEmBatch(ids).stream().map(usuario -> new AlunoResponse(usuario, modelMapper)).toList();
    }

    @GetMapping("/current")
    ResponseEntity<AlunoResponse> buscarAlunoAtual() throws AlunoNotFoundException, UsuarioNotFoundException {
        Usuario response = fachada.buscarAlunoAtual();
        return new ResponseEntity<>(new AlunoResponse(response, modelMapper), HttpStatus.OK);
    }

    @PostMapping("/public/batch")
    public ResponseEntity<List<AlunoPublicResponse>> listarAlunosPublicosEmBatch(@RequestBody List<UUID> ids) {
        List<AlunoPublicResponse> response = fachada.listarUsuariosEmBatch(ids).stream()
                .map(AlunoPublicResponse::new)
                .toList();

        return ResponseEntity.ok(response);
    }

}

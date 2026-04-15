package br.edu.ufape.sguAuthService.servicos;

import br.edu.ufape.sguAuthService.config.AuthenticatedUserProvider;
import br.edu.ufape.sguAuthService.dados.UsuarioRepository;


import br.edu.ufape.sguAuthService.exceptions.accessDeniedException.GlobalAccessDeniedException;
import br.edu.ufape.sguAuthService.exceptions.notFoundExceptions.UsuarioNotFoundException;
import br.edu.ufape.sguAuthService.models.Aluno;
import br.edu.ufape.sguAuthService.models.QUsuario;
import br.edu.ufape.sguAuthService.models.Usuario;

import br.edu.ufape.sguAuthService.models.Visitante;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class UsuarioService implements br.edu.ufape.sguAuthService.servicos.interfaces.UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    @Override
    public Usuario salvar(Usuario usuario) {
        Visitante visitante = new Visitante();
        usuario.adicionarPerfil(visitante);
        return usuarioRepository.save(usuario);
    }
    @Override
    public Usuario editarUsuario(Usuario novoUsuario) throws UsuarioNotFoundException {
        UUID idSessao = authenticatedUserProvider.getUserId();
        Usuario antigoUsuario =  usuarioRepository.findById(idSessao).orElseThrow(UsuarioNotFoundException::new);
        modelMapper.map(novoUsuario, antigoUsuario);
        return usuarioRepository.save(antigoUsuario);
    }

    @Override
    public Usuario buscarUsuario(UUID id, boolean isAdm, UUID sessionId) throws UsuarioNotFoundException {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(UsuarioNotFoundException::new);
        if(!isAdm && !usuario.getId().equals(sessionId)) {
            throw new GlobalAccessDeniedException("Você não tem permissão para acessar este recurso");
        }
        return usuario;
    }

    @Override
    public Usuario buscarUsuarioAtual() throws UsuarioNotFoundException{
        UUID idSessao = authenticatedUserProvider.getUserId();
        return usuarioRepository.findById(idSessao).orElseThrow(UsuarioNotFoundException::new);
    }

    @Override
    public Page<Usuario> listarUsuarios(Predicate predicate, Pageable pageable) {
        QUsuario qUsuario = QUsuario.usuario;
        BooleanBuilder filtroFixo = new BooleanBuilder();
        filtroFixo.and(qUsuario.ativo.isTrue());

        Predicate predicadoFinal = filtroFixo.and(predicate);

        return usuarioRepository.findAll(predicadoFinal, pageable);
    }


    @Override
    public void deletarUsuario(UUID sessionId) throws UsuarioNotFoundException {
        Usuario usuario = usuarioRepository.findById(sessionId).orElseThrow(UsuarioNotFoundException::new);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    public List<Usuario> buscarUsuariosPorIds(List<UUID> kcIds) {
        if (kcIds == null || kcIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Usuario> usuarios = usuarioRepository.findByIdIn(kcIds);

        Map<UUID, Usuario> usuarioMap = usuarios.stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        // O filtro "Objects::nonNull" garante que não devolveremos "nulls" no JSON se um UUID não for encontrado
        List<Usuario> users = kcIds.stream()
                .map(usuarioMap::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        logger.info("Buscados {} usuarios em batch.", users.size());
        return users;
    }



}

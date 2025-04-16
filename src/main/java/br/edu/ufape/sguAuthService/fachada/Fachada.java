package br.edu.ufape.sguAuthService.fachada;


import br.edu.ufape.sguAuthService.comunicacao.dto.documento.DocumentoResponse;
import br.edu.ufape.sguAuthService.config.AuthenticatedUserProvider;
import br.edu.ufape.sguAuthService.exceptions.unidadeAdministrativa.UnidadeAdministrativaNotFoundException;
import br.edu.ufape.sguAuthService.models.UnidadeAdministrativa;
import br.edu.ufape.sguAuthService.comunicacao.dto.auth.TokenResponse;
import br.edu.ufape.sguAuthService.exceptions.ExceptionUtil;
import br.edu.ufape.sguAuthService.exceptions.accessDeniedException.GlobalAccessDeniedException;
import br.edu.ufape.sguAuthService.exceptions.SolicitacaoDuplicadaException;
import br.edu.ufape.sguAuthService.exceptions.notFoundExceptions.*;
import br.edu.ufape.sguAuthService.exceptions.auth.KeycloakAuthenticationException;
import br.edu.ufape.sguAuthService.models.*;
import br.edu.ufape.sguAuthService.servicos.interfaces.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


@Component @RequiredArgsConstructor

public class Fachada {
    private static final Logger log = LoggerFactory.getLogger(Fachada.class);
    private final AlunoService alunoService;
    private final UnidadeAdministrativaService unidadeAdministrativaService;
    private final UsuarioService usuarioService;
    private final KeycloakServiceInterface keycloakService;
    private final CursoService cursoService;
    private final SolicitacaoPerfilService solicitacaoPerfilService;
    private final ArmazenamentoService armazenamentoService;
    private final PerfilService perfilService;
    private final ProfessorService professorService;
    private final TecnicoService tecnicoService;
    private final GestorService gestorService;
    private final TipoUnidadeAdministrativaService tipoUnidadeAdministrativaService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    // ================== Auth ================== //
    public TokenResponse login(String username, String password) {
        return keycloakService.login(username, password);
    }

    public TokenResponse refresh(String refreshToken) {
        return keycloakService.refreshToken(refreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        keycloakService.logout(accessToken, refreshToken);
    }

    public void resetPassword(String email) {
        keycloakService.resetPassword(email);
    }

    // ================== Aluno ================== //


    public List<Usuario> listarAlunos() {
        return alunoService.listarAlunos();
    }

    public Usuario buscarAluno(UUID id) throws AlunoNotFoundException, UsuarioNotFoundException {
        UUID sessionId = authenticatedUserProvider.getUserId();
        boolean isAdmin = keycloakService.hasRoleAdmin(sessionId.toString());
        return alunoService.buscarAluno(id, isAdmin, sessionId);
    }

    public Usuario buscarAlunoAtual() throws UsuarioNotFoundException {
        return alunoService.buscarAlunoAtual();
    }


    // ================== Professor ================== //
    public List<Usuario> listarProfessores() {
        return professorService.listarProfessores();
    }

    public Usuario buscarProfessor(UUID id) throws UsuarioNotFoundException, ProfessorNotFoundException {
        UUID sessionId = authenticatedUserProvider.getUserId();
        boolean isAdmin = keycloakService.hasRoleAdmin(sessionId.toString());
        return professorService.buscarProfessor(id, isAdmin, sessionId);
    }


    // ================== Tecnico ================== //

    public List<Usuario> listarTecnicos(){
        return tecnicoService.getTecnicos();
    }

    public Usuario buscarTecnico(UUID id) throws UsuarioNotFoundException, TecnicoNotFoundException {
        UUID sessionId = authenticatedUserProvider.getUserId();
        boolean isAdmin = keycloakService.hasRoleAdmin(sessionId.toString());
        return tecnicoService.buscarTecnico(id, isAdmin, sessionId);
    }

    public Usuario buscarTecnicoAtual() throws UsuarioNotFoundException, TecnicoNotFoundException {
        return tecnicoService.buscarTecnicoAtual();
    }



    // ================== Gestor ================== //
    public List<Usuario> listarGestores() {
        return gestorService.listarGestores();
    }

    public Usuario buscarGestor(UUID id) throws GestorNotFoundException, UsuarioNotFoundException {
        UUID sessionId = authenticatedUserProvider.getUserId();
        boolean isAdmin = keycloakService.hasRoleAdmin(sessionId.toString());
        return gestorService.buscarGestor(id, isAdmin, sessionId);
    }


    // ================== Usuario ================== //
    @Transactional
    public void removerUsuariosNaoVerificados(int horas) {
        List<UserRepresentation> usuariosNaoVerificados = keycloakService.listUnverifiedUsers();

        for (UserRepresentation user : usuariosNaoVerificados) {
            long diferenca = System.currentTimeMillis() - user.getCreatedTimestamp();
            if(diferenca > TimeUnit.HOURS.toMillis(horas)){
                try {
                    usuarioService.deletarUsuario(UUID.fromString(user.getId()));
                    keycloakService.deleteUser(user.getId());
                    log.info("Usuário não verificado removido");
                }
                 catch (Exception e){
                    log.error("Erro ao deletar usuário não verificado: {}", e.getMessage());
                }
            }
        }
    }
    @Transactional
    public Usuario salvarUsuario(Usuario usuario, String senha) {
        UUID userId = null;
            keycloakService.createUser(usuario.getEmail(), senha, "visitante");
            try {
                userId = UUID.fromString(keycloakService.getUserId(usuario.getEmail()));
                usuario.setId(userId);
                return usuarioService.salvar(usuario);
            }catch (DataIntegrityViolationException e){
                assert userId != null;
                keycloakService.deleteUser(userId.toString());
                throw ExceptionUtil.handleDataIntegrityViolationException(e);
            }catch (Exception e){
                assert userId != null;
                keycloakService.deleteUser(userId.toString());
                throw new RuntimeException("Ocorreu um erro inesperado ao salvar o usuário: "+ e.getMessage(), e);
            }
    }

    public Usuario editarUsuario(Usuario novoUsuario) throws UsuarioNotFoundException {
        return usuarioService.editarUsuario(novoUsuario);
    }

    public Usuario buscarUsuario(UUID id) throws UsuarioNotFoundException {
        UUID sessionId = authenticatedUserProvider.getUserId();
        boolean isAdmin = keycloakService.hasRoleAdmin(sessionId.toString());
        return usuarioService.buscarUsuario(id, isAdmin, sessionId);
    }

    public Usuario buscarUsuarioAtual() throws UsuarioNotFoundException{
        return usuarioService.buscarUsuarioAtual();
    }

    public List<Usuario> listarUsuarios() {return usuarioService.listarUsuarios();}

    public List<Usuario> listarUsuariosEmBatch(List<UUID> ids) {
        return usuarioService.buscarUsuariosPorIds(ids);
    }

    public void deletarUsuario() throws UsuarioNotFoundException {
        UUID idSessao = authenticatedUserProvider.getUserId();
        try {
            keycloakService.deleteUser(idSessao.toString()
            );
        } catch (KeycloakAuthenticationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        usuarioService.deletarUsuario(idSessao);}

    // ================== Curso ================== //
    public Curso salvarCurso(Curso curso){
        return cursoService.salvar(curso);
    }

    public Curso buscarCurso(Long id) throws CursoNotFoundException {
        return cursoService.buscar(id);
    }

    public List<Curso> listarCursos() {
        return cursoService.listar();
    }

    public List<Usuario> listarAlunosPorCurso(Long id){
        return cursoService.listarAlunosPorCurso(id);
    }

    public Curso editarCurso(Long id, Curso novoCurso) throws CursoNotFoundException{
        return cursoService.editar(id, novoCurso);
    }

    public void deletarCurso(Long id) throws CursoNotFoundException {
        cursoService.deletar(id);
    }

    // ================== SolicitacaoPerfil ================== //
    @Transactional
    public SolicitacaoPerfil solicitarPerfil(Perfil perfil,MultipartFile[] arquivos) throws UsuarioNotFoundException, SolicitacaoDuplicadaException {

        Perfil perfilSalvo = perfilService.salvar(perfil);
        Usuario solicitante = buscarUsuarioAtual();
        List<Documento> documentos = armazenamentoService.salvarArquivo(arquivos);
        return solicitacaoPerfilService.solicitarPerfil(perfilSalvo, solicitante, documentos);
    }

    public SolicitacaoPerfil buscarSolicitacao(Long id) throws SolicitacaoNotFoundException {
        UUID sessionId = authenticatedUserProvider.getUserId();
        SolicitacaoPerfil solicitacao = solicitacaoPerfilService.buscarSolicitacao(id);
        if(!solicitacao.getSolicitante().getId().equals(sessionId) && !keycloakService.hasRoleAdmin(String.valueOf(sessionId))){
            throw new GlobalAccessDeniedException("Você não tem permissão para acessar este recurso");
        }
        return solicitacaoPerfilService.buscarSolicitacao(id);
    }

    public List<SolicitacaoPerfil> buscarSolicitacoesUsuarioAtual() {
        return solicitacaoPerfilService.buscarSolicitacoesUsuarioAtual();
    }

    public List<SolicitacaoPerfil> buscarSolicitacoesPorId(UUID id) {
        return solicitacaoPerfilService.buscarSolicitacoesPorId(id);
    }

    public List<SolicitacaoPerfil> listarSolicitacoes() {
        return solicitacaoPerfilService.listarSolicitacoes();
    }

    public List<SolicitacaoPerfil> listarSolicitacoesPendentes() {
        return solicitacaoPerfilService.listarSolicitacoesPendentes();
    }

    public List<DocumentoResponse> listarDocumentosBase64(Long id) throws SolicitacaoNotFoundException, IOException {
        UUID sessionId = authenticatedUserProvider.getUserId();
        SolicitacaoPerfil solicitacao = solicitacaoPerfilService.buscarSolicitacao(id);
        if(!solicitacao.getSolicitante().getId().equals(sessionId) && !keycloakService.hasRoleAdmin(String.valueOf(sessionId))){
            throw new GlobalAccessDeniedException("Você não tem permissão para acessar este recurso");
        }
        return armazenamentoService.converterDocumentosParaBase64(solicitacao.getDocumentos());
    }



    @Transactional
    public SolicitacaoPerfil aceitarSolicitacao(Long id, SolicitacaoPerfil parecer) throws SolicitacaoNotFoundException, UsuarioNotFoundException {
        Usuario usuario = buscarUsuarioAtual();
        parecer.setResponsavel(usuario);
        SolicitacaoPerfil solicitacaoPerfil =  solicitacaoPerfilService.aceitarSolicitacao(id, parecer);
        String tipoPerfil = solicitacaoPerfil.getPerfil().getClass().getSimpleName().toLowerCase();
        keycloakService.addRoleToUser(String.valueOf(solicitacaoPerfil.getSolicitante().getId()), tipoPerfil);
        return solicitacaoPerfil;
    }

    @Transactional
    public SolicitacaoPerfil rejeitarSolicitacao(Long id, SolicitacaoPerfil parecer) throws SolicitacaoNotFoundException, UsuarioNotFoundException {
        Usuario usuario = buscarUsuarioAtual();
        parecer.setResponsavel(usuario);
        SolicitacaoPerfil solicitacaoRejeitada = solicitacaoPerfilService.rejeitarSolicitacao(id, parecer);
        Perfil perfil = solicitacaoRejeitada.getPerfil();
        solicitacaoRejeitada.setPerfil(null);
        perfilService.deletarPerfil(perfil.getId());
        return solicitacaoRejeitada;
    }



    // ================== Unidade Administrativa ================== //
    public UnidadeAdministrativa salvar(UnidadeAdministrativa unidadeAdministrativa, Long paiId) throws UnidadeAdministrativaNotFoundException {
        TipoUnidadeAdministrativa tipoUnidadeAdministrativa = tipoUnidadeAdministrativaService.
                buscar(unidadeAdministrativa.getTipoUnidadeAdministrativa().getId());
        return unidadeAdministrativaService.salvar(unidadeAdministrativa, tipoUnidadeAdministrativa, paiId);
    }
    public UnidadeAdministrativa buscarUnidadeAdministrativa(Long id) throws UnidadeAdministrativaNotFoundException{
        return unidadeAdministrativaService.buscarUnidadeAdministrativa(id);
    }
    public List<UnidadeAdministrativa> listarUnidadesAdministrativas() {
        return unidadeAdministrativaService.listarUnidadesAdministrativas();
    }

    public List<UnidadeAdministrativa> montarArvore() {
        return unidadeAdministrativaService.montarArvore();
    }

    public List<UnidadeAdministrativa> listarUnidadesFilhas(Long id) {
        return unidadeAdministrativaService.listarUnidadesFilhas(id);
    }

    public void deletarUnidadeAdministrativa(Long id) throws UnidadeAdministrativaNotFoundException{
        unidadeAdministrativaService.deletarUnidadeAdministrativa(id);
    }

    public UnidadeAdministrativa editarUnidadeAdministrativa(Long id, UnidadeAdministrativa novaUnidadeAdministrativa) throws UnidadeAdministrativaNotFoundException {
        return unidadeAdministrativaService.editarUnidadeAdministrativa(novaUnidadeAdministrativa, id);
    }

    public GestorUnidade adicionarGestor(Long unidadeId, GestorUnidade gestorUnidade, UUID gestorId) {
        Usuario gestor = gestorService.buscarGestor(gestorId, true, null);
        gestorUnidade.setGestor(gestor.getPerfil(Gestor.class).orElseThrow());
        return unidadeAdministrativaService.adicionarGestor(unidadeId, gestorUnidade);
    }

    public void removerGestor(Long unidadeId, UUID gestorId) {
        Usuario gestor = gestorService.buscarGestor(gestorId, true, null);
        unidadeAdministrativaService.removerGestor(unidadeId, gestor.getPerfil(Gestor.class).orElseThrow().getId());
    }

    public Usuario adicionarFuncionario(Long unidadeId, UUID usuarioId) {
        Usuario funcionario = usuarioService.buscarUsuario(usuarioId, true, null);
        unidadeAdministrativaService.adicionarFuncionario(unidadeId, funcionario);
        return funcionario;
    }

    public void removerFuncionario(Long unidadeId, UUID usuarioId) {
        Usuario funcionario = usuarioService.buscarUsuario(usuarioId, true, null);
        unidadeAdministrativaService.removerFuncionario(unidadeId, funcionario);
    }

    public UnidadeAdministrativa listarTecnicosUnidade(Long id) throws UnidadeAdministrativaNotFoundException{
        return unidadeAdministrativaService.buscarUnidadeAdministrativa(id);
    }


    // ==================Tipo Unidade Administrativa ================== //
     public TipoUnidadeAdministrativa salvarTipo(TipoUnidadeAdministrativa tipoUnidadeAdministrativa) {
        return tipoUnidadeAdministrativaService.salvar(tipoUnidadeAdministrativa);
    }

    public TipoUnidadeAdministrativa buscarTipo(Long id) throws TipoUnidadeAdministrativaNotFoundException {
        return tipoUnidadeAdministrativaService.buscar(id);
    }
    public List<TipoUnidadeAdministrativa> listarTipos() {
       return tipoUnidadeAdministrativaService.listar();

    }
    public TipoUnidadeAdministrativa editarTipo(Long id, TipoUnidadeAdministrativa novoTipo) throws TipoUnidadeAdministrativaNotFoundException {
        return tipoUnidadeAdministrativaService.editar(id, novoTipo);
    }

    public void deletarTipo(Long id) throws TipoUnidadeAdministrativaNotFoundException {
        tipoUnidadeAdministrativaService.deletar(id);
    }
}

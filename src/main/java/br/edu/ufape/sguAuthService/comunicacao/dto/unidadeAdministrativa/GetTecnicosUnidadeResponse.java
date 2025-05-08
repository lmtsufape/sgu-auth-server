package br.edu.ufape.sguAuthService.comunicacao.dto.unidadeAdministrativa;

import br.edu.ufape.sguAuthService.models.Tecnico;
import br.edu.ufape.sguAuthService.models.UnidadeAdministrativa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GetTecnicosUnidadeResponse {
    private List<Tecnico> tecnicos;

    public GetTecnicosUnidadeResponse(UnidadeAdministrativa unidadeAdministrativa, ModelMapper modelMapper) {
        this.tecnicos = unidadeAdministrativa.getTecnicos();
    }
}

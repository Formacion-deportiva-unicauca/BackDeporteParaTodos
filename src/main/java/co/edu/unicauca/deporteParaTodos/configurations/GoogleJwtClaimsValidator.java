package co.edu.unicauca.deporteParaTodos.configurations;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;

// Google emite "iss" como "https://accounts.google.com" o "accounts.google.com" segun el caso;
// se lee como String (getClaimAsString), no con Jwt.getIssuer(), porque ese metodo intenta
// parsear el valor como java.net.URL y "accounts.google.com" (sin esquema) no es una URL valida.
public class GoogleJwtClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static final Set<String> EMISORES_VALIDOS = Set.of(
            "https://accounts.google.com", "accounts.google.com");

    private static final OAuth2Error ERROR_AUDIENCIA = new OAuth2Error(
            "invalid_token", "El token no fue emitido para esta aplicacion", null);
    private static final OAuth2Error ERROR_EMISOR = new OAuth2Error(
            "invalid_token", "El token no fue emitido por Google", null);

    private final String clientIdEsperado;

    public GoogleJwtClaimsValidator(String clientIdEsperado) {
        this.clientIdEsperado = clientIdEsperado;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audiencia = token.getAudience();
        if (audiencia == null || !audiencia.contains(clientIdEsperado)) {
            return OAuth2TokenValidatorResult.failure(ERROR_AUDIENCIA);
        }
        String emisor = token.getClaimAsString("iss");
        if (emisor == null || !EMISORES_VALIDOS.contains(emisor)) {
            return OAuth2TokenValidatorResult.failure(ERROR_EMISOR);
        }
        return OAuth2TokenValidatorResult.success();
    }
}

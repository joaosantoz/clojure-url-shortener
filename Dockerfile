# syntax=docker/dockerfile:1

# =============================================================================
# Estagio 1 - build: compila o projeto e gera o uberjar
# =============================================================================
FROM clojure:temurin-21-tools-deps AS build

WORKDIR /build

# Copia primeiro so os arquivos de dependencias para aproveitar o cache de camadas:
# enquanto deps.edn nao mudar, o download das libs nao e refeito.
COPY deps.edn build.clj ./
RUN clojure -P && clojure -P -X:test

# Agora o codigo-fonte e os testes
COPY src ./src
COPY test ./test

# Os testes tambem rodam aqui: se quebrarem, a imagem nao e construida
RUN clojure -X:test
RUN clojure -T:build uber

# =============================================================================
# Estagio 2 - runtime: imagem final, so com a JRE e o jar
# =============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

ENV PORT=3000

WORKDIR /app

# curl e usado pelo HEALTHCHECK; appuser evita rodar a aplicacao como root
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --create-home --uid 10001 appuser

COPY --from=build /build/target/url-shortener-*-standalone.jar /app/app.jar

USER appuser

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD curl --fail "http://localhost:${PORT}/health" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

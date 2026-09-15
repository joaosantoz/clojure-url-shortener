# clojure-url-shortener

[![CI](https://github.com/joaosantoz/clojure-url-shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/joaosantoz/clojure-url-shortener/actions/workflows/ci.yml)
[![CD](https://github.com/joaosantoz/clojure-url-shortener/actions/workflows/cd.yml/badge.svg)](https://github.com/joaosantoz/clojure-url-shortener/actions/workflows/cd.yml)

Encurtador de URL escrito em Clojure, com API HTTP em Ring/Jetty.

Projeto desenvolvido na disciplina de DevOps (PUCPR) para praticar Git, CI/CD com
GitHub Actions e containerizacao com Docker.

## Endpoints

| Metodo | Rota        | Descricao                                        |
| ------ | ----------- | ------------------------------------------------ |
| GET    | `/`         | Descreve o servico e lista os endpoints          |
| GET    | `/health`   | Health check usado pelo Docker e pelos pipelines |
| POST   | `/shorten`  | Encurta uma URL (`{"url": "https://..."}`)       |
| GET    | `/links`    | Lista todos os links encurtados                  |
| GET    | `/{codigo}` | Redireciona (302) para a URL original            |

A mesma URL enviada duas vezes devolve sempre o mesmo codigo: a primeira
chamada responde `201 Created` e as seguintes respondem `200 OK`.

### Exemplo

```bash
curl -X POST http://localhost:3000/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.pucpr.br/graduacao/"}'
```

```json
{
  "id": "0",
  "url": "https://www.pucpr.br/graduacao/",
  "short_url": "http://localhost:3000/0",
  "novo": true
}
```

## Rodando localmente

Requisitos: JDK 21 e [Clojure CLI](https://clojure.org/guides/install_clojure).

```bash
clojure -M:run          # sobe o servidor em http://localhost:3000
clojure -X:test         # roda a suite de testes
clojure -T:build uber   # gera target/url-shortener-1.0.0-standalone.jar
```

## Rodando com Docker

```bash
docker build -t url-shortener:1.0.0 .
docker run -d --name url-shortener -p 8080:3000 url-shortener:1.0.0
docker ps
curl http://localhost:8080/health
```

O container sempre escuta na porta `3000` internamente; o `-p` decide em qual
porta do host ele e publicado. Se a `8080` estiver ocupada, troque apenas o
lado esquerdo (por exemplo `-p 9000:3000`).

Para encerrar:

```bash
docker rm -f url-shortener
```

O `Dockerfile` usa build multi-stage:

1. **build** — parte de `clojure:temurin-21-tools-deps`, baixa as dependencias,
   roda os testes e gera o uberjar.
2. **runtime** — parte de `eclipse-temurin:21-jre-jammy`, copia apenas o jar,
   roda como usuario sem privilegios (`appuser`) e expoe um `HEALTHCHECK`
   apontando para `/health`.

Como os testes rodam dentro do estagio de build, uma imagem so e produzida se a
suite passar.

## Variaveis de ambiente

| Variavel   | Padrao         | Descricao                                       |
| ---------- | -------------- | ----------------------------------------------- |
| `PORT`     | `3000`         | Porta em que o servidor escuta                  |
| `BASE_URL` | host da request | Prefixo usado ao montar o campo `short_url`     |

## Pipelines

### CI (`.github/workflows/ci.yml`)

Dispara em todo push e em toda pull request para a `main`:

- **Testes unitarios** — instala JDK 21 e Clojure CLI, usa cache das
  dependencias e roda `clojure -X:test`.
- **Build da imagem Docker** — constroi a imagem, sobe o container e valida o
  endpoint `/health`.

### CD (`.github/workflows/cd.yml`)

Dispara em pull requests para a `main` e em pushes na `main`:

- **build** — gera o uberjar e publica como artefato do GitHub Actions.
- **deploy** — baixa o artefato, sobe a aplicacao empacotada e roda um smoke
  test nos endpoints `/health`, `/shorten` e `/{codigo}`.

## Estrutura

```
.
├── .github/workflows/   # pipelines de CI e CD
├── src/url_shortener/   # codigo da aplicacao
│   ├── core.clj         # ponto de entrada (-main) e servidor Jetty
│   ├── handler.clj      # roteamento HTTP no formato Ring
│   └── store.clj        # armazenamento em memoria e base62
├── test/url_shortener/  # testes unitarios
├── build.clj            # script do tools.build (uberjar)
├── deps.edn             # dependencias e aliases
└── Dockerfile           # build multi-stage
```

## Licenca

MIT. Veja [LICENSE](LICENSE).

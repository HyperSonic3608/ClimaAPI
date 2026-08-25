# ClimaAPI

API REST em Spring Boot que consulta a Open-Meteo e devolve uma saída simples com apenas os campos meteorológicos disponíveis.

## Execução

### Windows
```bash
mvnw.cmd spring-boot:run
```

### Linux/macOS
```bash
./mvnw spring-boot:run
```

## Testes

### Windows
```bash
mvnw.cmd test
```

### Linux/macOS
```bash
./mvnw test
```

## Dependências usadas

- Java 25
- Spring Boot 4.1.1
- spring-boot-starter-webmvc
- spring-boot-starter-webmvc-test

Não foi adicionada biblioteca externa para formatação da saída; a resposta é montada com Java padrão e recursos já disponíveis no Spring Boot.

## Endpoints disponíveis

### `GET /clima`
Retorna os dados meteorológicos de Belo Horizonte em JSON, exibindo apenas os campos disponíveis:

- 🌡️ `temperaturaAtual`
- 💧 `umidadeDoAr`
- 💨 `velocidadeDoVento`
- 🧭 `direcaoDoVento`
- 🌧️ `condicaoClimatica`
- 🌡️ `temperaturaMaxima`
- 🌡️ `temperaturaMinima`
- ☁️ `descricaoDasCondicoesDoTempo`
- 📍 `localizacaoDaCidade`
- 🕐 `dataHoraConsulta`

Exemplo de resposta:
```json
{
  "localizacaoDaCidade": "Belo Horizonte - MG",
  "dataHoraConsulta": "24/08/2026 22:56",
  "temperaturaAtual": "24.5 °C",
  "umidadeDoAr": "63.0 %",
  "velocidadeDoVento": "14.2 km/h",
  "direcaoDoVento": "270.0°",
  "condicaoClimatica": "Nublado",
  "descricaoDasCondicoesDoTempo": "Céu encoberto.",
  "temperaturaMaxima": "29.1 °C",
  "temperaturaMinima": "18.4 °C"
}
```

## Como rodar localmente

1. Abra um terminal na raiz do projeto.
2. Execute `mvnw.cmd spring-boot:run` no Windows ou `./mvnw spring-boot:run` no Linux/macOS.
3. Acesse `GET http://localhost:8080/clima`.
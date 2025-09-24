# NTT_AccountService

Microservicio bancario desarrollado en el Bootcamp de Microservicios.
Este repositorio corresponde a la Entrega Final del proyecto, donde se implementa el microservicio de Cuentas.

- Estado: Entrega Final
- Dominio: Bancario / Cuentas
- Arquitectura: Microservicios, Reactive (WebFlux)
- Contrato: OpenAPI (`src/main/resources/api.yml`)

## Índice
- Descripción
- Arquitectura y Diagramas
- Collections Pruebas APIs
- Tecnologías
- Repos Relacionados
- Estructura del Proyecto
- Integraciones y Reglas
- Configuración (puertos, perfiles y variables)
- Ejecución local
- Ejecución con Docker Compose
- Salud y diagnóstico
- SonarQube


## Descripción

Este microservicio expone APIs para:
- Gestionar cuentas (CRUD) por cliente y tipo (ahorro, corriente, plazo fijo).
- Realizar operaciones de saldo: depósitos, retiros y ajustes de comisión.
- Aplicar políticas de operaciones gratuitas y cobro de comisiones por tipo de cuenta.
- Validar reglas de elegibilidad y beneficios (p. ej., VIP/pyme requieren tarjeta de crédito).
- Integrarse con servicios externos (Clientes y Créditos) de forma reactiva.

El contrato OpenAPI vive en `src/main/resources/api.yml` y las implementaciones en `api/AccountApiDelegateImpl.java`.


## Arquitectura y Diagramas

### Arquitectura general del ecosistema
![Imagen de WhatsApp 2025-09-23 a las 17 22 32_3a609d96](https://github.com/user-attachments/assets/e65d7f30-71cf-4cd7-99dc-bf90f4b0ab16)

### Diagramas UML (Interacción entre Clientes, Transacciones, Cuentas y Créditos)
![](https://github.com/user-attachments/assets/288b7378-24f4-4be6-97f2-167f06baee26)

### Diagrama de Secuencia CRUD Cuentas
<img width="981" height="1315" alt="image" src="https://github.com/user-attachments/assets/0e064823-14be-482d-917a-35402bed412f" />

Archivo local: [Diagramas/DiagramaSecuenciaAccount.png](Diagramas/DiagramaSecuenciaAccount.png)


## Tecnologías

- Java 11 + Spring Boot 2.7 (WebFlux)
- Spring Security (Resource Server, JWT - Keycloak)
- Spring Data Reactive MongoDB
- Redis (caché)
- Spring Cloud Config, Eureka
- Resilience4j (circuit breaker, time limiter)
- Jackson, OpenAPI (springdoc)
- JUnit 5, Mockito, Reactor Test, MockWebServer
- Docker/Docker Compose


## Collections Pruebas APIs

- Repositorio Postman Collections: https://github.com/DianaPortal/postman-collections-ms-NTTDATA
- Archivo local en este repo: `AccountService.postmanCollection.json`


## Repos Relacionados

- Config repo (.properties): https://github.com/ArturoRoncal2704/nttdata-config-repo
- Config Server: https://github.com/ArturoRoncal2704/nttdata-config-serve
- API Gateway: https://github.com/ArturoRoncal2704/ntt-api-gateway
- Eureka Server: https://github.com/ArturoRoncal2704/ntt-eureka-server
- Infra Kafka/Redis: https://github.com/DianaPortal/infra-docker-kafka-redis
- Keycloak: https://github.com/ArturoRoncal2704/infra-keycloak


## Estructura del Proyecto

- `src/main/java/com/nttdata/accountservice`
	- `api`: capa API (delegates, implementación de endpoints)
	- `config`: configuración (JWT/Seguridad, etc)
	- `integration`: clientes HTTP reactivos
		- `customers`: `CustomersClient`, `EligibilityResponse`
		- `credits`: `CreditsClient`, `CreditDTO`
	- `model/entity`: entidades de dominio (`Account`, `OpsCounter`)
	- `repository`: repositorios (Reactive Mongo)
	- `service`: lógica de dominio y orquestación
		- `impl`: `AccountServiceImpl`, `AccountMapper`
		- `policy`: `AccountPolicyService` (políticas por tipo)
		- `rules`: `AccountRulesService` (validaciones y beneficios)
	- `util`: utilitarios (`AccountNumberGenerator`)
- `src/main/resources`
	- `api.yml` (contrato OpenAPI)
	- `application.properties` (config local)
- `src/test/java/...` (tests unitarios y de servicios)


## Integraciones y Reglas

Integraciones HTTP (reactivo) vía `WebClient`:
- Clientes:
	- `services.customers.url`
		- Local: `http://localhost:8086/api/v1`
		- Eureka/LB: `lb://customers-service/api/v1`
	- `services.credits.url`
		- Local (ejemplo docker-compose de créditos): `http://localhost:8585/api`
		- Eureka/LB: `lb://credits-service`

Resilience4j (config repo):
```
resilience4j.circuitbreaker.instances.customers.slidingWindowSize=10
resilience4j.circuitbreaker.instances.customers.minimumNumberOfCalls=5
resilience4j.circuitbreaker.instances.customers.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.customers.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.customers.failureRateThreshold=50
resilience4j.timelimiter.instances.customers.timeoutDuration=2s

resilience4j.circuitbreaker.instances.credits.slidingWindowSize=10
resilience4j.circuitbreaker.instances.credits.minimumNumberOfCalls=5
resilience4j.circuitbreaker.instances.credits.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.credits.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.credits.failureRateThreshold=50
resilience4j.timelimiter.instances.credits.timeoutDuration=2s
```

Políticas y Beneficios (config repo):
```
policy.savings.freeOps=5
policy.savings.fee=1.50
policy.checking.freeOps=10
policy.checking.fee=0.90
policy.fixed.freeOps=0
policy.fixed.fee=0.00

benefit.savings.vip.requireCreditCard=true
benefit.checking.pyme.requireCreditCard=true
```


## Configuración (puertos, perfiles y variables)

- Puerto del servicio: `8085`
- Perfil por defecto en contenedor: `docker`
- Dockerfile (runtime): `eclipse-temurin:11-jre-alpine` con healthcheck en `/actuator/health`

Variables clave (docker-compose):
```
SPRING_CLOUD_CONFIG_URI=http://config-server:8888
SPRING_PROFILES_ACTIVE=docker
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
APP_AUTH_ALLOWED_ISSUERS=http://keycloak:8091/realms/nttdatabank,http://localhost:8091/realms/nttdatabank
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8091/realms/nttdatabank
SERVICES_CUSTOMERS_URL=http://customers-service:8086/api/v1
SERVICES_CREDITS_URL=http://credits-service:8585/api
```

Propiedades en el config repo (account-service.properties, local example):
```
server.port=8085
spring.data.mongodb.uri=mongodb+srv://ntt_access_proyects:<password>@nttdatabnkdb.g8qwtzr.mongodb.net/NTTDataBnkDB?retryWrites=true&w=majority&appName=NTTDataBnkDB
spring.data.mongodb.database=NTTDataBnkDB
services.customers.url=http://localhost:8086/api/v1
services.credits.url=lb://credits-service

eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.instance.appname=account-service
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true

spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8091/realms/nttdatabank
app.auth.allowed-issuers=http://keycloak:8091/realms/nttdatabank,http://localhost:8091/realms/nttdatabank
```


## Seguridad - JWT - Keycloak

- Autenticación: Bearer JWT
- Decoder reactivo personalizado: `CustomReactiveJwtDecoder`
- Config de filtros/paths: `SecurityConfig`
- Cabecera: `Authorization: Bearer <token>`
- Emisor (local): `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8091/realms/nttdatabank`
- Issuers permitidos: `app.auth.allowed-issuers` (local y docker)


## Eureka

- `eureka.client.service-url.defaultZone=http://localhost:8761/eureka` (docker: `http://eureka-server:8761/eureka`)
- `eureka.instance.appname=account-service`


## Ejecución local

Opción A: sin Config Server (usa `src/main/resources/application.properties`)

1) Preparar dependencias y pruebas
```powershell
./mvnw clean verify
```
2) Ejecutar la app
```powershell
./mvnw spring-boot:run
```

Opción B: con Config Server

1) Arrancar Config Server en `http://localhost:8888`
2) Ejecutar la app apuntando a Config Server
```powershell
$env:SPRING_CLOUD_CONFIG_URI="http://localhost:8888"; ./mvnw spring-boot:run
```


## Ejecución con Docker Compose

Prepara la red compartida de infraestructura (si no existe):
```powershell
docker network create infra-net
```

Levantar el servicio desde la raíz del proyecto:
```powershell
docker compose up -d --build
```

Requisitos: `config-server`, `eureka`, `redis`, `keycloak` y los demás servicios relacionados deben estar accesibles en la red `infra-net` o mediante hosts equivalentes.


## Salud y diagnóstico

- Health: http://localhost:8085/actuator/health
- Logs del contenedor:
```powershell
docker logs -f account-service
```


## SonarQube

Propiedades (en config repo):
```
sonar.projectKey=accountservice
sonar.projectName=account_service
sonar.projectVersion=0.0.1
sonar.sourceEncoding=UTF-8
sonar.host.url=http://localhost:9000
sonar.sources=src/main/java,src/main/resources
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.exclusions=target/generated-sources/openapi/**, **/com/nttdata/accountservice/api/*Api*.java, **/com/nttdata/accountservice/model/*, **/*Application.java
sonar.junit.reportPaths=target/surefire-reports
```

Ejecución:
```powershell
./mvnw clean verify sonar:sonar -Dsonar.login=$env:SONAR_TOKEN
```




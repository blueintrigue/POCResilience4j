## Resilience4j Gateway POC

Spring Boot 3 + Resilience4j POC demonstrating per-service circuit breakers, timeouts, half-open admission control, consecutive success/failure enforcement, metrics and health.

### Requirements covered

- Closed → Open after 50 consecutive failures: modeled via count-based window size 50 and 100% failure threshold; plus half-open enforcer for consecutive counts.
- Open → Half-Open after 1 minute: `waitDurationInOpenState` with automatic transition.
- Half-Open allows 50% of requests: custom HalfOpenAdmissionService fail-fasts the remainder with `CallNotPermittedException`.
- Half-Open → Open after 50 consecutive failures; Half-Open → Closed after 50 consecutive successes: enforced by `HalfOpenConsecutiveEnforcer` which listens to events and transitions programmatically.
- Per-service circuit breakers and fallbacks using reactive `WebClient`.
- Metrics via Micrometer/Prometheus; health summary under `/actuator/health`.

### Build & Run

```bash
mvn -DskipTests package
java -jar target/resilience4j-gateway-poc-0.0.1-SNAPSHOT.jar
```

### Endpoints

- Composite sample: `GET /composite/{id}`
- Actuator health: `GET /actuator/health` (includes `downstreamCircuitBreakers` details)
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

### Configuration

All settings are in `src/main/resources/application.yml` and are service-specific under `downstreams.services.<serviceName>`.

- `timeout`: default 2s; also wired to Resilience4j TimeLimiter
- `halfOpenAdmission.allowPercent`: percentage of requests allowed during HALF_OPEN
- `halfOpenConsecutive.successToClose` / `failureToOpen`: consecutive thresholds
- `resilience4j.circuitbreaker.instances.<service>`: window sizes, wait duration, etc.

Runtime overrides supported via Spring Cloud Config. Enable by setting:

```bash
SPRING_CLOUD_CONFIG_ENABLED=true \
SPRING_CLOUD_CONFIG_URI=http://config-server:8888 \
java -jar target/resilience4j-gateway-poc-0.0.1-SNAPSHOT.jar
```

### Testing

```bash
mvn test
```

Tests simulate 5xx responses and timeouts using OkHttp MockWebServer, and validate circuit opening behavior and timeout fallbacks.

# POCResilience4j

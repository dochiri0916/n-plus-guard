# N+1 Guard Plan

## 1. 배경과 목표

현재 저장소는 `Spring Boot 4.0.3 + Spring Data JPA + JUnit` 기반의 초기 프로젝트다.
이 모듈의 목적은 운영에서 N+1을 "추측"하는 수준이 아니라, 개발/테스트 단계에서 조기에 감지하고 필요하면 실패시키는 것이다.

핵심 목표는 다음과 같다.

- 쿼리 실행 정보를 공통 포맷으로 수집한다.
- 같은 요청/테스트/메서드 범위에서 반복되는 `SELECT` 패턴을 감지한다.
- 테스트에서 "허용 쿼리 수"를 선언적으로 검증할 수 있게 한다.
- 필요하면 개발 환경에서 경고 또는 예외로 차단할 수 있게 한다.

비목표도 명확히 둔다.

- SQL 최적화 전체를 자동화하지 않는다.
- 모든 ORM을 완벽 지원하는 범용 APM을 만들지 않는다.
- 운영 기본값을 즉시 `FAIL`로 두지 않는다.

## 2. 관측 지점 비교

### JDBC 레벨

장점:

- JPA/Hibernate 외 다른 JDBC 호출까지 한 곳에서 잡을 수 있다.
- 최종 실행 SQL 기준이라 실제 DB 왕복 수를 세기에 좋다.
- 테스트용 쿼리 수 제한 기능의 기반 계층으로 적합하다.

단점:

- "왜" 반복 쿼리가 발생했는지 ORM 문맥을 알기 어렵다.
- lazy association, collection fetch 같은 Hibernate 맥락은 별도 힌트가 필요하다.
- 바인딩 값까지 깊게 추적하려면 구현 복잡도가 올라간다.

결론:

- 반드시 필요하다.
- 최소 기능의 기준 계층은 JDBC로 둔다.

### Hibernate 레벨

장점:

- 엔티티/컬렉션 fetch, lazy loading 같은 ORM 맥락을 알 수 있다.
- `StatementInspector`, `Statistics`, 이벤트 리스너를 통해 N+1 판단 품질을 높일 수 있다.
- false positive를 줄이기 좋다.

단점:

- Hibernate 의존성이 강해진다.
- Spring Data JPA 외 사용 시 재사용성이 떨어진다.
- 버전 변화에 따라 유지보수 비용이 올라갈 수 있다.

결론:

- JDBC 위에 얹는 선택적 고도화 계층으로 둔다.
- 1차 MVP의 필수 조건은 아니다.

### 테스트 어노테이션 레벨

장점:

- 사용자가 가장 직관적으로 체감하는 기능이다.
- "이 use case는 최대 쿼리 2개" 같은 회귀 방지 장치가 된다.
- CI에서 실패시켜 사전 차단 목표에 가장 잘 맞는다.

단점:

- 아래쪽 계측 계층이 없으면 동작할 수 없다.
- flush/clear, 캐시, setup query 때문에 테스트 설계를 잘못하면 노이즈가 생긴다.

결론:

- 외부에 가장 먼저 노출할 기능은 테스트 어노테이션으로 잡는다.
- 내부적으로는 JDBC 계측 위에 구현한다.

## 3. 권장 아키텍처

권장 방향은 `JDBC 기반 공통 계측 + Hibernate 힌트 + 테스트 실패 어노테이션`의 혼합 구조다.

정리하면:

- 기반 수집은 JDBC에서 한다.
- 정교한 N+1 판별은 Hibernate 정보를 있으면 활용한다.
- 실제 차단은 우선 테스트에서 수행한다.
- 런타임 차단은 `dev/test` 프로필에서만 선택적으로 제공한다.

이 방향을 권장하는 이유:

- JDBC만으로는 범용성은 좋지만 설명력이 약하다.
- Hibernate만으로는 정확도는 좋지만 범위가 좁다.
- 테스트 기능만 먼저 만들면 내부 계측 재사용성이 떨어진다.

## 4. 핵심 개념 설계

### 4.1 QueryEvent

쿼리 1회 실행을 표현하는 공통 모델.

예상 필드:

- `sql`
- `normalizedSql`
- `queryType` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `OTHER`)
- `startedAt`, `duration`
- `threadId`
- `scopeId`
- `origin` (`JDBC`, `HIBERNATE`)
- `rowCount` 또는 영향 건수

### 4.2 QueryFingerprint

반복 쿼리를 판별하기 위한 정규화 키.

규칙 예시:

- 공백 표준화
- 리터럴 값 제거 또는 `?`로 치환
- alias 차이는 최대한 무시
- `IN (?, ?, ?)` 같이 개수만 다른 경우는 동일 패턴으로 볼지 옵션화

목표는 "같은 형태의 select가 같은 범위에서 반복됐는지"를 빠르게 보는 것이다.

### 4.3 GuardScope

쿼리를 집계하는 범위.

1차 지원 범위:

- 테스트 메서드
- 명시적인 서비스 메서드

2차 지원 범위:

- HTTP 요청 1회
- 트랜잭션 1회

Scope가 있어야 "이 테스트 안에서 select가 17번 발생" 같은 판단이 가능하다.

### 4.4 GuardPolicy

허용 규칙을 표현하는 정책 객체.

예시 규칙:

- 총 쿼리 수 최대치
- `SELECT` 최대치
- 동일 fingerprint 반복 최대치
- 반복 `SELECT` 감지 시 즉시 실패 여부
- 특정 SQL 패턴 제외 목록

### 4.5 GuardDecision

정책 평가 결과.

예상 상태:

- `PASS`
- `WARN`
- `FAIL`

실패 시 메시지는 단순히 "쿼리 수 초과"가 아니라 아래 정보가 있어야 한다.

- 총 실행 수
- 타입별 실행 수
- 반복된 fingerprint 상위 N개
- 의심되는 N+1 패턴 설명

## 5. 구현 구조 제안

현재는 단일 Gradle 모듈이므로, 처음부터 멀티 모듈로 쪼개기보다 패키지 경계로 시작하는 것이 현실적이다.
API가 안정되면 이후에 `core`, `spring-boot-starter`, `test-support`로 분리한다.

초기 패키지 예시:

- `com.dochiri.nplusguard.core`
- `com.dochiri.nplusguard.jdbc`
- `com.dochiri.nplusguard.hibernate`
- `com.dochiri.nplusguard.spring`
- `com.dochiri.nplusguard.test`

핵심 구성요소:

- `QueryCollector`
- `QueryNormalizer`
- `QueryFingerprintStrategy`
- `GuardScopeManager`
- `GuardPolicyEvaluator`
- `NPlusOneHeuristic`
- `GuardReportFormatter`

## 6. 계측 방식 상세

### 6.1 JDBC 계층

MVP는 `DataSource` 래퍼 기반으로 간다.

필요 기능:

- `prepareStatement(...)` 시 SQL 확보
- `execute`, `executeQuery`, `executeUpdate`, `executeBatch` 시점 기록
- 현재 활성 `GuardScope`로 이벤트 전달

주의:

- 단순 count만 할 때는 SQL 문자열만으로 충분하다.
- bind value 추적은 1차 범위에서 제외한다.
- 배치 쿼리는 1회로 셀지, batch item 수까지 셀지 정책으로 분리한다.

### 6.2 Hibernate 계층

Hibernate 연동은 다음 순서로 붙인다.

- `StatementInspector`로 SQL 가로채기
- `Statistics`로 entity/collection fetch 카운트 확인
- 필요하면 event listener로 lazy initialization 맥락 확보

여기서 얻은 정보는 JDBC 이벤트에 "설명용 메타데이터"로 합친다.

예시:

- `suspectedAssociation = Order.items`
- `fetchTrigger = LAZY_COLLECTION`
- `entityName = Member`

### 6.3 Scope 시작/종료

Scope는 명시적으로 열고 닫아야 한다.

후보:

- JUnit 5 extension
- Spring AOP annotation
- Servlet filter 또는 MVC interceptor

우선순위:

1. JUnit 5 extension
2. AOP annotation
3. HTTP request interceptor

## 7. N+1 판단 휴리스틱

N+1은 완벽 판별보다 "신뢰 가능한 의심 탐지 + 명시적 예산 검증"의 조합으로 접근한다.

1차 휴리스틱:

- 같은 `SELECT fingerprint`가 같은 scope에서 임계치 이상 반복되면 의심
- 반복 횟수 기본 임계치 예: `3`
- 첫 root query 뒤에 동일한 단건 조회가 연속 발생하면 가중치 상승

2차 휴리스틱:

- Hibernate가 `collection fetch` 또는 `entity fetch` 반복을 함께 보고하면 N+1 가능성을 높임
- `select ... where id = ?` 또는 `where fk = ?` 형태 반복은 대표 패턴으로 표시

제외 규칙:

- pagination count query
- schema/query validation 용 내부 쿼리
- 명시적으로 허용한 lookup query
- 의도된 batch loading

중요:

- 휴리스틱은 경고 품질을 높이는 수단이다.
- 최종 차단은 가능한 한 테스트의 명시적 budget 어노테이션에 맡긴다.

## 8. 외부 노출 API 초안

### 8.1 테스트용 어노테이션

가장 먼저 제공할 후보:

```java
@ExpectMaxQueries(total = 3, select = 2)
@ExpectMaxRepeatedSelect(count = 1)
```

또는 한 개로 합칠 수 있다.

```java
@QueryBudget(
    total = 3,
    select = 2,
    repeatedSelect = 1,
    mode = GuardMode.FAIL
)
```

권장:

- 어노테이션은 하나로 시작한다.
- 내부 속성으로 총량 제한과 반복 제한을 함께 둔다.

### 8.2 런타임용 어노테이션

후순위 후보:

```java
@GuardQueryBudget(select = 2, repeatedSelect = 1, mode = GuardMode.WARN)
```

용도:

- 서비스 메서드 단위로 개발 중 탐지
- 통합 테스트 외 수동 실행에서도 경고 확인

기본 정책:

- `prod`: 비활성 또는 `WARN`
- `dev`: `WARN`
- `test`: `FAIL`

## 9. 테스트 전략

반드시 고려할 포인트:

- persistence context 영향을 줄이기 위해 검증 전 `flush/clear`를 제어해야 한다.
- 테스트 데이터 준비 쿼리와 검증 대상 쿼리를 분리해야 한다.
- 1차 캐시, 2차 캐시, batch fetch 설정 여부에 따라 기대 쿼리 수가 달라진다.

테스트 분류:

- `unit`: 정규화, fingerprint, 정책 평가
- `integration`: 실제 JPA lazy loading 시 쿼리 수 검증
- `spring boot`: auto-configuration과 annotation 동작 검증

대표 시나리오:

1. `findAll()` 후 lazy association 순회 시 반복 select 감지
2. `join fetch` 적용 시 동일 테스트가 통과
3. DTO projection은 불필요 경고 없이 통과
4. pagination count query는 허용 규칙에 따라 제외

## 10. 단계별 구현 로드맵

### Phase 1. MVP

목표:

- 테스트에서 쿼리 수 제한으로 회귀 방지 가능하게 만들기

구현:

- JDBC `DataSource` 래퍼
- `QueryEvent`/`GuardScope`/`GuardPolicyEvaluator`
- JUnit 5 extension
- `@QueryBudget` 어노테이션
- 실패 리포트 출력

완료 기준:

- N+1이 발생하는 샘플 테스트가 실패한다.
- `join fetch` 또는 fetch 전략 수정 후 테스트가 통과한다.

### Phase 2. 반복 SELECT 기반 N+1 의심 탐지

목표:

- 단순 total count를 넘어 반복 패턴을 보여준다.

구현:

- SQL 정규화
- fingerprint 집계
- 상위 반복 쿼리 리포트
- `repeatedSelect` 제한 정책

완료 기준:

- 실패 메시지에 "어떤 쿼리가 몇 번 반복됐는지"가 나온다.

### Phase 3. Hibernate 힌트 통합

목표:

- N+1 판단 품질과 설명력을 높인다.

구현:

- `StatementInspector`
- `Statistics` 연동
- fetch trigger 메타데이터

완료 기준:

- 리포트에 연관 엔티티/컬렉션 힌트가 포함된다.
- false positive가 일부 줄어든다.

### Phase 4. 런타임 가드

목표:

- 테스트 외 개발 흐름에서도 guard를 재사용한다.

구현:

- 서비스 메서드용 AOP annotation
- HTTP request scope
- profile별 `WARN/FAIL/OFF` 설정

완료 기준:

- `dev`에서 특정 API 호출 시 경고 로그 또는 예외 발생

## 11. 설정 프로퍼티 초안

```yaml
nplus-guard:
  enabled: true
  mode: warn
  jdbc:
    enabled: true
  hibernate:
    enabled: false
  defaults:
    total: 0
    select: 0
    repeated-select: 0
  excludes:
    sql-patterns:
      - "select count(*)"
```

메모:

- `0`은 "제한 없음"으로 해석하는 편이 단순하다.
- 테스트 어노테이션 값이 전역 설정보다 우선한다.

## 12. 리스크와 의사결정 메모

### 운영 차단은 보수적으로

운영에서 예외를 던지면 정상 트래픽을 깨뜨릴 수 있다.
따라서 기본값은 테스트 실패 중심으로 두고, 런타임 차단은 opt-in으로 둔다.

### false positive를 줄이는 설계가 중요

N+1 탐지는 "반복 select"와 비슷하지만 완전히 같지는 않다.
단순 반복 카운트만으로 바로 N+1 단정하지 말고, 경고 메시지와 명시적 budget 검증을 병행해야 한다.

### 단일 모듈로 시작

지금 단계에서 멀티 모듈 분리는 비용이 더 크다.
우선 패키지 경계로 설계하고, 외부 공개 API가 안정되면 분리한다.

## 13. 최종 권장안

이 저장소에서는 아래 순서가 가장 현실적이다.

1. `JDBC 기반 QueryEvent 수집`을 먼저 만든다.
2. 그 위에 `JUnit 5 + @QueryBudget`으로 테스트 실패 기능을 붙인다.
3. 다음 단계에서 `fingerprint 기반 반복 SELECT 탐지`를 추가한다.
4. 마지막으로 `Hibernate 힌트`를 연결해 N+1 설명력을 높인다.

즉, 처음부터 Hibernate 전용 탐지기로 시작하지 말고, "범용 계측 -> 테스트 차단 -> 정교화" 순서로 간다.

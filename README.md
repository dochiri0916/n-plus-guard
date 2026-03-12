# N+1 Guard

`N+1 Guard`는 Spring Data JPA 환경에서 쿼리 수를 집계하고, 반복되는 `SELECT` 패턴을 감지해 N+1 문제를 테스트 단계에서 조기에 발견하기 위한 프로젝트입니다.

목표는 운영용 APM이 아니라, 테스트와 CI에서 쿼리 회귀를 빠르게 확인할 수 있는 가벼운 쿼리 카운터를 만드는 것입니다.

## Goal

현재 MVP는 아래 기능만 다룹니다.

- 테스트 또는 메서드 단위로 쿼리를 집계할 수 있는 `scope`
- 전체 쿼리 수와 `SELECT` 쿼리 수 카운트
- SQL 정규화와 fingerprint 기반의 동일 `SELECT` 패턴 반복 감지
- 임계치 초과 시 테스트 실패
- JUnit 5 기반 테스트에서 "최대 쿼리 수" 검증으로 회귀 차단

즉, "N+1을 자동으로 해결"하는 도구가 아니라, "N+1성 패턴을 빠르게 드러내고 막는" 도구를 목표로 합니다.

## How It Works

1. 실행된 SQL을 수집합니다.
2. SQL을 정규화해 literal 값, 공백, alias 차이를 줄입니다.
3. 같은 형태의 `SELECT`가 반복되면 동일 fingerprint로 묶습니다.
4. 하나의 테스트/메서드 범위에서 쿼리 수와 반복 패턴을 요약합니다.
5. 정책(`maxTotalQueries`, `maxSelectQueries`, `maxRepeatedSelectExecutions`)과 비교해 제한 초과 여부를 검사합니다.

## Done When

- 특정 서비스/테스트 실행 중 쿼리 수를 집계할 수 있다.
- 같은 형태의 `SELECT`가 반복 실행되면 감지할 수 있다.
- 허용한 쿼리 수를 넘으면 테스트를 실패시킬 수 있다.
- `fetch join` 적용 전후의 쿼리 수 차이를 테스트로 확인할 수 있다.

## TODO

1. Hibernate `StatementInspector`로 실제 SQL 수집 연결
2. JUnit 5 extension 또는 테스트 지원 API 추가
3. 샘플 시나리오에서 `N+1 발생 -> 테스트 실패 -> fetch join 적용 -> 테스트 통과` 흐름 검증

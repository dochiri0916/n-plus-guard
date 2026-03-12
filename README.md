# N+1 Guard

Spring Data JPA 환경에서 반복 `SELECT` 패턴을 테스트 단계에서 빠르게 탐지하고 회귀를 차단하기 위한 쿼리 카운터 프로젝트다.
여기에는 실행 가능한 코드와 핵심 요약만 두고, 설계 배경과 선택 이유는 블로그에 정리했다.

## 실험 범위

- Hibernate `StatementInspector`로 SQL 수집
- SQL 정규화(`normalizedSql`)와 패턴 키(`fingerprint`) 생성
- scope 단위 쿼리 집계와 반복 `SELECT` 감지
- `GuardPolicy` 허용 기준 초과 시 테스트 실패 처리
- JUnit Extension(`@EnableQueryGuard`, `@QueryBudget`) 기반 자동 검증
- 수동 검증 API(`QueryGuardAssertions.assertWithin`) 제공
- `sample-app`에서 N+1 실패 케이스와 fetch join 통과 케이스 검증

## 모듈 구성

- `core`: 정규화/패턴화/집계/검증 핵심 로직
- `spring-boot-starter`: SQL 수집 hook 자동 설정
- `test-support`: 테스트 어노테이션/Extension/검증 유틸
- `sample-app`: 재현 가능한 통합 테스트 예제

## 실행

```bash
./gradlew :core:test :sample-app:test
```

## 자세한 내용

- [N+1 문제를 선제적으로 막기위해 쿼리 카운터를 만들다](https://velog.io/@dochiri0916/N1-%EB%AC%B8%EC%A0%9C%EB%A5%BC-%EC%84%A0%EC%A0%9C%EC%A0%81%EC%9C%BC%EB%A1%9C-%EB%A7%89%EA%B8%B0%EC%9C%84%ED%95%B4-%EC%BF%BC%EB%A6%AC-%EC%B9%B4%EC%9A%B4%ED%84%B0%EB%A5%BC-%EB%A7%8C%EB%93%A4%EB%8B%A4)

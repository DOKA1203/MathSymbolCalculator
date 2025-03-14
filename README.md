# 코틀린 수학 표현식 라이브러리

계층적 구조의 수학 표현식을 처리하는 라이브러리로, 기호 연산과 변수 치환 평가 기능을 제공합니다.

## 주요 기능

- **기호 표현 지원**:
  - 정수/소수 표현
  - 변수 지원
  - 수학 상수 (π, e, 허수 단위 i)
  - 약분 가능한 분수
  - 임의 밑 로그
  - 지수/근호 연산
  - 삼각함수 (sin, cos, tan)
  - 사칙연산 (+, *)

- **고급 기능**:
  - 표현식 단순화
  - 수치 계산 기능
  - 변수 값 치환
  - 복잡한 표현식 포맷팅
  - 무제한 중첩 표현 지원

- **안전 기능**:
  - 0으로 나누기 방지
  - 누락 변수 감지
  - 오류 발생 시 null 반환

## 사용 방법

### 기본 표현식 생성
```kotlin
val exp = Expression(
    MathSymbol.Add(
        MathSymbol.Number(2),
        MathSymbol.Multiply(
            MathSymbol.Variable("x"),
            MathSymbol.Fraction(MathSymbol.Number(3), MathSymbol.Number(4))
        )
    )
)
```

### 표현식 단순화
```kotlin
val fra = Expression(
    MathSymbol.Fraction(MathSymbol.Number(12), MathSymbol.Number(18))
)
println(분수.simplify()) // 결과: 2/3
```

### 수치 계산
```kotlin
val 2cha_bang = Expression(
    MathSymbol.Exponent(MathSymbol.Variable("x"), MathSymbol.Number(2))
)
2cha_bang.evaluate(mapOf("x" to 4.0)) // 결과: 16.0
```
## 예제 코드
### 기본 사칙연산
```kotlin
Expression(
    MathSymbol.Add(
        MathSymbol.Number(5),
        MathSymbol.Multiply(MathSymbol.Number(3), MathSymbol.Number(4))
    )
).evaluate() // 결과: 17.0
```
## 삼각함수 사용
```kotlin
Expression(
    MathSymbol.Sin(MathSymbol.Fraction(MathSymbol.PI, MathSymbol.Number(2)))
).evaluate() // 결과: 1.0
```
## 로그 계산
```kotlin
Expression(
    MathSymbol.Log(MathSymbol.Number(8), MathSymbol.Number(2))
).evaluate() // 결과: 3.0
```
## 복합 표현식
```kotlin
Expression(
    MathSymbol.Add(
        MathSymbol.Exponent(MathSymbol.E, MathSymbol.PI),
        MathSymbol.Multiply(
            MathSymbol.Log(MathSymbol.Number(100)),
            MathSymbol.Root(MathSymbol.Number(64))
        )
    )
).evaluate() // 결과: ≈522.735
```




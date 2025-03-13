package kr.doka.lab

import java.math.RoundingMode
import kotlin.math.*

sealed class MathSymbol {
    // 기본 요소
    data class Number(val value: Int) : MathSymbol()
    data class Decimal(val value: Double) : MathSymbol()
    data class Variable(val name: String) : MathSymbol()

    // 수학 상수
    data object E : MathSymbol()
    data object PI : MathSymbol()
    data object ImaginaryUnit : MathSymbol()

    // 연산 및 함수
    data class Fraction(val numerator: MathSymbol, val denominator: MathSymbol) : MathSymbol()
    data class Log(
        val argument: MathSymbol,
        val base: MathSymbol = E  // 기본값으로 자연로그(e) 설정
    ) : MathSymbol()
    data class Add(val left: MathSymbol, val right: MathSymbol) : MathSymbol()
    data class Multiply(val left: MathSymbol, val right: MathSymbol) : MathSymbol()
    data class Exponent(val base: MathSymbol, val power: MathSymbol) : MathSymbol()
    data class Root(val radicand: MathSymbol, val degree: MathSymbol = Number(2)) : MathSymbol()
    data class Sin(val argument: MathSymbol) : MathSymbol()
    data class Cos(val argument: MathSymbol) : MathSymbol()
    data class Tan(val argument: MathSymbol) : MathSymbol()
}

class Expression(private val symbol: MathSymbol) {
    private fun wrapIfComplex(symbol: MathSymbol): String {
        return when(symbol) {
            is MathSymbol.Number,
            is MathSymbol.Decimal,
            is MathSymbol.Variable,
            is MathSymbol.E,
            is MathSymbol.PI,
            is MathSymbol.ImaginaryUnit -> Expression(symbol).toString()
            else -> "(${Expression(symbol)})"
        }
    }

    fun simplify(): Expression {
        return Expression(when (symbol) {
            is MathSymbol.Fraction -> simplifyFraction(symbol)
            else -> symbol
        })
    }

    private fun simplifyFraction(fraction: MathSymbol.Fraction): MathSymbol.Fraction {
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        return when {
            fraction.numerator is MathSymbol.Number && fraction.denominator is MathSymbol.Number -> {
                val num = fraction.numerator.value
                val den = fraction.denominator.value
                val commonDivisor = gcd(Math.abs(num), Math.abs(den))
                val sign = if (den < 0) -1 else 1
                MathSymbol.Fraction(
                    MathSymbol.Number(sign * num / commonDivisor),
                    MathSymbol.Number(sign * den / commonDivisor)
                )
            }
            else -> fraction
        }
    }

    override fun toString(): String = when (symbol) {
        is MathSymbol.Number -> symbol.value.toString()
        is MathSymbol.Decimal -> {
            if (symbol.value % 1.0 == 0.0) {
                symbol.value.toInt().toString()
            } else {
                "%.4f".format(symbol.value).trimEnd('0').trimEnd('.')
            }
        }
        is MathSymbol.Variable -> symbol.name
        MathSymbol.E -> "e"
        MathSymbol.PI -> "π"
        MathSymbol.ImaginaryUnit -> "i"

        is MathSymbol.Fraction -> {
            "${wrapIfComplex(symbol.numerator)}/${wrapIfComplex(symbol.denominator)}"
        }
        is MathSymbol.Log -> {
            val baseRepr = Expression(symbol.base)
            val argRepr = Expression(symbol.argument)

            when (symbol.base) {
                MathSymbol.E -> "ln($argRepr)"

                // 밑이 단순한 숫자/변수인 경우
                is MathSymbol.Number, is MathSymbol.Variable -> "log_${baseRepr}($argRepr)"

                // 복잡한 밑 표현의 경우 괄호 처리
                else -> "log_{${baseRepr}}($argRepr)"
            }
        }
        is MathSymbol.Add -> "${Expression(symbol.left)} + ${Expression(symbol.right)}"
        is MathSymbol.Multiply -> "${wrapIfComplex(symbol.left)} * ${wrapIfComplex(symbol.right)}"
        is MathSymbol.Exponent -> "${wrapIfComplex(symbol.base)}^${wrapIfComplex(symbol.power)}"
        is MathSymbol.Root -> when {
            (symbol.degree as? MathSymbol.Number)?.value == 2 -> "√${wrapIfComplex(symbol.radicand)}"
            else -> "${Expression(symbol.degree)}√${wrapIfComplex(symbol.radicand)}"
        }
        is MathSymbol.Sin -> "sin(${Expression(symbol.argument)})"
        is MathSymbol.Cos -> "cos(${Expression(symbol.argument)})"
        is MathSymbol.Tan -> "tan(${Expression(symbol.argument)})"
    }


    fun evaluate(context: Map<String, Double> = emptyMap()): Double? {
        return try {
            evaluateSymbol(symbol, context).toBigDecimal().setScale(10, RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            null
        }
    }

    private fun evaluateSymbol(symbol: MathSymbol, context: Map<String, Double>): Double {
        return when (symbol) {
            is MathSymbol.Number -> symbol.value.toDouble()
            is MathSymbol.Decimal -> symbol.value
            is MathSymbol.Variable -> context[symbol.name]
                ?: throw IllegalArgumentException("변수 ${symbol.name}에 대한 값이 제공되지 않았습니다")

            MathSymbol.E -> E
            MathSymbol.PI -> PI
            MathSymbol.ImaginaryUnit -> throw UnsupportedOperationException("복소수 계산은 지원되지 않습니다")

            is MathSymbol.Fraction -> {
                val num = evaluateSymbol(symbol.numerator, context)
                val den = evaluateSymbol(symbol.denominator, context)
                if (den == 0.0) throw ArithmeticException("0으로 나눌 수 없습니다")
                num / den
            }

            is MathSymbol.Add ->
                evaluateSymbol(symbol.left, context) + evaluateSymbol(symbol.right, context)

            is MathSymbol.Multiply ->
                evaluateSymbol(symbol.left, context) * evaluateSymbol(symbol.right, context)

            is MathSymbol.Exponent -> {
                val base = evaluateSymbol(symbol.base, context)
                val power = evaluateSymbol(symbol.power, context)
                base.pow(power)
            }

            is MathSymbol.Root -> {
                val radicand = evaluateSymbol(symbol.radicand, context)
                val degree = evaluateSymbol(symbol.degree, context)
                radicand.pow(1.0 / degree)
            }

            is MathSymbol.Log -> {
                val arg = evaluateSymbol(symbol.argument, context)
                val base = evaluateSymbol(symbol.base, context)
                ln(arg) / ln(base)
            }

            is MathSymbol.Sin -> sin(evaluateSymbol(symbol.argument, context))
            is MathSymbol.Cos -> cos(evaluateSymbol(symbol.argument, context))
            is MathSymbol.Tan -> tan(evaluateSymbol(symbol.argument, context))
        }
    }
}

fun main() {
    // 1. 기본 사칙연산
    val basicArithmetic = Expression(
        MathSymbol.Add(
            MathSymbol.Number(2),
            MathSymbol.Multiply(
                MathSymbol.Number(3),
                MathSymbol.Number(4)
            )
        )
    )
    println("1. 기본 연산: $basicArithmetic = ${basicArithmetic.evaluate()}")

    // 2. 분수 약분 예제
    val fraction = Expression(
        MathSymbol.Fraction(
            MathSymbol.Number(12),
            MathSymbol.Number(18)
        )
    )
    println("\n2. 분수 약분:")
    println("원본: $fraction")
    println("약분: ${fraction.simplify()}")
    println("계산: ${fraction.simplify().evaluate()}")

    // 3. 지수 및 근호
    val exponentRoot = Expression(
        MathSymbol.Add(
            MathSymbol.Exponent(
                MathSymbol.Number(2),
                MathSymbol.Number(3)
            ),
            MathSymbol.Root(MathSymbol.Number(9))
        )
    )
    println("\n3. 지수 및 근호: $exponentRoot = ${exponentRoot.evaluate()}")

    // 4. 다양한 로그
    val logExamples = listOf(
        Expression(MathSymbol.Log(MathSymbol.Exponent(MathSymbol.E, MathSymbol.Number(5)))),
        Expression(MathSymbol.Log(MathSymbol.Number(100), MathSymbol.Number(10))),
        Expression(MathSymbol.Log(
            MathSymbol.Number(8),
            MathSymbol.Fraction(MathSymbol.Number(1), MathSymbol.Number(2)))
        )
    )
    println("\n4. 로그 예제:")
    logExamples.forEach { println("$it = ${it.evaluate()}") }

    // 5. 삼각함수
    val trigExpr = Expression(
        MathSymbol.Add(
            MathSymbol.Sin(
                MathSymbol.Fraction(MathSymbol.PI, MathSymbol.Number(2))
            ),
            MathSymbol.Multiply(
                MathSymbol.Cos(MathSymbol.PI),
                MathSymbol.Tan(
                    MathSymbol.Fraction(MathSymbol.PI, MathSymbol.Number(4))
                )
            )
        )
    )
    println("\n5. 삼각함수: $trigExpr = ${trigExpr.evaluate()}")

    // 6. 변수 치환
    val quadratic = Expression(
        MathSymbol.Add(
            MathSymbol.Exponent(MathSymbol.Variable("x"), MathSymbol.Number(2)),
            MathSymbol.Add(
                MathSymbol.Multiply(MathSymbol.Number(3), MathSymbol.Variable("x")),
                MathSymbol.Number(2)
            )
        )
    )
    val context = mapOf("x" to 3.0)
    println("\n6. 변수 치환:")
    println("수식: $quadratic")
    println("x=3 일 때: ${quadratic.evaluate(context)}")

    // 7. 소수 연산
    val decimalExpr = Expression(
        MathSymbol.Multiply(
            MathSymbol.Decimal(3.5),
            MathSymbol.Decimal(2.0)
        )
    )
    println("\n7. 소수 연산: $decimalExpr = ${decimalExpr.evaluate()}")

    // 8. 복합 표현식
    val complexExpr = Expression(
        MathSymbol.Add(
            MathSymbol.Exponent(
                MathSymbol.E,
                MathSymbol.Fraction(MathSymbol.PI, MathSymbol.Number(2))
            ),
            MathSymbol.Multiply(
                MathSymbol.Log(MathSymbol.Number(8), MathSymbol.Number(2)),
                MathSymbol.Root(MathSymbol.Number(16))
            )
        )
    )
    println("\n8. 복합 표현식: $complexExpr ≈ ${complexExpr.evaluate()}")

    // 9. 오류 케이스
    val errorCases = listOf(
        Expression(MathSymbol.Fraction(MathSymbol.Number(5), MathSymbol.Number(0))),
        Expression(MathSymbol.Add(MathSymbol.Variable("y"), MathSymbol.Number(1)))
    )
    println("\n9. 오류 케이스:")
    errorCases.forEach { println("$it = ${it.evaluate()}") }
}

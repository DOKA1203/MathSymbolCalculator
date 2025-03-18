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
    data class Cot(val argument: MathSymbol) : MathSymbol()
    data class Sec(val argument: MathSymbol) : MathSymbol()
    data class Csc(val argument: MathSymbol) : MathSymbol()
}

class Expression(private val mathSymbol: MathSymbol) {
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
        return Expression(simplifySymbol(mathSymbol))
    }
    private fun simplifySymbol(symbol: MathSymbol): MathSymbol {
        return when (symbol) {
            is MathSymbol.Fraction -> simplifyFraction(symbol)
            is MathSymbol.Root -> simplifyRoot(symbol)
            is MathSymbol.Add -> simplifyAdd(symbol)
            is MathSymbol.Multiply -> simplifyMultiply(symbol)
            else -> symbol
        }
    }
    private fun simplifyMultiply(symbol: MathSymbol.Multiply): MathSymbol {
        val left = simplifySymbol(symbol.left)
        val right = simplifySymbol(symbol.right)

        if (left is MathSymbol.Fraction && right is MathSymbol.Fraction) {
            val leftNum = left.numerator
            val rightNum = right.numerator
            val leftDen = left.denominator
            val rightDen = right.denominator

            if (leftNum is MathSymbol.Number && leftDen is MathSymbol.Number && rightNum is MathSymbol.Number && rightDen is MathSymbol.Number) {
                return simplifySymbol(MathSymbol.Fraction(MathSymbol.Number(leftNum.value * rightNum.value), MathSymbol.Number(leftDen.value * rightDen.value)))
            }
        } else if (left is MathSymbol.Fraction && right is MathSymbol.Number) {
            val leftNum = left.numerator
            if  (leftNum is MathSymbol.Number) {
                return simplifySymbol(MathSymbol.Fraction(MathSymbol.Number(leftNum.value * right.value), left.denominator))
            }
        } else if (right is MathSymbol.Fraction &&  left is MathSymbol.Number) {
            val rightNum = right.numerator
            if  (rightNum is MathSymbol.Number) {
                return simplifySymbol(MathSymbol.Fraction(MathSymbol.Number(rightNum.value * left.value), right.denominator))
            }
        } else if (right is MathSymbol.Number && left is MathSymbol.Number) {

            return MathSymbol.Number(right.value * left.value)
        }
        return MathSymbol.Multiply(left, right)
    }
    private fun simplifyAdd(symbol: MathSymbol.Add): MathSymbol {
        val left = simplifySymbol(symbol.left)
        val right = simplifySymbol(symbol.right)

        // 두 항이 모두 일반 숫자일 경우
        if (left is MathSymbol.Number && right is MathSymbol.Number) {
            return MathSymbol.Number(left.value + right.value)
        }

        // 숫자를 분수로 변환하는 헬퍼 함수 (이미 Fraction 인 경우 그대로 반환)
        fun toFraction(ms: MathSymbol): MathSymbol.Fraction? {
            return when(ms) {
                is MathSymbol.Fraction -> ms
                is MathSymbol.Number -> MathSymbol.Fraction(ms, MathSymbol.Number(1))
                else -> null
            }
        }

        val leftFraction = toFraction(left)
        val rightFraction = toFraction(right)

        if (leftFraction != null && rightFraction != null) {
            // 분수 형태: leftFraction = a/b, rightFraction = c/d
            if (leftFraction.numerator is MathSymbol.Number && leftFraction.denominator is MathSymbol.Number &&
                rightFraction.numerator is MathSymbol.Number && rightFraction.denominator is MathSymbol.Number) {

                val newNumerator = leftFraction.numerator.value * rightFraction.denominator.value + rightFraction.numerator.value * leftFraction.denominator.value
                val newDenominator = leftFraction.denominator.value * rightFraction.denominator.value

                // 새 분수를 간소화해서 반환
                return simplifyFraction(MathSymbol.Fraction(MathSymbol.Number(newNumerator), MathSymbol.Number(newDenominator)))
            }
        }

        // 분수 덧셈이 적용되지 않는 경우 원래의 Add 형태로 반환
        return MathSymbol.Add(left, right)
    }



    private fun simplifyFraction(fraction: MathSymbol.Fraction): MathSymbol {
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val fractionNum = simplifySymbol(fraction.numerator)
        val fractionDen = simplifySymbol(fraction.denominator)

        return when {
            fractionNum is MathSymbol.Number && fractionDen is MathSymbol.Number -> {
                val num = fractionNum.value
                val den = fractionDen.value
                val commonDivisor = gcd(abs(num), abs(den))
                val sign = if (den < 0) -1 else 1
                if (sign * den / commonDivisor == 1) {
                    return MathSymbol.Number(sign * num / commonDivisor)
                }
                MathSymbol.Fraction(
                    MathSymbol.Number(sign * num / commonDivisor),
                    MathSymbol.Number(sign * den / commonDivisor)
                )
            }
            else -> fraction
        }
    }
    private fun simplifyRoot(root: MathSymbol.Root): MathSymbol {
        if (root.radicand is MathSymbol.Number) {
            var radicandValue = root.radicand.value
            val degreeValue = (root.degree as? MathSymbol.Number)?.value ?: return root // 루트 차수가 숫자가 아닐 경우 그대로 반환

            var factor = 1

            for (i in 2..radicandValue.toDouble().pow(1.0 / degreeValue).toInt()) {
                while (radicandValue % i.toDouble().pow(degreeValue.toDouble()).toInt() == 0) {
                    factor *= i
                    radicandValue /= i.toDouble().pow(degreeValue.toDouble()).toInt()
                }
            }

            return if (factor > 1) {

                if (radicandValue == 1) return MathSymbol.Number(factor)
                MathSymbol.Multiply(MathSymbol.Number(factor), MathSymbol.Root(MathSymbol.Number(radicandValue), root.degree))
            } else {
                root
            }
        }
        return root
    }

    override fun toString(): String = when (mathSymbol) {
        is MathSymbol.Number -> mathSymbol.value.toString()
        is MathSymbol.Decimal -> {
            if (mathSymbol.value % 1.0 == 0.0) {
                mathSymbol.value.toInt().toString()
            } else {
                "%.4f".format(mathSymbol.value).trimEnd('0').trimEnd('.')
            }
        }
        is MathSymbol.Variable -> mathSymbol.name
        MathSymbol.E -> "e"
        MathSymbol.PI -> "π"
        MathSymbol.ImaginaryUnit -> "i"

        is MathSymbol.Fraction -> {
            "${wrapIfComplex(mathSymbol.numerator)}/${wrapIfComplex(mathSymbol.denominator)}"
        }
        is MathSymbol.Log -> {
            val baseRepr = Expression(mathSymbol.base)
            val argRepr = Expression(mathSymbol.argument)

            when (mathSymbol.base) {
                MathSymbol.E -> "ln($argRepr)"

                // 밑이 단순한 숫자/변수인 경우
                is MathSymbol.Number, is MathSymbol.Variable -> "log_${baseRepr}($argRepr)"

                // 복잡한 밑 표현의 경우 괄호 처리
                else -> "log_{${baseRepr}}($argRepr)"
            }
        }
        is MathSymbol.Add -> "${Expression(mathSymbol.left)} + ${Expression(mathSymbol.right)}"
        is MathSymbol.Multiply -> "${wrapIfComplex(mathSymbol.left)} * ${wrapIfComplex(mathSymbol.right)}"
        is MathSymbol.Exponent -> "${wrapIfComplex(mathSymbol.base)}^${wrapIfComplex(mathSymbol.power)}"
        is MathSymbol.Root -> when {
            (mathSymbol.degree as? MathSymbol.Number)?.value == 2 -> "√${wrapIfComplex(mathSymbol.radicand)}"
            else -> "${Expression(mathSymbol.degree)}√${wrapIfComplex(mathSymbol.radicand)}"
        }
        is MathSymbol.Sin -> "sin(${Expression(mathSymbol.argument)})"
        is MathSymbol.Cos -> "cos(${Expression(mathSymbol.argument)})"
        is MathSymbol.Tan -> "tan(${Expression(mathSymbol.argument)})"
        is MathSymbol.Cot -> "cot(${Expression(mathSymbol.argument)})"
        is MathSymbol.Sec -> "sec(${Expression(mathSymbol.argument)})"
        is MathSymbol.Csc -> "csc(${Expression(mathSymbol.argument)})"
    }


    fun evaluate(context: Map<String, Double> = emptyMap()): Double? {
        return try {
            evaluateSymbol(mathSymbol, context).toBigDecimal().setScale(10, RoundingMode.HALF_UP).toDouble()
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
            is MathSymbol.Cot -> {
                val tanValue = tan(evaluateSymbol(symbol.argument, context))
                if (tanValue == 0.0) throw ArithmeticException("cot 정의역 오류")
                1.0 / tanValue
            }
            is MathSymbol.Sec -> {
                val cosValue = cos(evaluateSymbol(symbol.argument, context))
                if (cosValue == 0.0) throw ArithmeticException("sec 정의역 오류")
                1.0 / cosValue
            }
            is MathSymbol.Csc -> {
                val sinValue = sin(evaluateSymbol(symbol.argument, context))
                if (sinValue == 0.0) throw ArithmeticException("csc 정의역 오류")
                1.0 / sinValue
            }
        }
    }
}

fun main() {
    val cases = listOf(
        Expression(
            MathSymbol.Add(
                MathSymbol.Fraction(
                    MathSymbol.Number(10),
                    MathSymbol.Number(5)
                ),
                MathSymbol.Fraction(
                    MathSymbol.Number(5),
                    MathSymbol.Number(4)
                ),
            )
        ),
    )

    cases.forEach { println("$it = ${it.simplify()} = ${it.evaluate()}") } // (5 * 5)/(√16) = 25/4 = 6.25
}

package com.reinhardt.hbm.autocal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.Stack;

/** Literal 1.7.10 Calculator semantics used by MS-ES1's eval commands. */
public final class AutocalMath {
    private AutocalMath() {
    }

    public static double evaluateExpression(String input) {
        if (input.contains("^")) input = preEvaluatePower(input);

        char[] tokens = input.toCharArray();
        Stack<Double> values = new Stack<>();
        Stack<String> operators = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == ' ') continue;
            if ((tokens[i] >= '0' && tokens[i] <= '9') || tokens[i] == '.'
                    || (tokens[i] == '-' && (i == 0 || "+-*/^(".contains(String.valueOf(tokens[i - 1]))))) {
                StringBuilder buffer = new StringBuilder();
                if (tokens[i] == '-') {
                    buffer.append('-');
                    i++;
                }
                while (i < tokens.length && ((tokens[i] >= '0' && tokens[i] <= '9') || tokens[i] == '.')) {
                    buffer.append(tokens[i++]);
                }
                values.push(Double.parseDouble(buffer.toString()));
                i--;
            } else if (tokens[i] == '(') {
                operators.push("(");
            } else if (tokens[i] == ')') {
                while (!operators.isEmpty() && operators.peek().charAt(0) != '(') {
                    values.push(evaluateOperator(operators.pop().charAt(0), values.pop(), values.pop()));
                }
                operators.pop();
                if (!operators.isEmpty() && operators.peek().length() > 1) {
                    values.push(evaluateFunction(operators.pop(), values.pop()));
                }
            } else if ("+-*/^".indexOf(tokens[i]) >= 0) {
                while (!operators.isEmpty() && hasPrecedence(String.valueOf(tokens[i]), operators.peek())) {
                    values.push(evaluateOperator(operators.pop().charAt(0), values.pop(), values.pop()));
                }
                operators.push(String.valueOf(tokens[i]));
            } else if (tokens[i] == '!') {
                values.push((double) factorial((int) Math.round(values.pop())));
            } else if ((tokens[i] >= 'A' && tokens[i] <= 'Z') || (tokens[i] >= 'a' && tokens[i] <= 'z')) {
                StringBuilder characters = new StringBuilder();
                while (i < tokens.length && ((tokens[i] >= 'A' && tokens[i] <= 'Z') || (tokens[i] >= 'a' && tokens[i] <= 'z'))) {
                    characters.append(tokens[i++]);
                }
                String function = characters.toString();
                if (function.equalsIgnoreCase("pi")) values.push(Math.PI);
                else if (function.equalsIgnoreCase("e")) values.push(Math.E);
                else operators.push(function.toLowerCase(Locale.ROOT));
                i--;
            }
        }
        while (!operators.empty()) values.push(evaluateOperator(operators.pop().charAt(0), values.pop(), values.pop()));
        return values.pop();
    }

    private static double evaluateOperator(char operator, double x, double y) {
        return switch (operator) {
            case '+' -> y + x;
            case '-' -> y - x;
            case '*' -> y * x;
            case '/' -> y / x;
            case '^' -> Math.pow(y, x);
            default -> 0.0D;
        };
    }

    private static double evaluateFunction(String function, double x) {
        return switch (function) {
            case "sqrt" -> Math.sqrt(x);
            case "sin" -> Math.sin(x);
            case "cos" -> Math.cos(x);
            case "tan" -> Math.tan(x);
            case "asin" -> Math.asin(x);
            case "acos" -> Math.acos(x);
            case "atan" -> Math.atan(x);
            case "log" -> Math.log10(x);
            case "ln" -> Math.log(x);
            case "ceil" -> Math.ceil(x);
            case "floor" -> Math.floor(x);
            case "round" -> Math.round(x);
            default -> 0.0D;
        };
    }

    private static boolean hasPrecedence(String first, String second) {
        if (second.length() > 1) return false;
        char firstChar = first.charAt(0);
        char secondChar = second.charAt(0);
        if (secondChar == '(' || secondChar == ')') return false;
        return (firstChar != '*' && firstChar != '/' && firstChar != '^') || (secondChar != '+' && secondChar != '-');
    }

    private static String preEvaluatePower(String input) {
        do {
            int power = input.lastIndexOf('^');
            boolean previousParentheses = input.charAt(power - 1) == ')';
            int depth = previousParentheses ? 1 : 0;
            int baseStart = previousParentheses ? power - 2 : power - 1;
            for (; baseStart >= 0; baseStart--) {
                char token = input.charAt(baseStart);
                if (token == ')') {
                    if (previousParentheses) depth++;
                    else break;
                } else if (token == '(') {
                    if (previousParentheses && depth > 0) depth--;
                    else break;
                } else if ("+-*/^".indexOf(token) >= 0 && depth == 0) break;
            }
            baseStart++;
            if (depth > 0) throw new IllegalArgumentException("Incomplete parentheses");

            boolean nextParentheses = input.charAt(power + 1) == '(';
            depth = nextParentheses ? 1 : 0;
            int exponentEnd = nextParentheses ? power + 2 : power + 1;
            for (; exponentEnd < input.length(); exponentEnd++) {
                char token = input.charAt(exponentEnd);
                if (token == '(') {
                    if (nextParentheses) depth++;
                    else break;
                } else if (token == ')') {
                    if (nextParentheses && depth > 0) depth--;
                    else break;
                } else if ("+-*/^".indexOf(token) >= 0 && depth == 0) break;
            }
            if (depth > 0) throw new IllegalArgumentException("Incomplete parentheses");
            double base = evaluateExpression(input.substring(baseStart, power));
            double exponent = evaluateExpression(input.substring(power + 1, exponentEnd));
            String result = new BigDecimal(Math.pow(base, exponent), MathContext.DECIMAL64).toPlainString();
            input = input.substring(0, baseStart) + result + input.substring(exponentEnd);
        } while (input.contains("^"));
        return input;
    }

    private static int factorial(int value) {
        if (value < 0) throw new IllegalArgumentException("Factorial needs n >= 0");
        int result = 1;
        for (int i = 2; i <= value; i++) result *= i;
        return result;
    }
}

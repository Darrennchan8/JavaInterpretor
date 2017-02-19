import java.util.ArrayList;

/**
 * Created by Darren Chan on 1/16/2017.
 * This organizes all of our custom operators, separating the logic.
 */

class CustomOperators {
	static boolean apply(OperatorStore operators) {
		operators.add(new Operator("'", "'", 14) {
			protected boolean breakOnNewline() {return true;}

			public GenericVar compute(String str) {
				return new GenericVar(Type.string, str);
			}
		});
		operators.add(new Operator("\"", "\"", 14) {
			protected boolean breakOnNewline() {return true;}

			public GenericVar compute(String str) {
				return new GenericVar(Type.string, str);
			}
		});
		operators.add(new Operator("`", "`", 14) {
			public GenericVar compute(String str) {
				return new GenericVar(Type.string, str);
			}
		});
		operators.add(new Operator("(", ")", 14) {
			protected boolean supportsSequence() {return true;}

			public GenericVar compute(String str) {
				return new Interpreter().interpret(str);
			}
		});
		operators.add(new Operator(".", 14, OperatorType.between) {
			public GenericVar compute(GenericVar before, GenericVar after) throws InterpreterException {
				return before.getProperty(new GenericVar(Type.string, after.getVariableName()));
			}
		});
		operators.add(new Operator("{", "}", 14) {
			protected boolean supportsSequence() {return true;}
			protected boolean supportsTuple() {return true;}

			public GenericVar compute(String str) {
				return new Interpreter().interpret(str);
			}
		});
		operators.add(new Operator("[", "]", 14) {
			protected boolean supportsSequence() {return true;}

			public GenericVar compute(String str) throws InterpreterException {
				Sequence evaluatedValue = Sequence.toSequence(new Interpreter().interpret(str));
				return Sequence.toArray(evaluatedValue);
			}
		});
		operators.add(new AssignmentArithmeticOperator("++", 13, OperatorType.after) {
			public GenericVar set(GenericVar varName, GenericVar value) throws InterpreterException {
				Interpreter.setVariable(varName.getVariableName(), value);
				return varName;
			}

			public String applyOperation(String fix) {
				return String.valueOf(Double.parseDouble(fix) + 1);
			}
		});
		operators.add(new AssignmentArithmeticOperator("--", 13, OperatorType.after) {
			public GenericVar set(GenericVar varName, GenericVar value) throws InterpreterException {
				Interpreter.setVariable(varName.getVariableName(), value);
				return varName;
			}

			public String applyOperation(String fix) {
				return String.valueOf(Double.parseDouble(fix) - 1);
			}
		});
		operators.add(new LogicalOperator("!", 12, OperatorType.before) {
			public GenericVar compute(GenericVar value) throws InterpreterException {
				return new GenericVar(Type.bool, toBoolean(value.getType(), value.get()) ? "false" : "true", value.getVariableName());
			}
		});
		operators.add(new UnaryOperator("-", 12) {
			public GenericVar compute(GenericVar after) throws InterpreterException {
				String val = getExplicitType(after);
				if (!val.equals("NaN")) {
					val = String.valueOf(-Double.parseDouble(val));
				}
				return new GenericVar(Type.number, trim(val));
			}
		});
		operators.add(new UnaryOperator("+", 12) {
			public GenericVar compute(GenericVar after) throws InterpreterException {
				String val = getExplicitType(after);
				if (!val.equals("NaN")) {
					val = String.valueOf(Double.parseDouble(val));
				}
				return new GenericVar(Type.number, trim(val));
			}
		});
		operators.add(new AssignmentArithmeticOperator("++", 12, OperatorType.before) {
			public String applyOperation(String fix) {
				return String.valueOf(Double.parseDouble(fix) + 1);
			}
		});
		operators.add(new AssignmentArithmeticOperator("--", 12, OperatorType.before) {
			public String applyOperation(String fix) {
				return String.valueOf(Double.parseDouble(fix) - 1);
			}
		});
		operators.add(new ArithmeticOperator("**", 11) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Math.pow(Double.parseDouble(before), Double.parseDouble(after)));
			}
		});
		operators.add(new ArithmeticOperator("*", 10) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) * Double.parseDouble(after));
			}
		});
		operators.add(new ArithmeticOperator("/", 10) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) / Double.parseDouble(after));
			}
		});
		operators.add(new ArithmeticOperator("%", 10) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) % Double.parseDouble(after));
			}
		});
		operators.add(new ArithmeticOperator("+", 9) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) + Double.parseDouble(after));
			}
		});
		operators.add(new ArithmeticOperator("-", 9) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) - Double.parseDouble(after));
			}
		});
		operators.add(new LogicalOperator("<", 8) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				String num1 = getExplicitType(value1);
				String num2 = getExplicitType(value2);
				if (num1.equals("NaN") || num2.equals("NaN")) {
					return new GenericVar(Type.bool, "false");
				}
				return new GenericVar(Type.bool, Double.parseDouble(num1) < Double.parseDouble(num2) ? "true" : "false");
			}
		});
		operators.add(new LogicalOperator("<=", 8) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				String num1 = getExplicitType(value1);
				String num2 = getExplicitType(value2);
				if (num1.equals("NaN") || num2.equals("NaN")) {
					return new GenericVar(Type.bool, "false");
				}
				return new GenericVar(Type.bool, Double.parseDouble(num1) <= Double.parseDouble(num2) ? "true" : "false");
			}
		});
		operators.add(new LogicalOperator(">", 8) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				String num1 = getExplicitType(value1);
				String num2 = getExplicitType(value2);
				if (num1.equals("NaN") || num2.equals("NaN")) {
					return new GenericVar(Type.bool, "false");
				}
				return new GenericVar(Type.bool, Double.parseDouble(num1) > Double.parseDouble(num2) ? "true" : "false");
			}
		});
		operators.add(new LogicalOperator(">=", 8) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				String num1 = getExplicitType(value1);
				String num2 = getExplicitType(value2);
				if (num1.equals("NaN") || num2.equals("NaN")) {
					return new GenericVar(Type.bool, "false");
				}
				return new GenericVar(Type.bool, Double.parseDouble(num1) >= Double.parseDouble(num2) ? "true" : "false");
			}
		});
		operators.add(new LogicalOperator("==", 7) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				return new GenericVar(Type.bool, equals(value1, value2) ? "true" : "false");
			}
		});
		operators.add(new LogicalOperator("!=", 7) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				return new GenericVar(Type.bool, equals(value1, value2) ? "false" : "true");
			}
		});
		operators.add(new LogicalOperator("===", 7) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				return new GenericVar(Type.bool, equalsStrict(value1, value2) ? "true" : "false");
			}
		});
		operators.add(new LogicalOperator("!==", 7) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				return new GenericVar(Type.bool, equalsStrict(value1, value2) ? "false" : "true");
			}
		});
		operators.add(new LogicalOperator("&&", 6) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				return new GenericVar(Type.bool, toBoolean(value1.getType(), value1.get()) && toBoolean(value2.getType(), value2.get()) ? "true" : "false");
			}
		});
		operators.add(new LogicalOperator("||", 5) {
			public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
				return new GenericVar(Type.bool, toBoolean(value1.getType(), value1.get()) || toBoolean(value2.getType(), value2.get()) ? "true" : "false");
			}
		});
		operators.add(new Operator(":", 4, OperatorType.between) {
			public GenericVar compute(GenericVar result1, GenericVar result2) throws InterpreterException {
				return new GenericVar(result1, result2);
			}
		});
		operators.add(new LogicalOperator("?", 3) {
			protected boolean supportsTuple() {return true;}

			public GenericVar compute(GenericVar condition, GenericVar results) throws InterpreterException {
				if (results.getType() != Type.tuple) {
					throw new Error("Incomplete Ternary Operator");
				}
				return toBoolean(condition.getType(), condition.get()) ? results.getFirst() : results.getSecond();
			}
		});
		operators.add(new Operator("=", 2, OperatorType.between) {
			public GenericVar compute(GenericVar varName, GenericVar value) throws InterpreterException {
				return Interpreter.setVariable(varName.getVariableName(), value);
			}
		});
		operators.add(new AssignmentArithmeticOperator("**=", 2) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Math.pow(Double.parseDouble(before), Double.parseDouble(after)));
			}
		});
		operators.add(new AssignmentArithmeticOperator("*=", 2) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) * Double.parseDouble(after));
			}
		});
		operators.add(new AssignmentArithmeticOperator("/=", 2) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) / Double.parseDouble(after));
			}
		});
		operators.add(new AssignmentArithmeticOperator("%=", 2) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) % Double.parseDouble(after));
			}
		});
		operators.add(new AssignmentArithmeticOperator("+=", 2) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) + Double.parseDouble(after));
			}
		});
		operators.add(new AssignmentArithmeticOperator("-=", 2) {
			public String applyOperation(String before, String after) {
				return String.valueOf(Double.parseDouble(before) - Double.parseDouble(after));
			}
		});
		operators.add(new Operator(",", 1, OperatorType.between) {
			protected boolean supportsSequence() {return true;}

			public GenericVar compute(GenericVar previous, GenericVar next) throws InterpreterException {
				Sequence currSequence = Sequence.toSequence(previous);
				currSequence.add(next);
				return currSequence;
			}
		});
		return true;
	}
}


abstract class ArithmeticOperator extends Operator {
	String symbol;

	String trim(String str) {
		int start = 0;
		while (start < str.length() - 1 && str.charAt(start) == '0' && str.charAt(start + 1) != '.') {
			start++;
		}
		int end = str.length() - 1;
		while (end >= 0 && str.charAt(end) == '0') {
			end--;
		}
		str = str.substring(start, end + 1);
		return str.charAt(str.length() - 1) == '.' ? str.substring(start, str.length() - 1) : str;
	}

	String verifyNumeric(String _value) {
		boolean decimal = false;
		for (int i = 0; i != _value.length(); i++) {
			if (_value.charAt(i) < '0' || _value.charAt(i) > '9') {
				if (_value.charAt(i) == '.') {
					if (decimal) {
						return "NaN";
					} else {
						decimal = true;
					}
				} else if (_value.charAt(i) == '-' || _value.charAt(i) == '+') {
					if (i != 0) {
						return "NaN";
					}
				} else {
					return "NaN";
				}
			}
		}
		return _value;
	}

	String getImplicitValue(GenericVar value) {
		switch (value.getType()) {
			case number: return value.get();
			case bool: return value.get().equals("true") ? "1" : "0";
			case object: return "0";
			case string: return verifyNumeric(value.get());
			case undefined: return "NaN";
			default: return value.get();
		}
	}

	ArithmeticOperator(String symbol, int priority) {
		super(symbol, priority, OperatorType.between);
		this.symbol = symbol;
	}

	ArithmeticOperator(String symbol, int priority, OperatorType type) {
		super(symbol, priority, type);
		this.symbol = symbol;
	}

	abstract public String applyOperation(String before, String after);

	public GenericVar compute(GenericVar param1, GenericVar param2) throws InterpreterException {
		if (symbol.equals("+") && (param1.getType() == Type.string || param2.getType() == Type.string)) {
			return new GenericVar(Type.string, param1.get() + param2.get());
		}
		String computedValue1 = getImplicitValue(param1);
		String computedValue2 = getImplicitValue(param2);
		if (computedValue1.equals("NaN") || computedValue2.equals("NaN")) {
			return new GenericVar(Type.number, "NaN");
		}
		return new GenericVar(Type.number, trim(applyOperation(computedValue1, computedValue2)));
	}
}

class AssignmentArithmeticOperator extends ArithmeticOperator {
	AssignmentArithmeticOperator(String symbol, int priority) {
		super(symbol, priority);
	}

	AssignmentArithmeticOperator(String symbol, int priority, OperatorType type) {
		super(symbol, priority, type);
	}

	public String applyOperation(String before, String after) {
		throw new Error("applyOperation not initialized!");
	}

	public String applyOperation(String fix) {
		throw new Error("applyOperation not initialized!");
	}

	public GenericVar set(GenericVar varName, GenericVar value) throws InterpreterException {
		return Interpreter.setVariable(varName.getVariableName(), value);
	}

	public GenericVar compute(GenericVar varName, GenericVar value) throws InterpreterException {
		if (symbol.equals("+=") && (varName.getType() == Type.string || value.getType() == Type.string)) {
			return set(varName, new GenericVar(Type.string, varName.get() + value.get(), varName.getVariableName()));
		}
		String computedValue1 = getImplicitValue(varName);
		String computedValue2 = getImplicitValue(value);
		if (computedValue1.equals("NaN") || computedValue2.equals("NaN")) {
			return set(varName, new GenericVar(Type.number, "NaN", varName.getVariableName()));
		}
		return set(varName, new GenericVar(Type.number, trim(applyOperation(computedValue1, computedValue2)), varName.getVariableName()));
	}

	public GenericVar compute(GenericVar side) throws InterpreterException {
		String computedValue = getImplicitValue(side);
		if (computedValue.equals("NaN")) {
			return set(side, new GenericVar(Type.number, "NaN", side.getVariableName()));
		}
		return set(side, new GenericVar(Type.number, trim(applyOperation(computedValue)), side.getVariableName()));
	}
}

abstract class UnaryOperator extends Operator {
	UnaryOperator(String symbol, int priority) {
		super(symbol, priority, OperatorType.before);
	}

	static String getExplicitType(GenericVar value) {
		switch (value.getType()) {
			case string: {
				String _value = value.get();
				boolean decimal = false;
				for (int i = 0; i != _value.length(); i++) {
					if (_value.charAt(i) < '0' && _value.charAt(i) > '9') {
						if (_value.charAt(i) == '.') {
							if (decimal) {
								return "NaN";
							} else {
								decimal = true;
							}
						} else if (_value.charAt(i) == '-' || _value.charAt(i) == '+') {
							if (i != 0) {
								return "NaN";
							}
						} else {
							return "NaN";
						}
					}
				}
				return _value;
			}
			case number: return value.get();
			case bool: return value.get().equals("true") ? "1" : "0";
			case object: return "0";
			case undefined: return "NaN";
			default: return value.get();
		}
	}

	String trim(String str) {
		int start = 0;
		while (start < str.length() - 1 && str.charAt(start) == '0' && str.charAt(start + 1) != '.') {
			start++;
		}
		int end = str.length() - 1;
		while (end >= 0 && str.charAt(end) == '0') {
			end--;
		}
		str = str.substring(start, end + 1);
		return str.charAt(str.length() - 1) == '.' ? str.substring(start, str.length() - 1) : str;
	}

	abstract public GenericVar compute(GenericVar after) throws InterpreterException;
}

class LogicalOperator extends ArithmeticOperator {
	LogicalOperator(String symbol, int priority) {
		super(symbol, priority);
	}

	LogicalOperator(String symbol, int priority, OperatorType op) {
		super(symbol, priority, op);
	}

	String getExplicitType(GenericVar value) {
		return UnaryOperator.getExplicitType(value);
	}

	public String applyOperation(String before, String after) {
		throw new Error("Not used");
	}

	boolean equalsStrict(GenericVar value1, GenericVar value2) {
		if (value1.getType() != value2.getType()) {
			return false;
		}
		switch (value1.getType()) {
			case number: return Double.parseDouble(value1.get()) == Double.parseDouble(value2.get());
			default: return value1.get().equals(value2.get());
		}
	}

	boolean equals(GenericVar value1, GenericVar value2) {
		if ((value1.getType() == Type.number && value2.getType() == Type.string) || (value1.getType() == Type.string && value2.getType() == Type.number)) {
			String str1 = verifyNumeric(value1.get());
			String str2 = verifyNumeric(value2.get());
			return !(str1.equals("NaN") || str2.equals("NaN")) && Double.parseDouble(str1) == Double.parseDouble(str2);
		}
		boolean undef1 = value1.getType() == Type.object && value1.get().equals("null") || value1.getType() == Type.undefined;
		boolean undef2 = value2.getType() == Type.object && value2.get().equals("null") || value2.getType() == Type.undefined;
		if (undef1 != undef2) {
			return false;
		} else if (undef1) {
			return true;
		}
		if ((value1.getType() == Type.number && verifyNumeric(value1.get()).equals("NaN")) || (value2.getType() == Type.number && verifyNumeric(value2.get()).equals("NaN"))) {
			return false;
		}
		if (value1.getType() == value2.getType()) {
			return equalsStrict(value1, value2);
		}
		switch (value1.getType()) {
			case string: {
				switch (value2.getType()) {
					case bool: {
						return (value1.get().length() == 0) == (value2.get().equals("false"));
					}
					case object: {
						return false;
					}
				}
				break;
			}
			case number: {
				switch (value2.getType()) {
					case bool: {
						double value = Double.parseDouble(value1.get());
						return value == 0 ? (value2.get().equals("false")) : (value == 1 && value2.get().equals("true"));
					}
					case object: {
						return false;
					}
				}
				break;
			}
			case bool: {
				switch (value2.getType()) {
					case string: {
						return (value2.get().length() == 0) == (value1.get().equals("false"));
					}
					case number: {
						double value = Double.parseDouble(value2.get());
						return value == 0 ? (value1.get().equals("false")) : (value == 1 && value1.get().equals("true"));
					}
					case object: {
						return false;
					}
				}
				break;
			}
			case object: {
				switch (value2.getType()) {
					case string: {
						return false;
					}
					case number: {
						return false;
					}
					case bool: {
						return false;
					}
				}
				break;
			}
		}
		return false;
	}

	boolean toBoolean(Type type, String value) {
		switch(type) {
			case string: return value.length() != 0;
			case number: return Double.parseDouble(value) != 0;
			case bool: return value.equals("true");
			case object: return false;
			case undefined: return false;
		}
		return false;
	}

	public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
		throw new Error("LogicalOperator Not Initialized!");
	}

	public GenericVar compute(GenericVar value) throws InterpreterException {
		throw new Error("LogicalOperator Not Initialized!");
	}
}

class Sequence extends GenericVar {
	private ArrayList<GenericVar> mSequence = new ArrayList<>();

	private Sequence(GenericVar var) {
		super(Type.sequence, "");
		mSequence.add(var);
	}

	void add(GenericVar var) {
		System.out.println("Adding: " + var);
		mSequence.add(var);
	}

	static Sequence toSequence(GenericVar var) {
		if (var.getType() == Type.sequence) {
			return (Sequence) var;
		} else {
			return new Sequence(var);
		}
	}

	static GenericVar toArray(Sequence seq) throws InterpreterException {
		GenericVar array = new GenericVar(Type.object, seq.get());
		for (int i = 0; i != seq.mSequence.size(); i++) {
			array.addProperty(new GenericVar(Type.number, String.valueOf(i)), seq.mSequence.get(i));
		}
		return array;
	}

	public String toString() {
		return mSequence.toString();
	}
}
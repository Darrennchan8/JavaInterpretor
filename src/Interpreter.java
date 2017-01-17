import java.util.ArrayList;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;


enum Type {
	string,
	number,
	bool,
	undefined,
	object,
	tuple,
	sequence
}

enum OperatorType {
	before,
	between,
	after,
	wrapper
}


class InterpreterException extends Exception {
	private static final long serialVersionUID = 0;
	InterpreterException(String msg) {
		super(msg);
	}
}


class GenericVar {
	private Type mType;
	private String mValue;
	private String mVariableName = null;
	private GenericVar[] mTupleVars;

	GenericVar(Type type, String value, String variableName) {
		this.mType = type;
		this.mValue = value;
		this.mVariableName = variableName;
	}

	GenericVar(Type type, String value) {
		this.mType = type;
		this.mValue = value;
	}

	GenericVar(GenericVar var1, GenericVar var2) {
		this.mType = Type.tuple;
		this.mTupleVars = new GenericVar[] {var1, var2};
	}

	Type getType() {
		return mType;
	}

	String get() {
		if (mType == Type.tuple) {
			throw new Error("Unable to directly access tuple.");
		}
		return mValue;
	}

	GenericVar getFirst() {
		if (mType != Type.tuple) {
			throw new Error("Not a tuple.");
		}
		return mTupleVars[0];
	}

	GenericVar getSecond() {
		if (mType != Type.tuple) {
			throw new Error("Not a tuple.");
		}
		return mTupleVars[1];
	}

	String getVariableName() {
		if (mType == Type.tuple) {
			throw new Error("Unable to access variable name of tuple.");
		}
		return mVariableName;
	}

	void setVariableName(String var) {
		if (mType == Type.tuple) {
			throw new Error("Unable to access variable name of tuple.");
		}
		mVariableName = var;
	}

	public String toString() {
		switch(this.mType) {
			case string: return '"' + mValue + '"';
			case object: return '{' + mValue + '}';
			case tuple: return mTupleVars[0] + " : " + mTupleVars[1];
			default: return mValue;
		}
	}
}


class Operator {
	private String symbol;
	private String symbol2;
	private int priority;
	private OperatorType operatorType;
	private boolean mBreakOnSemicolon = true;

	String getSymbol() {
		return symbol;
	}

	String getSecondSymbol() {
		if (operatorType != OperatorType.wrapper) {
			throw new Error("OperatorType is " + operatorType + ", not wrapper.");
		}
		return symbol2;
	}

	int getPriority() {
		return priority;
	}

	OperatorType getOperatorType() {
		return operatorType;
	}

	protected boolean supportsTuple() {
		return false;
	}

	protected boolean supportsSequence() {
		return false;
	}

	boolean breakOnSemicolon() {
		return mBreakOnSemicolon;
	}

	protected boolean breakOnNewline() {
		return false;
	}

	Operator(String symbol, int priority, OperatorType type) {
		this.symbol = symbol;
		this.priority = priority;
		this.operatorType = type;
	}

	Operator(String openSymbol, String closeSymbol, int priority) {
		this.symbol = openSymbol;
		this.symbol2 = closeSymbol;
		this.priority = priority;
		this.operatorType = OperatorType.wrapper;
		mBreakOnSemicolon = false;
	}

	public GenericVar compute(GenericVar param1, GenericVar param2) throws InterpreterException {
		throw new Error("Method compute(GenericVar, GenericVar) Not initialized");
	}

	public GenericVar compute(GenericVar param1) throws InterpreterException {
		throw new Error("Method compute(GenericVar) Not initialized");
	}

	public GenericVar compute(String param1) throws InterpreterException {
		throw new Error("Method compute(String) Not initialized");
	}

	public String toString() {
		return "Bound_Operator[" + symbol + "]";
	}
}


class OperatorStore {
	private ArrayList<Operator> operators = new ArrayList<>();

	ArrayList<Operator> get(String str, int start) {
		ArrayList<Operator> qualified = new ArrayList<>();
		int max = 0;
		for (Operator o : operators) {
			String symbol = o.getSymbol();
			if (symbol.length() >= max && symbol.length() + start <= str.length() && symbol.equals(str.substring(start, start + symbol.length()))) {
				max = symbol.length();
				qualified.add(o);
			}
		}
		for (int i = 0; i != qualified.size(); i++) {
			/* Longer match takes priority over the priority attribute */
			if (qualified.get(i).getSymbol().length() < max) {
				qualified.remove(i--);
			}
		}
		return qualified;
	}

	void add(Operator operator) {
		operators.add(operator);
	}
}


class CommandChain {
	private ArrayList<Object> chain = new ArrayList<>();
	private ArrayList<Boolean> operator = new ArrayList<>();

	void add(GenericVar var) {
		chain.add(var);
		operator.add(false);
	}

	void add(ArrayList<Operator> o) {
		chain.add(o);
		operator.add(true);
	}

	private SimpleEntry<Integer, Integer> getMaxOpPos() throws InterpreterException {
		int maxPrecedence = 0;
		int maxIndex = 0;
		int iii = 0;
		boolean set = false;
		for (int i = 0; i != operator.size(); i++) {
			if (operator.get(i)) {
				ArrayList qualified = (ArrayList) chain.get(i);
				for (int ii = 0; ii != qualified.size(); ii++) {
					Operator op = (Operator) qualified.get(ii);
					if (op.getPriority() > maxPrecedence) {
						if (op.getOperatorType() == OperatorType.before && (i < 1 || operator.get(i - 1)) && i < operator.size() - 1 && !operator.get(i + 1) ||
								op.getOperatorType() == OperatorType.between && i > 0 && i < operator.size() - 1 && !operator.get(i + 1) && !operator.get(i - 1) ||
								op.getOperatorType() == OperatorType.after && (i > operator.size() - 2 || operator.get(i + 1)) && i > 0 && !operator.get(i - 1)) {
							maxPrecedence = op.getPriority();
							maxIndex = i;
							iii = ii;
							set = true;
						}
					}
				}
			}
		}
		if (!set) {
			throw new InterpreterException("Unexpected Operator");
		}
		return new SimpleEntry<>(maxIndex, iii);
	}

	private boolean operators() {
		for (Boolean i : operator) {
			if (i) {
				return true;
			}
		}
		return false;
	}

	GenericVar evaluate() throws InterpreterException {
		while (operators()) {
			SimpleEntry<Integer, Integer> details = getMaxOpPos();
			int i = details.getKey();
			int ii = details.getValue();
			Operator op = (Operator) ((ArrayList) chain.get(i)).get(ii);
			GenericVar value;
			switch (op.getOperatorType()) {
				case before: {
					GenericVar after = (GenericVar) chain.get(i + 1);
					if (after.getType() == Type.tuple && !op.supportsTuple()) {
						throw new InterpreterException("Unexpected colon");
					}
					if (after.getType() == Type.sequence && !op.supportsSequence()) {
						throw new InterpreterException("Unexpected sequence of values");
					}
					value = op.compute(after);
					chain.set(i, value);
					chain.remove(i + 1);
					operator.remove(i);
					break;
				}
				case between: {
					GenericVar before = (GenericVar) chain.get(i - 1);
					chain.remove(--i);
					operator.remove(i);
					GenericVar after = (GenericVar)chain.get(i + 1);
					chain.remove(i + 1);
					operator.remove(i);
					if ((before.getType() == Type.tuple || after.getType() == Type.tuple) && !op.supportsTuple()) {
						throw new InterpreterException("Unexpected colon");
					}
					if ((before.getType() == Type.sequence || after.getType() == Type.sequence) && !op.supportsSequence()) {
						throw new InterpreterException("Unexpected sequence of values");
					}
					value = op.compute(before, after);
					chain.set(i, value);
					break;
				}
				case after: {
					GenericVar before = (GenericVar) chain.get(i - 1);
					if (before.getType() == Type.tuple && !op.supportsTuple()) {
						throw new InterpreterException("Unexpected colon");
					}
					if (before.getType() == Type.sequence && !op.supportsSequence()) {
						throw new InterpreterException("Unexpected sequence of values");
					}
					value = op.compute(before);
					chain.set(i, value);
					chain.remove(i - 1);
					operator.remove(i);
					break;
				}
				default: {
					throw new Error("Unhandled operator type: " + op.getOperatorType());
				}
			}
		}
		if (chain.size() == 0) {
			return new GenericVar(Type.undefined, "undefined");
		} else if (chain.size() == 1) {
			return (GenericVar)chain.get(0);
		} else {
			throw new InterpreterException("Unexpected value");
		}
	}

	int size() {
		return chain.size();
	}
}


class Interpreter {
	private static HashMap<String, GenericVar> variables = new HashMap<>();
	private OperatorStore operators = new OperatorStore();

	private static GenericVar getVariable(String varName) throws InterpreterException {
		GenericVar entry = variables.get(varName);
		if (entry == null) {
			return new GenericVar(Type.undefined, "undefined", varName);
		} else {
			return entry;
		}
	}

	static GenericVar setVariable(String varName, GenericVar value) throws InterpreterException {
		variables.put(varName, value);
		value.setVariableName(varName);
		return getVariable(varName);
	}

	private GenericVar getValue(String value) throws InterpreterException {
		Type type = null;
		String computedValue = null;
		if (value.length() > 1) {
			switch (value) {
				case "undefined": {
					type = Type.undefined;
					computedValue = value;
					break;
				}
				case "false":
				case "true": {
					type = Type.bool;
					computedValue = value;
					break;
				}
				case "null": {
					type = Type.object;
					computedValue = value;
					break;
				}
			}
		}
		if (type == null) {
			boolean number = true;
			boolean decimal = false;
			for (int i = 0; i != value.length(); i++) {
				if (value.charAt(i) < '0' || value.charAt(i) > '9') {
					if (value.charAt(i) == '.') {
						if (decimal) {
							number = false;
							break;
						} else {
							decimal = true;
						}
					} else {
						number = false;
						break;
					}
				}
			}
			if (number) {
				type = Type.number;
				computedValue = value;
			}
		}
		if (type == null) {
			return getVariable(value);
		} else {
			return new GenericVar(type, computedValue);
		}
	}

	private GenericVar evaluate(String command) throws InterpreterException {
		GenericVar lastAnswer = new GenericVar(Type.undefined, "undefined");
		int KCommandLength = command.length();
		for (int index = 0; index < KCommandLength; index++) {
			CommandChain commandChain = new CommandChain();
			int rangeSelectorBefore = index; /* Used for both wrapper operators and plain-text tokens IE: null, undefined, 123.7, ... */
			boolean rangeOpen = false;
			Operator wrapper = null;
			int finalValue = KCommandLength;
			ArrayList<Operator> previousSupported = null;
			ArrayList<Operator> supported = null;
			for (int i = index; i < KCommandLength; i++) {
				boolean newLine = command.charAt(i) == '\n';
				boolean semicolon = command.charAt(i) == ';';
				if (wrapper == null) {
					boolean zeroLength = command.substring(rangeSelectorBefore, i).trim().length() == 0;
					if (!zeroLength) {
						previousSupported = supported;
					}
					supported = operators.get(command, i);
					int KSupportedLength = supported.size();
					if (KSupportedLength == 0) {
						if (!rangeOpen || zeroLength) {
							if (previousSupported != null) {
								for (int ii = 0, iii = previousSupported.size(); ii != iii; ii++) {
									if (newLine || semicolon) {
										newLine = newLine && previousSupported.get(ii).breakOnNewline();
										semicolon = semicolon && previousSupported.get(ii).breakOnSemicolon();
									} else {
										break;
									}
								}
							}
						}
						if (!rangeOpen) {
							rangeOpen = true;
							rangeSelectorBefore = i;
						}
					} else {
						for (int ii = 0; ii != KSupportedLength; ii++) {
							if (newLine || semicolon) {
								newLine = newLine && supported.get(ii).breakOnNewline();
								semicolon = semicolon && supported.get(ii).breakOnSemicolon();
							} else {
								break;
							}
						}
						if (rangeOpen) {
							rangeOpen = false;
							String str = command.substring(rangeSelectorBefore, i).trim();
							if (str.length() != 0) {
								commandChain.add(getValue(str));
							}
						}
						int maxOpLength = 1;
						for (int ii = 0; ii != KSupportedLength; ii++) {
							Operator currOperator = supported.get(ii);
							int symbolLength = currOperator.getSymbol().length();
							if (symbolLength > maxOpLength) {
								maxOpLength = symbolLength;
							}
							if (currOperator.getOperatorType() == OperatorType.wrapper) {
								wrapper = currOperator;
								rangeSelectorBefore = i + symbolLength;
								break;
							}
						}
						if (wrapper == null) {
							commandChain.add(supported);
							i += maxOpLength - 1;
						}
					}
				} else {
					if (newLine || semicolon) {
						newLine = newLine && wrapper.breakOnNewline();
						semicolon = semicolon && wrapper.breakOnSemicolon();
					}
					String secondSymbol = wrapper.getSecondSymbol();
					int KLength = secondSymbol.length();
					if (command.substring(i, i + KLength).equals(secondSymbol)) {
						commandChain.add(wrapper.compute(command.substring(rangeSelectorBefore, i)));
						i += KLength - 1;
						wrapper = null;
						previousSupported = null;
						supported = null;
					}
				}
				if (newLine || semicolon) {
					index = i;
					finalValue = i;
					break;
				}
			}
			if (wrapper == null) {
				if (rangeOpen) {
					String str = command.substring(rangeSelectorBefore, finalValue).trim();
					if (str.length() != 0) {
						commandChain.add(getValue(str));
					}
				}
			} else {
				throw new InterpreterException("Unclosed token");
			}
			index = finalValue;
			if (commandChain.size() != 0) {
				lastAnswer = commandChain.evaluate();
			}
		}
		return lastAnswer;
	}

	private GenericVar run(String code) throws InterpreterException {
		/* Structural Wrappers (braces, parentheses) should come into play here. */
		return evaluate(code);
	}

	GenericVar interpret(String code) {
		try {
			return run(code);
		} catch (InterpreterException err) {
			return new GenericVar(Type.string, err.getMessage() + "\nProgram Terminated.");
		}
	}

	Interpreter() {
		CustomOperators.apply(operators);
		variables.put("exit", new GenericVar(Type.string, "exit", "exit"));
	}
}

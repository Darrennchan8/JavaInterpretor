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
    private String mUnparsedCode = null;
    private GenericVar[] mTupleVars;

    GenericVar(Type type, String value, String variableName) {
        this.mType = type;
        this.mValue = value;
        this.mUnparsedCode = variableName;
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

    String getUnformattedString() {
        if (mType == Type.tuple) {
            throw new Error("Unable to access unformatted string of tuple.");
        }
        return mUnparsedCode;
    }

    void setUnformattedString(String var) {
        if (mType == Type.tuple) {
            throw new Error("Unable to access unformatted string of tuple.");
        }
        mUnparsedCode = var;
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
    protected boolean mSupportsTuple = false;
    protected boolean mmSupportsSequence = false;
    protected boolean mBreakOnSemicolon = true;
    protected boolean mBreakOnNewline = false;

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

    boolean supportsTuple() {
        return mSupportsTuple;
    }

    boolean mSupportsSequence() {
        return mmSupportsSequence;
    }

    boolean breakOnSemicolon() {
        return mBreakOnSemicolon;
    }

    boolean breakOnNewline() {
        return mBreakOnNewline;
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

    public void add() {
    }

    private SimpleEntry<Integer, Integer> getMaxOpPos() throws InterpreterException {
        int maxPrecedence = 0;
        int maxIndex = 0;
        int iii = 0;
        boolean set = false;
        for (int i = 0; i != operator.size(); i++) {
            if (operator.get(i)) {
                ArrayList<Operator> qualified = (ArrayList<Operator>) chain.get(i);
                for (int ii = 0; ii != qualified.size(); ii++) {
                    Operator op = qualified.get(ii);
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
                    if (!(after.getType() == Type.tuple) || op.supportsTuple()) {
                        value = op.compute(after);
                        chain.set(i, value);
                        chain.remove(i + 1);
                        operator.remove(i);
                        break;
                    }
                    throw new InterpreterException("Unexpected colon");
                }
                case between: {
                    GenericVar before = (GenericVar) chain.get(i - 1);
                    chain.remove(--i);
                    operator.remove(i);
                    GenericVar after = (GenericVar)chain.get(i + 1);
                    chain.remove(i + 1);
                    operator.remove(i);
                    if (!(before.getType() == Type.tuple) || !(after.getType() == Type.tuple) || op.supportsTuple()) {
                        value = op.compute(before, after);
                        chain.set(i, value);
                        break;
                    }
                    throw new InterpreterException("Unexpected colon");
                }
                case after: {
                    GenericVar before = (GenericVar) chain.get(i - 1);
                    if (!(before.getType() == Type.tuple) || op.supportsTuple()) {
                        value = op.compute(before);
                        chain.set(i, value);
                        chain.remove(i - 1);
                        operator.remove(i);
                        break;
                    }
                    throw new InterpreterException("Unexpected colon");
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
        value.setUnformattedString(varName);
        return getVariable(varName);
    }

    private static String toObject(String objInternals) {
        return objInternals;
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
        operators.add(new Operator("'", "'", 14) {
            protected boolean mBreakOnNewline = true;

            public GenericVar compute(String str) {
                return new GenericVar(Type.string, str);
            }
        });
        operators.add(new Operator("\"", "\"", 14) {
            protected boolean mBreakOnNewline = true;

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
            protected boolean mSupportsSequence = true;

            public GenericVar compute(String str) {
                return new Interpreter().interpret(str);
            }
        });
        operators.add(new Operator("{", "}", 14) {
            protected boolean supportsTuple = true;
            protected boolean mSupportsSequence = true;

            public GenericVar compute(String str) {
                return new Interpreter().interpret(str);
            }
        });
        operators.add(new Operator("[", "]", 14) {
            protected boolean mSupportsSequence = true;

            public GenericVar compute(String str) {
                return Sequence.toSequence(new Interpreter().interpret(str));
            }
        });
        operators.add(new AssignmentArithmeticOperator("++", 13, OperatorType.after) {
            public GenericVar set(GenericVar varName, GenericVar value) throws InterpreterException {
                Interpreter.setVariable(varName.getUnformattedString(), value);
                return varName;
            }

            public String applyOperation(String fix) {
                return String.valueOf(Double.parseDouble(fix) + 1);
            }
        });
        operators.add(new AssignmentArithmeticOperator("--", 13, OperatorType.after) {
            public GenericVar set(GenericVar varName, GenericVar value) throws InterpreterException {
                Interpreter.setVariable(varName.getUnformattedString(), value);
                return varName;
            }

            public String applyOperation(String fix) {
                return String.valueOf(Double.parseDouble(fix) - 1);
            }
        });
        operators.add(new LogicalOperator("!", 12, OperatorType.before) {
            public GenericVar compute(GenericVar value) throws InterpreterException {
                return new GenericVar(Type.bool, boolize(value.getType(), value.get()) ? "false" : "true", value.getUnformattedString());
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
                return new GenericVar(Type.bool, boolize(value1.getType(), value1.get()) && boolize(value2.getType(), value2.get()) ? "true" : "false");
            }
        });
        operators.add(new LogicalOperator("||", 5) {
            public GenericVar compute(GenericVar value1, GenericVar value2) throws InterpreterException {
                return new GenericVar(Type.bool, boolize(value1.getType(), value1.get()) || boolize(value2.getType(), value2.get()) ? "true" : "false");
            }
        });
        operators.add(new Operator(":", 4, OperatorType.between) {
            public GenericVar compute(GenericVar result1, GenericVar result2) throws InterpreterException {
                return new GenericVar(result1, result2);
            }
        });
        operators.add(new LogicalOperator("?", 3) {
            protected boolean supportsTuple = true;

            public GenericVar compute(GenericVar condition, GenericVar results) throws InterpreterException {
                if (results.getType() != Type.tuple) {
                    throw new Error("Incomplete Ternary Operator");
                }
                return boolize(condition.getType(), condition.get()) ? results.getFirst() : results.getSecond();
            }
        });
        operators.add(new Operator("=", 2, OperatorType.between) {
            public GenericVar compute(GenericVar varName, GenericVar value) throws InterpreterException {
                return setVariable(varName.getUnformattedString(), value);
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
            public GenericVar compute(GenericVar previous, GenericVar next) throws InterpreterException {
                Sequence currSequence = Sequence.toSequence(previous);
                currSequence.add(next);
                return currSequence;
            }
        });
        variables.put("exit", new GenericVar(Type.string, "exit", "exit"));
    }
}

abstract class ArithmeticOperator extends Operator {
    String symbol;

    String trim(String str) {
        int start = 0;
        for (; start < str.length() - 1 && str.charAt(start) == '0' && str.charAt(start + 1) != '.'; start++) {
        }
        int end = str.length() - 1;
        for (; end >= 0 && str.charAt(end) == '0'; end--) {
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
        return Interpreter.setVariable(varName.getUnformattedString(), value);
    };

    public GenericVar compute(GenericVar varName, GenericVar value) throws InterpreterException {
        if (symbol.equals("+=") && (varName.getType() == Type.string || value.getType() == Type.string)) {
            return set(varName, new GenericVar(Type.string, varName.get() + value.get(), varName.getUnformattedString()));
        }
        String computedValue1 = getImplicitValue(varName);
        String computedValue2 = getImplicitValue(value);
        if (computedValue1.equals("NaN") || computedValue2.equals("NaN")) {
            return set(varName, new GenericVar(Type.number, "NaN", varName.getUnformattedString()));
        }
        return set(varName, new GenericVar(Type.number, trim(applyOperation(computedValue1, computedValue2)), varName.getUnformattedString()));
    }

    public GenericVar compute(GenericVar side) throws InterpreterException {
        String computedValue = getImplicitValue(side);
        if (computedValue.equals("NaN")) {
            return set(side, new GenericVar(Type.number, "NaN", side.getUnformattedString()));
        }
        return set(side, new GenericVar(Type.number, trim(applyOperation(computedValue)), side.getUnformattedString()));
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
        for (; start < str.length() - 1 && str.charAt(start) == '0' && str.charAt(start + 1) != '.'; start++) {
        }
        int end = str.length() - 1;
        for (; end >= 0 && str.charAt(end) == '0'; end--) {
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

    boolean boolize(Type type, String value) {
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
    private ArrayList<GenericVar> sequence = new ArrayList<>();

    Sequence(GenericVar var) {
        super(Type.sequence, "");
        sequence.add(var);
    }

    void add(GenericVar var) {
        sequence.add(var);
    }

    static Sequence toSequence(GenericVar var) {
        if (var.getType() == Type.sequence) {
            return (Sequence) var;
        } else {
            return new Sequence(var);
        }
    }

    public String toString() {
        return sequence.toString();
    }
}

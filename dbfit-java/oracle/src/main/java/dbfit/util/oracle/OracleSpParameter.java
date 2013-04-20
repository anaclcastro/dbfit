package dbfit.util.oracle;

import dbfit.util.DbParameterAccessor;
import static dbfit.util.DbParameterAccessor.INPUT;
import static dbfit.util.DbParameterAccessor.OUTPUT;
import static dbfit.util.DbParameterAccessor.INPUT_OUTPUT;
import static dbfit.util.DbParameterAccessor.RETURN_VALUE;

public class OracleSpParameter {
    protected int direction; // In terms of DbParameterAccessor constants
    protected SpGeneratorOutput out = null;
    protected String dataType; // original type name in the original sp
    protected String id; // id to be used for generating param/arg names
    protected String prefix; // prefix to be used for all names to avoid conflicts

    public static OracleSpParameter newInstance(String paramName, int direction,
                            String dataType) {
        return newInstance(paramName, direction, dataType, "x");
    }

    public static OracleSpParameter newInstance(String paramName, int direction,
                            String dataType, String prefix) {
        return new OracleSpParameter(paramName, direction, dataType, prefix);
    }

    protected OracleSpParameter(String paramName, int direction, String dataType,
                                String prefix) {
        this.direction = direction;
        this.dataType = dataType;
        this.id = paramName;
        this.prefix = prefix;
    }

    protected int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }
    
    public boolean isReturnValue() {
        return getDirection() == RETURN_VALUE;
    }

    public boolean isOutputOrReturnValue() {
        switch (getDirection()) {
            case RETURN_VALUE:
            case OUTPUT:
            case INPUT_OUTPUT:
                return true;
            default:
                return false;
        }
    }

    public boolean isInOrInout() {
        switch (getDirection()) {
            case INPUT:
            case INPUT_OUTPUT:
                return true;
        }

        return false;
    }

    public boolean isOutOrInout() {
        switch (getDirection()) {
            case OUTPUT:
            case INPUT_OUTPUT:
                return true;
        }

        return false;
    }

    private boolean isInput() {
        return getDirection() == INPUT;
    }

    public boolean isBooleanInOrInout() {
        return isBoolean() && isInOrInout();
    }

    public boolean isBooleanOutOrInout() {
        return isBoolean() && isOutOrInout();
    }

    protected String getDataType() {
        return dataType;
    }

    private boolean needsArgumentTypeChange() {
        return isBooleanOutOrInout();
    }

    private String getWrapperArgumentType() {
        return needsArgumentTypeChange() ? "VARCHAR2" : getDataType();
    }

    public boolean isBoolean() {
        return getDataType().equals("BOOLEAN");
    }

    public String getDirectionName() {
        switch (getDirection()) {
            case INPUT_OUTPUT:
                return "IN OUT";
            case OUTPUT:
                return "OUT";
            case INPUT:
                return "IN";
            default:
                return "RETURN";
        }
    }

    public String getShortDirectionName() {
        switch (getDirection()) {
            case INPUT_OUTPUT:
                return "inout";
            case OUTPUT:
                return "out";
            case INPUT:
                return "in";
            default:
                return "ret";
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOutput(SpGeneratorOutput out) {
        this.out = out;
    }

    protected SpGeneratorOutput append(String s) {
        if (out != null) {
            out.append(s);
        }

        return out;
    }

    private String getWrapperArgumentName() {
        return isReturnValue() ? "" : prefixed(id);
    }

    public String getWrapperVarName() {
        String varid = isReturnValue() ? "" : id + "_";
        return prefixed("v_" + varid + getShortDirectionName());
    }

    public String toString() {
        if (out == null) {
            return "";
        } else {
            return out.toString();
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    private void declareArgumentOrReturnValue() {
        out.append(getWrapperArgumentName())
            .append(" ")
            .append(getDirectionName())
            .append(" ").append(getWrapperArgumentType());
    }

    public void declareArgument() {
        declareArgumentOrReturnValue();
    }

    public void declareReturnValue() {
        declareArgumentOrReturnValue();
    }

    private void initializeVariable() {
        if (getDirection() == INPUT_OUTPUT) {
            out.append(" := ").append(chr2bool(getWrapperArgumentName()));
        }
    }

    public void declareVariable() {
        if (needsArgumentTypeChange() || isReturnValue()) {
            out.append("        ")
                .append(getWrapperVarName())
                .append(" ").append(getDataType());

            initializeVariable();

            out.append(";\n");
        }
    }

    public void assignOutputVariable() {
        if (needsArgumentTypeChange()) {
            out.append("        ")
                .append(getWrapperArgumentName())
                .append(" := ")
                .append(bool2chr(getWrapperVarName()))
                .append(";\n");
        }
    }

    public void genWrapperCallArgument() {
        genWrapperCallArgument("?");
    }

    public void genWrapperCallArgument(String varname) {
        if (isBoolean() && isInput()) {
            out.append(chr2bool(varname));
        } else {
            out.append(varname);
        }
    }

    public void genSpCallArgumentWithinWrapper() {
        if (needsArgumentTypeChange()) {
            out.append(getWrapperVarName());
        } else {
            out.append(getWrapperArgumentName());
        }
    }

    private String prefixed(String expr) {
        return prefix + "_" + expr;
    }

    public static String callExpr(String func, String args) {
        String ws = (args.trim().length() == 0) ? "" : " ";
        return func + "(" + ws + args + ws + ")";
    }

    private String prefixedCallExpr(String func, String args) {
        return callExpr(prefixed(func), args);
    }

    private String chr2bool(String arg) {
        return prefixedCallExpr("chr2bool", arg);
    }

    private String bool2chr(String arg) {
        return prefixedCallExpr("bool2chr", arg);
    }
}


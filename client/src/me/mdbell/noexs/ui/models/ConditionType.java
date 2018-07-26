package me.mdbell.noexs.ui.models;

public enum ConditionType {
    EQUALS("Equals", "=="),
    NOT_EQUAL("Not Equal", "!="),
    LESS_THAN("Less Than", "<"),
    LESS_THAN_OR_EQUAL("Less Than or Equal", "<="),
    GREATER_THAN("Greater Than", ">"),
    GREATER_OR_EQUAL("Greater or Equal", ">=");
    String str, operator;

    ConditionType(String str, String operator) {
        this.str = str;
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public String toString() {
        return str;
    }

}

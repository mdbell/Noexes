package me.mdbell.noexs.ui.models;

import me.mdbell.util.ILocalized;

public enum ConditionType implements ILocalized {
    EQUALS("search.cond_types.equals", "=="),
    NOT_EQUAL("search.cond_types.not_equal", "!="),
    LESS_THAN("search.cond_types.less_than", "<"),
    LESS_THAN_OR_EQUAL("search.cond_types.less_than_or_equal", "<="),
    GREATER_THAN("search.cond_types.greater_than", ">"),
    GREATER_OR_EQUAL("search.cond_types.greater_than_or_equal", ">=");
    String key, operator;

    ConditionType(String key, String operator) {
        this.key = key;
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public String getKey() {
        return key;
    }

}

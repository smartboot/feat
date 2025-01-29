package tech.smartboot.feat.ai.chat;

public class Function {
    private String name;
    private String description;
    private FunctionParameters parameters;

    public Function(String name) {
        this.name = name;
        this.parameters = new FunctionParameters();
    }

    public static Function of(String name) {
        return new Function(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public Function description(String description) {
        this.description = description;
        return this;
    }

    public Function addIntParam(String name, String value) {
        return addIntParam(name, value, true);
    }

    public Function addIntParam(String name, String value, boolean required) {
        return addParam(name, value, ParameterProperty.TYPE_INTEGER, required);
    }


    public Function addDoubleParam(String name, String value, boolean required) {
        return addParam(name, value, ParameterProperty.TYPE_DOUBLE, required);
    }

    public Function addDoubleParam(String name, String value) {
        return addDoubleParam(name, value, true);
    }

    public Function addStringParam(String name, String value, boolean required) {
        return addParam(name, value, ParameterProperty.TYPE_STRING, required);
    }

    public Function addStringParam(String name, String value) {
        return addStringParam(name, value, true);
    }

    public Function addParam(String name, String description, String type, boolean required) {
        parameters.addParameter(name, description, type, required);
        return this;
    }

    public FunctionParameters getParameters() {
        return parameters;
    }

    public void setParameters(FunctionParameters parameters) {
        this.parameters = parameters;
    }
}

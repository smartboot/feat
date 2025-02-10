package tech.smartboot.feat.ai.chat.entity;

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

    public Function addIntParam(String name, String description) {
        return addIntParam(name, description, true);
    }

    public Function addIntParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_INTEGER, required);
    }


    public Function addDoubleParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_DOUBLE, required);
    }

    public Function addDoubleParam(String name, String description) {
        return addDoubleParam(name, description, true);
    }

    public Function addStringParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_STRING, required);
    }

    public Function addStringParam(String name, String description) {
        return addStringParam(name, description, true);
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

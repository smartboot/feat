/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.mcp.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 6/28/25
 */
public class Prompt {
    private String name;
    private String title;
    private String description;
    private final List<Argument> arguments = new ArrayList<>();


    protected Prompt(String name) {
        this.name = name;
    }

    public static Prompt of(String name) {
        return new Prompt(name);
    }

    public static Argument argument(String name, String description) {
        return new Argument(name, description, false);
    }

    public static Argument requiredArgument(String name, String description) {
        return new Argument(name, description, true);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public Prompt title(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Prompt description(String description) {
        this.description = description;
        return this;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public Prompt arguments(Argument... arguments) {
        if (arguments == null) {
            return this;
        }
        this.arguments.addAll(Arrays.asList(arguments));
        return this;
    }


    public static class Argument {
        private String name;
        private String description;
        private boolean required;

        private Argument(String name, String description, boolean required) {
            this.name = name;
            this.description = description;
            this.required = required;
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

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}

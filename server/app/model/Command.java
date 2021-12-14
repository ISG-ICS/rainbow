package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Command {
    /**
     * load
     *
     * cluster [key] [order]
     *  - key: String
     *  - order: String
     *    - original
     *    - reverse
     *    - spatial
     *    - reverse-spatial
     */
    public String action;
    public String[] arguments;
}

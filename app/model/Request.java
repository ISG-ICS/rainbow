package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {
    /**
     * - query
     *   - Query existing algorithm for given keyword
     *
     * - cmd
     *   - Execute commands
     *
     * - transfer
     *   - Transfer raw data of given keyword in indicated "format"
     *
     * - progress-transfer
     *   - Transfer raw data of given keyword in indicated "format" progressively
     *
     * - analysis
     *   - Query statistics of algorithm for given keyword
     */
    public String type; // "query"/"cmd"/"transfer"/"progress-transfer"/"analysis"
    public String keyword;
    public Query query;
    public Command[] cmds;
    public Analysis analysis;
}

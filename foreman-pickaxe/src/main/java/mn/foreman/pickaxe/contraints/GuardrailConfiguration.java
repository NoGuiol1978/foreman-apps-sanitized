package mn.foreman.pickaxe.contraints;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/** A guardrail configuration. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardrailConfiguration {

    /** The key. */
    public String apiKey;

    /** The client ID. */
    public Integer clientId;

    /** The control. */
    public Boolean control;

    /** The macs. */
    public List<String> macs;

    /** The ranges. */
    public List<String> ranges;

    /** The rate limit configurations. */
    public Map<String, Integer> rateLimits;
}

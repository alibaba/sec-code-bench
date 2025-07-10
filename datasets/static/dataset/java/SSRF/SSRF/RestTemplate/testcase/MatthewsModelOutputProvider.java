<filename>OpenDashboard-legacy-master/src/main/java/od/providers/modeloutput/MatthewsModelOutputProvider.java<fim_prefix>

package od.providers.modeloutput;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import od.framework.model.RiskScore;
import od.providers.ProviderData;
import od.providers.ProviderException;
import od.providers.matthews.MatthewsClient;
import od.providers.matthews.MatthewsProvider;

import org.apereo.lai.ModelOutput;
import org.apereo.lai.impl.ModelOutputImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("modeloutput_matthews")
public class MatthewsModelOutputProvider extends MatthewsProvider implements ModelOutputProvider {

  private static final String KEY = "modeloutput_matthews";
  private static final String BASE = "MATTHEWS_MODELOUTPUT";
  private static final String NAME = String.format("%s_NAME", BASE);
  private static final String DESC = String.format("%s_DESC", BASE);
       
  @PostConstruct
  public void init() {
    providerConfiguration = getDefaultMatthewsConfiguration();
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDesc() {
    return DESC;
  }

  @Override
  public Page<ModelOutput> getModelOutputForContext(ProviderData providerData, String tenantId, String contextId, Pageable page)
      throws ProviderException {
    MatthewsClient mc = new MatthewsClient(providerData.findValueForKey("base_url"), providerData.findValueForKey("key"), providerData.findValueForKey("secret"));
    String endpoint = providerData.findValueForKey("base_url").concat("/api/risks/").concat(contextId).concat("/latest");

    RestTemplate restTemplate = mc.getRestTemplate();
    HttpHeaders headers = mc.getHeaders();
    

    <fim_suffix>

    ResponseEntity<RiskScore[]> response 
      = restTemplate.exchange(endpoint, HttpMethod.GET, new HttpEntity<>(headers), RiskScore[].class);
    
    RiskScore [] scores = response.getBody();
    
    List<ModelOutput> modelResults = new ArrayList<>();
    if (scores != null && scores.length > 0) {
      
      for (RiskScore rs : scores) {
        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("ALTERNATIVE_ID", rs.getUserSourcedId());
        outputParams.put("CLASS_SOURCED_ID", contextId);
        outputParams.put("RISK_SCORE", rs.getScore());
         
        ModelOutputImpl output = new ModelOutputImpl(outputParams,rs.getDateTime() != null ? Date.from(rs.getDateTime().toInstant(ZoneOffset.UTC)) : null);
        output.setUserSourcedId(rs.getUserSourcedId());

        modelResults.add(output);
      }
      
    }
    
    return new PageImpl<ModelOutput>(modelResults);
  }

}
<fim_middle>
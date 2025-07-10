<filename>KDT-AI-1-LectureNotes-main/Spring/JaekyoungKim/demo/src/main/java/kr/eddi/demo/fastapi/basics/service/FastApiServiceImpl.java<fim_prefix>

package kr.eddi.demo.fastapi.basics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
public class FastApiServiceImpl implements FastApiService {
    @Override
    public void stringRequest(String fastapiRequestUrl) {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();

        converters.add(new FormHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(converters);

        String result = restTemplate.getForObject(
                fastapiRequestUrl,
                String.class);

        log.info("result: " + result);
    }

    @Override
    public Integer integerRequest(String fastapiRequestUrl) {
        RestTemplate restTemplate = new RestTemplate();

        <fim_suffix>

        return result;
    }
}
<fim_middle>
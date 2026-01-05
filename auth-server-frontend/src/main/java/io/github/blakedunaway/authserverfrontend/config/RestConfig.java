package io.github.blakedunaway.authserverfrontend.config;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestConfig {

    private RestTemplate createTemplateForUrl(final String serviceUser, final String servicePassword, final String serviceBaseUrl) {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(serviceUser, servicePassword);
        provider.setCredentials(AuthScope.ANY, credentials);

        final HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider)
                                                   .setMaxConnTotal(200).setMaxConnPerRoute(100)
                                                   //Consider enabling?
                                                   //.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(60000).setSocketTimeout(60000).build())
                                                   .build();

        final HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(client);

        final DefaultUriTemplateHandler defaultTemplateHandler = new DefaultUriTemplateHandler();
        defaultTemplateHandler.setBaseUrl(serviceBaseUrl);

        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory);
        restTemplate.setUriTemplateHandler(defaultTemplateHandler);
        return restTemplate;
    }

}


package org.deklanowski.eip.routing.aggregator.internal;

import org.deklanowski.eip.camel.CamelMessagePublisher;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.deklanowski.eip.spi.Transformer;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AggregatingRouterStringToStringTest extends CamelTestSupport
{
    private static final  String DIRECT_START = "direct:start";
    private static final  String MOCK_RESULT = "mock:result";
    public static final String TEST_ROUTE = "test.route";

    @EndpointInject(uri = MOCK_RESULT)
    private MockEndpoint result;

    private Transformer<String,String> transformer = input -> input;


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        ProducerTemplate producer = context.createProducerTemplate();
        producer.setDefaultEndpointUri(MOCK_RESULT);

        return new AggregatingRouter(
                TEST_ROUTE,
                DIRECT_START,
                transformer,
                "${in.header.id} == 'declan'",
                1,
                1000L,
                new CamelMessagePublisher(producer)
        );
    }


    @org.junit.Test
    public void testRoute() throws InterruptedException {
        result.expectedMessageCount(1);
        template.sendBodyAndHeader(DIRECT_START,"<name>test</name>","id","declan");
        result.assertIsSatisfied();
    }

}
